# BluZ — Журнал рефакторинга

Полный журнал всех изменений, внесённых в проект в ходе улучшения архитектуры.
Дата сессии: 2026-05-23.

---

## Phase 1 — Рефакторинг BLE-слоя

### Проблемы исходного кода

1. **UI-код внутри BLE-коллбэков** — `onCharacteristicChanged` напрямую
   обновлял View-элементы из GATT-потока. Риск гонок данных и крэшей.

2. **Глобальный буфер** — `GO.receiveData: UByteArray(9760)` и `GO.sendCS`
   — общее изменяемое состояние для BLE-парсинга.

3. **Баг двойного удаления в writeNext():**
   ```kotlin
   // БЫЛО (баг):
   var data = writeBuffer!!.removeAt(0)   // вне synchronized — первый элемент теряется
   synchronized(writeBuffer!!) {
       data = if (writeBuffer!!.isNotEmpty()) writeBuffer!!.removeAt(0) else null
       writePending = data != null
   }
   ```

4. **Устаревший API** — `characteristic.value` deprecated в Android 13+.

5. **GPS в BLE-классе** — `getLastKnownLocation()` был в `BluetoothInterface`.

### Что сделано

**Создан `DeviceFrame.kt`:**
- `data class DeviceFrame` — иммутабельная модель пакета
- `data class LogEntry(timestamp: UInt, action: UByte)`
- `data class HardwareConfig` — 29 полей аппаратной конфигурации
- `sealed class BleStatus` — Connecting / Connected / Disconnected / Error

**Переписан `BluetoothInterface.kt`:**
- Добавлены реактивные потоки:
  ```kotlin
  val deviceFrames: SharedFlow<DeviceFrame>  // extraBufferCapacity = 4
  val status: StateFlow<BleStatus>
  ```
- `processIncomingPacket()` — всё декодирование в одном методе, результат
  испускается через `_deviceFrames.tryEmit(frame)`
- Исправлен `writeNext()` — единственный `removeAt(0)` внутри `synchronized`
- Deprecated API подавлен `@Suppress("DEPRECATION")`, добавлен современный
  путь для Android 13+ (`gatt.writeCharacteristic(char, data, type)`)
- `receiveData` и `sendCS` убраны из `GO` и стали локальными

**Изменён `MainActivity.kt`:**
- Добавлен импорт `kotlinx.coroutines.flow.collect`
- Добавлены методы: `observeBleStatus()`, `observeDeviceFrames()`,
  `applyFrameToState()`, `applyFrameToUi()`, `recordTrackPoint()`,
  `getLastKnownLocation()`, `getFreshLocation()`
- Все подписки через `lifecycleScope.launch` (отменяются при уничтожении Activity)

**Изменён `globalObj.kt`:**
- Удалено: `public var receiveData: UByteArray = UByteArray(9760)`
- Удалено: `public var sendCS: UShort = 0u`

### Исправленные ошибки компиляции

| Файл | Строка | Ошибка | Исправление |
|------|--------|--------|------------|
| `BluetoothInterface.kt` | 343 | `ByteArray?.isNullOrEmpty()` не существует | `data == null \|\| data.isEmpty()` |
| `BluetoothInterface.kt` | 479-489 | `UByte != UInt` — нет такого оператора | `val flags0 = d[60].toUInt()` |

---

## Phase 2 — ViewModel, ориентация, MapKit lifecycle

### Проблемы

1. **Нет ViewModel** — нет источника истины, переживающего поворот экрана.
2. **Только landscape** — `android:screenOrientation="landscape"` в манифесте.
3. **MapKit lifecycle неправильный** — `onStart()` вызывался в `onViewCreated`
   (только один раз), `onStop()` был закомментирован.
4. **Нет INTERNET permission** — карта Яндекс не загружала тайлы.

### Что сделано

**Создан `DeviceViewModel.kt`:**
```kotlin
data class DeviceUiState(
    val bleStatus: BleStatus,
    val totalPulses: UInt, val pulsesPerSec: UInt, val avgCps: Float,
    val measurementTime: UInt, val temperature: Float, val batteryVoltage: Float,
    val overload: Boolean, val frameType: Int, val hw: HardwareConfig?,
    val dosimeterData: DoubleArray, val logEntries: List<LogEntry>,
    val spectrumData: DoubleArray?, val historyData: DoubleArray?
)

class DeviceViewModel : ViewModel() {
    val state: StateFlow<DeviceUiState>
    fun observeBle(btt: BluetoothInterface)   // viewModelScope.launch
}
```

**Добавлена зависимость:**
- `libs.versions.toml`: `lifecycleViewmodel = "2.8.7"`
- `libs.versions.toml`: `androidx-lifecycle-viewmodel-ktx = { ... }`
- `build.gradle`: `implementation libs.androidx.lifecycle.viewmodel.ktx`

**`MainActivity.kt`:**
```kotlin
val deviceViewModel: DeviceViewModel by viewModels()
// в onCreate:
deviceViewModel.observeBle(GO.BTT)
```

**`AndroidManifest.xml`:**
- Добавлено: `<uses-permission android:name="android.permission.INTERNET" />`
- Удалено: `android:screenOrientation="landscape"`

**`NumberFragment.kt`:**
- Заменён закомментированный блок на рабочие lifecycle методы:
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
- Убраны вызовы `MapKitFactory.getInstance().onStart()` и `mapView.onStart()`
  из `onViewCreated`

**`BluZ/build.gradle` (только для Windows-сборки):**
```groovy
kotlin { jvmToolchain(21) }   // было 17; JDK 21 из Android Studio JBR
kotlinOptions { jvmTarget = '17' }   // было закомментировано
```

**`BluZ/local.properties` (создан для Windows-сборки, не в git):**
```properties
MAPKIT_API_KEY=<ключ_из_кабинета_Yandex>
sdk.dir=C\:\\Users\\motok\\AppData\\Local\\Android\\Sdk
```

### Проверено на устройстве
- ✅ Приложение открывается
- ✅ Обе ориентации работают
- ✅ Карта Яндекс отображает тайлы

---

## Phase 3 — UI redesign (в планах)

Пользователь создаёт новый дизайн через Claude Design.
Промпт для Claude Design сохранён в `DESIGN_PROMPT.md`.

Планируемые технические изменения:
- Разделить `NumberFragment.kt` на 6 отдельных фрагментов
- Заменить ViewPager2 + свайп на Navigation Component + BottomNavigationView
- Новые XML-лэйауты с поддержкой WindowInsets (notch, rounded corners)
- Адаптивная вёрстка для portrait и landscape
- Тёмная тема, крупные элементы управления

---

## Технический долг (не исправлено, но задокументировано)

| Проблема | Файл | Приоритет |
|---------|------|----------|
| `public` перед всеми полями (избыточно в Kotlin) | `globalObj.kt` | Низкий |
| Закомментированный мёртвый код | везде | Низкий |
| `java.sql.Array` импорт не используется | `globalObj.kt` | Низкий |
| `ForceLoadContentObserver` импорт не используется | `globalObj.kt` | Низкий |
| God-object `globalObj` с 200+ полями | `globalObj.kt` | Высокий (Phase 3) |
| `NumberFragment` 173 KB — монолит | `NumberFragment.kt` | Высокий (Phase 3) |
| `MainScope()` в некоторых местах (риск утечки) | разные | Средний |
| Миграция БД: `MIGRATION_2_3` объявлена как `Migration(1,3)` | `AppDatabase.kt` | Высокий |
