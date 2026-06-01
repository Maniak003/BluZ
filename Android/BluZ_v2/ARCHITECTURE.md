# BluZ Android — Архитектура приложения

## Обзор

Приложение состоит из трёх слоёв. После Phase 1–2 рефакторинга они чётко разделены:

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  MainActivity · NumberFragment · Drawing classes    │
│                    ↑ observe                        │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                     │
│   DeviceViewModel · DeviceUiState · BleStatus       │
│                    ↑ collect                        │
├─────────────────────────────────────────────────────┤
│                   BLE Layer                         │
│   BluetoothInterface · DeviceFrame · HardwareConfig │
│                    ↑ parse                          │
├─────────────────────────────────────────────────────┤
│               Hardware (BLE device)                 │
└─────────────────────────────────────────────────────┘
```

---

## Поток данных

```
BluZ-устройство (BLE)
        │ onCharacteristicChanged (GATT фоновый поток)
        ▼
BluetoothInterface.processIncomingPacket()
  • сборка MTU-фрагментов в буфер receiveData[9760]
  • проверка контрольной суммы (CRC16 Little-Endian)
  • декодирование: дозиметр, лог, спектр, HardwareConfig
        │
        ├──► _deviceFrames: MutableSharedFlow<DeviceFrame>
        │         │ extraBufferCapacity = 4
        │         ▼
        │    DeviceViewModel.observeBle()
        │         │ viewModelScope.launch
        │         ▼
        │    _state: MutableStateFlow<DeviceUiState>
        │         │
        │         ├──► MainActivity.observeDeviceFrames()
        │         │         → applyFrameToState(frame) → GO.*
        │         │         → applyFrameToUi(frame) → View
        │         │
        │         └──► (будущие фрагменты подпишутся напрямую)
        │
        └──► _status: MutableStateFlow<BleStatus>
                  │
                  ├──► DeviceViewModel (обновляет state.bleStatus)
                  └──► MainActivity.observeBleStatus() → indicatorBT.color
```

---

## Ключевые классы

### BluetoothInterface
**Файл:** `BluetoothInterface.kt`

Единственное место, где происходит работа с BLE. Инкапсулирует:
- Сканирование BLE-устройств (`leScanCallback`)
- GATT-подключение и обнаружение сервисов
- Сборку многофрагментных пакетов
- Декодирование данных → `DeviceFrame`
- Очередь записи с защитой от потокобезопасности (`writeBuffer + synchronized`)

**Публичный API:**
```kotlin
val deviceFrames: SharedFlow<DeviceFrame>   // новый пакет от устройства
val status: StateFlow<BleStatus>            // состояние подключения
var sendBuffer: UByteArray                  // буфер команды (255 байт)
fun sendCommand(cmd: UByte)                 // отправить команду
fun write(data: ByteArray)                  // отправить данные
fun destroyDevice()                         // отключиться и освободить ресурсы
```

---

### DeviceFrame
**Файл:** `DeviceFrame.kt`

Иммутабельная модель одного полного пакета от устройства.
Создаётся в `BluetoothInterface`, потребляется в UI/ViewModel.

```kotlin
data class DeviceFrame(
    val frameType: Int,          // 0-6 (тип пакета)
    val totalPulses: UInt,       // суммарный счёт
    val pulsesPerSec: UInt,      // CPS за последнюю секунду
    val avgCps: Float,           // скользящее среднее CPS
    val measurementTime: UInt,   // секунды
    val temperature: Float,      // °C (SiPM)
    val batteryVoltage: Float,   // В
    val dosimeterData: DoubleArray,   // 512 значений
    val logEntries: List<LogEntry>,   // до 50 записей
    val spectrumData: DoubleArray?,   // null если frameType == 0
    val historyData: DoubleArray?,    // null если frameType < 4
    val hw: HardwareConfig,
    val overload: Boolean
)
```

**Типы пакетов:**

| frameType | Содержимое | MTU-фрагментов |
|-----------|-----------|----------------|
| 0 | Только дозиметр | 6 |
| 1 | Дозиметр + спектр 1024 | 16 |
| 2 | Дозиметр + спектр 2048 | 23 |
| 3 | Дозиметр + спектр 4096 | 40 |
| 4 | Дозиметр + история 1024 | 16 |
| 5 | Дозиметр + история 2048 | 23 |
| 6 | Дозиметр + история 4096 | 40 |

---

### DeviceViewModel
**Файл:** `DeviceViewModel.kt`

ViewModel с `StateFlow<DeviceUiState>`. Переживает пересоздание Activity
(поворот экрана, смена конфигурации). Подписывается на `BluetoothInterface`
через `viewModelScope` (отменяется автоматически при уничтожении ViewModel).

```kotlin
class DeviceViewModel : ViewModel() {
    val state: StateFlow<DeviceUiState>
    fun observeBle(btt: BluetoothInterface)
}
```

Доступен из Activity через `by viewModels()`.
В `MainActivity`: `val deviceViewModel: DeviceViewModel by viewModels()`

---

### globalObj (GO)
**Файл:** `globalObj.kt`

Глобальный синглтон `val GO = globalObj()` — god-object, исторически
используемый как шина данных между всеми компонентами приложения.

**Группы полей:**
- `propCfg*` — ключи SharedPreferences (константы)
- `draw*` — ссылки на Canvas-объекты рисования
- `HWprop*`, `HWCoef*` — аппаратная конфигурация из пакета
- `PCounter`, `pulsePerSec`, `cps`, `messTm` — счётчики измерений
- `tempMC`, `battLevel`, `overloadFlag` — состояние устройства
- `mapView`, `mapWindow`, `map` — Yandex MapKit объекты
- `locationManager`, `lastPointLoc` — GPS
- `currentTrck`, `trackIsRecordeed` — управление треком
- `BTT: BluetoothInterface` — BLE-интерфейс
- `drawSPECTER`, `drawHISTORY`, `drawDOZIMETER`, `drawLOG` — рисующие объекты
- `impArr[32]` — цветные иконки меток на карте (радужный градиент)
- `radClrs[32]`, `radClrsKml[32]` — 32 цвета от синего до красного
- View-ссылки: `indicatorBT`, `txtStat1/2/3`, `scanButton`, `btnSpecterSS`, и т.д.

**Статус:** god-object подлежит поэтапному устранению (Phase 3+).
На данный момент `DeviceViewModel` дублирует часть данных как шаг миграции.

---

### MainActivity
**Файл:** `MainActivity.kt`

`FragmentActivity` — точка входа приложения. Ответственности:
- Создание и инициализация `GO`
- Запуск BLE-таймера и подписка на потоки данных
- `observeBleStatus()` — обновление индикатора подключения
- `observeDeviceFrames()` → `applyFrameToState()` + `applyFrameToUi()`
- GPS: `getLastKnownLocation()`, `getFreshLocation()`, `recordTrackPoint()`
- Управление разрешениями (Bluetooth, Location)
- Обработка аппаратных кнопок громкости (управление уровнями)
- WindowInsets / edge-to-edge

---

### NumberFragment
**Файл:** `NumberFragment.kt` (~173 KB, ~3200 строк)

Один класс на все 6 вкладок. В `onViewCreated` разветвляется по `GO.pagerFrame`:

| pagerFrame | Вкладка | Layout |
|------------|---------|--------|
| 0 | Спектр | `spectrum_layout` |
| 1 | История | `history_layout` |
| 2 | Дозиметр | `dozimetr_layout` |
| 3 | Лог | `log_layout` |
| 4 | Настройки | `setup_layout` |
| 5 | Карта | `map_layout` |

**Lifecycle MapKit** (только pagerFrame == 5):
```kotlin
override fun onStart() {
    if (GO.pagerFrame == 5 && ::mapViewPlan.isInitialized) {
        MapKitFactory.getInstance().onStart()
        mapViewPlan.onStart()
    }
}
override fun onStop() {
    if (GO.pagerFrame == 5 && ::mapViewPlan.isInitialized) {
        mapViewPlan.onStop()
        MapKitFactory.getInstance().onStop()
    }
}
```

**Подлежит разделению** на 6 отдельных фрагментов (Phase 3).

---

### BleMonitoringService
**Файл:** `bgService.kt`

Foreground Service для фонового мониторинга BLE и GPS.
- Тип: `location | connectedDevice`
- Содержит магнитометр (усреднение по 20 измерениям)
- Логика периодического сканирования BLE в фоне
- Уведомление в статус-баре при активной записи трека

---

### Рисующие классы

Все рисуют на `Bitmap` через `Canvas` и передают в `ImageView`.

| Класс | Данные | Описание |
|-------|--------|----------|
| `drawSpecter` | `spectrData: DoubleArray(4096)` | Спектр: линия или гистограмма, лог/лин шкала |
| `drawHistory` | `historyData: DoubleArray(4096)` | История спектра |
| `drawDozimeter` | `dozimeterData: DoubleArray(512)` | Гистограмма дозиметра + SMA |
| `drawLogs` | `logData: Array<LG>(50)` | Лог событий, скроллируемый |
| `drawCursor` | — | Курсор на графике спектра |
| `drawExmple` | — | Пример/образец |

---

### База данных (Room)

**Схема v3:**

```
tracks
  id           INTEGER PK autoincrement
  name         TEXT
  created_at   INTEGER (Unix timestamp)
  is_active    BOOLEAN
  is_hidden    BOOLEAN
  cps2urh      REAL

track_details                    ← FK → tracks.id (CASCADE DELETE)
  id           INTEGER PK
  track_id     INTEGER
  latitude     REAL
  longitude    REAL
  accuracy     REAL
  altitude     REAL
  speed        REAL
  cps          REAL
  magnitude    REAL              ← магнитное поле (от магнитометра)
  timestamp    INTEGER

detectors
  id           INTEGER PK
  name         TEXT
  change_at    INTEGER
  chi_vector   BLOB
  curActive    INTEGER
```

---

### GPS и картирование

**ContinuousLocationManager** (`gpsLocation.kt`):
- Обёртка над `FusedLocationProviderClient`
- Callback-based обновления с интервалом 5 секунд
- Алгоритм Haversine для расстояний с поправкой на кривизну Земли
- Расчёт zoom-уровня карты по Меркаторной проекции

**Цветовая шкала радиации** (32 уровня):
- `GO.radClrs[32]` — для Yandex MapKit (AARRGGBB)
- `GO.radClrsKml[32]` — для KML-экспорта (AABBGGRR)
- Градиент: синий (0) → зелёный (15) → жёлтый (20) → красный (31)
- Уровень выбирается пропорционально `pulsesPerSec` относительно пороговых значений

---

## Инициализация при запуске

```
Application.onCreate()
  └── MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
  └── MapKitFactory.initialize(this)

MainActivity.onCreate()
  ├── GO = globalObj()           (глобальный синглтон)
  ├── GO.mainContext = appContext
  ├── GO.BTT = BluetoothInterface()
  ├── GO.PP = propControl()      (SharedPreferences)
  ├── setupViewPager()           (6 фрагментов NumberFragment)
  ├── requestPermissions()       (BT + Location)
  ├── GO.startBluetoothTimer()   (таймер сканирования)
  ├── deviceViewModel.observeBle(GO.BTT)
  ├── observeBleStatus()         (lifecycleScope)
  └── observeDeviceFrames()      (lifecycleScope)
```

---

## Разрешения Android

| Разрешение | Назначение |
|-----------|-----------|
| `INTERNET` | Загрузка тайлов Yandex MapKit |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | BLE (API ≤ 30) |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` | BLE (API ≥ 31) |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS + BLE scan |
| `FOREGROUND_SERVICE` | BleMonitoringService |
| `FOREGROUND_SERVICE_LOCATION` | Геолокация в фоне |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | BLE в фоне |
| `POST_NOTIFICATIONS` | Уведомление сервиса |
| `READ_EXTERNAL_STORAGE` | Чтение файлов (API ≤ 28) |
