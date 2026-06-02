# BluZ Android — Рефакторинг BLE-слоя (Phase 1)

## Контекст

Проект BluZ — портативный сцинтилляционный гамма-спектрометр с GPS-картированием.
Исходный GitHub: https://github.com/Maniak003/BluZ

Цель сессии: постепенное улучшение архитектуры мобильного Android-приложения
(Kotlin, BLE, Yandex MapKit, ViewPager2, Room DB), не ломая рабочую функциональность.

---

## Что нашли — проблемы исходного кода

### 1. BluetoothInterface.kt — прямая работа с UI внутри BLE-коллбэков

Исходный `BluetoothInterface.kt` (~1163 строки) нарушал принцип единственной ответственности:
- Внутри `onCharacteristicChanged` напрямую обновлялись View-элементы (`GO.txtStat3.text = ...`)
- Внутри `onConnectionStateChange` менялись цвета индикаторов
- BLE-коллбэки вызывались в фоновом потоке, но там шло обращение к UI — потенциальные
  гонки данных и крэши

### 2. Глобальный объект как шина данных

`globalObj.kt` содержал поля `receiveData: UByteArray(9760)` и `sendCS: UShort`,
которые служили общим буфером для разбора входящих пакетов. Это делало BLE-парсинг
зависимым от внешнего глобального состояния.

### 3. Баг двойного удаления в writeNext()

Исходный код:
```kotlin
// БЫЛО (баг):
private fun writeNext() {
    var data = writeBuffer!!.removeAt(0)   // <-- убирает элемент СНАРУЖИ synchronized
    synchronized(writeBuffer!!) {
        data = if (writeBuffer!!.isNotEmpty())
            writeBuffer!!.removeAt(0)      // <-- пытается убрать ЕЩЁ ОДИН элемент
        else null
        writePending = data != null
    }
    // первый removeAt(0) всегда терял данные при непустой очереди
}
```
При непустой очереди первый `removeAt(0)` (вне `synchronized`) сразу выбрасывался,
а запись шла со второго элемента. Первый пакет бесследно терялся.

### 4. Устаревший API characteristic.value

`characteristic.value` и `characteristic.setValue(bytes)` помечены `@Deprecated`
начиная с Android 13 (API 33). Новый API: `gatt.writeCharacteristic(char, bytes, type)`.

### 5. GPS-методы в BLE-классе

`getLastKnownLocation()` и `getFreshLocation()` находились в `BluetoothInterface.kt`,
хотя требуют `Context` активности и `FusedLocationProviderClient` — это слой UI/Activity.

### 6. MainScope() с риском утечки

Места, где запускались корутины через `MainScope().launch { }`, не привязаны к
жизненному циклу активности и не отменяются при её уничтожении.

---

## Что сделали — изменения

### Новый файл: DeviceFrame.kt

Создан файл `BluZ/src/main/java/ru/starline/bluz/DeviceFrame.kt`.

Содержит три data-класса и sealed-класс:

```
DeviceFrame          — полный пакет данных от устройства после разбора
  ├── frameType: Int              (0=дозиметр, 1-3=спектр 1024/2048/4096, 4-6=история)
  ├── totalPulses: UInt           — суммарный счёт импульсов
  ├── pulsesPerSec: UInt          — импульсы за последнюю секунду
  ├── avgCps: Float               — скользящее среднее CPS от прошивки
  ├── measurementTime: UInt       — время измерения, секунды
  ├── temperature: Float          — температура SiPM, °C
  ├── batteryVoltage: Float       — напряжение батареи, В
  ├── dosimeterData: DoubleArray  — 512 значений гистограммы дозиметра
  ├── logEntries: List<LogEntry>  — до 50 записей аппаратного лога
  ├── spectrumData: DoubleArray?  — спектр (1024/2048/4096) или null
  ├── historyData: DoubleArray?   — история-спектр или null
  ├── hw: HardwareConfig          — аппаратная конфигурация из пакета
  └── overload: Boolean           — флаг перегрузки детектора

LogEntry             — одна запись лога
  ├── timestamp: UInt
  └── action: UByte

HardwareConfig       — аппаратные настройки устройства
  ├── ledKvant, soundKvant        — светодиод/звук «квант»
  ├── soundLevel1/2/3             — пороги звукового сигнала
  ├── vibroLevel1/2/3             — пороги вибросигнала
  ├── autoStartSpectrometer       — авто-старт спектрометра
  ├── click10, led10              — клик/LED на 10 имп/с
  ├── level1/2/3: Int             — пороговые уровни CPS
  ├── cps2ur: Float               — коэффициент перевода CPS → мкР/ч
  ├── hVoltage: UShort            — высокое напряжение (10-bit, 0..1023)
  ├── comparator: UShort          — порог компаратора (10-bit)
  ├── coef1024/2048/4096 A/B/C    — калибровочные коэффициенты спектра (9 float)
  ├── acquireValue: UShort        — длительность накопления
  ├── bitsChan: UByte             — глубина квантования (16..32, def=20)
  ├── sampleTime: UByte           — время выборки (0..7)
  ├── spectrometerTime: UInt      — время спектрометра
  └── spectrometerPulse: UInt     — порог импульсов спектрометра

BleStatus (sealed)   — статус BLE-соединения
  ├── Connecting
  ├── Connected
  ├── Disconnected
  └── Error(message: String)
```

### BluetoothInterface.kt — полная переработка

**Добавлены реактивные потоки данных:**

```kotlin
// Испускает полностью разобранный пакет после проверки контрольной суммы
private val _deviceFrames = MutableSharedFlow<DeviceFrame>(extraBufferCapacity = 4)
val deviceFrames: SharedFlow<DeviceFrame> = _deviceFrames

// Статус BLE-подключения, наблюдаемый из UI
private val _status = MutableStateFlow<BleStatus>(BleStatus.Disconnected)
val status: StateFlow<BleStatus> = _status
```

**Структура приёма пакетов:**

Устройство BluZ передаёт данные по BLE порциями (MTU = 248 байт).
Полный пакет может занимать от 6 до 40 MTU-фрагментов в зависимости от типа:

| frameType | Описание              | MTU-фрагментов | Итого байт |
|-----------|-----------------------|----------------|------------|
| 0         | Только дозиметр       | 6              | ~1488      |
| 1         | Спектр 1024           | 16             | ~3968      |
| 2         | Спектр 2048           | 23             | ~5704      |
| 3         | Спектр 4096           | 40             | ~9920      |
| 4         | История 1024          | 16             | ~3968      |
| 5         | История 2048          | 23             | ~5704      |
| 6         | История 4096          | 40             | ~9920      |

**Разметка буфера `receiveData` (9760 байт):**

```
Смещение   Размер     Содержимое
0..33      34 байта   Заголовок: счётчики, температура, напряжение
34..99     66 байт    Конфигурация: коэффициенты, пороги, флаги
100..1123  1024 байта Гистограмма дозиметра (512 × uint16)
1124..1423 300 байт   Лог (50 записей × 6 байт: 4 байта timestamp + 2 байта action)
1424..     2..8 Кбайт Спектр или история (N × uint16, сжатые)
```

**Сжатие спектра (логарифмическое):**
```
compressed_value = uint16 из BLE-пакета
mult = bitsChan / 65535.0 * ln(2.0)
real_value = exp(compressed_value * mult) - 1.0
```

**Исправленный writeNext():**
```kotlin
// СТАЛО (правильно):
private fun writeNext() {
    val data: ByteArray?
    synchronized(writeBuffer!!) {
        data = if (writeBuffer!!.isNotEmpty()) {
            writePending = true
            writeBuffer!!.removeAt(0)   // единственное удаление, внутри synchronized
        } else {
            writePending = false
            null
        }
    }
    data?.let { /* записать в характеристику */ }
}
```

**Поддержка deprecated API:**
```kotlin
// Android < 13 (API < 33):
@Suppress("DEPRECATION")
wrCharacteristic!!.value = it
@Suppress("DEPRECATION")
gatt!!.writeCharacteristic(wrCharacteristic)

// Android 13+ (API 33):
gatt!!.writeCharacteristic(wrCharacteristic!!, it, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
```

**Смещение sendCS из globalObj в локальную переменную:**
```kotlin
// БЫЛО: GO.sendCS накапливался в globalObj
// СТАЛО: cs вычисляется локально внутри sendCommand()
fun sendCommand(cmd: UByte) {
    ...
    var cs: UShort = 0u
    for (i in 0..241) cs = (cs + sendBuffer[i]).toUShort()
    sendBuffer[242] = (cs and 255u).toUByte()
    sendBuffer[243] = ((cs.toUInt() shr 8) and 255u).toUByte()
    write(sendBuffer.toByteArray())
}
```

**Что убрано из BluetoothInterface:**
- Все прямые обращения к View-элементам (`GO.txtStat3`, `GO.indicatorBT`, и т.д.)
- GPS-методы (`getLastKnownLocation`, `getFreshLocation`) — перенесены в MainActivity
- `GO.receiveData` — теперь локальный `private val receiveData = UByteArray(9760)`
- `GO.sendCS` — вычисляется локально

### globalObj.kt — удалены мёртвые поля

Удалены два поля, ставшие неиспользуемыми после рефакторинга:

```kotlin
// УДАЛЕНО (было 9760 байт глобального буфера):
public var receiveData: UByteArray = UByteArray(9760)

// УДАЛЕНО (вычислялось локально в sendCommand):
public var sendCS: UShort = 0u
```

Проверено grep-ом по всему проекту — ноль ссылок после удаления.

### MainActivity.kt — подписка на потоки данных

**Добавлен импорт:**
```kotlin
import kotlinx.coroutines.flow.collect
```
(нужен для вызова `.collect { }` на SharedFlow/StateFlow)

**Подписки запускаются в `onCreate` после `GO.startBluetoothTimer()`:**
```kotlin
observeBleStatus()
observeDeviceFrames()
```

**observeBleStatus() — реакция на статус подключения:**
```kotlin
private fun observeBleStatus() {
    lifecycleScope.launch {
        GO.BTT.status.collect { status ->
            val color = when (status) {
                is BleStatus.Connected    -> GO.mainContext.getColor(R.color.Green)
                is BleStatus.Connecting   -> GO.mainContext.getColor(R.color.Yellow)
                is BleStatus.Disconnected -> GO.mainContext.getColor(R.color.Red)
                is BleStatus.Error        -> GO.mainContext.getColor(R.color.Red)
            }
            GO.indicatorBT.setBackgroundColor(color)
        }
    }
}
```
Использует `lifecycleScope` — корутина автоматически отменяется при уничтожении Activity.

**observeDeviceFrames() — обработка каждого пакета:**
```kotlin
private fun observeDeviceFrames() {
    lifecycleScope.launch {
        GO.BTT.deviceFrames.collect { frame ->
            applyFrameToState(frame)   // копирует поля в GO.*
            applyFrameToUi(frame)      // обновляет View
        }
    }
}
```

**applyFrameToState(frame) — синхронизация с globalObj:**

Копирует все поля `DeviceFrame` в соответствующие переменные `GO.*`:
- `GO.PCounter`, `GO.pulsePerSec`, `GO.cps`, `GO.messTm` — счётчики
- `GO.tempMC`, `GO.battLevel`, `GO.overloadFlag` — состояние устройства
- `GO.HWprop*`, `GO.HWCoef*`, `GO.HWAqureValue` и т.д. — аппаратная конфигурация
- `GO.specterType`, `GO.drawSPECTER.ResolutionSpectr`, `GO.HWspectrResolution` — метаданные спектра

**applyFrameToUi(frame) — обновление UI (suspend, Main-поток):**
- Устанавливает индикатор в Green
- Вызывает `GO.showStatistics()`
- Вычисляет и отображает CPS / дозу в мкР/ч или мкЗв/ч
- Копирует `dosimeterData` → `GO.drawDOZIMETER.dozimeterData`, перерисовывает
- Копирует `logEntries` → `GO.drawLOG.logData`, обновляет лог
- Если `historyData != null` — обновляет `GO.drawHISTORY`, перерисовывает
- Если `spectrumData != null` — обновляет `GO.drawSPECTER`, перерисовывает
- Управляет текстом/цветом кнопки старт/стоп спектрометра

**GPS и запись трека (перенесено из BluetoothInterface):**
```kotlin
private val fusedLocationClient by lazy {
    LocationServices.getFusedLocationProviderClient(this)
}

// Сначала пробует кэшированную позицию, затем запрашивает свежую (таймаут 10 с)
private suspend fun recordTrackPoint(frame: DeviceFrame) {
    val location = getLastKnownLocation() ?: getFreshLocation()
    val detail = TrackDetail(
        trackId   = GO.currentTrck,
        latitude  = location?.latitude  ?: 0.0,
        longitude = location?.longitude ?: 0.0,
        ...
        cps       = frame.pulsesPerSec.toFloat(),
        timestamp = System.currentTimeMillis() / 1000
    )
    GO.dao.insertPoint(detail)   // Room suspend DAO — безопасно из корутины
    // добавляет цветной плейсмарк на карту Яндекс
}
```

---

## Итоговая схема потока данных (Phase 1)

```
BluZ-устройство (BLE)
        │
        ▼  onCharacteristicChanged (фоновый GATT-поток)
BluetoothInterface
  processIncomingPacket()
    │  сборка MTU-фрагментов
    │  проверка контрольной суммы
    │  декодирование: дозиметр, лог, спектр, HardwareConfig
    ▼
  DeviceFrame (data class)
        │
        ├──► _deviceFrames (SharedFlow) ──► MainActivity.observeDeviceFrames()
        │                                        ├── applyFrameToState(frame) → GO.*
        │                                        └── applyFrameToUi(frame)    → View
        │
        └──► _status (StateFlow) ──────────► MainActivity.observeBleStatus()
                                                     └── indicatorBT.color
```

---

## Phase 2 — ViewModel и мелкие правки (выполнено)

### Новый файл: DeviceViewModel.kt

```
DeviceUiState        — снимок состояния устройства для UI
  ├── bleStatus: BleStatus
  ├── totalPulses, pulsesPerSec, avgCps, measurementTime
  ├── temperature, batteryVoltage, overload
  ├── frameType, hw: HardwareConfig?
  ├── dosimeterData: DoubleArray (512)
  ├── logEntries: List<LogEntry>
  ├── spectrumData: DoubleArray?
  └── historyData: DoubleArray?

DeviceViewModel : ViewModel()
  ├── state: StateFlow<DeviceUiState>   — наблюдаемое состояние
  └── observeBle(btt)                   — подписывается на BTT.status и BTT.deviceFrames
                                          через viewModelScope (переживает поворот экрана)
```

**DeviceViewModel подключён в MainActivity:**
```kotlin
val deviceViewModel: DeviceViewModel by viewModels()
// в onCreate():
deviceViewModel.observeBle(GO.BTT)
```

**Зависимость добавлена** в `libs.versions.toml` и `build.gradle`:
```
androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7
```

### AndroidManifest.xml

- Добавлено `<uses-permission android:name="android.permission.INTERNET" />` — карты Яндекс не загружались без него
- Убрано `android:screenOrientation="landscape"` — приложение теперь поддерживает обе ориентации

### NumberFragment.kt — MapKit lifecycle

Было: `MapKitFactory.getInstance().onStart()` и `mapView.onStart()` вызывались в `onViewCreated` — только один раз при создании view.

Стало: правильные lifecycle-методы в классе фрагмента:
```kotlin
override fun onStart() {
    super.onStart()
    if (GO.pagerFrame == 5 && ::mapViewPlan.isInitialized) {
        MapKitFactory.getInstance().onStart()
        mapViewPlan.onStart()
    }
}
override fun onStop() {
    super.onStop()
    if (GO.pagerFrame == 5 && ::mapViewPlan.isInitialized) {
        mapViewPlan.onStop()
        MapKitFactory.getInstance().onStop()
    }
}
```

### Статус проверки на устройстве
- ✅ Приложение открывается в портретной и ландшафтной ориентации
- ✅ Карта Яндекс отображает тайлы
- ✅ BUILD SUCCESSFUL

---

## Что НЕ изменено (намеренно)

- `NumberFragment.kt` — 173 KB, содержит весь UI. Оставлен нетронутым в Phase 1.
- `drawSpecter.kt`, `drawDozimeter.kt`, `drawHistory.kt`, `drawLogs.kt` — рисующие классы.
- Yandex MapKit интеграция — отображение карты и плейсмарков.
- `DosimeterDao.kt`, Room-сущности — база данных треков.
- Навигация ViewPager2 + FragmentStateAdapter — 6 вкладок.
- `globalObj.kt` — остаётся god-объектом (устраняется в Phase 3).

---

## Известные оставшиеся проблемы (для следующих фаз)

### Phase 2 — ViewModel
- `applyFrameToState` по-прежнему пишет напрямую в `GO.*` — нужен `DeviceViewModel`
  со `StateFlow<DeviceUiState>`, тогда фрагменты подписываются на ViewModel, а не на GO
- `globalObj` содержит ~200+ публичных `var` полей без инкапсуляции
- `MainScope()` используется в некоторых местах — нужно заменить на `viewModelScope`

### Phase 3 — Навигация
- `NumberFragment.kt` — один файл на весь UI (~173 KB). Нужно разбить на:
  - `DosimeterFragment`
  - `SpectrumFragment`
  - `HistoryFragment`
  - `MapFragment`
  - `LogFragment`
  - `SettingsFragment`
- Заменить ViewPager2 + ручное управление видимостью на Navigation Component

### Общий технический долг
- `public` перед свойствами/методами в Kotlin — избыточно (default = public)
- Закомментированный мёртвый код по всему проекту
- `java.sql.Array` импортирован в `globalObj.kt` — не используется
- `androidx.loader.content.Loader.ForceLoadContentObserver` — не используется

---

## Файлы, изменённые в Phase 1

| Файл | Изменение |
|------|-----------|
| `DeviceFrame.kt` | **Создан** — data-классы для модели данных |
| `BluetoothInterface.kt` | **Переписан** — убран UI-код, добавлены SharedFlow/StateFlow |
| `MainActivity.kt` | **Дополнен** — observeBleStatus/observeDeviceFrames/recordTrackPoint |
| `globalObj.kt` | **Минорное** — удалены 2 мёртвых поля (receiveData, sendCS) |

---

## Сборка на Windows — настройка и результат

### Среда сборки
- Android Studio: `C:\Program Files\Android\Android Studio`
- JDK (JBR): `C:\Program Files\Android\Android Studio\jbr` — OpenJDK 21.0.10
- Android SDK: `C:\Users\motok\AppData\Local\Android\Sdk`
- Build Tools 36 и Platform 36 — установлены Gradle автоматически при первой сборке

### Изменения в gradle.properties (Windows-override, НЕ коммитить в git)

```properties
# Оригинальная строка автора (Linux):
#org.gradle.java.home=/usr/lib/jvm/temurin-17-jdk-amd64
# Windows-override (Android Studio JBR):
org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr
```

### Созданные local.properties файлы (НЕ коммитить в git)

**`BluZ_LPM/local.properties`** (корень проекта):
```properties
sdk.dir=C\:\\Users\\motok\\AppData\\Local\\Android\\Sdk
```

**`BluZ_LPM/BluZ/local.properties`** (модуль, нужен для getMapkitApiKey()):
```properties
sdk.dir=C\:\\Users\\motok\\AppData\\Local\\Android\\Sdk
MAPKIT_API_KEY=
```
(MAPKIT_API_KEY оставлен пустым — карта Яндекс не будет работать в debug-сборке,
но компиляция и весь остальной функционал в порядке)

### Изменения в BluZ/build.gradle (Windows-override)

```groovy
// Было:
kotlin { jvmToolchain(17) }
kotlinOptions { //jvmTarget = '17' }

// Стало (JDK 21 из Android Studio не поддерживает toolchain(17)):
kotlin { jvmToolchain(21) }
kotlinOptions { jvmTarget = '17' }
```

Обоснование: `jvmToolchain(21)` использует JDK 21 для компиляции, но `kotlinOptions
{ jvmTarget = '17' }` обеспечивает совместимость байткода Java 17. Для Android это
безопасно — D8/R8 конвертирует всё в DEX-байткод при сборке.

### Исправленные ошибки компиляции в новом коде

**Ошибка 1 — BluetoothInterface.kt:343**
```
None of the following candidates is applicable:
fun Array<*>?.isNullOrEmpty(): Boolean
```
`ByteArray` — примитивный массив, для него нет `isNullOrEmpty()`.
Исправлено: `data.isNullOrEmpty()` → `data == null || data.isEmpty()`

**Ошибка 2 — BluetoothInterface.kt:479-489**
```
Operator '!=' cannot be applied to 'UByte' and 'UInt'
```
`flags0 = d[60]` имело тип `UByte`, а `1u`/`0u` — `UInt`.
Оператор `and` между `UByte` и `UInt` не определён в Kotlin.
Исправлено: `val flags0 = d[60].toUInt()` — явное приведение к `UInt`.

### Результат

```
BUILD SUCCESSFUL in 19s
Output: BluZ/build/outputs/apk/debug/BluZ-debug.apk (130 MB)
```

### Для автора (Linux)

На Linux `gradle.properties` и `build.gradle` нужно вернуть к оригиналу:
```properties
org.gradle.java.home=/usr/lib/jvm/temurin-17-jdk-amd64
```
```groovy
kotlin { jvmToolchain(17) }
kotlinOptions { //jvmTarget = '17' }
```
Файлы `local.properties` в обеих папках игнорируются git-ом (`.gitignore`).
