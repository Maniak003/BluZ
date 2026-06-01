# BluZ — Состояние проекта

Последнее обновление: 2026-05-29 (автокалибровка стабилизирована, документация прошла)

---

## ТЕКУЩЕЕ СОСТОЯНИЕ (2026-05-29)

Автокалибровка Ra-226 **стабилизирована и задокументирована**. На последней итерации:

**Алгоритм добавлен/доделан:**
- 3 критерия сходимости компаратора (`tuneComparatorAtCurrentHv`): стандартный ratio<1.5, sharp-drop (3× падение и max<100), стабилизация (±30% подряд 2 итерации, max<100). Нужны для большого кристалла CsI 10×10×50 — естественный фон ~30 имп/с не даёт ratio упасть ниже 2-3, без доп. критериев фаза не завершалась.
- Финальный safety-step `+baseStep АЦП` (`finalizeCompWithSafetyStep`) после сходимости — гарантия что остаточная иголка не вылезет при изменении температуры/ВВ.
- Retry на read-back ВВ/comp: 5 сек → повтор `cmd_setup` → ещё 10 сек.
- Активное ожидание очистки буфера в фазе компаратора (`clearAndAccumulateByTime`) — до 5 сек или `spectrData.sum() < 100`.

**Режимы переделаны:**
- `PRIMARY` — полная (ВВ + комп + полином, ~2.5М имп)
- `HV_AND_COMPARATOR` — только аппаратные параметры без перезаписи полинома (~1М имп). Заменил старый `RECALIBRATION` — «повторная» с использованием сохранённой sensitivity была не нужна.
- `POLYNOMIAL_ONLY` — только полином (~1.5М имп), просит источник через отдельный `UserPrompt.PlaceSourceForPolynomOnly` (заголовок «Поднесите источник» вместо «Верните»).

**UI:**
- Все диалоги автокалибровки в стиле приложения (`dialog_bz_*.xml`): bz_bg, bz_text, bz_accent, шрифт bz_inter_bold. Хелпер `makeStyledBzDialog` обнуляет системный фон.
- Standalone кнопка «Подбор компаратора» рядом с «Автокалибровка по Ra-226» — `startComparatorOnly()`. Без UserPrompt'а, пользователь сам убирает источник.
- Universal preparation-диалог с дисклеймером «Автокалибровка может ошибаться…».

**Документация:**
- `AutoCalibrationController.kt` — KDoc на всех функциях, removed 8 dead constants, добавлена раскладка sendBuffer для `cmd_setup`.
- `AutoCalibrator.kt` — KDoc на всех функциях, поправлена несостыковка top-10 vs реальных top-20 кандидатов.
- `SettingsFragment.kt` — KDoc на новых хелперах (makeStyledBzDialog, showStyledMessage, launchControllerProcedure, showStandaloneComparatorTuningDialog).
- `BluZ_docs.md` раздел 0.31 — обновлены режимы, добавлены пункты 12–19 в историю эволюции (top-20 cands, активное ожидание, retry, 3 критерия, safety-step, новые режимы, prompt PlaceSourceForPolynomOnly, стилизация).
- Поправлены латиница/кириллица артефакты («compаратор» → «компаратор», «Pинициализация» → «Реинициализация» в drawSpecter.kt).

**Принцип Surgical Changes** (CLAUDE-2.md) — нарушал, был возвращён. Меняем только то, что попросили; новое поведение для отдельного режима — через новый sealed-вариант UserPrompt, а не модификацию глобального текста.

**Файлы кода (актуальные):**
- `AutoCalibrator.kt` — peak detection, pattern matching 82/352/609, linear fallback по паре с prominence-критерием.
- `AutoCalibrationController.kt` — 4 фазы, локальная sensitivity, retry read-back, 3 критерия комп, safety-step.
- `Mtrx.kt` — без побочных эффектов записи в GO, флаг `solved`.
- `SettingsFragment.kt` — UI диалогов автокалибровки (стилизованные XML).
- `setup_layout.xml` — кнопки `buttonAutoCalibrate` и `buttonTuneComparator`.
- `res/layout/dialog_bz_autocalib_*.xml`, `dialog_bz_message.xml` — стилизованные диалоги.

**Открытые вопросы:**
- Боевая валидация большого кристалла CsI 10×10×50 после добавления 3 критериев сходимости — должна происходить при следующем запуске; если сходимость нестабильна, возможно нужно понизить порог `STABLE_TOL` или продлить количество стабильных итераций.
- Финальная подгонка компаратора (фаза 4) на боевом железе пока не протестирована — все прошлые тесты падали раньше неё. Сейчас алгоритм должен дойти до неё, потому что предыдущие фазы стабилизированы.

---

Этот файл — единственный источник правды о состоянии проекта. Поддерживается в актуальном состоянии при каждой итерации.  
Связанная техническая документация: [BluZ_docs.md](BluZ_docs.md).

---

## Сделано (Phase 1+2+3)

### Phase 1+2 — BLE-рефакторинг

- `BluetoothInterface.kt` переписан на `SharedFlow<DeviceFrame>` / `StateFlow<BleStatus>`
- `DeviceViewModel` подключён (MVVM)
- MapKit lifecycle исправлен (onStart/onStop)
- APK debug: `C:\Bluz\BluZ-debug.apk`

### Phase 3 — Infrastructure

- `res/values/colors.xml` — bz_* токены (22 цвета), `values-night/` для тёмной темы
- `res/values/dimens.xml` — spacing, radius, sizes
- `res/values/themes.xml` — Material3 тема с bz_* цветами, тёмные status/nav бары
- `res/menu/bottom_nav_menu.xml` — 5 пунктов: Спектр / История / Доза / Карта / Настройки
- `res/drawable/ic_nav_*.xml` — 5 stroke-иконок навигации
- `res/color/bz_nav_icon.xml`, `bz_nav_text.xml` — селекторы цветов
- `res/layout/activity_main.xml` — StatusStrip + ViewPager2 + кастомная BottomNav (top-indicator)
- `res/layout-land/activity_main.xml` — StatusStrip + ViewPager2 + NavigationRail

### Phase 3 — Restyling

- `spectr_layout.xml`, `history_layout.xml`, `dozimetr_layout.xml`, `map_layout.xml`, `log_layout.xml` — тёмный фон, bz_* цвета, полупрозрачная панель управления
- `setup_layout.xml` — карточный ScrollView с 9 секциями, массовая замена цветов

### Phase 3 — Kotlin

- `NumberAdapter.kt` — 6 → 5 вкладок, возвращает конкретные классы фрагментов
- `buttonColor.kt` — nav-методы no-op, остался `setSpecterColor`
- `globalObj.kt` — убраны `btnSetup` / `btnMap` lateinit, `bColor = val`
- `MainActivity.kt` — старые 6 кнопок заменены на `setupNavigation()` + `onBackPressed()`

### Phase 3 — Fragment split (2026-05-23)

- `SpectrumFragment.kt` — pagerFrame 0 (Spectrum, touch handler, MLEM/SMA/MEDIAN, calibrate)
- `HistoryFragment.kt` — pagerFrame 1 (load/save/clear history)
- `DoseFragment.kt` — pagerFrame 2 (clear dose)
- `BluZMapFragment.kt` — pagerFrame 3 (MapKit lifecycle, redrawtMap, setupMapClickListener)
- `SettingsFragment.kt` — pagerFrame 4 (все настройки, color picker, write to device, диалоги)
- `LogFragment.kt` — доступен из Settings через LogActivity
- `NumberFragment.kt` — УДАЛЁН
- `gpsLocation.kt` — fix: `ContinuousLocationManager(context: Bundle → Any?)`

### Phase 3 — Rotation support (2026-05-23, проверено)

- `AndroidManifest.xml` — нет `configChanges`, Activity пересоздаётся при повороте
- `App.kt` — draw-объекты (`drawSPECTER` / `HISTORY` / `DOZIMETER` / `LOG` / `CURSOR` / `Examp`) живут в `Application`, переживают пересоздание Activity. Данные спектра / истории сохраняются.
- `SpectrumFragment` / `HistoryFragment` / `DoseFragment` — `OnGlobalLayoutListener` пересоздаёт bitmap под новые размеры

### Phase 3 — Logs access (2026-05-23, проверено)

- `LogActivity.kt` — отдельная активность, хостит `LogFragment` через `FragmentContainerView`
- `activity_log.xml` — заголовок "Logs" + ImageButton back + контейнер
- `setup_layout.xml` — кнопка `buttonLogs` в карточке "ДЕЙСТВИЯ"
- `AndroidManifest.xml` — `LogActivity` зарегистрирована с `parentActivityName=".MainActivity"`

### Phase 3 — Database cleanup (2026-05-23)

- `AppDatabase.kt` — удалены все 3 миграции (были кривые: `MIGRATION_2_3` имел `Migration(1,3)`, колонки SQL не совпадали с entity по camelCase/snake_case, не было `curActive`). `fallbackToDestructiveMigration(true)` — старые БД сносятся при апгрейде. Решено пользователем: «пользователей мало, переживут».

### Release-сборка (2026-05-24)

- Сборка `releaseTest` APK → `C:\Bluz\BluZ-releaseTest.apk` (130 МБ)
- Установка на Xiaomi 11T — подтверждена пользователем ✓
- Документация исходного кода: `C:\Bluz\BluZ_docs.md` (22 раздела, полная, точная)

---

## UX-итерация по обратной связи (2026-05-25)

Подробности — раздел 0 в `C:\Bluz\BluZ_docs.md`. Все правки проверены пользователем.

1. **StatusStrip восстанавливается после поворота** — `MainActivity.restoreStatusStripFromState()` берёт температуру / АКБ / CPS из кэшированных `GO.tempMC` / `battLevel` / `pulsePerSec` / `overloadFlag`. Вызывается в `onCreate` после `findViewById` для `txtStat1..3, txtCompMED, txtIsotopInfo`. Защита от первого запуска: `if (GO.battLevel <= 0f) return`.
2. **Калибровка АКБ** — кнопка `buttonCalibrateBatt` в карточке "ДЕЙСТВИЯ" в `setup_layout`. `SettingsFragment.showBatteryCalibrationDialog()`: диапазон 2.00–5.00 В, принимает `.` и `,` (`DigitsKeyListener`). Отправка через `sendCommand(7u)` + float в `sendBuffer[4..7]`. Старый click на `GO.txtStat1` удалён.
3. **Ночной режим карты = тема** — удалены `cbNightMapMode` / `nightMapModeEnab` / `propNightMode`. `BluZMapFragment:488`: `GO.map?.isNightModeEnabled = !ThemePrefs.isDayTheme(requireContext())`.
4. **Таймер времени с днями** — `globalObj.formatClock(totalSec, showSeconds)`. Флаги `clockShowSecondsSpec` / `clockShowSecondsHist` независимы. Клик на `bzRecClock` (Spectrum) / `bzHistDuration` (History).
5. **Клик по дозе → переключение мкР / мкЗв** — `MainActivity.toggleDoseUnits()` + `applyDoseReadouts()`. Сохраняет `GO.unitsMess` в SharedPreferences `propUnits`. Кликабельны: `bzSpecDoseValue/Unit`, `bzSpecAvgValue/Unit`, `bzDoseHeroValue/Unit`, `bzDoseAvgLabel`.
6. **Автоскейл единиц мк → м → база** — `formatDoseScaled(microR, useSievert)`. Пороги 1 000 и 1 000 000. Применяется во всех dose-readouts.
7. **Маркер местоположения на карте** — красный крест 40×40 px (`createMyLocationCrossImage` → `ImageProvider.fromBitmap`) + серый полупрозрачный круг радиуса `location.accuracy`. `updateMyLocationOnMap(location)`. Без обводки, fillColor `0x40808080`.
8. **GPS включается при открытии карты** — `onStart()` зовёт `startLocationUpdates()`, `onStop()` останавливает (если `!trackIsRecordeed`). `locationManager` теперь создаётся всегда в `onViewCreated`.
9. **Защита от mapObjects.clear()** — после каждого clear (создание/удаление/выбор трека) обнуляются `myLocationCross/Accuracy`. В `updateMyLocationOnMap` проверка `isValid`. Решает краш `Native object's weak_ptr expired`.
10. **Курсор спектра пересоздаётся при повороте** — флаг `GO.drawCursorObjectInit` (по схеме `drawObjectInit`). `OnGlobalLayoutListener` на `cursorView` в `SpectrumFragment`. Bitmap создаётся под актуальные width×height.
11. **Курсор до низа графика в portrait** — баг в `drawCursor.showCorsor`: `drawLine(x, 0, x, HSize)` → `VSize`. HSize — ширина, VSize — высота.

**Принцип:** при следующих UX-улучшениях dose / clock — переиспользовать `applyDoseReadouts()` / `formatClock()` / `formatDoseScaled()`, не дублировать форматирование inline. Маркер местоположения и круг точности — единственные «вечные» MapObject-ы; все остальные ставятся через `redrawtMap` → `mapObjects.clear()`. После каждого clear обязательно `resetMyLocationMarkers()`.

---

## UX-итерация по обратной связи (2026-05-26)

Подробности — разделы 0.12–0.19 в `C:\Bluz\BluZ_docs.md`. Все правки проверены пользователем.

12. **Реорганизация bottom nav** — убран `tab_history`, добавлен `tab_exit` с иконкой `ic_bz_exit.xml`. `NumberAdapter` теперь 4 страницы (Spectrum / Dose / Map / Settings). Exit-таб — не страница, а действие → `performExit()`. Аналогично для landscape rail.
13. **Swipe-pager внутри Spectrum** — `specBottomPager` (ViewPager2) с двумя страницами `bz_spec_bottom_meas` / `bz_spec_bottom_history`. При swipe одновременно переключается chart card сверху: `specterView ↔ historyView`, скрываются `cursorView`, `specToolbar`, `buttonSpecterSS`. Меняются title и subtitle. Лениво инициализируется bitmap истории при первом показе (view был `gone` → 0×0).
14. **Guard в drawHistory.redrawSpecter** — добавлен `if (!this::histCanvas.isInitialized || HSize <= 0 || VSize <= 0) return`. Фикс краша `lateinit property histCanvas has not been initialized`.
15. **Общий аптайм в статусбаре** — `bzClockValue` всегда показывает `GO.messTm` (флаг `clockShowSecondsHist`), независимо от вкладки. `bzRecClock` (нижний таймер Spectrum tab) — отдельно `GO.spectrometerTime` (`clockShowSecondsSpec`).
16. **Из bz_spec_bottom_history.xml убран таймер** — `bzHistDuration` удалён, размер `bzHistIntegralValue` / `bzHistAvgCpsValue` поднят до 18sp, отступ между блоками 20dp. В `BottomSwipeAdapter`: `GO.bzHistDuration = null`.
17. **Подсказки про свайп** — `bzRecLabel` → «ИДЁТ ИЗМЕРЕНИЕ · СВАЙП ВЛЕВО — ПРОСМОТР ИСТОРИИ»; история → «ИСТОРИЯ · СВАЙП ВПРАВО — СПЕКТР». `letterSpacing=0.04`, `maxLines=1`.
18. **Тихое уведомление сервиса** — новый канал `ble_monitor_silent` с `IMPORTANCE_LOW`, старый `ble_monitor_channel` удаляется через `deleteNotificationChannel`. На уведомлениях: `setPriority(PRIORITY_LOW)`, `setOnlyAlertOnce(true)`, на `updateNotification` дополнительно `setSilent(true)`.
19. **Прочерки в уведомлении до первого пакета** — флаг `hasFirstPacket` в `BleMonitoringService`. До первого `dao.insertPoint()` → «CPS: — / — uR/h … Ожидание данных от прибора…». После — обычный формат.

**Принципы итерации:**
- Любой layout, где view стартует с `visibility="gone"`, должен инициализировать bitmap **лениво** — при первом показе, иначе `OnGlobalLayoutListener` отработает на 0×0 view и пропустит создание canvas.
- Уведомление сервиса меняем через смену ID канала + `deleteNotificationChannel` старого. Изменить importance существующего канала программно нельзя.

---

## UX-итерация по обратной связи (2026-05-26 вечер)

Подробности — разделы 0.20–0.27 в `C:\Bluz\BluZ_docs.md`. Все правки проверены пользователем.

20. **Discovery-режим сканирования + диалог выбора BluZ** — `BluetoothInterface.scanForDevices(durationMs, onFound, onComplete): Job` собирает все устройства; кастомный диалог `dialog_bz_scan.xml` с countdown 15с, динамическим списком, кнопками «Закрыть»/«Повторить поиск». Switch «Загружать настройки с BluZ» в карточке ПОДКЛЮЧЕНИЕ — `propAutoLoadDeviceCfg`. При выборе MAC и включённом switch — auto-подключение и `readConfigFormDevice() + writeConfigParameters() + reloadConfigParameters()`.
21. **Ping устройства (колокольчик в строке списка)** — `ic_bz_bell.xml`, `SettingsFragment.pingDevice(mac)`. Быстрый путь для уже подключённого MAC (мгновенно `sendCommand(6u)`). Для другого MAC: `destroyDevice + first { !Connected } + initLeDevice + first { Connected } + sendCommand(6u)`. BLE не позволяет «бросить в воздух» — всегда через GATT-write.
22. **Информация об изотопе на спектре** — `txtIsotopInfo` перенесён из hidden 1×1 в FrameLayout chart card (`top|end`), фон `#99000000`. Биндинг в `SpectrumFragment.onViewCreated`, обнуление в `onDestroyView`. `GO.txtIsotopInfo` → nullable. В `drawCursor` добавлен guard от выхода за границы массива при расчёте активности (фикс краша `ArrayIndexOutOfBoundsException: index=-30`).
23. **Безопасный парсинг полей настроек** — все `.toInt()`/`.toFloat()`/`.toShort()`/`.toUShort()` в Save/Write блоках заменены на `*OrNull() ?: default`. Точность измерения (aqureEdit) — через `Int.toShort()` truncate, чтобы значения >32767 не падали.
24. **CPS вместо дозы при отсутствии коэффициента** — флаг `hasCoef = GO.propCPS2UR > 0f` в `MainActivity.applyDoseReadouts()` и `bgService.updateNotification()`. Если `false` → значения как cps без авто-скейла.
25. **Реальный диапазон энергий в subtitle Spectrum** — `globalObj.updateSpecSubtitle()`. Первый канал = `propComparator / bitsChannel`, последний = `channels-1`, энергия через полином. Если калибровки нет → только «КАНАЛОВ · N». `coerceAtLeast(0)` защищает от отрицательных значений. Вызывается из `SpectrumFragment.onViewCreated`, page change callback, `reloadConfigParameters`, обработчика Save.
26. **Подписи «cps» у порогов тревоги** — `TextView` после `editLevel1/2/3` в `setup_layout.xml` (`bz_jetbrains_mono`, `bz_text_dim`, 11sp).
27. **Многоуровневая alarm-пилюля на вкладке Доза** — вместо NORMAL/OVERLOAD пять состояний: NORMAL → L1 → L2 → L3 → OVERLOAD. Цвет L2 — новый ресурс `bz_alert_l2` (`#FF6B35` light / `#FF8650` night). Стиль pill (фон + цвет текста) согласован с чипами L1/L2/L3 в карточке УРОВНИ в настройках.

**Принципы итерации:**
- `BluetoothInterface` теперь предоставляет два режима: legacy `startScan(EditText)` (первое устройство) и новый `scanForDevices(durationMs, onFound, onComplete): Job` (накопительный, отменяемый). Используем второй для UI выбора.
- При смене целевого MAC через `pingDevice` обязательно ждать `status.first { !Connected }` **до** `initLeDevice()`, иначе `first { Connected }` сработает от старого соединения.
- Все парсинги `String → Int/Float/UShort` в обработчиках Save/Write — через `*OrNull()` с дефолтом. Старый прямой парсинг был источником повторяющихся крашей.
- В `drawCursor` (и подобных индексных расчётах по spectrData) проверять границы массива перед чтением: `low >= 0 && high < n`.
- При любом выходе за границы физических величин (отрицательная энергия, отсутствующий коэф) — graceful fallback на упрощённое отображение (cps вместо дозы, «0 кэВ» вместо «-1 кэВ»).

---

## UX-итерация по обратной связи (2026-05-27)

Подробности — раздел 0.28 в `C:\Bluz\BluZ_docs.md`. Проверено пользователем.

28. **Фикс краша при повороте на вкладке «Спектр»** — `SpectrumFragment.onViewCreated`, callback `pager.registerOnPageChangeCallback.onPageSelected` падал с `NullPointerException` на `subtitleTv.text`/`titleTv.text`. Причина: id `bzSpecTitle`/`bzSpecSubtitle` есть только в portrait-layout (в landscape — компактный header без заголовка), `findViewById` в landscape возвращал `null`, а локальные переменные были объявлены как non-null. После поворота callback срабатывал сразу при биндинге pager-а → краш. Фикс: типы локальных `subtitleTv`/`titleTv` сделаны `TextView?`, доступ через safe-call.
29. **Индикатор RSSI в шапке** — между `bzBtDot` и `bzBtIcon` добавлен `TextView bzBtRssi` (10sp, mono-bold). `MainActivity.applyBtRssi()` ставит текст и цвет: «—» / `bz_text_dim` если `Current_RSSI <= -400`, иначе цифры + цвет по диапазонам `< -95` → красный, `-95..-75` → жёлтый, `≥ -75` → зелёный. Вызов в `applyFrameToUi`, `restoreStatusStripFromState`, и в `observeBleStatus` при потере соединения (с предварительным `Current_RSSI = -400`).
30. **Переключатель «АКБ в процентах» + цветовая индикация напряжения** — switch `bzSwitchBatteryPercent` в карточке «ВНЕШНИЙ ВИД» в `setup_layout.xml`. Поле `GO.showBatteryPercent` + `propShowBatteryPercent` (SharedPreferences). `globalObj.batteryPercent(voltage)` — табличная аппроксимация Li-ion (11 точек: 4.20→100, 4.10→90, 4.00→80, 3.90→70, 3.80→50, 3.70→30, 3.60→15, 3.50→10, 3.30→5, 3.00→1, 2.90→0) с линейной интерполяцией. `globalObj.formatBattery(voltage)` — `XX%` или `X.XXV`. `MainActivity.applyBatteryDisplay(voltage)` — текст + цвет аналогично RSSI: `≥3.3В` зелёный, `3.0..3.3В` жёлтый, `<3.0В` красный. Цвет — всегда по физическому напряжению, не по проценту.
31. **Автокалибровка спектрометра по Ra-226** — кнопка «Автокалибровка по Ra-226» в карточке КАЛИБРОВКА под полями полинома, плюс отдельная кнопка «Подбор компаратора» рядом. Автокалибровка подбирает `propHVoltage`, `propComparator` и полином `propCoefXXX A/B/C` под цель «верх шкалы ≈3500 кэВ, пик 609 кэВ Bi-214 в канале `channels × 609/3500`». Три режима (`AutoCalibrationController.Mode`):
    - **Полная** (`PRIMARY`) — ВВ + компаратор + полином, ~2.5М импульсов
    - **Высокое и компаратор** (`HV_AND_COMPARATOR`) — ВВ + компаратор без перезаписи полинома, ~1М импульсов
    - **Только полином** (`POLYNOMIAL_ONLY`) — пересчёт энергетической шкалы при текущих ВВ и комп, ~1.5М импульсов

    Время в диалоге не указывается — оно зависит от активности источника (5–30 мин). В диалоге показывается количество импульсов накопления — оно информативнее.

    **Якорные пики на маленьком детекторе (NaI ⌀10×40 / CsI ~10×10×50)**: `82 / 352 / 609` кэВ (высокоэнергетические 1120/1764 неразличимы в шуме на этом размере кристалла). Якорная пара fallback для линейного fit: `82 / 609`. Вспомогательные: `186 / 242 / 295`.

    **4 фазы алгоритма**:
    1. **Компаратор без источника при V=100** — итеративное повышение с переменным шагом (×4/×2/×1 в зависимости от ratio noise/plateau). Три критерия сходимости: ratio<1.5, sharp-drop (3× падение и max<100), стабилизация (±30% подряд 2 итерации). Накопление по времени (30 сек/итерация) — нужны 3 критерия для большого кристалла CsI 10×10×50 (фон ~30 имп/с не даёт ratio упасть ниже 2-3). Финальный safety-step `+baseStep АЦП`.
    2. **Bracket scan ВВ V=500 и V=100** — накопление по 500к импульсов на каждом, поиск пика 609 справа налево, линейная интерполяция к target_ch = `channels × 609/3500`. Возвращает `BracketResult(tunedV, nearestV, nearestCh)` для refine.
    3. **3 итерации refine**: накопление 500к → `AutoCalibrator.analyze` → `predictChannel(609)` → корректировка V через **локальную sensitivity** `(curCh - prevCh) / (curV - prevV)` с clamp `|deltaV| ≤ |currV - prevV|`. На первой итерации prev = ближайшая bracket-точка.
    4. **Финальная подгонка компаратора при готовом V** — повтор фазы 1 без принудительной установки V=100. Иголка при финальном V тоньше, comp должен быть меньше чтобы не отрезать первые каналы.

    В режиме `HV_AND_COMPARATOR` пропускается фаза 3 (полином не пересчитывается, в `pendingResult` возвращаются исходные коэффициенты). В режиме `POLYNOMIAL_ONLY` пропускаются фазы 1, 2, 4 — только одна итерация полинома при текущих ВВ/комп.

    **Файлы**: `AutoCalibrator.kt` (peak detection + pattern matching на тройку 82/352/609 на топ-20 кандидатов с sanity-проверкой монотонности и `|A| ≤ 5e-3` + линейный fallback по паре + sealed Result), `AutoCalibrationController.kt` (корутинная машина 4 фаз через StateFlow + `requestUserAction` + `applyHvAndClearSpectrum` + `accumulateByCounts` + `clearAndAccumulateByTime` с активным ожиданием очистки буфера + retry на read-back).

    **UI**: 4 стилизованных диалога в `res/layout/dialog_bz_*.xml` (выбор режима, инструкция/подтверждение, прогресс, результат) в палитре приложения (bz_bg/bz_text/bz_accent/bz_inter_bold). Хелпер `makeStyledBzDialog` создаёт `AlertDialog` и обнуляет системный фон через прозрачный `ColorDrawable`. Хелпер `showStyledMessage` — универсальный 2-кнопочный диалог. Обёртка `launchControllerProcedure` объединяет запуск, подписку на StateFlow, обработку UserPrompt'ов, прогресс-диалог и переход к диалогу результата.

    **UserPrompt'ы** (sealed class):
    - `RemoveSourceForCompTuning` — фазы 1 и 4
    - `PlaceSourceForCalibration` — фаза 2 (заголовок «Верните источник»)
    - `RemoveSourceForBackgroundCheck` — резерв
    - `PlaceSourceForPolynomOnly` — POLYNOMIAL_ONLY (заголовок «Поднесите источник», т.к. фаза 1 пропущена)

    **Защиты**:
    - Sanity монотонности полинома `2A·(N-1)+B > 0` — отбраковывает квадратичные подгонки с большим отрицательным A (математически правильные, но E(N-1)<0).
    - Активное ожидание очистки буфера после `cmd_clear_specter` (до 5 сек или sum<5% target) — без этого следующая фаза видела остаточный спектр от предыдущей.
    - Read-back подтверждение для HV и компаратора **с retry**: 5 сек → повтор `cmd_setup` → ещё 10 сек. При таймауте — warning, но процедура продолжается.
    - Локальная sensitivity вместо глобальной — около финального V зависимость V↔ch нелинейная, глобальная даёт промах в разы.
    - Пауза 3 сек после `requestUserAction` + cmd_clear_specter — пользователь нажимает «Готово» сразу, но физически перемещает источник за секунды.
    - Поиск пика 609 справа налево — на маленьком детекторе выше 609 ничего нет, первый встреченный пик гарантированно правильный.
    - Финальный safety-step компаратора `+baseStep АЦП` (≈3 канала спектра) — гарантия что остаточная иголка не вылезет при изменении температуры/ВВ.
    - 4 защитных Result: WeakSignal (мало счётов), NoRa226Pattern (тройка/пара не найдена), UnreasonableFit (полином нефизичный), Ok (всё ок).

    **Поправлен баг в `Mtrx.sysEq()`**: убраны побочные эффекты записи в `GO.propCoefXXX*` — раньше при `mainDet==0` затирали реальные коэффициенты во время перебора троек в pattern matching. Теперь `Mtrx.solved` + запись в GO делает только вызывающий код.

    **Карта BLE-команд в `BluZ_docs.md` раздел 0.31** актуализирована по прошивке (`Core/Inc/main.h` enum `commandTx`): `0=cmd_setup, 1=cmd_clear_specter, 2=cmd_startup_spectrometer, 3-8=...`.

**Принципы итерации:**
- Любая View, которой может не быть в одном из layout-вариантов (portrait/land), биндится как nullable + safe-call в коде. Особенно — те, на которые ссылаются callback-и, срабатывающие при создании view (page change, layout listener и т. д.).
- Цветовая раскраска значений в шапке: зелёный/жёлтый/красный через `bz_bt_on / bz_bt_warn / bz_bt_off` — общие пороги по «физике» (RSSI в dBm, напряжение в В), а не по производной величине (проценты). Делает поведение предсказуемым при переключении формата.
- Для SOC Li-ion использовать таблицу из 8–11 точек + линейную интерполяцию, а не полином: реальная разрядная кривая S-образная, парабола её не воспроизводит. Точность ±5% — достаточно для UI без fuel-gauge чипа.
- В `globalObj.kt` есть «токсичный» импорт `java.sql.Array` — перебивает `kotlin.Array`. Новые типы в файле — через `List<...>` либо явным `kotlin.Array<...>`.
- Для длительных операций с прибором (фазы автокалибровки) — корутины + `StateFlow` для UI, `Job.cancel()` для отмены. Используется `viewLifecycleOwner.lifecycleScope` чтобы корутина умирала вместе с фрагментом. Контроллер — отдельный класс с `start()`/`cancel()`/`applyPending()`/`resolveUserPrompt()`, чтобы UI просто подписывался на состояние без переплетения логики.
- BLE-команда `cmd_setup` (0u) собирается из `prop*` значений GO + `sendCommand(0u)` — единственный канал отправки конфигурации в прибор. Контроллер автокалибровки **дублирует** Save-блок `SettingsFragment` намеренно: рефакторить рабочий код ради новой функции — выходит за рамки задачи.
- **Калибровка детектора**: якорные пики Ra-226 нужно подбирать под размер кристалла — на ⌀10 мм только `82/352/609` кэВ, на больших ещё `1120/1764`. Менять `AutoCalibrator.anchorEnergiesKev` при смене железа.
- **Bracket scan + локальная sensitivity** — устойчивая схема для нелинейной зависимости V↔ch. Глобальная sensitivity из крайних точек bracket scan используется только на первой коррекции; дальше — локальная по двум последним измерениям.
- **Накопление по импульсам** (не по времени) — адаптивно к активности источника. Активное ожидание очистки буфера после `cmd_clear_specter` критично: фрейм с очищенным буфером приходит с задержкой ~1 сек, без ожидания следующая фаза видит остаточный спектр.
- **Sanity монотонности полинома** обязателен. `Mtrx.sysEq` математически решит любую систему 3 уравнений, но решение может быть физически невалидным (немонотонный квадратичный полином с большим |A|). Проверять `2A·(N-1)+B > 0`.
- Якорные пики Ra-226 в равновесии: 352 / 609 / 1764 кэВ. Если выбираем меньше/больше или другой источник — менять `AutoCalibrator.anchorEnergiesKev`. `auxEnergiesKev` используется только для score, не для решения.

---

## Осталось

### Средний приоритет

- [ ] Новые XML-лэйауты фрагментов по дизайну (`fragment_spectrum.xml` с readout cards и т. д.)
- [ ] Landscape-лэйауты фрагментов (сейчас `layout-land` только для `activity_main`)
- [ ] Проверить, что после поворота все вкладки работают (не только видимая — соседние фрагменты могут держать ссылки на старые View через `GO.*`)

### Низкий приоритет / Known bugs

- [ ] При смене пакета на release нужно обновить Yandex MapKit API key
- [ ] **Ожидает правок прошивки:** прибор не передаёт текущий накопленный спектр, когда запись остановлена (шлёт только `frame.frameType=0` без `spectrumData`). На стороне приложения сделать ничего нельзя без новой BLE-команды. Текущий протокол: `5u` — запрос исторического спектра (приходит в `historyData`, рисуется на вкладке History), но это, видимо, сохранённый ранее, а не текущий буфер. Решено 2026-05-25 не пилить workaround через подмену `historyData` → `drawSPECTER`, ждём прошивку.
- [ ] **Background BLE scan throttling (Android-баг):** `BleMonitoringService` иногда не сразу цепляет рекламные пакеты от прибора — нужно открыть приложение, чтобы данные «прицепились». Это известное системное ограничение Android на background BLE scan (троттлинг вне foreground). Решено 2026-05-26 не лезть. Если будет много жалоб от пользователей — исследовать, возможные направления: уменьшить restart-интервал scanner (сейчас 5 мин), обрабатывать `onScanFailed` с перезапуском, использовать PendingIntent-scan (система сама будит сервис при обнаружении).
