# BluZ — Руководство разработчика

> **Цель документа.** Дать человеку, который никогда не видел этот проект, возможность за час понять архитектуру, найти нужное место и внести корректную правку.

Связанные документы:
- [`BluZ_docs.md`](BluZ_docs.md) — техническая документация (22 раздела + журнал UX-итераций)
- [`project_state.md`](project_state.md) — что сделано, что осталось, известные ограничения
- [`AutoCalib_PeakDetection_Math.md`](AutoCalib_PeakDetection_Math.md) — математика автокалибровки по Ra-226
- [`BLE_PROTOCOL.md`](BLE_PROTOCOL.md) — формат BLE-фрейма (исторический документ)

---

## Оглавление

1. [Что такое BluZ](#1-что-такое-bluz)
2. [Архитектура за 5 минут](#2-архитектура-за-5-минут)
3. [Карта репозитория](#3-карта-репозитория)
4. [Жизненный цикл приложения](#4-жизненный-цикл-приложения)
5. [BLE-стек](#5-ble-стек)
6. [Глобальное состояние GO](#6-глобальное-состояние-go)
7. [UI-слой: MainActivity и фрагменты](#7-ui-слой-mainactivity-и-фрагменты)
8. [Отрисовка спектра и графиков](#8-отрисовка-спектра-и-графиков)
9. [База данных Room](#9-база-данных-room)
10. [Карта и GPS](#10-карта-и-gps)
11. [Фоновый сервис записи трека](#11-фоновый-сервис-записи-трека)
12. [Тема и поворот экрана](#12-тема-и-поворот-экрана)
13. [Типовые задачи](#13-типовые-задачи)
14. [Подводные камни](#14-подводные-камни)
15. [Сборка и подпись](#15-сборка-и-подпись)
16. [Отладка](#16-отладка)
17. [Что в работе и известные ограничения](#17-что-в-работе-и-известные-ограничения)
18. [Генерация документации через Dokka](#18-генерация-документации-через-dokka)

---

## 1. Что такое BluZ

**BluZ** — сцинтилляционный гамма-спектрометр с GPS. Аппаратно:
- Сцинтиллятор (NaI или CsI) + кремниевый фотоумножитель (SiPM)
- АЦП с настраиваемым порогом компаратора
- Bluetooth Low Energy (5.0)
- Прошивка на C, собственный протокол поверх стандартного GATT

**Приложение** — Android-клиент. Подключается к прибору по BLE, принимает фреймы с дозиметрическими и спектральными данными, рисует графики, ведёт треки на карте Яндекс.

Прибор работает автономно. Приложение — для:
- Просмотра спектра в реальном времени
- Калибровки (соотношение канал ↔ энергия)
- Записи треков радиационного фона по координатам GPS
- Управления параметрами прибора (ВВ, компаратор, пороги тревоги, и т.д.)

Целевая аудитория приложения — энтузиасты-радиолюбители, дозиметристы, специалисты по разведке загрязнений.

---

## 2. Архитектура за 5 минут

```
Прибор BluZ ──BLE/GATT──┐
                        │ ScanResult / NotifyChars
                        ▼
            ┌────────────────────────┐
            │  BluetoothInterface    │  собирает фрейм из MTU-чанков,
            │  (BluetoothInterface.kt)│  проверяет CRC, эмитит DeviceFrame
            └───────────┬────────────┘
                        │ SharedFlow<DeviceFrame>
                        │ StateFlow<BleStatus>
                        ▼
       ┌────────────────────────────────┐
       │ MainActivity / DeviceViewModel │ копирует hw → GO, рисует UI
       └────────────────┬───────────────┘
                        │
            ┌───────────┼──────────────┬─────────────────┐
            ▼           ▼              ▼                 ▼
       ┌────────┐  ┌─────────┐  ┌──────────────┐  ┌──────────────┐
       │Spectrum│  │  Dose   │  │BluZMapFragment│  │SettingsFrag  │
       │Fragment│  │Fragment │  │ + Yandex MapKit│  │              │
       └────────┘  └─────────┘  └───────┬───────┘  └──────────────┘
                                        │
                                ┌───────▼────────┐
                                │ Room database  │
                                │ tracks /       │
                                │ track_details /│
                                │ detectors      │
                                └────────────────┘

При выходе из приложения, если идёт запись трека:
            ┌────────────────────────┐
            │ BleMonitoringService   │  Foreground service
            │ (bgService.kt)         │  слушает advertising,
            └────────────────────────┘  пишет точки в Room
```

**Ключевые архитектурные решения:**

- **Реактивный BLE** через `SharedFlow<DeviceFrame>` / `StateFlow<BleStatus>` — подписаться может любой слой.
- **Глобальный синглтон `GO`** (`globalObj.kt`) — центральное хранилище состояния. **Антипаттерн с точки зрения чистой архитектуры**, но переписывать ради DI/MVVM на полную никто не будет. Все знают, где смотреть.
- **`MainActivity` расширяет `FragmentActivity`, не `AppCompatActivity`** — критично для темы (см. раздел 12).
- **Draw-объекты живут в `Application`** (`App.onCreate`) — переживают пересоздание Activity при повороте, в них хранятся буферы спектра (4096 значений), которые нельзя терять.
- **Сервис подключается к прибору не через GATT, а через BLE-advertising** — экономит батарею и не конфликтует с активным GATT-соединением UI.

---

## 3. Карта репозитория

```
C:\Bluz\
├── BluZ\Android\BluZ_LPM\         ← Android-проект (Gradle)
│   ├── BluZ\                       ← модуль приложения
│   │   ├── build.gradle            ← версия 1.12 (code 12), signing, deps
│   │   └── src\main\
│   │       ├── AndroidManifest.xml
│   │       ├── java\ru\starline\bluz\
│   │       │   ├── App.kt          ← Application (живёт в YandexMapActivity.kt)
│   │       │   ├── MainActivity.kt ← 970 строк, центральный класс
│   │       │   ├── LogActivity.kt  ← отдельная Activity для логов
│   │       │   ├── globalObj.kt    ← синглтон GO — 1100+ строк состояния
│   │       │   ├── BluetoothInterface.kt    ← BLE-стек, ~700 строк
│   │       │   ├── DeviceFrame.kt           ← DTO для BLE-фрейма
│   │       │   ├── DeviceViewModel.kt       ← MVVM-обёртка над BLE flows
│   │       │   ├── bgService.kt    ← BleMonitoringService (foreground)
│   │       │   ├── *Fragment.kt    ← 6 фрагментов (Spectrum, History, Dose, Map, Settings, Log)
│   │       │   ├── NumberAdapter.kt         ← ViewPager2 адаптер
│   │       │   ├── draw*.kt        ← 6 классов отрисовки (Canvas+Bitmap)
│   │       │   ├── calculateDose.kt         ← DoseCalculator (χ-вектор)
│   │       │   ├── Mtrx.kt         ← решение системы 3 уравнений (калибровка)
│   │       │   ├── AutoCalibrator.kt         ← автокалибровка по Ra-226: поиск пиков + полином
│   │       │   ├── AutoCalibrationController.kt ← корутинная машина фаз автокалибровки
│   │       │   ├── SaveBqMon.kt    ← экспорт спектра в XML/CSV
│   │       │   ├── gpsLocation.kt  ← ContinuousLocationManager
│   │       │   ├── ThemePrefs.kt   ← переключение день/ночь темы
│   │       │   ├── propControl.kt  ← обёртка SharedPreferences
│   │       │   ├── intervalTimer.kt         ← переподключение BLE по таймеру
│   │       │   ├── buttonColor.kt  ← (legacy) цвета кнопок
│   │       │   └── data\
│   │       │       ├── AppDatabase.kt
│   │       │       ├── DatabaseConverters.kt
│   │       │       ├── dao\DosimeterDao.kt
│   │       │       └── entity\*.kt
│   │       └── res\
│   │           ├── layout\         ← portrait XML
│   │           ├── layout-land\    ← landscape XML
│   │           ├── values\colors.xml          ← светлая тема, токены bz_*
│   │           ├── values-night\colors.xml    ← тёмная тема
│   │           ├── values\themes.xml          ← Material3
│   │           ├── values-night\themes.xml
│   │           ├── drawable\       ← векторные иконки ic_bz_*, фоны bg_bz_*
│   │           ├── color\          ← state-list color selectors
│   │           └── font\           ← Inter, JetBrains Mono (downloadable Google Fonts)
│   ├── gradle.properties           ← java.home (Linux-путь автора)
│   └── settings.gradle
│
├── BluZ_docs.md                    ← техническая документация (22 раздела)
├── DEVELOPER_GUIDE.md              ← этот файл
├── project_state.md                ← что сделано / осталось
├── BLE_PROTOCOL.md                 ← старое описание протокола
├── BluZ-debug.apk                  ← последняя debug-сборка
├── BluZ-releaseTest.apk            ← последний RC
└── icons\                          ← сторонние иконки для импорта
```

---

## 4. Жизненный цикл приложения

### 4.1 Запуск

```
1. Android создаёт App (Application)
   → App.onCreate() в YandexMapActivity.kt
   → ThemePrefs.apply(isDayTheme)         // тема прикручивается ДО Activity
   → MapKitFactory.setApiKey + initialize // Yandex MapKit
   → создаются singleton draw-объекты в GO

2. Android создаёт MainActivity (launcher Activity)
   → attachBaseContext оборачивает Context с правильным uiMode (тема)
   → onCreate:
     a) setContentView(activity_main)
     b) checkAndRequestPermissions (BLE, location, notifications)
     c) GO.PP = propControl(this); GO.readConfigParameters()
     d) GO.loadIsotop() (справочник из 47 изотопов)
     e) Инициализация ViewPager2 + NumberAdapter (4 страницы)
     f) findViewById для всех bz*-вьюх в StatusStrip и legacy txtStat1..3
     g) restoreStatusStripFromState() — берёт значения из кэша
     h) setupNavigation() — кастомные табы Spectrum/Dose/Map/Settings/Exit
     i) lifecycleScope.launch { GO.BTT.deviceFrames.collect { ... applyFrameToState + applyFrameToUi } }
     j) lifecycleScope.launch { GO.BTT.status.collect { ... observeBleStatus } }
     k) onResume: applySystemBarsForTheme, проверка фонового сервиса
```

### 4.2 Получение фрейма от прибора

```
BLE-чип → onCharacteristicChanged (BluetoothInterface.kt)
→ processIncomingPacket(value)
  → сборка фрейма из нескольких MTU-чанков
  → проверка CRC
  → парсинг полей в DeviceFrame
  → _deviceFrames.emit(frame)

MainActivity (подписчик SharedFlow)
→ applyFrameToState(frame): копирует hw* и frame-поля в GO.HW* / GO.cps / etc
→ applyFrameToUi(frame): пишет в bzCpsValue, bzTempValue, bzBattValue,
                         applyDoseReadouts(), история/спектр в drawHISTORY/drawSPECTER
```

### 4.3 Поворот экрана

```
Android уничтожает MainActivity → создаёт новую
  - draw-объекты в App переживают (хранят bitmap-буферы и spectrData)
  - флаг GO.drawObjectInit = true → следующий init() пересоздаст bitmap под новые размеры
  - В onCreate новой Activity: findViewById возвращает НОВЫЕ view (нулевые)
  - restoreStatusStripFromState() заполняет из GO кэша
  - Фрагменты пересоздаются (ViewPager2 + FragmentStateAdapter), их onViewCreated
    переподцепляет ссылки в GO (drawSPECTER.imgView = новая ImageView и т.д.)
```

---

## 5. BLE-стек

### 5.1 UUIDs

| Назначение | UUID |
|-----------|------|
| Service | `0000fe80-cc7a-482a-984a-7f2ed5b3e58f` |
| RX (notify, приём от прибора) | `0000fe81-8e22-4541-9d4c-21edae82ed19` |
| TX (write, отправка команд) | `0000fe82-8e22-4541-9d4c-21edae82ed19` |

### 5.2 Формат фрейма

Длина зависит от типа фрейма (`frameType` в байте 3):

| Тип | Содержимое | Размер |
|---|---|---|
| 0 | Дозиметр + лог | ~750 байт |
| 1 | + спектр 1024 канала | ~2800 байт |
| 2 | + спектр 2048 каналов | ~4800 байт |
| 3 | + спектр 4096 каналов | ~8800 байт |
| 4–6 | + история (тот же набор разрешений) | соответственно |

Каждый фрейм режется на MTU-чанки (~248 байт payload при MTU 251). `BluetoothInterface.processIncomingPacket` собирает их обратно: первый байт пакета — порядковый номер (0 — старт нового фрейма).

**Структура шапки фрейма (байты 0–58):**

```
0-2   : header '<B>' (0x3C 0x42 0x3E)
3     : frameType (0..6)
4     : reserved
5-8   : totalPulses (uint32 little-endian)
9-12  : pulsesPerSec (uint32)
13-16 : measurementTime (uint32, секунды)
17-20 : avgCps (float)
21-22 : temperature (float16, °C)
23-24 : batteryVoltage (float16, В)
25-30 : коэф полинома A/B/C для 1024 каналов (float16 × 3)
31-32 : коэф CPS→μR/h (float16)
33    : ВВ
34    : порог компаратора
35-37 : пороги тревоги L1/L2/L3
38    : битовая конфигурация (LED/звук/вибро)
39    : ещё конфигурация (autoStart, ×10-делители)
40-51 : коэф полинома для 2048 и 4096 каналов
52-53 : spectrometerTime
54-55 : spectrometerPulse
56    : точность дозиметра
57(L) : бит/канал АЦП
57(H) : три бита — sample time АЦП
58    : end of header
```

**Данные:**
- 50–561: dosimeterData (512 × uint8)
- 562–661: 50 записей лога (timestamp uint32 + action uint8)
- 662+: spectrumData или historyData (uint16 × resolution)
- последние 2 байта: CRC (uint16 = sum(bytes[0..241]) % 65536)

### 5.3 Команды

Приложение шлёт команды через `sendCommand(cmd: UByte)`. Буфер `sendBuffer: UByteArray(255)` формируется заранее в `SettingsFragment.btnWriteToDevice` для сложных команд.

| Код | Команда | Параметры |
|---|---|---|
| 0 | Стоп | — |
| 1 | Очистить спектр | — |
| 2 | Старт/стоп спектрометра (toggle) | — |
| 3 | Сброс дозиметра | — |
| 4 | Очистить логи прибора | — |
| 5 | Запросить историю | — |
| 6 | Найти прибор (звук+вибро на 3 сек) | — |
| 7 | Передать реальное напряжение АКБ | `sendBuffer[4..7]` — float |
| 8 | Очистить историю | — |

### 5.4 Сценарий подключения

```
1. UI вызывает GO.BTT.initLeDevice() (с заполненным GO.LEMAC)
2. BluetoothInterface.connectGatt(autoConnect=false)
3. onConnectionStateChange(CONNECTED) → requestMtu(251)
4. onMtuChanged → discoverServices
5. onServicesDiscovered → setCharacteristicNotification(RX, true)
6. С этого момента приходят onCharacteristicChanged каждую секунду
```

`GO.tmFull` — `intervalTimer`, который через `MyTimerTask.run()` каждые 10 сек проверяет: если `connected == false`, вызывает `destroyDevice() + initLeDevice()`. Это автоматический реконнект.

---

## 6. Глобальное состояние GO

`globalObj.kt` (псевдоним `GO`) — большой объект-синглтон. Содержит:

**Категории полей:**
- **Контекст и BLE:** `mainContext`, `BTT: BluetoothInterface`, `LEMAC`, `Current_RSSI`
- **Флаги состояния:** `allPermissionAccept`, `initBT`, `initDOZ`, `overloadFlag`, `configDataReady`, `needTerminate`, `specterRunning`
- **Измерения:** `PCounter`, `pulsePerSec`, `cps`, `cpsAVG`, `messTm`, `spectrometerTime`, `spectrometerPulse`, `tempMC`, `battLevel`
- **Параметры спектра:** `specterType`, `specterGraphType`, `spectrResolution`, `xZoom`, `xPosition`, `rejectChann`, `windowSMA`, `realResolution`
- **`prop*` поля** — то что в SharedPreferences (то что в UI настройках)
- **`HW*` поля** — то что пришло из последнего фрейма прибора
- **Цвета графиков:** `ColorLin`, `ColorLog`, `ColorFone`, `ColorDosimeter`, и т.д.
- **`radClrs[32]` / `radClrsKml[32]`** — палитра для карты (синий→красный по CPS)
- **GPS:** `map`, `locationManager`, `impArr[32]`, `currentTrck`
- **UI-ссылки** (lateinit или nullable):
  - StatusStrip: `bzCpsValue`, `bzClockValue`, `bzTempValue`, `bzBattValue`, `bzBtDot`, `bzBtIcon`
  - Hero (Spectrum): `bzSpecDoseValue`, `bzSpecAvgValue`, `bzRecClock`, etc — все nullable, привязываются в SpectrumFragment.onViewCreated, обнуляются в onDestroyView
  - Hero (Dose): `bzDoseHeroValue`, `bzDoseStatusPill`, `bzDoseStatusTitle`
  - History: `bzHistDuration` (удалён 2026-05-26), `bzHistIntegralValue`, `bzHistAvgCpsValue`
- **Settings UI:** `editLevel1/2/3`, `editHVoltage`, `editComparator`, `cbSoundLevel*`, `rbResolution*`, и куча других

**Методы:**
- `showStatistics()` — формирует тексты в StatusStrip + secondary stats
- `formatClock(totalSec, showSeconds)` — длительность в формате `hh:mm:ss` или `dd:hh:mm:ss` или `N сек`
- `updateSpecSubtitle()` — пересчёт subtitle над «Гамма-спектр» (количество каналов и диапазон)
- `readConfigParameters() / writeConfigParameters()` — IO в SharedPreferences `device.properties`
- `readConfigFormDevice()` — переносит `HW*` → `prop*` (вызывается из UI после прихода фрейма)
- `loadIsotop()` — заполняет 47 изотопов справочника
- `findIsotop(energy)` — поиск изотопа по энергии с допуском `realResolution` каналов
- `createRainbowColors()` — заполняет `impArr[32]` для карты
- `startBluetoothTimer()` — запускает `tmFull` (auto-reconnect)

### Почему GO такой большой и не разносится по слоям

Исторические причины + миграционная цена. Переход на DI (Hilt/Koin) + ViewModels потребует переработать всё. Сейчас работает, читабельно. Соблюдаются неписаные правила:
- Ссылки на View, привязанные к фрагменту, делать nullable (`TextView?`) и обнулять в `onDestroyView` — иначе утечки.
- Lateinit-ссылки на View Activity не обнуляем (Activity живёт долго).
- Числовые данные (`cps`, `tempMC`, etc) — обычные поля. Не сбрасывать без причины.

---

## 7. UI-слой: MainActivity и фрагменты

### 7.1 MainActivity

**Базовый класс:** `FragmentActivity` (НЕ `AppCompatActivity` — критично для темы, см. раздел 12).

**Ключевые методы:**

| Метод | Что делает |
|---|---|
| `onCreate` | Полная инициализация. См. раздел 4.1 |
| `onResume` | Проверка статус-сервиса, повторное применение темы |
| `attachBaseContext(newBase)` | Оборачивает Context через `ThemePrefs.wrapContextWithTheme` |
| `applyFrameToState(frame)` | Раскладывает `DeviceFrame` в поля GO |
| `applyFrameToUi(frame)` | Обновляет StatusStrip, dose-readouts, спектр, дозиметр, лог |
| `applyDoseReadouts()` | Только dose-секция (cps/мкР·ч/мкЗв·ч, pill уровня тревоги). Не зависит от фрейма — берёт из GO |
| `formatDoseScaled(microR, useSievert)` | Авто-скейл μ→м→база, см. раздел 0.6 в BluZ_docs.md |
| `toggleDoseUnits()` | Переключение мкР/ч ↔ мкЗв/ч + сохранение в SharedPreferences |
| `applySpecterButton()` | Рендерит иконку и фон кнопки Start/Stop из `GO.specterRunning` |
| `setupNavigation()` | Кастомные табы (Spectrum/Dose/Map/Settings/Exit) — 4 страницы + Exit как действие |
| `recordTrackPoint(frame)` | При активной записи трека вставляет TrackDetail в Room |
| `performExit()` | Запуск фонового сервиса (если запись активна) + finishAndRemoveTask |
| `restoreStatusStripFromState()` | После поворота — восстановление значений из GO |

### 7.2 Фрагменты

`NumberAdapter` создаёт 4 фрагмента (FragmentStateAdapter):

| Index | Класс | Layout |
|---|---|---|
| 0 | `SpectrumFragment` | `spectr_layout.xml` |
| 1 | `DoseFragment` | `dozimetr_layout.xml` |
| 2 | `BluZMapFragment` | `map_layout.xml` |
| 3 | `SettingsFragment` | `setup_layout.xml` |

**Tab «Выход»** — не страница, а action. При клике вызывается `performExit()`.

**`SpectrumFragment`** — самый сложный:
- Привязка `drawSPECTER.imgView`, `drawHISTORY.imgView`, `drawCURSOR.cursorView`
- Touch-handler на cursorView: 1 палец — курсор, 2 пальца — pinch-zoom
- Внутренний ViewPager2 (`specBottomPager`) с двумя страницами: «ИДЁТ ИЗМЕРЕНИЕ» + clock и «ИСТОРИЯ» + Integral/CPS
- При swipe нижнего pager переключается chart card сверху: `specterView ↔ historyView`, скрываются курсор и toolbar
- Кнопки Start/Stop, Save, Clear, Calibrate

**`DoseFragment`** — отображает гистограмму дозиметра (512 значений). Hero: текущая мощность дозы + статус-pill (NORMAL/L1/L2/L3/OVERLOAD).

**`BluZMapFragment`** — Yandex MapKit. Запись треков, отображение, экспорт KML/CSV, маркер «моё место» (красный крест + серый круг точности).

**`SettingsFragment`** — карточный ScrollView с 9 секциями: Подключение, Спектр, Дозиметр, Уровни тревоги, Калибровка, Звук/LED/Вибро, Карта/Трек, Приложение, Действия. Сохраняет в SharedPreferences (`prop*`-поля). Отправляет команду Write (`sendCommand` + сложный sendBuffer). В секции «Калибровка» — кнопки **«Автокалибровка по Ra-226»** и **«Подбор компаратора»** (см. раздел 13.6 и `BluZ_docs.md` 0.31).

**`LogFragment`** — открывается из Settings через отдельную `LogActivity` (не в bottom nav).

---

## 8. Отрисовка спектра и графиков

Каждый из draw-классов держит свой `Bitmap` + `Canvas`, рисует через `drawLine` / `drawText`. Bitmap привязывается к `ImageView` (`setImageBitmap`).

### 8.1 drawSpecter

512 строк. Самый сложный:
- `init()` — создаёт `specBitmap` под размер `imgView` (или пересоздаёт если `GO.drawObjectInit == true`)
- `redrawSpecter(spType, offSetX)` — пересчёт + отрисовка:
  - SMA-фильтр (окно `GO.windowSMA`) если `flagSMA`
  - Медианный фильтр (3 точки) если `flagMEDIAN`
  - MLEM (энергокомпенсация) — обновляет `compMED` через `DoseCalculator.calculateH10DoseSafe`
  - Поиск максимума → коэффициенты масштабирования `koefLin` / `koefLog`
  - Цикл по каналам: рисует две линии (linear/log) или две полоски (histogram)
- `clearSpecter()` — заливка чёрным
- `resetSpecter()` — обнуляет `spectrData[]` и `tmpSpecterData[]`

### 8.2 drawHistory

Похож на drawSpecter, но без MLEM, zoom/pan, calibrate. Только пересчёт + отрисовка.

### 8.3 drawDozimeter

Отдельный класс для гистограммы дозиметра (512 значений с прибора, без коэффициентов). Имеет swap-буфер для SMA-сглаживания.

### 8.4 drawCursor

Overlay поверх специтра:
- `init()` — создаёт `cursorBitmap`
- `showCorsor(x, y)`:
  - Стирает старый курсор (`hideCursor` через `PorterDuff.Mode.CLEAR`)
  - Рисует вертикальную линию + кружок + подпись «keV/channel»
  - Если калибровка задана → ищет изотоп через `GO.findIsotop(energy)`
  - Если изотоп найден → расчёт активности по площади пика − фоновая активность по краям

### 8.5 drawLogs

НЕ canvas! Работает с `TextView` + HTML-форматирование. Метод `appendAppLogs(text, level)` накапливает в `GO.appLogBuffer` строки с цветами (0=error, 1=info, 2=warn, 3=success, 4=debug, 5=trace).

### 8.6 drawExmple

Превью-канвас в SettingsFragment для предварительного просмотра цветов спектра при изменении настроек.

---

## 9. База данных Room

**Версия БД:** 3 · `fallbackToDestructiveMigration(true)` (миграции удалены — старые БД сносятся при апгрейде).

**3 сущности:**

**`tracks` (Track.kt):**
| Колонка | Тип | Назначение |
|---|---|---|
| `id` | Long PK auto | |
| `name` | String | имя трека |
| `created_at` | Long | timestamp (сек) |
| `isActive` | Boolean | флаг активного (запись идёт сюда) |
| `isHidden` | Boolean | помечен удалённым (мягкое удаление) |
| `cps2urh` | Float | коэф CPS→μR/h на момент создания трека (для корректной отрисовки в будущем) |

**`track_details` (TrackDetail.kt):**
Точка трека. FK на `tracks.id` с CASCADE.
| Колонка | Тип |
|---|---|
| `id` | Long PK auto |
| `trackId` | Long FK |
| `latitude`, `longitude` | Double |
| `accuracy` | Float (метры) |
| `cps` | Float |
| `altitude` | Double |
| `speed` | Float |
| `magnitude` | Double (магнитное поле, мкТл) |
| `timestamp` | Long (сек) |

**`detectors` (DetectorType.kt):**
χ-векторы для разных детекторов.
| Колонка | Тип |
|---|---|
| `id` | Long PK auto |
| `name` | String |
| `changeAt` | Long |
| `chiVector` | DoubleArray (1024 значения, сериализуется через `DatabaseConverters`) |
| `curActive` | Boolean |

**`DosimeterDao`** — все запросы. Suspend-функции для Room — вызываются из корутин (`viewLifecycleOwner.lifecycleScope.launch`).

**Доступ:** `GO.dao` (через `AppDatabase.getDatabase(context).dosimeterDao()`). Singleton инициализирован в `MainActivity.onCreate`.

---

## 10. Карта и GPS

**SDK:** Yandex MapKit `4.19.0-lite`.

**Жизненный цикл MapView**: критично — `onStart()` / `onStop()` фрагмента вызывают `MapKitFactory.getInstance().onStart()` и `mapViewPlan.onStart()` (и stop соответственно). Иначе утечки.

**API-ключ:** `BuildConfig.MAPKIT_API_KEY` из `local.properties` (не коммитится).

**GPS:** `ContinuousLocationManager` (gpsLocation.kt) — обёртка над `FusedLocationProviderClient`. Интервал 5 сек, приоритет HIGH_ACCURACY. Старт в `onStart`, стоп в `onStop` (если нет активной записи трека).

**Маркер «моё место»:**
- `myLocationCross: PlacemarkMapObject?` — красный крест 40×40 (Bitmap + ImageProvider)
- `myLocationAccuracy: CircleMapObject?` — серый полупрозрачный круг радиуса `location.accuracy`
- Обновляются в `updateMyLocationOnMap(location)` — на каждый GPS callback
- **Подводный камень:** `mapObjects.clear()` (вызывается при создании/удалении/выборе трека) уничтожает их → нужно обнулять ссылки + `isValid` проверка в начале update-метода

**Запись трека:**
- `MainActivity.recordTrackPoint(frame)` — при `GO.trackIsRecordeed && GO.currentTrck > 0` вставляет TrackDetail
- Координаты берёт из `GO.lastPointLoc` (последний полученный location)
- Магнитное поле сейчас НЕ собирается в фрагменте (только в фоновом сервисе)

**Цветовая шкала:** `GO.radClrs[32]` (синий→красный). Каждой точке присваивается цвет по нормализованному CPS относительно min/max трека.

**Экспорт:**
- KML (`saveTrackType == 0`): полосатый файл с `<Placemark>` для каждой точки, color из `radClrsKml[32]`
- CSV (`saveTrackType == 1`): таблица timestamp, lat, lon, alt, speed, accuracy, CPS, magnetic
- Папка: `/Documents/BluZ/`

---

## 11. Фоновый сервис записи трека

**Класс:** `BleMonitoringService` (`bgService.kt`).

**Когда запускается:** в `MainActivity.performExit()`, если `GO.trackIsRecordeed` (пользователь выходит из приложения при активной записи).

**Что делает:**
1. Foreground service с notification (тихий канал `ble_monitor_silent`, IMPORTANCE_LOW)
2. BLE-сканирование с фильтром по MAC (`TARGET_DEVICE_MAC`)
3. **НЕ подключается по GATT** — слушает advertising-пакеты от прибора
4. Извлекает CPS из manufacturer data (companyId 0x0030, 4 байта uint32 little-endian)
5. Получает GPS через свой `FusedLocationProviderClient`
6. Снимает магнитометр (`SensorManager.TYPE_MAGNETIC_FIELD`), усредняет по 20 измерениям
7. Пишет точку в Room (`dao.insertPoint(TrackDetail(...))`)
8. Обновляет notification: CPS, доза, magnitude, RSSI

**Параметры через SharedPreferences `app_state`:**
- `device_mac` (записывает `MainActivity.performExit`)
- `current_track_id`
- `cps_2_doze`
- `is_ble_service_running` (флаг)

**Остановка:** через action `STOP_SERVICE` (кнопка в уведомлении) или при возврате в приложение (`MainActivity.onResume` останавливает).

**Известное ограничение:** Android может троттлить background BLE scan вне foreground — сервис иногда не сразу цепляет пакеты. Это системное ограничение, не баг.

---

## 12. Тема и поворот экрана

### 12.1 Тема (день/ночь)

`MainActivity extends FragmentActivity, не AppCompatActivity`. Это **критично**: `AppCompatDelegate.setDefaultNightMode` работает только с `AppCompatActivity`. Поэтому собственное решение:

```kotlin
// ThemePrefs.kt
fun wrapContextWithTheme(base: Context): Context {
    val cfg = Configuration(base.resources.configuration)
    cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (isDayTheme(base)) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
    return base.createConfigurationContext(cfg)
}

// MainActivity.kt
override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ThemePrefs.wrapContextWithTheme(newBase))
}
```

После этого `values-night/*` подключается корректно.

**Переключение темы во время работы:**
```kotlin
ThemePrefs.setDayTheme(requireContext(), checked)
view.post { requireActivity().recreate() }
```

**Подводный камень.** В `applyFrameToUi` все вызовы `ContextCompat.getColor(...)` должны использовать `this` (Activity-контекст с темой), а НЕ `GO.mainContext` (Application-контекст). Иначе цвета будут от ночной темы даже в дневной.

### 12.2 Поворот экрана

`AndroidManifest.xml` — **нет** `android:configChanges="orientation|screenSize"`. Activity пересоздаётся → Android сам подбирает `layout/` или `layout-land/`.

**Что переживает пересоздание:**
- `App` (Application) и все singleton draw-объекты в нём (`GO.drawSPECTER`, `drawHISTORY`, etc) — там данные спектра (4096 значений).
- Поля `GO.*` — обычные значения, lateinit View-ссылки сбрасываются.

**Что НЕ переживает:**
- `MainActivity` сам — пересоздаётся.
- View-ссылки в GO (lateinit) — нужно повторное `findViewById` в новой Activity / Fragment.
- Bitmap внутри draw-объектов — пересоздаются под новые размеры view (флаги `drawObjectInit`, `drawObjectInitHistory`, `drawDozObjectInit`, `drawCursorObjectInit`).

**Pattern для draw-классов:**
```kotlin
fun init() {
    if (!this::imgView.isInitialized) return
    if (GO.drawObjectInit || !this::specBitmap.isInitialized) {
        if (imgView.width == 0 || imgView.height == 0) return  // layout ещё не отработал
        specBitmap = createBitmap(imgView.width, imgView.height)
        specCanvas = Canvas(specBitmap)
        GO.drawObjectInit = false
    }
    imgView.setImageBitmap(specBitmap)
}
```

**Pattern для фрагментов:**
```kotlin
override fun onViewCreated(view: View, ...) {
    GO.drawSPECTER.imgView = view.findViewById(R.id.specterView)
    view.findViewById<ImageView>(R.id.specterView).viewTreeObserver.addOnGlobalLayoutListener {
        // когда view получит размеры
        GO.drawObjectInit = true
        GO.drawSPECTER.init()
        GO.drawSPECTER.clearSpecter()
        GO.drawSPECTER.redrawSpecter(...)
    }
}
```

---

## 13. Типовые задачи

### 13.1 Добавить новую BLE-команду

**1. На стороне прошивки** — реализовать. Команда — это `sendBuffer[3]` (см. раздел 5.3).

**2. В приложении:**
- В нужном фрагменте/обработчике: `GO.BTT.sendCommand(N.toUByte())`
- Если команде нужны параметры — заполнить `GO.BTT.sendBuffer[4..]` до вызова `sendCommand`

Пример (калибровка АКБ — команда 7):
```kotlin
val convVal = ByteBuffer.allocate(4).putFloat(value).array()
GO.BTT.sendBuffer[4] = convVal[0].toUByte()
GO.BTT.sendBuffer[5] = convVal[1].toUByte()
GO.BTT.sendBuffer[6] = convVal[2].toUByte()
GO.BTT.sendBuffer[7] = convVal[3].toUByte()
GO.BTT.sendCommand(7u)
```

### 13.2 Добавить новую вкладку

**1.** В `NumberAdapter.createFragment(position)` добавить `case`.
**2.** В `MainActivity.setupNavigation()`:
- `tabSpecs.add(BzTabSpec(R.id.tab_xxx, R.drawable.ic_nav_xxx, "Метка"))`
- `railSpecs.add(BzTabSpec(R.id.rail_xxx, ...))`
**3.** В `activity_main.xml` и `activity_main-land.xml` добавить `<include layout="@layout/item_bz_nav_tab" android:id="@+id/tab_xxx" ... />` (и `rail_xxx` для landscape).
**4.** Создать иконку `ic_nav_xxx.xml` в drawable.
**5.** Создать сам фрагмент + layout.
**6.** Обновить `applyStatVisibility` в setupNavigation если для новой вкладки нужно особое поведение.

### 13.3 Добавить новую настройку (числовое поле)

**1.** В `setup_layout.xml` добавить EditText с id, например `editFoo`.
**2.** В `globalObj.kt`:
- Объявить поле: `var propFoo: Int = 0`
- Объявить ключ: `val propCfgFoo: String = "Foo"`
- В `readConfigParameters()`: `GO.propFoo = GO.PP.getPropInt(propCfgFoo)`
- В `writeConfigParameters()`: `GO.PP.setPropInt(propCfgFoo, GO.propFoo)`
**3.** В `SettingsFragment.kt`:
- В `onViewCreated`: `GO.editFoo = view.findViewById(R.id.editFoo)`
- В обработчике Save: `GO.propFoo = GO.editFoo.text.toString().trim().toIntOrNull() ?: 0` (важно — не `.toInt()` напрямую!)
- В `reloadConfigParameters()`: `GO.editFoo.setText(GO.propFoo.toString())`

### 13.4 Добавить новый показатель в hero-readout

**1.** В соответствующий `bz_spec_*` или другой layout добавить TextView с id, например `@+id/bzNewValue`.
**2.** В `globalObj.kt` объявить: `var bzNewValue: TextView? = null`.
**3.** В фрагменте `onViewCreated`: `GO.bzNewValue = view.findViewById(R.id.bzNewValue)`.
**4.** В `onDestroyView`: `GO.bzNewValue = null`.
**5.** В `MainActivity.applyFrameToUi` или `applyDoseReadouts`: `GO.bzNewValue?.text = "..."`.

### 13.5 Изменить формат BLE-фрейма

**1. Прошивка** — главная сторона. Помнить про CRC.
**2.** В `BluetoothInterface.processIncomingPacket`:
- Найти место парсинга поля
- Сместить смещения если изменилась длина шапки
- Если новое поле — добавить в `DeviceFrame` или `HardwareConfig`
**3.** В `MainActivity.applyFrameToState`: разложить новое поле в GO.
**4.** В UI — отобразить.

### 13.6 Разобраться в автокалибровке по Ra-226

Энергетическую шкалу (полином `E = A·ch² + B·ch + C`), высокое напряжение и компаратор можно подобрать автоматически по источнику Ra-226. Две точки входа в `SettingsFragment` (секция «Калибровка»):

- **«Автокалибровка по Ra-226»** → `showAutoCalibrationModeDialog()` → 3 режима (`AutoCalibrationController.Mode`): `PRIMARY` (ВВ+компаратор+полином), `HV_AND_COMPARATOR` (без полинома), `POLYNOMIAL_ONLY` (только полином).
- **«Подбор компаратора»** → `startComparatorOnly()` — standalone-подбор уровня компаратора при текущих ВВ/полиноме.

Разделение ответственности:

| Слой | Файл | Что делает |
|---|---|---|
| Чистый расчёт | `AutoCalibrator.kt` | Поиск пиков, pattern-matching на тройку 82/352/609 кэВ, решение полинома, sanity-проверки. Без UI и I/O. |
| Оркестрация фаз | `AutoCalibrationController.kt` | Корутинная машина: подбор компаратора → bracket scan ВВ → refine полинома → финальная подгонка. `StateFlow<State>`, запись в прибор через `cmd_setup`. |
| UI | `SettingsFragment.kt` | Стилизованные диалоги `dialog_bz_*.xml`, подписка на `state`, prompt'ы «уберите/поднесите источник». |

Алгоритм поиска пиков и все формулы — в `AutoCalib_PeakDetection_Math.md`. Архитектура фаз, режимы, защиты и история эволюции — в `BluZ_docs.md` раздел 0.31. Контроллер **не пишет** результат сразу: формирует `PendingResult` и ждёт подтверждения пользователя (`applyPending`).

---

## 14. Подводные камни

### 14.1 «Я меняю настройку — а ничего не отображается»

Скорее всего вы пишете в `prop*` (UI-настройки), а UI читает `HW*` (то что пришло из прибора). Чтобы UI отразил — нужно либо:
- Записать в прибор (`Write` в settings) + дождаться следующего фрейма (там в hw придёт новое значение, `applyFrameToState` обновит `HW*`)
- Либо в коде вручную скопировать `prop*` → `HW*` + дёрнуть рендер

### 14.2 «Падает с NumberFormatException на пустом поле»

Никогда не используйте `.toInt()` / `.toFloat()` напрямую на `EditText.text.toString()`. Всегда:
```kotlin
val v = field.text.toString().trim().toIntOrNull() ?: 0
```
Десятичные числа дополнительно `.replace(',', '.')` (русская локаль).

### 14.3 «Падает с ArrayIndexOutOfBoundsException в drawCursor»

В функциях, работающих с `spectrData[i]` или `historyData[i]`, проверять границы массива:
```kotlin
if (i < 0 || i >= spectrData.size) return
```

### 14.4 «View null после swipe в ViewPager2»

Внутренний pager (`specBottomPager` в SpectrumFragment) пересоздаёт holders. `GO.bzHistDuration` и т.д. обнуляются и перепривязываются в `BottomSwipeAdapter.onBindViewHolder`. Не делать `bz*` lateinit — только nullable.

### 14.5 «Канвас спектра пустой после поворота»

Bitmap не пересоздался. Проверьте, что:
- `GO.drawObjectInit = true` выставлен перед вызовом `init()`
- Вызов `init()` идёт из `OnGlobalLayoutListener` (когда view уже получил размеры)
- `imgView.width > 0 && height > 0` на момент init

### 14.6 «MapObject крашит карту»

Если делаете `map.mapObjects.clear()` — обязательно обнуляйте кешированные ссылки на свои `PlacemarkMapObject` / `CircleMapObject`. Или проверяйте `obj.isValid` перед вызовом методов.

### 14.7 «Цвета не меняются при переключении темы»

Используйте `ContextCompat.getColor(this, ...)` где `this` — Activity context (с темой). НЕ `GO.mainContext` (Application context — там нет переопределения uiMode).

### 14.8 «При выходе ничего не записывается в трек»

Проверьте:
- `GO.trackIsRecordeed == true` (включена запись)
- Foreground service запущен (см. логи "BleMonitoringService onStartCommand")
- SharedPreferences `app_state` содержит `device_mac`, `current_track_id`, `cps_2_doze` — `MainActivity.performExit` это пишет ДО `startForegroundService`

### 14.9 «Сервис не цепляет advertising-пакеты»

Это известный лимит Android для background BLE scan. См. раздел 17.

---

## 15. Сборка и подпись

### 15.1 Конфигурация

**`gradle.properties`:** `org.gradle.java.home` указывает на JDK 17 (Linux) или JDK 21 (Windows JBR Android Studio). Перед коммитом **вернуть Linux-путь автора** — иначе CI на Linux сломается.

**`build.gradle (BluZ module)`:** `kotlin { jvmToolchain(21) }` (Windows) или `jvmToolchain(17)` (Linux). Тоже вернуть перед коммитом.

**`local.properties`:** SDK path + `MAPKIT_API_KEY` + параметры подписи (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). **НЕ коммитить** (в `.gitignore`).

**Подпись:** `signingConfigs.BluZ` читает путь к `.jks`, пароли и alias из `local.properties` через хелпер `getLocalProperty(...)`. Если `KEYSTORE_FILE` не задан — конфиг пустой: `debug`/`releaseTest` подписываются debug-ключом и собираются как обычно, а `assembleRelease` без этих параметров завершится ошибкой подписи. Секретов в `build.gradle` нет.

### 15.2 Варианты сборки

| Variant | minify | shrinkResources | debuggable | signing | Назначение |
|---|---|---|---|---|---|
| `debug` | нет | нет | да | debug | разработка |
| `releaseTest` | нет | нет | **да** | debug | RC, удобно ловить краши |
| `release` | да | да | нет | BluZ-key | production |

### 15.3 Команды

```bash
# Из C:\Bluz\BluZ\Android\BluZ_LPM\
./gradlew assembleDebug
./gradlew assembleReleaseTest
./gradlew assembleRelease

# APK лежат в BluZ/build/outputs/apk/{debug|releaseTest|release}/

# Установка через adb
adb install -r path/to/apk
```

### 15.4 ABI

Только `arm64-v8a` (см. `splits.abi` в build.gradle). x86/x86_64 не собираются — это сэкономило ~40% размера APK.

---

## 16. Отладка

### 16.1 Логи

Tag — `BluZ-BT` для большинства мест. В коде:
```kotlin
Log.d("BluZ-BT", "Frame start: type=$frameType mtu=$mtuLen pulses=$totalPulses")
GO.drawLOG.appendAppLogs("Описание", level)  // в in-app лог
```

**Логи в реальном времени:**
```bash
adb logcat -s BluZ-BT
```

**Только падения:**
```bash
adb logcat -d *:E | grep AndroidRuntime
```

### 16.2 In-app лог

`LogActivity` показывает накопленный `GO.appLogBuffer` с цветовой кодировкой. Открывается из Settings → «Журнал событий». Уровень логирования настраивается полем «Уровень логирования (0–5)»:
- 0: только ошибки
- 1: ошибки + инфо
- 2: + предупреждения
- 3: + успех
- 4: + отладка
- 5: + трасс

### 16.3 Логи BLE-фреймов

Поставить `Log.d("BluZ-BT", "Frame: ...")` в `BluetoothInterface.processIncomingPacket` после успешной сборки. Или включить уровень debug в `appLogLevel = 4`, тогда лог придёт в in-app журнал.

### 16.4 Запись логов в файл

В Settings есть кнопка «Сохранить App-лог» → `/Documents/BluZ/applog_*.html`. HTML с цветами.

### 16.5 Просмотр БД

```bash
adb root        # на rooted-устройстве
adb shell
cd /data/data/ru.starline.bluz/databases/
sqlite3 dosimeter.db
.schema
SELECT * FROM tracks LIMIT 10;
```

Или через Android Studio: View → Tool Windows → App Inspection → Database Inspector (требует USB).

---

## 17. Что в работе и известные ограничения

### 17.1 Прошивка прибора

- **Прибор не передаёт накопленный спектр в режиме «запись остановлена»** — шлёт только frame type 0 без spectrumData. Команда `5u` (запрос истории) даёт `historyData` (сохранённое во flash). Если нужно живой буфер при остановленной записи — требуется новая команда в прошивке.
- **Background BLE scan throttling.** Android может троттлить background scan. `BleMonitoringService` иногда не сразу цепляет advertising. Решений на стороне приложения нет; можно сократить restart-интервал (сейчас 5 мин) или использовать PendingIntent-scan.

### 17.2 Архитектурный долг

- `globalObj` (`GO`) — слишком большой синглтон. Переход на Hilt + ViewModels потребует серьёзной переработки.
- Часть полей `lateinit` без guard — может крашить если обращение раньше биндинга. Постепенно переводим на nullable.
- `HistoryFragment.kt` оставлен в репозитории, но не используется (с UX-итерации 2026-05-26). Можно удалить.
- Yandex MapKit API key захардкожен через `BuildConfig` — при смене пакета нужно перерегистрировать.

### 17.3 UI/UX

- Landscape для большинства фрагментов не подогнан индивидуально — используется один и тот же layout как в portrait.
- Локализация только ru/en, фрагментарно (часть строк в кода).
- Темизация: токены `bz_*` покрывают почти всё, но кое-где в коде ещё `Color.RED` / `0xFF...` вместо ресурсов.

См. также `project_state.md` → раздел «Осталось» — там более актуальный список.

---

## 18. Генерация документации через Dokka

Автор подключает Dokka в свою ветку. После того как Dokka появится в `build.gradle`, генерация выполняется командой:

```bash
./gradlew dokkaHtml      # HTML
./gradlew dokkaGfm       # Markdown (GitHub-flavored)
```

Артефакты в `BluZ/build/dokka/html/` или `dokka/gfm/`.

### 18.1 Что Dokka берёт из кода

KDoc-комментарии (вариант Javadoc для Kotlin):

```kotlin
/**
 * Применяет конфигурацию из прибора к локальным prop*-полям и сохраняет.
 *
 * Вызывается, когда пользователь нажал Read в настройках или включён
 * автозагрузка-switch при выборе MAC.
 *
 * @see writeConfigParameters
 */
fun readConfigFormDevice() { ... }
```

**Теги:**
- `@param имя описание` — параметры
- `@return описание` — возвращаемое значение
- `@throws Тип описание` — исключения
- `@see` — ссылки на связанные сущности
- `@sample` — ссылка на пример использования (отдельный файл с функцией)
- `@since`, `@deprecated`

### 18.2 Где обязательно добавлять KDoc

- **Все публичные классы** — короткое описание назначения
- **Все публичные методы** с непрозрачным контрактом — что делает, аргументы, возврат, побочные эффекты
- **Поля `globalObj`** — что хранит и кто пишет/читает
- **Поля `DeviceFrame` / `HardwareConfig`** — единицы измерения, диапазоны

### 18.3 Где НЕ обязательно

- Внутренние мелкие helper-функции с понятным именем
- Self-evident getter-ы / setter-ы

### 18.4 Совет по стилю KDoc

Первое предложение — однострочное summary. Дальше — детали. Markdown работает (`**bold**`, списки, code-блоки).

---

## Куда смотреть дальше

| Если нужно… | Открой |
|---|---|
| Общее представление о приложении | этот документ |
| Подробное описание BLE-протокола | раздел 5 + `BLE_PROTOCOL.md` |
| API-документация по конкретному классу | Dokka HTML (после генерации) |
| Автокалибровка: алгоритм и формулы | `AutoCalib_PeakDetection_Math.md` + `BluZ_docs.md` 0.31 |
| Журнал UX-изменений | `BluZ_docs.md` раздел 0 |
| Что сделано / осталось | `project_state.md` |
| Архитектурные решения и причины | `BluZ_docs.md` разделы 2–4 |
| Известные баги и обходы | раздел 14 этого документа + `project_state.md` |

Удачи в погружении.
