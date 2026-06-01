# BluZ — Техническая документация

**Версия:** 1.12 (code 12)  
**Пакет:** `ru.starline.bluz`  
**Target SDK:** 36 · Min SDK: 27 (Android 8.1+)  
**ABI:** arm64-v8a  
**Дата документа:** 2026-05-29

---

## Содержание

0. [Журнал изменений (UX-итерация 2026-05-25)](#0-журнал-изменений-ux-итерация-2026-05-25)
1. [Обзор приложения](#1-обзор-приложения)
2. [Архитектура](#2-архитектура)
3. [Точка входа — App](#3-точка-входа--app)
4. [MainActivity](#4-mainactivity)
5. [BLE-протокол и BluetoothInterface](#5-ble-протокол-и-bluetoothinterface)
6. [Модели данных DeviceFrame / BleStatus](#6-модели-данных-deviceframe--blestatus)
7. [DeviceViewModel](#7-deviceviewmodel)
8. [Глобальное состояние — globalObj / GO](#8-глобальное-состояние--globalobj--go)
9. [Фрагменты](#9-фрагменты)
   - [SpectrumFragment](#91-spectrumfragment)
   - [HistoryFragment](#92-historyfragment)
   - [DoseFragment](#93-dosefragment)
   - [BluZMapFragment](#94-bluzMapFragment)
   - [SettingsFragment](#95-settingsfragment)
10. [Фоновый сервис — BleMonitoringService](#10-фоновый-сервис--blemonitoringservice)
11. [Классы отрисовки](#11-классы-отрисовки)
    - [drawSpecter](#111-drawspecter)
    - [drawHistory](#112-drawhistory)
    - [drawDozimeter](#113-drawdozimeter)
    - [drawCursor](#114-drawcursor)
    - [drawLogs](#115-drawlogs)
    - [drawExmple](#116-drawexmple)
12. [Вычисления дозы — DoseCalculator](#12-вычисления-дозы--dosecalculator)
13. [Калибровка — Mtrx](#13-калибровка--mtrx)
14. [Экспорт данных — SaveBqMon](#14-экспорт-данных--savebqmon)
15. [База данных Room](#15-база-данных-room)
16. [GPS и карта](#16-gps-и-карта)
17. [Вспомогательные классы](#17-вспомогательные-классы)
18. [Тема и стили](#18-тема-и-стили)
19. [Ресурсы](#19-ресурсы)
20. [Манифест и разрешения](#20-манифест-и-разрешения)
21. [Сборка](#21-сборка)
22. [Формат BLE-фрейма (подробно)](#22-формат-ble-фрейма-подробно)

---

## 0. Журнал изменений (UX-итерации 2026-05-25 — 2026-05-29)

По обратной связи от пользователей внесён набор UX-правок в три итерации. Изменения распределены между `MainActivity.kt`, `SettingsFragment.kt`, `SpectrumFragment.kt`, `HistoryFragment.kt`, `DoseFragment.kt`, `BluZMapFragment.kt`, `bgService.kt`, `BluetoothInterface.kt`, `globalObj.kt`, `drawCursor.kt`, `drawHistory.kt`, `setup_layout.xml`, `spectr_layout.xml` (portrait+land), `activity_main.xml` (portrait+land), `colors.xml` (light+night) и новыми drawable/layout файлами `ic_bz_exit.xml`, `ic_bz_bell.xml`, `bz_spec_bottom_meas.xml`, `bz_spec_bottom_history.xml`, `dialog_bz_scan.xml`, `item_bz_scan_device.xml`.

- **0.1–0.11** — UX-итерация 2026-05-25 (status strip restore, калибровка АКБ, ночной режим карты = тема, форматы таймеров, переключение единиц дозы, автоскейл μ→м→база, маркер местоположения на карте, GPS при открытии, защита маркера от `mapObjects.clear()`, пересоздание курсора при повороте, портретная высота курсора).
- **0.12–0.19** — UX-итерация 2026-05-26 утро (реорганизация навигации с заменой History на Exit, swipe-pager внутри Spectrum, guard в drawHistory, общий аптайм в статусбаре, удаление таймера из истории, подсказки про свайп, тихое уведомление сервиса, прочерки до первого пакета).
- **0.20–0.27** — UX-итерация 2026-05-26 вечер (discovery-режим сканирования + диалог выбора BluZ, ping устройства колокольчиком, инфо об изотопе на спектре, безопасный парсинг полей настроек, CPS вместо дозы при отсутствии коэффициента, реальный диапазон энергий в subtitle Spectrum, подписи cps у порогов тревоги, многоуровневая alarm-пилюля).
- **0.28–0.30** — UX-итерация 2026-05-27 (фикс краша при повороте на вкладке «Спектр», индикатор RSSI в шапке, переключатель «АКБ в процентах» с табличной аппроксимацией Li-ion).
- **0.31** — UX-итерация 2026-05-27..29 (автокалибровка спектрометра по Ra-226: режимы PRIMARY / HV_AND_COMPARATOR / POLYNOMIAL_ONLY, стилизованные диалоги в палитре приложения, 3 критерия сходимости компаратора для большого кристалла, retry на read-back, отдельная кнопка standalone-подбора компаратора).

### 0.1 Восстановление StatusStrip после поворота экрана

**Проблема.** При повороте `MainActivity` пересоздаётся → `findViewById` находит новые пустые TextView → значения CPS, времени, температуры и напряжения АКБ пропадают до прихода следующего BLE-фрейма (~1 сек "мигания").

**Решение.** `MainActivity.restoreStatusStripFromState()` — после биндинга всех `bzXxxValue` View восстанавливает значения из закэшированных `GO.tempMC`, `GO.battLevel`, `GO.pulsePerSec`, `GO.overloadFlag`. Также вызывает `GO.showStatistics()` для часов и второй строки статистики.

```kotlin
private fun restoreStatusStripFromState() {
    if (GO.battLevel <= 0f) return  // фрейм ещё не приходил — placeholder из layout
    GO.bzCpsValue.text = GO.pulsePerSec.toString()
    GO.bzCpsValue.setTextColor(ContextCompat.getColor(this,
        if (GO.overloadFlag) R.color.bz_danger else R.color.bz_accent))
    GO.bzStatusStrip.setBackgroundColor(ContextCompat.getColor(this,
        if (GO.overloadFlag) R.color.bz_danger_soft else R.color.bz_surface))
    GO.bzTempValue.text = "%d°C".format(GO.tempMC.toInt())
    GO.bzBattValue.text = "%.2fV".format(GO.battLevel)
    GO.showStatistics()
}
```

Вызывается из `onCreate()` после инициализации `GO.txtStat1`, `txtStat2`, `txtStat3`, `txtCompMED`, `txtIsotopInfo` (которые нужны внутри `showStatistics()`), но до подписки на BLE-фреймы.

### 0.2 Калибровка АКБ возвращена в Settings

**Проблема.** В первой версии приложения по клику на отображение напряжения в шапке вызывался диалог калибровки АКБ. В Phase 3 редизайне `txtStat1` стал невидимым и функционал отвалился.

**Решение.**
1. Удалён старый click-handler с `GO.txtStat1` в `MainActivity.kt`.
2. В карточку «ДЕЙСТВИЯ» (`setup_layout.xml`) добавлена кнопка `buttonCalibrateBatt` "Калибровка АКБ" — отдельной строкой под `buttonLogs` / `buttonFind`.
3. `SettingsFragment.showBatteryCalibrationDialog()` — кастомный `AlertDialog`:
   - Заголовок: **"Калибровка АКБ"**
   - Строка: **"Напряжение: %.2f В"** (текущее `GO.battLevel`)
   - `EditText` с hint **"введите измеренное"** и `DigitsKeyListener.getInstance("0123456789.,")` — разрешён ввод только цифр и обоих разделителей
   - Парсинг: `raw.trim().replace(',', '.').toFloatOrNull()`
   - Диапазон **2.00 – 5.00 В включительно**; вне диапазона → Toast "Некорректное значение. Допустимо 2.00–5.00 В"
   - При валидном значении: float упаковывается в `sendBuffer[4..7]` через `ByteBuffer.allocate(4).putFloat()`, отправляется `sendCommand(7u)`, обновляется `GO.battLevel`, вызывается `GO.showStatistics()`
   - Если прибор не подключён (`!GO.BTT.connected`) → Toast "Прибор не подключён", диалог не открывается

### 0.3 Ночной режим карты = тема приложения

**Проблема.** Отдельный чекбокс «Ночной режим карты» в настройках дублировал глобальный выбор темы.

**Решение.** Полностью удалено:
- `CheckBox CBNightMode` в `setup_layout.xml`
- `lateinit var cbNightMapMode: CheckBox` и `var nightMapModeEnab: Boolean` в `globalObj.kt`
- Сохранение/чтение `propNightMode` в `writeConfigParameters` / `readConfigParameters`
- Слушатель чекбокса в `SettingsFragment.kt`

В `BluZMapFragment.kt` строка `488` теперь:
```kotlin
GO.map?.isNightModeEnabled = !ThemePrefs.isDayTheme(requireContext())
```

При переключении темы вызывается `requireActivity().recreate()` → `BluZMapFragment.onViewCreated()` отрабатывает заново → `isNightModeEnabled` пересчитывается автоматически.

### 0.4 Формат таймера с днями + клик-переключение в секунды

**Проблема.** Время замера показывалось как `hh+dd*24:mm:ss` → при работе больше суток получалось громоздкое "47:23:01". Также не было удобного способа увидеть точное число секунд.

**Решение.** Универсальная функция `globalObj.formatClock(totalSec, showSeconds)`:

| Условие | Формат | Пример |
|---------|--------|--------|
| `showSeconds == true` | `"$totalSec сек"` | `"86405 сек"` |
| `dd > 0` | `"%02d:%02d:%02d:%02d".format(dd, hh, mm, ss)` | `"02:03:45:12"` |
| иначе | `"%02d:%02d:%02d".format(hh, mm, ss)` | `"03:45:12"` |

Два независимых флага в `GO`:
- `clockShowSecondsSpec: Boolean = false` — для таймера спектра (`bzRecClock` + `bzClockValue` на вкладке Spectrum)
- `clockShowSecondsHist: Boolean = false` — для таймера дозиметра (`bzHistDuration` + `bzClockValue` на остальных вкладках)

Клики:
- `SpectrumFragment.onViewCreated`: `GO.bzRecClock?.setOnClickListener { GO.clockShowSecondsSpec = !GO.clockShowSecondsSpec; GO.showStatistics() }`
- `HistoryFragment.onViewCreated`: `GO.bzHistDuration?.setOnClickListener { GO.clockShowSecondsHist = !GO.clockShowSecondsHist; GO.showStatistics() }`

`showStatistics()` теперь использует `formatClock()` вместо прямого `String.format` — флаги учитываются автоматически. Время дозиметра и время спектрометра выводятся независимыми форматтерами.

### 0.5 Клик по дозе/мощности дозы → переключение мкР/ч ↔ мкЗв/ч

**Проблема.** Единицы менялись только через RadioGroup в настройках — неудобно.

**Решение.** Рефактор `applyFrameToUi` в `MainActivity.kt`: блок отрисовки dose-readouts вынесен в публичный метод `applyDoseReadouts()`. Он использует кэшированные `GO.pulsePerSec`, `GO.cps`, `GO.overloadFlag`, `GO.unitsMess`, `GO.propCPS2UR` — не зависит от фрейма и может быть вызван в любой момент.

Новый метод `MainActivity.toggleDoseUnits()`:
```kotlin
fun toggleDoseUnits() {
    GO.unitsMess = if (GO.unitsMess == 1) 0 else 1
    GO.PP.setPropInt(GO.propUnits, GO.unitsMess)  // персист в SharedPreferences
    applyDoseReadouts()                            // мгновенная перерисовка
}
```

Клик-листенеры на dose-readouts:
- `SpectrumFragment`: `bzSpecDoseValue`, `bzSpecAvgValue`, `bzSpecDoseUnit`, `bzSpecAvgUnit`
- `DoseFragment`: `bzDoseHeroValue`, `bzDoseHeroUnit`, `bzDoseAvgLabel`

Все они через `(activity as? MainActivity)?.toggleDoseUnits()` переключают глобальное состояние. Настройка в `Settings.rgUnit` отразит новое значение при следующем `reloadConfigParameters`.

### 0.6 Автомасштабирование единиц: мк → м → база

**Проблема.** При высоких полях излучения значения раздувались (`12500000.00 мкР/ч`) — нечитаемо.

**Решение.** Функция `MainActivity.formatDoseScaled(microR: Float, useSievert: Boolean): Pair<String, String>` — автоматически выбирает порядок единицы:

```kotlin
private fun formatDoseScaled(microR: Float, useSievert: Boolean): Pair<String, String> {
    var value: Float = if (useSievert) microR * 0.01f else microR
    val units: Array<String> = if (useSievert)
        arrayOf("мкЗв/ч", "мЗв/ч", "Зв/ч")
    else
        arrayOf("мкР/ч", "мР/ч", "Р/ч")

    val label: String; val fmt: String
    when {
        value >= 1_000_000f -> { value /= 1_000_000f; label = units[2]; fmt = "%.3f" }
        value >= 1_000f     -> { value /= 1_000f;     label = units[1]; fmt = "%.3f" }
        else                -> { label = units[0]; fmt = if (useSievert) "%.3f" else "%.2f" }
    }
    return Pair(fmt.format(value), label)
}
```

| Значение (мкР/ч) | мкР-семейство | мкЗв-семейство (×0.01) |
|---|---|---|
| 425 | `425.00 мкР/ч` | `4.250 мкЗв/ч` |
| 1 500 | `1.500 мР/ч` | `15.000 мкЗв/ч` |
| 250 000 | `250.000 мР/ч` | `2.500 мЗв/ч` |
| 2 500 000 | `2.500 Р/ч` | `25.000 мЗв/ч` |

Мощность дозы и средняя считаются независимо — могут оказаться в разных шкалах одновременно. Применяется ко всем dose-readouts: Spectrum hero, Dose hero, legacy `txtStat3`. Состояние pill «NORMAL/OVERLOAD» работает как раньше.

### Что осталось неизменным

- Сохранение `GO.unitsMess` — теперь происходит и через клик (`toggleDoseUnits`), и через "Save" в настройках. Чтение из SharedPreferences `propUnits` без изменений.
- RadioGroup `rgUnit` в SettingsFragment продолжает работать — пользователь может выбирать единицу как кликом, так и через настройки.
- `bzHistAvgCpsValue` (история — среднее CPS) автоскейлу **не** подвергается: это CPS, а не доза.
- `bzCpsValue` в StatusStrip — это `pulsesPerSec`, а не доза, тоже без шкалирования.

### 0.7 Маркер текущего местоположения на карте

**Проблема.** Карта не показывала, где пользователь физически находится — никакого индикатора текущей позиции.

**Решение.** В `BluZMapFragment` добавлены два MapObject-а:

- `myLocationCross: PlacemarkMapObject?` — красный крест 40×40 px, толщина 2 px, anti-aliased. Bitmap рисуется в `createMyLocationCrossImage()` → `ImageProvider.fromBitmap()`. Lazy-initialized.
- `myLocationAccuracy: CircleMapObject?` — круг радиуса = `location.accuracy` (метры). Без обводки, заливка `0x40808080` (серый, 25 % alpha). Рисуется только если `location.hasAccuracy() && location.accuracy > 0f`.

`updateMyLocationOnMap(location: Location)` — создаёт или обновляет геометрию маркеров. Вызывается:
- При первичной инициализации в `onViewCreated` (если есть `lastKnownLocation`)
- В callback `ContinuousLocationManager` — на каждое обновление GPS
- В `getLocationModern()` / `requestFreshLocationModern()` — fallback-пути

`onDestroyView()` обнуляет ссылки (маркеры привязаны к старому `MapView`, при пересоздании View будут созданы заново).

### 0.8 GPS включается при открытии карты

**Проблема.** До этой правки `startLocationUpdates()` вызывался **только** при старте записи трека. При обычном открытии карты GPS оставался выключен — маркер показывал кэшированную `getLastKnownLocation()`, часто устаревшую.

**Решение.**
- В `onViewCreated` `locationManager` теперь создаётся всегда (раньше — только если `lastKnownLocation != null`); сразу вызывается `startLocationUpdates()` — на случай, если `onStart` отработал до создания manager.
- `onStart()` дополнительно вызывает `GO.locationManager?.startLocationUpdates()` — каждый раз, когда вкладка карты становится видимой.
- `onStop()` вызывает `stopLocationUpdates()`, но **только если** `!GO.trackIsRecordeed` — при активной записи трека обновления продолжаются.

При первом холодном fix может пройти 5–15 секунд — это нормально для GPS.

### 0.9 Защита маркера от mapObjects.clear()

**Проблема.** Краш `Native object's weak_ptr for MapObject has expired` при создании / удалении / выборе трека.

**Причина.** В трёх местах фрагмента вызывается `GO.map?.mapObjects?.clear()` — он уничтожает **все** объекты на карте, включая `myLocationCross` и `myLocationAccuracy`. Kotlin-ссылки оставались, но указывали на разрушенные native-объекты с истёкшим `weak_ptr`. Следующий location callback пытался `cross.geometry = point` → краш на нативном уровне.

**Решение — двойная защита:**

1. После каждого `mapObjects.clear()` (3 места — удаление трека, создание нового, выбор из списка) добавлен вызов `resetMyLocationMarkers()`, обнуляющий обе ссылки. На следующем GPS-обновлении маркеры пересоздаются автоматически в `updateMyLocationOnMap`.

2. В начале `updateMyLocationOnMap` — проверка `isValid` как защитный слой:
   ```kotlin
   if (myLocationCross?.isValid == false) myLocationCross = null
   if (myLocationAccuracy?.isValid == false) myLocationAccuracy = null
   ```
   На случай, если `clear()` будет добавлен ещё где-то в будущем.

### 0.10 Bitmap курсора пересоздаётся при повороте экрана

**Проблема.** В портретной ориентации после поворота из landscape вертикальная линия курсора рисовалась смещённо, не там, где пользователь тапнул.

**Причина.** `GO.drawCURSOR` — singleton в Application, переживает пересоздание Activity при повороте. Его `init()` имел условие `if (!drawCursorInit)` — пересоздавал bitmap **только один раз**. После поворота `cursorView` получал новые размеры (`width`/`height`), а bitmap внутри `drawCURSOR` оставался от первой ориентации. `cursorCanvas` рисовал в системе координат старого canvas, а пиксели отображались на view с другими размерами.

Дополнительно: `init()` не вызывался при создании фрагмента — только лениво из `showCorsor()` по первому тапу.

**Решение — по схеме `drawSpecter`:**

- В `globalObj.kt` добавлен флаг `drawCursorObjectInit: Boolean = true` (рядом с `drawObjectInit` / `drawObjectInitHistory` / `drawDozObjectInit`).
- `drawCursor.init()` переписан:
  - Проверяет `!this::cursorView.isInitialized` — выходит, если view не привязан
  - Условие пересоздания: `GO.drawCursorObjectInit || !this::cursorBitmap.isInitialized`
  - При успешной инициализации сбрасывает флаг и обнуляет `oldX/oldY` (иначе `hideCursor` в новой ориентации стирал бы пиксели по координатам прошлой ориентации)
- В `SpectrumFragment.onViewCreated` добавлен `OnGlobalLayoutListener` для `cursorView` (по аналогии с уже существующим для `specterView`) — выставляет `GO.drawCursorObjectInit = true` и вызывает `init()`. Bitmap создаётся под актуальные `width × height` для каждой ориентации.

### 0.11 Вертикальный курсор до низа графика в портретной ориентации

**Проблема.** В портретной ориентации вертикальная линия курсора не доходила до низа спектра. В альбомной — выглядела правильно.

**Причина.** В `drawCursor.showCorsor()` была старая опечатка:
```kotlin
cursorCanvas.drawLine(x, 0.0f, x, HSize.toFloat(), aCursor);  // HSize — это ширина!
```
Линия рисовалась до Y = `HSize` (ширина canvas), а должно — до Y = `VSize` (высота).

- В landscape `HSize > VSize` → линия выходит за canvas → обрезается до `VSize` → выглядит правильно (повезло).
- В portrait `HSize < VSize` → линия физически короче высоты canvas → видно «не доходит».

**Решение.** Замена `HSize.toFloat()` → `VSize.toFloat()` в одной строке. `hideCursor()` уже использовал `VSize` — не трогал.

### 0.12 Реорганизация навигации: 4 страницы + кнопка «Выход»

**Что изменилось.** Bottom navigation теперь содержит 5 элементов: `tab_spectrum`, `tab_dose`, `tab_map`, `tab_settings`, **`tab_exit`** (новая иконка `ic_bz_exit.xml` — дверной проём со стрелкой вправо). Вкладка `tab_history` удалена из nav-бара и из `NumberAdapter` (теперь 4 страницы). Аналогично в landscape — `rail_*` (включая `rail_exit`).

**Маппинг таб → действие** (`MainActivity.setupNavigation`):
- Если `index >= pageCount` (т.е. 4 — это «Выход») → `performExit()`
- Иначе → обычный `setCurrentItem(index)`

`HistoryFragment.kt` оставлен в репозитории, но в `NumberAdapter` не используется. Канвас истории, кнопки Load/Save/Clear и числовые показатели перенесены внутрь `SpectrumFragment` — доступ через swipe.

`applyStatVisibility` в `setupNavigation` обновлён под новые индексы:
- `0` (Спектр) → `GO.showStatistics()`
- `1` (Доза) → `drawDOZIMETER.Init()` + `redrawDozimeter()` + `showStatistics()`
- остальное → ничего

### 0.13 Двухуровневая навигация на вкладке «Спектр»: верхний chart + нижний swipe-pager

**Идея.** Вкладка Spectrum теперь содержит два визуально связанных режима — **живой спектр** и **накопленный спектр истории**. Переключение единым жестом: горизонтальный свайп в нижней строке pager-а синхронно меняет и chart card сверху.

**Layout-структура** (portrait и landscape `spectr_layout.xml`):

```
┌─────────────────────────────────────────────────────────┐
│ Title row: "Гамма-спектр" / "История"   [Save] [Start]  │  ← title (Start/Stop теперь здесь, как в landscape)
├─────────────────────────────────────────────────────────┤
│ Hero readouts: МОЩНОСТЬ ДОЗЫ / СРЕДНЯЯ                  │  ← общие для обеих страниц
├─────────────────────────────────────────────────────────┤
│ Chart card                                              │
│  ┌─────────────────────────────────────────────┐        │
│  │ specterView (VISIBLE на page 0)             │        │
│  │ historyView (VISIBLE на page 1)             │        │  ← FrameLayout-стек, виден один
│  │ cursorView  (GONE на page 1)                │        │
│  └─────────────────────────────────────────────┘        │
│ Toolbar SMA/MED/MLEM/Calibrate/Clear (GONE на page 1)   │  ← id="@+id/specToolbar"
├─────────────────────────────────────────────────────────┤
│ specBottomPager (ViewPager2, 2 страницы):               │  ← horizontal swipe
│ - page 0 (bz_spec_bottom_meas):    label + clock        │
│ - page 1 (bz_spec_bottom_history): label + Integral/CPS │
│                                    + buttons L/S/Clear  │
└─────────────────────────────────────────────────────────┘
```

**`SpectrumFragment.BottomSwipeAdapter`** — `RecyclerView.Adapter<VH>` с двумя view-types. В `onBindViewHolder` для каждой страницы перепривязывает соответствующие `GO.*` references (`bzRecClock` / `bzHistIntegralValue` / `bzHistAvgCpsValue`) и навешивает onClick.

**`pager.registerOnPageChangeCallback`** — при `onPageSelected(position)`:
- `position == 1` (история): `specterView GONE`, `historyView VISIBLE`, `cursorView GONE`, `specToolbar GONE`, `buttonSpecterSS GONE`. Title → «История», subtitle → «ИСТОРИЯ · НАКОПЛЕННЫЙ СПЕКТР».
- `position == 0` (спектр): всё обратно.

При первом переходе на page 1 `historyView` ещё не имел layout-размеров (был `GONE` → `width=height=0`). Решение — ленивая инициализация: если `view.width > 0 && height > 0` → сразу `init() + redrawSpecter()`, иначе одноразовый `OnGlobalLayoutListener` ждёт layout.

### 0.14 Guard на histCanvas в `drawHistory.redrawSpecter`

**Проблема.** Краш `lateinit property histCanvas has not been initialized` при свайпе на историю в первый раз.

**Причина.** `historyView` стартовал с `visibility="gone"` → `init()` срабатывал на 0×0 view, видел `HSize==0 || VSize==0` и пропускал создание `histBitmap`/`histCanvas`. Потом `redrawSpecter()` падал на `histCanvas.drawLine`.

**Решение.** В `drawHistory.redrawSpecter` добавлен guard в начале (по аналогии с `drawSpecter`):
```kotlin
if (!this::histCanvas.isInitialized || HSize <= 0 || VSize <= 0) return
```

### 0.15 Статусбар: общий аптайм прибора независимо от вкладки

**Что было.** В `globalObj.showStatistics()` логика разветвлялась: на вкладке «Спектр» (`viewPager.currentItem == 0`) `bzClockValue` показывал время работы спектрометра, на остальных — время дозиметра (общий аптайм). При переключении вкладок цифра «прыгала».

**Что стало.** `bzClockValue` **всегда** показывает `GO.messTm` (общий аптайм, флаг `clockShowSecondsHist`). `bzRecClock` (нижний таймер на странице «Спектр») продолжает показывать `GO.spectrometerTime` (флаг `clockShowSecondsSpec`) — это время работы именно спектрометра. `bzHistDuration` удалён из layout-а истории (см. 0.16).

### 0.16 Из нижней строки «История» убран таймер, подняты Интеграл и Ср.CPS

В `bz_spec_bottom_history.xml`:
- Удалён `TextView bzHistDuration` (таймер аптайма) — теперь только в статусбаре сверху
- Размер значений `bzHistIntegralValue` и `bzHistAvgCpsValue` увеличен с 14sp до 18sp (как был у удалённого таймера), отступ между блоками увеличен до 20dp

В `SpectrumFragment.BottomSwipeAdapter.onBindViewHolder` для page 1: `GO.bzHistDuration = null` (поле в `globalObj` остаётся nullable — `globalObj.showStatistics()` уже использует `bzHistDuration?.text = ...`, ничего не падает).

### 0.17 Подсказки про свайп

В метках:
- `bz_spec_bottom_meas`: `bzRecLabel` → **«ИДЁТ ИЗМЕРЕНИЕ · СВАЙП ВЛЕВО — ПРОСМОТР ИСТОРИИ»**
- `bz_spec_bottom_history`: метка → **«ИСТОРИЯ · СВАЙП ВПРАВО — СПЕКТР»**

`letterSpacing` уменьшен с `0.12` до `0.04`, добавлен `maxLines="1"`.

### 0.18 Тихое уведомление BleMonitoringService

**Что было.** Канал `ble_monitor_channel` с `IMPORTANCE_DEFAULT` → каждое обновление уведомления (раз в секунду при активном сканировании) звякало звуком и показывало heads-up.

**Что стало.** В `bgService.kt`:

```kotlin
private fun createNotificationChannel() {
    val manager = getSystemService(NotificationManager::class.java)
    // Удаляем старый канал — изменить importance существующего нельзя (Android спецификация).
    try { manager.deleteNotificationChannel("ble_monitor_channel") } catch (_: Exception) {}

    val channel = NotificationChannel(
        "ble_monitor_silent",
        "Запись трека (тихое)",
        NotificationManager.IMPORTANCE_LOW   // без звука, без heads-up; иконка в шторке остаётся
    ).apply {
        description = "BLE-сканирование и запись точек трека в фоне"
        setSound(null, null)
        enableVibration(false)
        enableLights(false)
    }
    manager.createNotificationChannel(channel)
}
```

Оба билдера (`createNotification`, `updateNotification`) обновлены:
- ID канала → `ble_monitor_silent`
- `setPriority(PRIORITY_LOW)` — для совместимости с pre-O
- `setOnlyAlertOnce(true)` — звук только один раз даже на DEFAULT-канале
- `setSilent(true)` в `updateNotification` — явный запрет alert-эффектов на каждое обновление

**Совместимость с уже установленным приложением.** При первом запуске сервиса после апдейта старый канал удаляется через `deleteNotificationChannel`. До этого момента у пользователя в настройках уведомлений приложения ещё может торчать старая запись.

### 0.19 Прочерки в уведомлении до первого BLE-пакета

**Что было.** При старте сервиса `updateNotification(0f, 0)` выводил ложные нули: `CPS: 0 / 0.00 uR/h ... RSSI: 0 dBm`.

**Что стало.** Флаг `private var hasFirstPacket: Boolean = false`. Три ветки в `updateNotification`:
- `!hasFirstPacket` → прочерки + строка «Ожидание данных от прибора…»
- первый пакет получен + магнитометр работает → формат с `Magnitude`
- первый пакет получен, магнитометра нет → формат без `Magnitude`

В `saveToTrack` после успешного `dao.insertPoint()`: `hasFirstPacket = true`. После перезапуска сервиса (например, через action `STOP_SERVICE` + новый запуск) флаг сбрасывается.

### 0.20 Discovery-режим сканирования + диалог выбора BluZ

**Что было.** Кнопка Scan в настройках цепляла первое же найденное устройство по имени `"BluZ"` и подставляла MAC в поле. Если рядом несколько приборов — выбирался случайный.

**Что стало.**

**`BluetoothInterface.scanForDevices(durationMs, onFound, onComplete): Job`** — discovery-режим:
- Накапливает все устройства с именем `"BluZ"` в `linkedSetOf<String>` (порядок обнаружения, без дублей)
- `onFound(mac)` вызывается на UI-потоке при каждом НОВОМ устройстве
- `onComplete()` — по таймауту или отмене (cancel() возвращённой Job)
- Старый `startScan(EditText)` / `stopScan` сохранены для совместимости

**Кастомный диалог `dialog_bz_scan.xml`** в `SettingsFragment.startDeviceDiscovery()`:
- Заголовок «Поиск BluZ» + countdown в скобках («(15с)»), countdown скрывается по завершению
- ScrollView с динамически добавляемыми строками устройств (`item_bz_scan_device.xml`)
- Внизу две кнопки в одном стиле: «Закрыть» + «Повторить поиск»
- `setOnDismissListener` отменяет `scanJob` и `scanCountdownJob`
- Длительность одного цикла: 15 сек (`scanDurationMs`)

**Иконка колокольчика `ic_bz_bell.xml`** в каждой строке списка — `pingDevice(mac)`. См. [0.21](#021-ping-устройства--опознавание-blu_z-в-эфире).

**Switch «Загружать настройки с BluZ»** в карточке «ПОДКЛЮЧЕНИЕ» (`switchAutoLoadDeviceCfg`):
- Поле `GO.autoLoadDeviceCfg: Boolean` + ключ `propAutoLoadDeviceCfg`
- `applySelectedMac(mac)`:
  - Если switch выключен → просто сохранение MAC + `tmFull.startTimer()`
  - Если включён → `initLeDevice()` + ждёт первый фрейм через `deviceFrames.first()` с таймаутом 15 сек, потом `readConfigFormDevice() + writeConfigParameters() + reloadConfigParameters()` + Toast «Настройки загружены с прибора»

### 0.21 Ping устройства — опознавание BluZ в эфире

**Назначение.** Колокольчик рядом с каждым MAC в диалоге поиска. Шлёт команду `find` (`sendCommand(6u)`) — прибор включает звук/вибро. Пользователь слышит сигнал и понимает, какой прибор соответствует MAC.

**`SettingsFragment.pingDevice(mac)`:**

1. **Быстрый путь:** `if (GO.BTT.connected && GO.LEMAC == mac) { sendCommand(6u); return }` — на повторный клик по тому же MAC ответ мгновенный, без переподключения.
2. **Переключение на другой MAC:**
   - `GO.tmFull.stopTimer()` + `GO.BTT.destroyDevice()` — рвём текущее GATT
   - Меняем `GO.LEMAC = mac` ПОСЛЕ destroy (иначе autoreconnect-таймер мог подцепиться к новому MAC сам)
   - `status.first { it !is Connected }` с таймаутом 2 сек — ждём пока StateFlow покинет Connected, иначе следующий `first { Connected }` сработал бы от старого состояния
   - `initLeDevice() + tmFull.startTimer()`
   - `status.first { it is Connected }` с таймаутом 10 сек → `sendCommand(6u)`

**Важное замечание (физика BLE).** Команда не может быть «брошена в воздух» — приложение → прибор всегда через connection-oriented GATT-write. Чтобы прибор отреагировал, требуется connection. Advertising broadcast работает только в обратную сторону (прибор → телефон). Это ограничение протокола, не баг.

### 0.22 Информация об изотопе на спектре

**Что было.** `txtIsotopInfo` существовал как hidden 1×1 в `activity_main.xml`. В `drawCursor.kt` он заполнялся при тапе по пику (если есть калибровка), но не отображался.

**Что стало.**

- `txtIsotopInfo` перенесён в FrameLayout chart card на странице Spectrum (`spectr_layout.xml` portrait + landscape), `layout_gravity="top|end"`, полупрозрачный чёрный фон `#99000000`, белый текст, `bz_jetbrains_mono_bold`.
- `GO.txtIsotopInfo: TextView` lateinit → `TextView? = null` nullable.
- Биндинг переехал из `MainActivity.onCreate` в `SpectrumFragment.onViewCreated`, обнуление в `onDestroyView`.
- `drawCursor.showCorsor()` управляет `visibility`: VISIBLE при найденном изотопе, INVISIBLE если изотоп не найден или калибровки нет.
- Формат: `"Cs-137 662кэВ"` или `"Cs-137 662кэВ · 1234 Бк"` (с активностью если в справочнике задана `Activity > 0`).

**Guard от выхода за границы массива** при расчёте активности (`drawCursor.kt`):
```kotlin
val low = isotop.Channel - GO.realResolution
val high = isotop.Channel + GO.realResolution
val n = GO.drawSPECTER.spectrData.size
if (isotop.Activity == 0 || low < 0 || high >= n) {
    GO.txtIsotopInfo?.text = "${isotop.Name} ${isotop.Energy}кэВ"  // без активности
} else {
    // окно [low..high] целиком внутри спектра — считаем активность
    ...
}
```

### 0.23 Безопасный парсинг полей настроек

**Что было.** Save / Write блоки парсили все числовые поля напрямую: `.toInt()`, `.toFloat()`, `.toShort()`, `.toUShort()`. Пустое поле → `NumberFormatException` → краш.

**Что стало.** Все парсинги в обоих блоках обёрнуты в `*OrNull() ?: default`:

```kotlin
GO.propLevel1 = GO.editLevel1.text.toString().trim().toIntOrNull() ?: 0
GO.propCPS2UR = GO.editCPS2Rh.text.toString().trim().replace(',', '.').toFloatOrNull() ?: 0f
GO.propHVoltage = (GO.editHVoltage.text.toString().trim().toIntOrNull() ?: 0).toUShort()
val aqureRaw = GO.aqureEdit.text.toString().trim().toIntOrNull() ?: 0
val convVal2 = ByteBuffer.allocate(2).putShort(aqureRaw.toShort()).array()
```

**Отдельный случай — точность измерения (aqureEdit).** Поле передаётся как `Short` (2 байта), но протокол ожидает unsigned 16-bit (0..65535). Прямой `Short.parseShort("33795")` падает (Short.MAX_VALUE = 32767). Решение: парсим как Int, потом `.toShort()` — это truncate без exception (биты `33795 = 0x8403` сохраняются, прибор читает как UShort и получает корректное значение).

### 0.24 CPS вместо дозы при отсутствии коэффициента

**Что было.** При `GO.propCPS2UR == 0f` все dose-readouts показывали `"0.00 мкР/ч"` — пользователь видел ложные нули и думал, что прибор не работает.

**Что стало.** В `MainActivity.applyDoseReadouts()` и `bgService.updateNotification()` добавлена ветка `hasCoef = GO.propCPS2UR > 0f`:
- **`hasCoef = true`** — старая логика: `formatDoseScaled` с авто-скейлом μ → м → база, единицы `мкР/ч` / `мкЗв/ч`.
- **`hasCoef = false`** — показываем CPS:
  - `bzSpecDoseValue` / `bzDoseHeroValue` = `pulsePerSec` (целое, `"%d"`)
  - `bzSpecAvgValue` / `bzDoseAvgLabel` = `avgCps` (`"%.2f"`)
  - Все unit-labels = `"cps"`
- `txtStat3` (legacy) — соответственно либо `"CPS:N (X.XX мР/ч) Avg:Y.YY мР/ч"`, либо `"CPS:N Avg:Y.YY cps"`.
- В уведомлении сервиса — аналогично, при `cps2doze <= 0` строка с дозой опускается.

**Pill «NORMAL/L1/L2/L3/OVERLOAD»** и цвет текста значения работают независимо от коэффициента (см. [0.27](#027-многоуровневая-alarm-пилюля-на-вкладке-доза)).

### 0.25 Subtitle на вкладке «Спектр»: реальный диапазон

**Что было.** Над «Гамма-спектр» статичный subtitle `"КАНАЛ · 2048 · 0 – 3 МэВ"` — не отражает реальных настроек.

**Что стало.** В `globalObj.updateSpecSubtitle()`:

```kotlin
val channels = when (GO.spectrResolution) {
    1 -> 2048; 2 -> 4096; else -> 1024
}
val (a, b, c) = коэф калибровки для текущего разрешения
if (a == 0f) {
    tv.text = "КАНАЛОВ · $channels"     // калибровки нет
    return
}
val bitsCh = if (GO.bitsChannel in 1..64) GO.bitsChannel else 20
val firstCh = (GO.propComparator.toInt() / bitsCh).coerceIn(0, channels - 1)
val lastCh = channels - 1
val eFirst = (a * firstCh * firstCh + b * firstCh + c).toInt().coerceAtLeast(0)
val eLast  = (a * lastCh * lastCh + b * lastCh + c).toInt().coerceAtLeast(0)
tv.text = "КАНАЛОВ · $channels · $eFirst – $eLast кэВ"
```

**Первый «живой» канал** = `propComparator / bitsChannel`:
- `propComparator` — порог компаратора в **отсчётах АЦП** (10-битное значение из BLE-фрейма, 0..1023)
- `bitsChannel` — отсчётов АЦП на 1 канал спектра (16..32, дефолт 20)

`coerceAtLeast(0)` защищает от отрицательных энергий при экзотических калибровках с `C < 0`.

**Когда обновляется.** Через флаги `GO.bzSpecSubtitle: TextView?` + `GO.bzSpecPageIsHistory: Boolean`:
- При `onViewCreated` `SpectrumFragment` (initial)
- В page change callback (page 0 — Spectrum)
- В `SettingsFragment.reloadConfigParameters()` (после Read с прибора / auto-load)
- В обработчике Save (`writeConfigParameters` + `updateSpecSubtitle`)

На странице history subtitle = `"ИСТОРИЯ · НАКОПЛЕННЫЙ СПЕКТР"`, и `updateSpecSubtitle()` ничего не делает (`bzSpecPageIsHistory` блокирует).

### 0.26 Подписи единиц у порогов тревоги

В `setup_layout.xml` после каждого `editLevel1/2/3` добавлен `TextView` с текстом `"cps"` (`bz_jetbrains_mono`, `bz_text_dim`, 11sp) — чтобы пользователь понимал единицы порога.

### 0.27 Многоуровневая alarm-пилюля на вкладке «Доза»

**Что было.** На вкладке Dose сверху — pill `"NORMAL"` / `"OVERLOAD"` (только два состояния), title `"В норме"` / `"Перегрузка"`.

**Что стало.** Пять состояний — соответствуют порогам в настройках (L1/L2/L3) и overload-флагу прибора. В `MainActivity.applyDoseReadouts()`:

```kotlin
val cpsNow = GO.pulsePerSec.toInt()
val alarmLvl = when {
    GO.overloadFlag -> 4
    GO.propLevel3 > 0 && cpsNow >= GO.propLevel3 -> 3
    GO.propLevel2 > 0 && cpsNow >= GO.propLevel2 -> 2
    GO.propLevel1 > 0 && cpsNow >= GO.propLevel1 -> 1
    else -> 0
}
```

| Уровень | Pill | Pill bg | Pill text color | Title |
|---|---|---|---|---|
| 4 OVERLOAD | OVERLOAD | `bg_bz_chip_danger` | `bz_on_danger` | Перегрузка |
| 3 L3 | L3 | `bg_bz_chip_danger` | `bz_on_danger` | Превышение порога 3 |
| 2 L2 | L2 | `bg_bz_chip_accent` | `bz_alert_l2` (новый, `#FF6B35` / `#FF8650` night) | Превышение порога 2 |
| 1 L1 | L1 | `bg_bz_chip_accent` | `bz_warn` | Превышение порога 1 |
| 0 NORMAL | NORMAL | `bg_bz_chip_accent` | `bz_accent` | В норме |

Стиль соответствует чипам «L1/L2/L3» в карточке «УРОВНИ» в настройках — визуально единый. Пороги с `propLevelN == 0` не учитываются (пользователь не задал — не срабатывает).

### 0.28 Фикс краша при повороте экрана на вкладке «Спектр»

**Проблема.** При повороте экрана приложение валилось с `NullPointerException`:

```
java.lang.NullPointerException: Attempt to invoke virtual method
    'void android.widget.TextView.setText(java.lang.CharSequence)' on a null object reference
    at ru.starline.bluz.SpectrumFragment$onViewCreated$13.onPageSelected(SpectrumFragment.kt:315)
```

**Причина.** В `SpectrumFragment.onViewCreated` локальные переменные для callback нижнего ViewPager2 объявлены как non-null:

```kotlin
val subtitleTv = view.findViewById<TextView>(R.id.bzSpecSubtitle)
val titleTv = view.findViewById<TextView>(R.id.bzSpecTitle)
```

ID `bzSpecTitle` / `bzSpecSubtitle` существуют **только** в `layout/spectr_layout.xml` (portrait). В `layout-land/spectr_layout.xml` сделан компактный header без заголовка — для экономии вертикального места. После поворота `findViewById` возвращал `null`, а сразу же по биндингу адаптера ViewPager2 вызывал `onPageSelected(0)` → `subtitleTv.text = …` → краш.

**Фикс.** Типы локальных переменных сделаны nullable, обращения — через safe-call:

```kotlin
// В landscape-варианте title/subtitle нет (компактный header), findViewById вернёт null —
// используем safe-call ниже, иначе при повороте NPE в onPageSelected.
val subtitleTv: TextView? = view.findViewById(R.id.bzSpecSubtitle)
val titleTv: TextView? = view.findViewById(R.id.bzSpecTitle)

// внутри onPageSelected:
subtitleTv?.text = "ИСТОРИЯ · НАКОПЛЕННЫЙ СПЕКТР"
titleTv?.text = if (showHistory) "История" else "Гамма-спектр"
```

`GO.bzSpecSubtitle` уже был nullable, его не трогали — поведение `updateSpecSubtitle()` не изменилось.

**Принцип, который надо удерживать.** Любая View, которой может не быть в одном из layout-вариантов (portrait/land), должна биндиться как nullable + safe-call. Особенно если на неё ссылаются callback-и, срабатывающие при создании view (page change, layout listener и т. п.) — они выстреливают синхронно по биндингу адаптера и не дают шанса «отложить» инициализацию.

### 0.29 Индикатор RSSI в StatusStrip

**Что было.** В шапке (StatusStrip) показывался только статус подключения — точка + иконка Bluetooth, без числовой оценки качества сигнала. RSSI читался через `gatt.readRemoteRssi()` на каждом фрейме, хранился в `GO.Current_RSSI`, использовался в legacy `txtStat2` (теперь невидимом).

**Что стало.** Между `bzBtDot` и `bzBtIcon` в обоих layout-ах (`layout/activity_main.xml` и `layout-land/activity_main.xml`) добавлен `TextView bzBtRssi` (10sp, mono-bold, дефолт «—»).

Новое поле `lateinit var bzBtRssi: TextView` в `globalObj`, биндинг `findViewById` в `MainActivity.onCreate`.

`MainActivity.applyBtRssi()` — публичный helper:

```kotlin
fun applyBtRssi() {
    val rssi = GO.Current_RSSI
    val colorRes: Int
    val text: String
    if (rssi <= -400) {            // sentinel «нет данных» — нет связи или RSSI ещё не прочитан
        text = "—"
        colorRes = R.color.bz_text_dim
    } else {
        text = rssi.toString()
        colorRes = when {
            rssi < -95 -> R.color.bz_bt_off    // красный
            rssi < -75 -> R.color.bz_bt_warn   // жёлтый
            else       -> R.color.bz_bt_on     // зелёный
        }
    }
    GO.bzBtRssi.text = text
    GO.bzBtRssi.setTextColor(ContextCompat.getColor(this, colorRes))
}
```

Вызывается из `applyFrameToUi` (на каждом фрейме) и `restoreStatusStripFromState` (после поворота). В `observeBleStatus` при потере соединения сбрасывается `GO.Current_RSSI = -400` + вызов `applyBtRssi()` — иначе на экране висело бы последнее значение зелёным цветом.

**Подбор порогов.** Изначально были классические радиолюбительские `-110 / -90 dBm`, но в реальном использовании с прибором на расстоянии ~1 м RSSI редко превышает `-50 dBm`, а связь рвётся уже после `-90 dBm`. Сдвинули на 15 единиц вверх: `-95 / -75 dBm` — теперь цвет реально отражает близость прибора.

### 0.30 Переключатель «АКБ в процентах» + цветовая индикация напряжения

**Что было.** Напряжение АКБ в StatusStrip всегда отображалось в вольтах (`X.XXV`), цвет статичный. Для пользователя «3.72 В» — это много или мало? Неочевидно.

**Что стало.**

**Переключатель в Настройки → ВНЕШНИЙ ВИД** (`bzSwitchBatteryPercent`, рядом с «Дневная тема»):

- Текст: «АКБ в процентах»
- Подпись: «заряд вместо напряжения, точность ±5%»
- Биндинг и обработчик в `SettingsFragment.setupAppearance` (или соответствующем месте) — пишет в `GO.showBatteryPercent` + SharedPreferences, и сразу вызывает `MainActivity.applyBatteryDisplay(GO.battLevel)` для мгновенного отклика без ожидания BLE-фрейма.

**Поле `globalObj.showBatteryPercent: Boolean`** + ключ `propShowBatteryPercent`. Сериализация в `writeConfigParameters` / `readConfigParameters`.

**Табличная аппроксимация SOC** (`globalObj.batteryPercent(voltage: Float): Int`):

```kotlin
val table: List<Pair<Float, Int>> = listOf(
    4.20f to 100,
    4.10f to  90,
    4.00f to  80,
    3.90f to  70,
    3.80f to  50,
    3.70f to  30,
    3.60f to  15,
    3.50f to  10,
    3.30f to   5,
    3.00f to   1,
    2.90f to   0,
)
```

Между узлами — линейная интерполяция. Выше 4.20 В → 100%, ниже 2.90 В → 0%. Стандартная разрядная кривая LiCoO2/LiPo с cut-off 2.9 В (Li-ion прибора).

**Почему таблица, а не полином.** Реальная разрядная кривая Li-ion — S-образная, с почти плоской «полкой» в районе 3.7–3.9 В. Полином 2-го порядка эту полку не воспроизведёт; 3–4-й порядок усложняет код без существенного выигрыша. Таблица из 11 точек с линейной интерполяцией даёт точность ±3–5% от реального SOC — этого более чем достаточно для UI-индикатора (реальный SOC зависит ещё от тока разряда, температуры и износа аккумулятора, без fuel-gauge чипа точнее не получится).

**Цветовая индикация** (`MainActivity.applyBatteryDisplay(voltage)`) — те же цвета и принцип, что у RSSI:

| Диапазон | Цвет |
|----------|------|
| `≥ 3.3 В` | зелёный (`bz_bt_on`) |
| `3.0..3.3 В` | жёлтый (`bz_bt_warn`) |
| `< 3.0 В` | красный (`bz_bt_off`) |

Работает одинаково в режиме «вольты» и «проценты» — раскраска идёт по физическому напряжению, не по проценту. До прихода первого фрейма (`battLevel <= 0f`) helper возвращает рано, placeholder из layout остаётся.

`applyBatteryDisplay` вызывается из тех же мест, что и `applyBtRssi`: `applyFrameToUi`, `restoreStatusStripFromState`, и из обработчика переключателя в `SettingsFragment` (мгновенный отклик).

**Helper `globalObj.formatBattery(voltage)`** — возвращает либо `"X.XXV"`, либо `"XX%"` в зависимости от `showBatteryPercent`. Используется внутри `applyBatteryDisplay`.

**Подводный камень.** В `globalObj.kt` в импортах оказался лишний `import java.sql.Array` — он перебивает `kotlin.Array<...>` в типах. При добавлении `batteryPercent` пришлось писать `List<Pair<Float, Int>>` вместо `Array<Pair<Float, Int>>`. Импорт оставлен — он используется где-то ещё в файле (точнее: не используется, но удалять отдельно — выходит за рамки задачи).

### 0.31 Автокалибровка спектрометра по Ra-226

**Что было.** Энергетическая калибровка делалась только вручную: пользователь набирал спектр, тапал по трём пикам и вводил их энергии. ВВ и компаратор подбирал «на глаз».

**Что стало.** Полноценный мастер автокалибровки в карточке «КАЛИБРОВКА» (`setup_layout.xml`), под полями полинома: **«Автокалибровка по Ra-226»**. Подбирает `propHVoltage`, `propComparator` и полином `propCoefXXX A/B/C` так, чтобы спектр был натянут на диапазон ~3500 кэВ с пиком 609 кэВ Bi-214 примерно в канале `channels × 609/3500` (для 1024 это канал 178).

Три режима выбора (`AutoCalibrationController.Mode`):

| Режим                  | ВВ        | Компаратор             | Полином       | Импульсов |
|------------------------|-----------|------------------------|---------------|----------:|
| Полная (`PRIMARY`)              | подбор    | подбор + фин. подгонка | пересчёт      | ~2.5М     |
| Высокое и компаратор (`HV_AND_COMPARATOR`) | подбор    | подбор + фин. подгонка | **не пересчитывается** | ~1М       |
| Только полином (`POLYNOMIAL_ONLY`)         | не трогаем | не трогаем             | пересчёт      | ~1.5М     |

**В диалоге выбора время выполнения не указывается** — оно зависит от активности источника (от единиц до десятков минут на одну фазу). Информативнее количество импульсов на накопление.

**Standalone-подбор компаратора** (отдельная кнопка «Подбор компаратора» в той же карточке) — `AutoCalibrationController.startComparatorOnly()`. Запускает только фазу 4 при текущих ВВ и полиноме. UI-диалог говорит «Подбор выполняется при естественном фоне…» — пользователь должен сам убрать источник перед запуском, BLE-сигнал и UserPrompt не вызываются.

#### 0.31.1 Структура кода

- **`AutoCalibrator.kt`** — чистая расчётная логика без UI:
  - peak detection (SMA-сглаживание + локальные максимумы + 3σ-порог над скользящим минимумом фона)
  - pattern matching на якорную тройку Ra-226 (`82 / 352 / 609` кэВ для маленького детектора) с sanity-проверкой монотонности полинома
  - fallback на линейный fit по паре `(82, 609)` если квадратичный не сошёлся
  - уточнение центроидами в окне ±0.7·FWHM
  - решение системы через `Mtrx` (с проверкой `solved`)
  - sealed `Result`: `Ok` / `WeakSignal` / `NoRa226Pattern` / `UnreasonableFit`
- **`AutoCalibrationController.kt`** — корутинная машина фаз:
  - `StateFlow<State>` для прогресса
  - `cancel()` через `Job.cancel`
  - управление прибором через `cmd_setup` (0u) и `cmd_clear_specter` (1u)
  - `sendFullConfigToDevice()` — намеренное дублирование Save-блока из `SettingsFragment` (не хочется рефакторить рабочий код ради автокалибровки)
  - `requestUserAction(prompt, msg)` — блокирует выполнение до ответа пользователя, держит паузу 3 сек на физическое перемещение источника и очищает буфер
  - `applyHvAndClearSpectrum(newHv)` — write + read-back подтверждение + стабилизация + clear
  - `accumulateByCounts(target, channels)` — накопление до N импульсов с **активным ожиданием очистки буфера** перед стартом
- **`SettingsFragment.kt`**: `showAutoCalibrationModeDialog`, `showAutoCalibrationInstructionAndStart`, `launchAutoCalibration`, `showStandaloneComparatorTuningDialog`, `launchControllerProcedure`, `showAutoCalibrationResultDialog`, `makeStyledBzDialog`, `showStyledMessage` — UI; подписка на `state` через `viewLifecycleOwner.lifecycleScope`.
- **Стиль диалогов.** Все 4 диалога автокалибровки (выбор режима, инструкция/подтверждение, прогресс, результат) лежат в `res/layout/dialog_bz_*.xml` и используют палитру приложения (`bz_bg`, `bz_text`, `bz_accent`, шрифт `bz_inter_bold`). Хелпер `makeStyledBzDialog` создаёт `AlertDialog` и обнуляет системный фон через `setBackgroundDrawable(ColorDrawable(TRANSPARENT))`.

#### 0.31.2 Физика для маленького детектора

Прибор использует сцинтилляторы **NaI(Tl) ⌀10×40 мм** или **CsI(Tl) ~10×10×50 мм**. На таком детекторе эффективность регистрации при высоких энергиях резко падает — пики 1120 и 1764 кэВ Bi-214 утопают в шуме. Из всех гамма-линий Ra-226 в равновесии надёжно видны:
- **82 кэВ** — рентгены свинца (K-альфа Pb-214/Bi-214), **ярчайший** пик на маленьком кристалле
- **352 кэВ** — Pb-214 (ослаблен в 5–10× от 82)
- **609 кэВ** — Bi-214 (ослаблен в 15–30× от 82)

Якорная тройка `[82, 352, 609]` (`AutoCalibrator.anchorEnergiesKev`) — это **physical truth** для прибора. Якорная пара fallback `[82, 609]` (`fallbackAnchorPair`) — для линейной калибровки. Вспомогательные линии `[186, 242, 295]` (`auxEnergiesKev`) — для score в pattern matching.

#### 0.31.3 Алгоритм по фазам

```
┌──────────────────────────────────────────────────────────────┐
│ 0. Запуск спектрометра (cmd_startup_spectrometer 2u если не идёт) │
│    Сброс полинома к 1:1 (A=0, B=1, C=0)                      │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ ФАЗА 1: Компаратор без источника при V=100                   │
│ • Запрос «Уберите источник»                                  │
│ • V=100 (максимум усиления → иголка максимальна)             │
│ • comp=100, итеративно поднимаем переменным шагом:           │
│     ratio > 100: step ×4 (быстро уходим от массивной иголки) │
│     ratio > 10:  step ×2                                     │
│     ratio ≤ 10:  baseStep (точное попадание)                 │
│ • Накопление по времени (30 сек/итерация) — clearAndAccumulate│
│   ByTime с активным ожиданием очистки буфера                 │
│ • Три критерия сходимости (любой → выход + safety-step +1):  │
│   1) standard ratio < 1.5                                    │
│   2) drop: prev/curr ≥ 3× и curr < 100 — иголка убита       │
│   3) stable: |curr-prev|/prev < 30% подряд 2 раза, curr<100  │
│   Критерии 2 и 3 нужны на большом кристалле (CsI 10×10×50,   │
│   фон ~30 имп/с): ratio не падает ниже ~3 из-за фона        │
│ • baseStep = 3/5/10 каналов спектра × bitsChannel (АЦП)      │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ ФАЗА 2: Bracket scan ВВ при V=500 и V=100                    │
│ • Запрос «Поднесите источник»                                │
│ • V=500, накопление 500к импульсов, ch_609 справа налево     │
│ • V=100, накопление 500к импульсов, ch_609 справа налево     │
│ • Глобальная sensitivity = (ch100-ch500)/(100-500), запись   │
│   в GO.hvSensitivity                                         │
│ • V_tuned = 500 + (targetCh − ch500)/sensitivity             │
│ • Возвращает BracketResult(tunedV, nearestV, nearestCh) —    │
│   ближайшая измеренная точка для refine 1                    │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ ФАЗА 3 (×3 итераций): накопление + полином + коррекция V    │
│ • Apply finalHv → накопление 500к импульсов                  │
│ • AutoCalibrator.analyze → Ok / WeakSignal / NoRa226Pattern  │
│   / UnreasonableFit                                          │
│ • Если Ok: predictChannel(609) = curCh609                   │
│ • Локальная sensitivity:                                     │
│     localSens = (curCh609 - prevCh609) / (currV - prevV)    │
│   (на 1-й итерации prev = ближайшая точка bracket scan)     │
│ • Коррекция: deltaV = (targetCh - curCh609) / localSens     │
│   с clamp |deltaV| ≤ |currV - prevV| (защита от прыжка     │
│   за уже проверенный интервал)                               │
│ • Если newV == finalHv → converged, выход                    │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ ФАЗА 4: Финальная подгонка компаратора при готовом V        │
│ • Запрос «Уберите источник ещё раз»                          │
│ • Компаратор был подобран при V=100 (макс иголка); при       │
│   финальном V иголка меньше → текущий comp может отрезать   │
│   полезные первые каналы спектра                             │
│ • Повторяем алгоритм фазы 1 БЕЗ принудительной установки V=100│
│ • Результат → pendingResult.newComparator                    │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ Phase.AWAITING_APPLY: диалог результата                      │
│ Пользователь видит: HV (old → new), Comp (old → new),       │
│ полином A/B/C, верх шкалы (E_last), найденные пики с невязкой│
│ По «Применить» → sendFullConfigToDevice() записывает всё    │
└──────────────────────────────────────────────────────────────┘
```

#### 0.31.4 Ключевые защиты

**Sanity полинома** (`AutoCalibrator.checkPolynomialSanity`):
- **Монотонность**: `2A·(channels-1) + B > 0` (энергия монотонно растёт с каналом). Это отбраковывает квадратичные подгонки с большим отрицательным `A`, которые численно описывают тройку пиков, но физически дают убывающую энергию.
- `|A| ≤ 5e-3` — реальная кривизна детектора мала (изначально было 5e-4, расширено после того как валидные тройки на 4096-канальном спектре давали `A ≈ 1.5e-3`)
- `B` в коридоре `0.2× .. 5×` ожидаемого `TARGET_FULL_RANGE_KEV/channels`
- `|C| ≤ 300 кэВ`

**Накопление по импульсам, а не по времени** (`accumulateByCounts`):
- `ACC_REFINE_IMPULSES = 500_000` — большое число чтобы пик 609 имел достаточную статистику (на маленьком детекторе он ослаблен)
- Активное ожидание очистки буфера после `cmd_clear_specter` — без этого следующая фаза могла видеть остаточный спектр от предыдущей (фрейм с очищенным буфером приходит с задержкой ~1 сек)
- Таймаут 10 минут — защита от очень слабого источника

**Локальная sensitivity в refine**:
- Глобальная `GO.hvSensitivity` (от bracket scan V=100..V=500) может сильно отличаться от реальной около финального V — зависимость V↔ch нелинейная.
- На каждой итерации refine считаем локальную sens по двум последним измерениям `(V, ch_609)`. Первая итерация — `prev` инициализируется из bracket scan (ближайшая к финальному V точка).

**Read-back подтверждение с retry** для ВВ и компаратора (`writeAndConfirmHv` / `writeAndConfirmComp`):
- После `cmd_setup` ждём до 5 сек фрейм с `frame.hw.hVoltage == newHv` (или `frame.hw.comparator == newComp`)
- Если не подтверждено — **повторяем `cmd_setup`** и ждём ещё до 10 сек
- При финальном неуспехе — warning в лог, но процедура продолжается (прошивка иногда не обновляет поле в фрейме сразу, хотя физически применяет значение)

**Пауза после `requestUserAction`** — 3 сек на физическое перемещение источника + `cmd_clear_specter` после, чтобы остаточные счёты во время диалога не попали в следующую фазу.

**Защита от шумовой иголки в `findBrightestPeakCh`**:
- Поиск справа налево (на маленьком детекторе первый встретившийся пик гарантированно 609, не 82)
- Двойной критерий значимости: `v > globalMax × 10%` И `prominence > 10σ`
- Проверка FWHM ≥ 4 каналов (отсев узких выбросов)

**Финальный safety-step компаратора** (`finalizeCompWithSafetyStep`): после сходимости фазы 1 или 4 к значению `comp_conv` добавляется `+1 базовый шаг` АЦП — гарантия что остаток шумовой иголки не вылезет при изменении температуры/ВВ. `baseStep` маленький (~60 АЦП = 3 канала спектра), полезные первые каналы не отрезает.

**UserPrompt'ы, по одному на каждую точку вмешательства пользователя:**
- `RemoveSourceForCompTuning` — фазы 1 и 4 (запуск подбора без источника)
- `PlaceSourceForCalibration` — фаза 2 (стандартный заголовок «Верните источник»)
- `RemoveSourceForBackgroundCheck` — резерв на будущее (проверка фона после применения)
- `PlaceSourceForPolynomOnly` — режим POLYNOMIAL_ONLY (заголовок «Поднесите источник», т.к. в этом режиме пользователь источник ещё не приносил)

**«Характеристики прибора»** в SharedPreferences:
- `propHvSensitivity` — глобальная sensitivity из bracket scan, сохраняется при `applyPending`
- `propCompNoiseLevel` — последний рабочий уровень компаратора

#### 0.31.5 Параметры алгоритма (константы в `AutoCalibrationController`)

| Константа | Значение | Назначение |
|-----------|---------:|------------|
| `MIN_HV_DAC` | 100 | Минимум DAC ВВ (=максимум реального усиления) |
| `MAX_HV_DAC` | 1023 | Максимум DAC |
| `HV_STABILIZE_MS` | 5000 | Пауза после смены ВВ (SiPM) |
| `HV_BRACKET_LOW` | 100 | Низкий V для bracket scan |
| `HV_BRACKET_HIGH` | 500 | Высокий V для bracket scan |
| `ACC_REFINE_IMPULSES` | 500_000 | Накопление в фазах 2 и 3 |
| `ACC_TIMEOUT_MS` | 600_000 | Таймаут накопления (10 мин) |
| `REFINE_ITERATIONS` | 3 | Число итераций refine |
| `MAX_COMP_ITERATIONS` | 20 | Лимит итераций фазы компаратора |
| `comparatorStepInSpectrumChannels(channels)` | 3/5/10 | Шаг компаратор в каналах спектра для 1024/2048/4096 |
| `AutoCalibrator.TARGET_FULL_RANGE_KEV` | 3500 | Целевой верх шкалы в кэВ |
| `AutoCalibrator.MIN_COUNTS_PER_PEAK` | 500 | Минимум счётов в окне ±FWHM каждого якорного пика |

#### 0.31.6 Поведение при слабом/неверном источнике

Алгоритм возвращает один из защитных результатов:
- `WeakSignal` — какой-то из якорных пиков набрал <500 счётов в окне ±FWHM
- `NoRa226Pattern` — ни одна тройка кандидатов не прошла sanity-check (и линейный fallback по паре 82/609 тоже не прошёл)
- `UnreasonableFit` — pattern matching сошёлся, но финальный полином даёт нефизичные коэффициенты

**Записать криво в прибор алгоритм не может** — либо отказ с понятным сообщением, либо корректный полином.

#### 0.31.7 История эволюции алгоритма (по обратной связи)

1. **Якоря 352/609/1764 → 82/352/609.** На большом детекторе (⌀40 мм) видна вся «гребёнка» Ra-226; на маленьком (⌀10 мм) выше 609 кэВ ничего не различимо. Якорь ВВ тоже сменился: вместо «1764 в нужный канал» теперь «609 в нужный канал».
2. **Поиск ярчайшего пика справа налево.** На малом детекторе **82 кэВ** ярчайший (а не 609). Чтобы алгоритм фазы ВВ устойчиво ловил **именно 609**, идём от правого края спектра — гарантированно первый широкий пик это 609.
3. **Bracket scan V=1023/100 → V=500/100.** Полный диапазон даёт слишком крутую интерполяцию около V=100 (где обычно финал); сужение интервала ближе к рабочей зоне.
4. **Накопление по времени → по импульсам.** Адаптивно к активности источника. 500к импульсов на пике 609 кэВ — достаточно для надёжного центроида.
5. **Bracket scan + 1 коррекция → bracket scan + 3 итерации refine.** Зависимость V↔ch нелинейная — за одну коррекцию точно не попадаешь. Три итерации с локальной sensitivity сходятся стабильно.
6. **Натяжка шкалы через E_last → корректировка V через предсказание ch_609.** Раньше после полинома мерили `E(channels-1)` и корректировали V пропорционально; теперь через `predictChannel(609)` и локальную sensitivity — точнее на нелинейности.
7. **`Mtrx`: побочный эффект записи в GO убран.** Раньше при `mainDet==0` функция писала нули в `GO.propCoefXXX*` — а pattern matching перебирал десятки троек и при первой вырожденной затирал реальные коэффициенты. Теперь `Mtrx.solved` + запись в GO делает только вызывающий код.
8. **Sanity check: монотонность полинома** добавлена после того как тройка `(28, 91, 169) → (82, 352, 609)` дала `A=-0.0065, B=11, C=-207` — математически правильное решение, но `E(1023)=-2030 кэВ`. Теперь такие полиномы отбраковываются.
9. **Активное ожидание очистки буфера** после `cmd_clear_specter` — раньше следующая фаза видела остаточный спектр (фрейм с очищенным буфером приходит с задержкой), считала что counts уже набраны и анализировала старые данные.
10. **Финальная подгонка компаратора при готовом V.** Компаратор, подобранный при V=100, может «съесть» полезные первые каналы при финальном V (где иголка тоньше). Дополнительная фаза 4 переподбирает comp на финальном V.
11. **Подбор компаратора методом «обнули → найди конец иголки → добавь запас»** оказался неустойчивым — пользователь не успевал убрать источник, остаточные счёты пика 82 кэВ принимались за «конец иголки». Заменено на **итеративное повышение** при V=100 с переменным шагом (×4/×2/×1 в зависимости от ratio) и порогом сходимости 1.5.
12. **Top-10 → top-20 кандидатов в pattern matching.** На маленьком детекторе пик 609 кэВ в 20–40× слабее пика 82 — может не попасть в топ-10 по prominence. Перебор C(20,3)=1140 троек укладывается в единицы миллисекунд, надёжность вырастает.
13. **Активное ожидание очистки буфера в фазе компаратора** (`clearAndAccumulateByTime`). Без него все итерации показывали одинаковые значения шума — буфер `spectrData` не успевал реально очиститься между итерациями (фрейм с пустым буфером приходит с задержкой ~1 сек).
14. **Retry для read-back подтверждения** ВВ/комп. Изначально была одна попытка с 5-сек таймаутом; на медленных BLE-соединениях прибор не успевал отразить новое значение. Теперь 5 сек → повтор `cmd_setup` → ещё 10 сек.
15. **Три критерия сходимости в `tuneComparatorAtCurrentHv`** (стандартный порог + sharp drop + стабилизация). На большом кристалле CsI 10×10×50 (фон ~30 имп/с) метрика `ratio = maxNoise / maxPlateau` упирается в плато ~2–3 из-за статистики фона, даже когда иголка фактически убита. Дополнительные критерии (резкое падение и стабилизация) ловят оба этих случая.
16. **Финальный safety-step +1**. После сходимости комп получает запас в `+baseStep` АЦП (≈3 канала спектра). На границе сходимости иголка «почти убита» — один шаг сверху гарантирует что остатки не вылезут при изменении ВВ или температуры. Раньше safety-step был выключен (давал слишком высокий comp), сейчас восстановлен с маленьким baseStep.
17. **Mode `RECALIBRATION` → `HV_AND_COMPARATOR`.** Семантика сменилась: «повторная» (использовала сохранённую sensitivity) была не нужна, потому что bracket scan не такой долгий. Новый режим — «ВВ и компаратор без перезаписи полинома» — для случая когда пользователь хочет переподобрать только аппаратные параметры, оставив энергетическую шкалу как настроил вручную.
18. **Отдельный prompt `PlaceSourceForPolynomOnly`** для режима POLYNOMIAL_ONLY. Стандартный `PlaceSourceForCalibration` имеет заголовок «Верните источник» (источник уже убирался для фазы 1); в POLYNOMIAL_ONLY фаза 1 пропущена, и пользователь источник ещё не приносил — корректный заголовок «Поднесите источник».
19. **Стилизованные диалоги.** AlertDialog'и заменены на свои XML-разметки (`dialog_bz_*.xml`) с палитрой и шрифтами приложения. Хелпер `makeStyledBzDialog` устанавливает прозрачный фон окна — иначе системная подложка создаёт двойную рамку поверх `bz_bg`.

**Подводный камень `Mtrx`.** При вырожденной системе (`mainDet == 0`) `sysEq()` раньше сбрасывал коэффициенты жёстко в `propCoef1024A/B/C` независимо от текущего разрешения. Поправлено: побочные эффекты убраны полностью, есть флаг `solved` для вызывающего кода.

**Карта BLE-команд (актуализированная по прошивке).** В `Core/Inc/main.h` объявлен `enum commandTx`:

| Код | Имя в прошивке             | Действие |
|----:|----------------------------|----------|
| 0   | `cmd_setup`                | Отправить полный конфиг (HV/comparator/polynom/...). Применяется сразу. |
| 1   | `cmd_clear_specter`        | Очистить буфер спектра в приборе. |
| 2   | `cmd_startup_spectrometer` | Старт/стоп спектрометра. |
| 3   | `cmd_clear_dosimeter`      | Сброс дозиметра. |
| 4   | `cmd_clear_logs`           | Очистка лога событий. |
| 5   | `cmd_history_request`      | Запрос исторического спектра. |
| 6   | `cmd_find_device`          | Звук + вибро (опознавание прибора). |
| 7   | `cmd_calibrate_batt`       | Калибровка АКБ (parameter float в `sendBuffer[4..7]`). |
| 8   | `cmd_clear_history`        | Очистка истории. |

Это отличается от старого описания в разделе 5.7 (там было `6 = Write config, 7 = Read config`) — старое описание устарело; раздел 5.7 надо тоже актуализировать при следующей правке.

---

## 1. Обзор приложения

**BluZ** — Android-приложение для управления и мониторинга портативного дозиметра-спектрометра ионизирующего излучения, взаимодействующего с устройством через Bluetooth Low Energy (BLE).

**Основные возможности:**
- Приём и отображение спектра излучения в реальном времени (1024 / 2048 / 4096 каналов)
- Дозиметрия с энергетической компенсацией (χ-вектор)
- История спектров (хранится в памяти прибора)
- GPS-трекинг с цветовой кодировкой мощности дозы
- Экспорт спектра в BqMon XML и CSV
- Экспорт треков в KML и CSV
- Управление параметрами прибора (ВВ, компаратор, пороги, полиномы)
- Дневная / ночная тема UI
- Фоновый сервис для записи трека при свёрнутом приложении

---

## 2. Архитектура

```
┌─────────────────────────────────────────────────────┐
│                    App (Application)                │
│  - ThemePrefs.apply()                               │
│  - MapKitFactory.init()                             │
│  - Создание singleton draw-объектов                 │
└────────────────────────┬────────────────────────────┘
                         │
              ┌──────────▼──────────┐
              │    MainActivity     │  FragmentActivity
              │  ViewPager2 + 5 tab │
              └──────────┬──────────┘
          ┌──────────────┼──────────────────────┐
          │              │                      │
   ┌──────▼──────┐  ┌────▼────────────┐  ┌─────▼──────┐
   │SpectrumFrag │  │HistoryFragment  │  │DoseFrag     │
   │HistoryFrag  │  │BluZMapFragment  │  │SettingsFrag │
   └─────────────┘  └─────────────────┘  └────────────┘
          │                                      │
   ┌──────▼──────────────────────────────────────▼──┐
   │              globalObj (GO)                    │
   │  Центральный синглтон — состояние + ссылки UI  │
   └──────────────────────┬─────────────────────────┘
                          │
          ┌───────────────┼───────────────────┐
          │               │                   │
   ┌──────▼──────┐  ┌─────▼────────┐  ┌──────▼──────┐
   │BluetoothIf  │  │ Room Database│  │Draw objects │
   │(BLE frames) │  │Tracks/Detec  │  │Bitmap canvas│
   └─────────────┘  └──────────────┘  └─────────────┘
          │
   ┌──────▼──────────────────┐
   │  BleMonitoringService   │  Foreground Service
   │  (background recording) │
   └─────────────────────────┘
```

**Шаблон:** MVVM + корутины. Реактивные потоки: `SharedFlow<DeviceFrame>` для фреймов, `StateFlow<BleStatus>` для состояния подключения.

**Основные слои:**

| Слой | Классы |
|------|--------|
| BLE-коммуникация | `BluetoothInterface`, `BleMonitoringService` |
| Модели данных | `DeviceFrame`, `HardwareConfig`, `LogEntry`, `BleStatus` |
| Состояние | `globalObj` (GO), `DeviceViewModel` |
| Персистентность | Room: `AppDatabase`, `DosimeterDao`, `Track`, `TrackDetail`, `DetectorType` |
| UI | `MainActivity`, 5 фрагментов, `NumberAdapter` |
| Отрисовка | `drawSpecter`, `drawHistory`, `drawDozimeter`, `drawCursor`, `drawLogs`, `drawExmple` |
| Вычисления | `DoseCalculator`, `Mtrx` |
| Сервисы | `BleMonitoringService` |
| Утилиты | `SaveBqMon`, `ContinuousLocationManager`, `intervalTimer`, `buttonColor`, `propControl`, `ThemePrefs` |

---

## 3. Точка входа — App

**Файл:** `YandexMapActivity.kt` (класс `App`)

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePrefs.apply(ThemePrefs.isDayTheme(this))       // Применить тему до создания Activity
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY) // Yandex MapKit
        MapKitFactory.initialize(this)
        GO.drawSPECTER   = drawSpecter()      // Singleton draw-объекты
        GO.drawDOZIMETER = drawDozimeter()    // Создаются в Application.onCreate,
        GO.drawHISTORY   = drawHistory()      // чтобы пережить пересоздание Activity
        GO.drawLOG       = drawLogs()         // при повороте экрана
        GO.drawCURSOR    = drawCursor()
        GO.drawExamp     = drawExmple()
    }
}
```

**Ключевой момент:** draw-объекты создаются именно здесь, а не в `MainActivity`, потому что `MainActivity` пересоздаётся при повороте экрана. Bitmap-буферы внутри draw-объектов пересоздаются при следующем `init()` (флаг `drawObjectInit = true`).

---

## 4. MainActivity

**Файл:** `MainActivity.kt` (~960 строк)  
**Базовый класс:** `FragmentActivity` *(не* `AppCompatActivity` — важно для темы)

### 4.1 Жизненный цикл

```
onCreate()
  ├── Инициализация ViewPager2, NavigationBar
  ├── checkAndRequestPermissions()
  ├── Инициализация базы данных
  ├── GO.loadIsotop() — загрузка справочника изотопов
  ├── GO.readConfigParameters() — загрузка настроек из SharedPreferences
  ├── applySystemBarsForTheme() — цвета системных баров
  └── Наблюдение за BleStatus / DeviceFrame через корутины

onResume()
  └── applySystemBarsForTheme()
      ├── statusBarColor = bz_bg (с учётом темы)
      ├── navigationBarColor = bz_bg
      └── isAppearanceLightStatusBars = isDayTheme
```

### 4.2 Критические детали

**Контекст и тема.** `MainActivity` расширяет `FragmentActivity`, поэтому `setDefaultNightMode` не применяется автоматически до следующей Activity. Решение — `attachBaseContext`:

```kotlin
override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ThemePrefs.wrapContextWithTheme(newBase))
}
```

`ThemePrefs.wrapContextWithTheme` создаёт `createConfigurationContext` с принудительным `UI_MODE_NIGHT_YES/NO`.

**Публичные методы (вызываемые из фрагментов):**

| Метод | Назначение |
|-------|-----------|
| `applyDoseReadouts()` | Перерисовка всех dose-readouts (Spectrum hero, Dose hero, legacy txtStat3) из кэшированного состояния `GO`. Не зависит от свежего фрейма. |
| `toggleDoseUnits()` | Переключение `GO.unitsMess` (мкР/ч ↔ мкЗв/ч), персист в SharedPreferences, немедленная перерисовка. |
| `restoreStatusStripFromState()` (private) | Восстановление StatusStrip из `GO` после поворота экрана. См. раздел [0.1](#01-восстановление-statusstrip-после-поворота-экрана). |
| `formatDoseScaled(microR, useSievert)` (private) | Автоскейл мк → м → база. См. раздел [0.6](#06-автомасштабирование-единиц-мк--м--база). |

**Цвета в `applyFrameToUi`.** Все вызовы `ContextCompat.getColor(...)` используют `this` (Activity-контекст с темой), а не `GO.mainContext` (Application-контекст без переопределения темы). Это критично: использование applicationContext вернёт цвета ночной темы независимо от выбранной темы.

### 4.3 Обработка BLE-фреймов

```
BluetoothInterface.deviceFrames (SharedFlow<DeviceFrame>)
    └── MainActivity.collect()
        ├── applyFrameToState(frame)  — обновить GO.cps, GO.PCounter, GO.tempMC, etc.
        └── applyFrameToUi()          — обновить bzCpsValue, bzTempValue, bzBattValue, etc.
```

`applyFrameToUi()` обновляет:
- Статус-стрип: CPS, время, температура, заряд батареи
- Цвет иконки BT (зелёный/оранжевый/красный по RSSI)
- Hero-значения на вкладках Spectrum, History, Dose
- Пилюли уровней (L1/L2/L3)

### 4.4 Навигация

Кастомная Bottom Navigation (не `BottomNavigationView`):
- **5 элементов с 2026-05-26:** Spectrum · Dose · Map · Settings · **Exit**
- Pages в `NumberAdapter`: только **4** (Spectrum, Dose, Map, Settings). Exit-таб — не страница, а действие (`performExit()`).
- В `setupNavigation()`: `if (index >= pageCount) performExit() else setCurrentItem(index)`.
- Верхний индикатор (View 32×3 dp, `bz_accent`) автоматически не подсвечивает Exit (его индекс ≥ pageCount).
- `item_bz_nav_tab.xml`: FrameLayout → ImageView + TextView с `duplicateParentState=true` и `android:tint="@color/bz_nav_icon"`
- `android:tint` (не `app:tint`) реагирует на состояние drawable с `duplicateParentState`
- В landscape: Rail-навигация (`item_bz_nav_rail.xml`) с pill-фоном для выбранного состояния. Тот же набор: `rail_spectrum / rail_dose / rail_map / rail_settings / rail_exit`.
- История доступна через swipe внутри Spectrum tab (см. [0.13](#013-двухуровневая-навигация-на-вкладке-спектр-верхний-chart--нижний-swipe-pager)).

### 4.5 Запись трека GPS

```kotlin
fun recordTrackPoint() {
    val location = GO.locationManager?.lastLocation
    val cps      = GO.cps
    val magnitude = magnetometer.average()  // Магнитное поле
    dao.insertPoint(TrackDetail(trackId = GO.currentTrck, ...))
}
```

Точка записывается при каждом новом BLE-фрейме, если включена запись трека.

### 4.6 Завершение приложения

`performExit()` — показывает диалог: выйти полностью или перевести в фоновый сервис. Если фоновый режим: `startForegroundService(BleMonitoringService)`, затем `moveTaskToBack(true)`. Если полный выход: `GO.needTerminate = true` → `intervalTimer` вызывает `System.exit(0)`.

---

## 5. BLE-протокол и BluetoothInterface

**Файл:** `BluetoothInterface.kt` (~619 строк)

### 5.1 UUIDs

| Роль | UUID |
|------|------|
| Service | `0000fe80-cc7a-482a-984a-7f2ed5b3e58f` |
| RX (notify, приём) | `0000fe81-8e22-4541-9d4c-21edae82ed19` |
| TX (write, отправка) | `0000fe82-8e22-4541-9d4c-21edae82ed19` |

### 5.2 Параметры соединения

- MTU: запрашивается 251 байт, полезная нагрузка пакета: 248 байт
- Таймаут сканирования: 10 000 мс
- Имя устройства для фильтрации: `"BluZ"`

### 5.3 Формат фрейма

```
Смещение  Размер  Описание
0-2       3 байт  Заголовок '<B>' (0x3C 0x42 0x3E)
3         1 байт  Тип фрейма (0-6)
4         1 байт  Зарезервировано
5-8       4 байт  totalPulses (uint32)
9-12      4 байт  pulsesPerSec (uint32)
13-16     4 байт  measurementTime в секундах (uint32)
17-20     4 байт  avgCps (float)
21-22     2 байт  temperature (float16 → °C)
23-24     2 байт  batteryVoltage (float16 → В)
25-42     18 байт Конфигурация (коэф. полинома 1024, CPS2UR, HV, компаратор, пороги, bitcfg)
43-46     4 байт  Коэф. полинома 2048 (A, B, C)
...       ...     (полный формат — см. раздел 22)
50-561    512 байт Данные дозиметра (512 × uint8)
562-661   100 байт Данные лога (50 × 2 байта: timestamp + act)
662+      Данные спектра (1024 × 2 байт = тип 1; 2048 × 2 = тип 2; 4096 × 2 = тип 3)
-2, -1    2 байт  Контрольная сумма (sum(bytes[0..241]), little-endian uint16)
```

### 5.4 Типы фреймов

| Тип | Содержимое |
|-----|-----------|
| 0 | Дозиметр + лог (6 MTU) |
| 1 | Дозиметр + лог + спектр 1024 каналов (14 MTU) |
| 2 | Дозиметр + лог + спектр 2048 каналов (23 MTU) |
| 3 | Дозиметр + лог + спектр 4096 каналов (39 MTU) |
| 4 | Дозиметр + лог + исторический спектр 1024 |
| 5 | Дозиметр + лог + исторический спектр 2048 |
| 6 | Дозиметр + лог + исторический спектр 4096 |

### 5.5 Стек-машина соединения

```
startScan() → обнаружение устройства → initLeDevice()
  └── connectGatt()
      └── onConnectionStateChange(CONNECTED)
          └── requestMtu(251)
              └── onMtuChanged()
                  └── discoverServices()
                      └── onServicesDiscovered()
                          └── setCharacteristicNotification(RX, true)
                              └── Готов к приёму фреймов
```

### 5.6 Сборка многопакетного фрейма

Один логический фрейм передаётся несколькими MTU-пакетами. `processIncomingPacket()`:
1. Первый байт пакета — порядковый номер (0 = начало нового фрейма)
2. Данные копируются в накопительный буфер
3. При получении ожидаемого последнего пакета — проверка CRC, затем эмиссия `DeviceFrame` в `SharedFlow`

### 5.7 Команды управления

Команды отправляются через `sendCommand(cmd: Byte)`:

| Команда | Действие |
|---------|---------|
| 0 | Стоп спектрометра |
| 1 | Старт спектрометра |
| 2 | Сброс дозиметра |
| 3 | Очистка данных дозиметра |
| 4 | Очистка логов |
| 5 | Запрос истории |
| 6 | Запись конфигурации в прибор |
| 7 | Чтение конфигурации из прибора |
| 8 | Очистка истории |

### 5.8 Реактивные потоки

```kotlin
val deviceFrames: SharedFlow<DeviceFrame>  // Горячий поток — новый фрейм при каждой успешной сборке
val status: StateFlow<BleStatus>            // Текущее состояние соединения
```

`BleStatus` — sealed class: `Connecting`, `Connected(rssi)`, `Disconnected`, `Error(message)`.

---

## 6. Модели данных DeviceFrame / BleStatus

**Файл:** `DeviceFrame.kt`

### DeviceFrame

```kotlin
data class DeviceFrame(
    val frameType: Int,            // 0-6
    val totalPulses: UInt,         // Всего импульсов
    val pulsesPerSec: UInt,        // За последнюю секунду
    val avgCps: Float,             // Среднее CPS
    val measurementTime: UInt,     // Время измерения, сек
    val temperature: Float,        // °C
    val batteryVoltage: Float,     // В
    val overload: Boolean,         // Флаг перегрузки
    val dosimeterData: DoubleArray,// 512 элементов — гистограмма дозиметра
    val logEntries: List<LogEntry>,// До 50 записей лога
    val spectrumData: DoubleArray?,// null для типа 0; 1024/2048/4096 для типов 1-3
    val historyData: DoubleArray?, // null для типов 0-3; спектр для типов 4-6
    val hw: HardwareConfig         // Конфигурация железа
)
```

### HardwareConfig

31 поле:
- `ledKvant`, `soundKvant` — индикация каждого кванта
- `click10`, `led10` — делитель частоты клика/LED (/10)
- `level1`, `level2`, `level3` — пороги CPS для трёх уровней тревоги
- `soundLevel1/2/3`, `vibroLevel1/2/3` — включение звука/вибро по уровням
- `cps2ur` — коэффициент CPS→мкР/ч
- `coef1024A/B/C`, `coef2048A/B/C`, `coef4096A/B/C` — коэффициенты полинома канал→энергия (keV): `E = A·ch² + B·ch + C`
- `hv` — высокое напряжение (ADC)
- `comparator` — порог компаратора АЦП
- `autoStartSpectrometer` — автозапуск при включении
- `spectrResolution` — разрядность (0=1024, 1=2048, 2=4096)
- `aqureValue` — погрешность измерения дозиметра
- `bitsChan` — разрядность канала АЦП
- `sampleTime` — время выборки АЦП
- `spectrometerTime`, `spectrometerPulse` — время работы и число импульсов спектрометра

### LogEntry

```kotlin
data class LogEntry(val tm: UInt, val act: UByte)
```

Коды `act`:

| Код | Событие |
|-----|---------|
| 0 | Нет события |
| 1 | Включение прибора |
| 2 | Превышение уровня 1 |
| 3 | Превышение уровня 2 |
| 4 | Превышение уровня 3 |
| 5 | Возврат к норме |
| 6 | Сброс дозиметра |
| 7 | Сброс спектрометра |
| 8 | Запись во flash |
| 9 | Запуск спектрометра |
| 10 | Стоп спектрометра |
| 11 | Очистка лога |
| 12 | Изменение разрешения |
| 13 | Изменение разрядности канала |
| 14 | Перегрузка |
| 15 | Калибровка батареи |
| 16 | Низкий заряд батареи |
| 17 | Очистка истории |
| 18 | Требуется обновление прошивки |

---

## 7. DeviceViewModel

**Файл:** `DeviceViewModel.kt` (~84 строки)

ViewModel для MVVM-подхода. Держит `StateFlow<DeviceUiState>`.

```kotlin
data class DeviceUiState(
    val cps: Float,
    val avgCps: Float,
    val temperature: Float,
    val batteryVoltage: Float,
    val measurementTime: UInt,
    val totalPulses: UInt,
    val overload: Boolean,
    val spectrumData: DoubleArray?,
    val historyData: DoubleArray?
)
```

Реализован кастомный `equals()`/`hashCode()` для корректного сравнения `DoubleArray`, чтобы избежать лишних recomposition.

`observeBle()` — подписывается на `BluetoothInterface.deviceFrames` и `BluetoothInterface.status`, обновляя `_state`. Вызывается из `MainActivity.onCreate()`.

---

## 8. Глобальное состояние — globalObj / GO

**Файл:** `globalObj.kt` (~1019 строк)

`GO` — псевдоним объекта-синглтона `globalObj`. Является центральным хранилищем состояния приложения.

### 8.1 Структура полей

**Контекст и BLE:**
```
mainContext: Context       — applicationContext (НЕ использовать для цветов!)
BTT: BluetoothInterface    — экземпляр BLE
LEMAC: String              — MAC-адрес устройства
Current_RSSI: Int          — текущий RSSI (−400 = нет данных)
```

**Флаги состояния:**
```
allPermissionAccept: Boolean   — все разрешения получены
initBT: Boolean                — BT инициализирован
initDOZ: Boolean               — дозиметр инициализирован
overloadFlag: Boolean          — флаг перегрузки прибора
configDataReady: Boolean       — получены параметры прибора
needTerminate: Boolean         — запрос на завершение приложения
```

**Измерения:**
```
PCounter: UInt       — всего принято частиц (дозиметр)
cps: Float           — CPS за текущий цикл
cpsAVG: Float        — среднее CPS за длительный интервал
messTm: UInt         — время измерения дозиметра (сек)
spectrometerTime: UInt   — время работы спектрометра (сек)
spectrometerPulse: UInt  — импульсы спектрометра
tempMC: Float        — температура МК (°C)
battLevel: Float     — напряжение батареи (В)
pulsePerSec: UInt    — CPS за 1 сек
compMED: Float       — энергокомпенсированный МЭД (мкР/ч)
unitsMess: Int       — единицы: 0 = мкР/ч, 1 = мкЗв/ч
```

**Параметры отображения спектра:**
```
specterType: Int          — тип фрейма из прибора (0=1024, 1=2048, 2=4096)
specterGraphType: Int     — 0 = линия, 1 = гистограмма
spectrResolution: Int     — разрешение из настроек
xZoom: Float             — масштаб по X (1..5)
xPosition: Float         — смещение по X (панорамирование)
rejectChann: Int         — начальные каналы, игнорируемые при отрисовке (дефолт 10)
windowSMA: Int           — окно SMA-фильтра (дефолт 5)
realResolution: Int      — разрешение на 662 кЭв в каналах (для поиска изотопов, дефолт 30)
clockShowSecondsSpec: Boolean — переключатель таймера спектра (false=hh:mm:ss/dd:hh:mm:ss, true=секунды)
clockShowSecondsHist: Boolean — переключатель таймера дозиметра/истории
drawCursorObjectInit: Boolean — флаг запроса пересоздания bitmap курсора при повороте экрана
bzSpecSubtitle: TextView?  — ссылка на subtitle над «Гамма-спектр»; обновляется через GO.updateSpecSubtitle()
bzSpecPageIsHistory: Boolean — флаг текущей страницы внутреннего pager на Spectrum tab
autoLoadDeviceCfg: Boolean  — switch «Загружать настройки с BluZ» при выборе MAC из диалога поиска
txtIsotopInfo: TextView?   — подсказка изотопа на спектре (внутри chart card, биндится в SpectrumFragment)
```

**Параметры прибора (prop* — из SharedPreferences, HWprop* — из прибора):**
```
propSoundKvant, propLedKvant             — квантовая индикация
propLevel1/2/3                           — пороги CPS
propSoundLevel1/2/3, propVibroLevel1/2/3 — включение звука/вибро
propCPS2UR: Float                        — коэффициент CPS→мкР/ч
propCoef1024/2048/4096 A/B/C: Float      — коэффициенты полинома
propHVoltage, propComparator             — ВВ и компаратор
```

**Цвета графиков (Int = ARGB):**
```
ColorLin, ColorLog       — линейный/логарифмический спектр (режим "линия")
ColorFone, ColorFoneLg   — фоновый спектр линейный/логарифмический
ColorLinGisto, ColorLogGisto     — то же в режиме "гистограмма"
ColorFoneGisto, ColorFoneLgGisto
ColorDosimeter, ColorDosimeterSMA — дозиметр и SMA-сглаживание
ColorActiveCursor        — цвет курсора
```

**Цветовая шкала радиации (32 уровня, AARRGGBB):**
```
radClrs[0..31]     — от синего (0x7F0117FF) до красного (0x7FFE0000)
radClrsKml[0..31]  — то же в формате KML (AABBGGRR)
```

**GPS и карта:**
```
map: Map?                        — Yandex MapKit Map
locationManager: ContinuousLocationManager?
impArr: Array<ImageProvider?>[32] — иконки точек (32 цвета)
impBLACK: ImageProvider           — чёрная иконка (нет данных)
currentTrck: Long                — ID активного трека
currentDetector: Long            — ID активного детектора
trackIsRecordeed: Boolean        — запись трека включена
saveTrackType: Int               — 0=KML, 1=CSV
```

**UI-ссылки (lateinit):**  
StatusStrip: `bzCpsValue`, `bzClockValue`, `bzTempValue`, `bzBattValue`, `bzBtDot`, `bzBtIcon`  
Spectrum hero (nullable): `bzSpecDoseValue`, `bzSpecAvgValue`, `bzRecClock`, `bzRecLabel`  
History hero (nullable): `bzHistDuration`, `bzHistIntegralValue`, `bzHistAvgCpsValue`  
Dose hero (nullable): `bzDoseHeroValue`, `bzDoseHeroUnit`, `bzDoseAvgLabel`, `bzDoseStatusPill`  
Settings: `cbFullScrn`, `editPolinomA/B/C`, `rbResolution1024/2048/4096`, ...  

### 8.2 Ключевые методы

**`showStatistics()`** — обновляет `txtStat1/2` (время, батарея, RSSI, точность) и `txtCompMED` (МЭД). При `viewPager.currentItem == 0` показывает время спектрометра, иначе — дозиметра.

**`readConfigParameters()` / `writeConfigParameters()`** — сериализация параметров в/из `SharedPreferences` ("device.properties").

**`formatClock(totalSec: Int, showSeconds: Boolean): String`** — форматирование длительности с учётом дней. См. раздел [0.4](#04-формат-таймера-с-днями--клик-переключение-в-секунды).

**`updateSpecSubtitle()`** — пересчитывает subtitle «КАНАЛОВ · N · X – Y кэВ» над заголовком Spectrum tab. Использует текущие коэф калибровки, `propComparator`, `bitsChannel` и `spectrResolution`. См. раздел [0.25](#025-subtitle-на-вкладке-спектр-реальный-диапазон).

**`loadIsotop()`** — инициализирует справочник из 47 изотопов. Каждый изотоп: `Name: String`, `Energy: Int` (keV), `Channel: Int`, `Activity: Int` (0=нет активности). Примеры: K-40 (1460 keV), Cs-137 (662 keV), Am-241 (60 keV).

**`findIsotop(energy: Int)`** — поиск по энергии с допуском ±`realResolution` каналов.

**`readConfigFromDevice(hw: HardwareConfig)`** — копирует поля `HardwareConfig` в `HWprop*`-поля GO.

**`startBluetoothTimer()`** — запускает `intervalTimer` для автоматического переподключения (каждые 10 сек, начиная с 2 сек).

**`createRainbowColors(context, minCPS, maxCPS)`** — создаёт массив `impArr[32]` из `ImageProvider` с точками 32 цветов для карты.

---

## 9. Фрагменты

**Файл адаптера:** `NumberAdapter.kt`

```kotlin
override fun createFragment(position: Int): Fragment = when (position) {
    0 -> SpectrumFragment()
    1 -> HistoryFragment()
    2 -> DoseFragment()
    3 -> BluZMapFragment()
    4 -> SettingsFragment()
    else -> SpectrumFragment()
}
```

---

### 9.1 SpectrumFragment

**Файл:** `SpectrumFragment.kt` (~287 строк)  
**Layout:** `spectr_layout.xml` (portrait), `spectr_layout-land.xml` (landscape)

**Назначение:** отображение текущего спектра излучения **и** просмотр накопленного спектра истории (через horizontal swipe в нижнем pager-е, см. [0.13](#013-двухуровневая-навигация-на-вкладке-спектр-верхний-chart--нижний-swipe-pager)).

**UI-элементы:**
- `specterView` (ImageView) — canvas спектра, цепляется к `GO.drawSPECTER.imgView`
- `historyView` (ImageView, overlay в chart card) — canvas истории, цепляется к `GO.drawHISTORY.imgView`, `visibility="gone"` пока page 0
- `cursorView` (ImageView) — overlay курсора, цепляется к `GO.drawCURSOR.cursorView` (скрывается на page 1)
- `btnSpecterSS` (ImageButton) — старт/стоп записи спектра (в title row рядом с Save; скрывается на page 1)
- `btnSaveBQ` (ImageButton) — экспорт спектра
- `specToolbar` (LinearLayout с SMA/MED/MLEM/Calibrate/Clear; скрывается на page 1)
- `cbSMA`, `cbMED`, `cbMLEM` — фильтры; `btnCalibrate` — режим калибровки (3 точки)
- `txtIsotopInfo` (TextView) — информация об изотопе под курсором
- `specBottomPager` (ViewPager2) — нижний swipe-pager, 2 страницы:
  - page 0 `bz_spec_bottom_meas`: `bzRecLabel` («ИДЁТ ИЗМЕРЕНИЕ · …»), `bzRecClock` (время спектрометра)
  - page 1 `bz_spec_bottom_history`: метка «ИСТОРИЯ · …», `bzHistIntegralValue`, `bzHistAvgCpsValue`, кнопки Load/Save/Clear

**Touch-управление:**
- 1 палец: перемещение курсора → `GO.drawCURSOR.showCorsor(x, y)`
- 2 пальца: pinch-zoom (xZoom от 1 до 5), pan (xPosition)

**Инициализация canvas:** через `OnGlobalLayoutListener` на `specterView`:
```kotlin
specterView.viewTreeObserver.addOnGlobalLayoutListener {
    GO.drawObjectInit = true
    GO.drawSPECTER.init()
    GO.drawSPECTER.clearSpecter()
    GO.drawSPECTER.redrawSpecter(...)
}
```

**`applySpecterButtonState(running: Boolean)`:**
- `running = true` → иконка стоп + фон `bg_bz_circle_danger`
- `running = false` → иконка плей + фон `bg_bz_circle_accent`

**Калибровка:** кнопка `btnCalibrate` переключает режим. В режиме калибровки тап по спектру фиксирует пару (канал, энергия). После 3 точек вызывается `Mtrx.sysEq()` для решения системы уравнений и получения коэффициентов A, B, C.

**onDestroyView:** обнуляет nullable ссылки (bzSpecDoseValue, bzSpecAvgValue, etc.) в GO.

---

### 9.2 HistoryFragment (УДАЛЁН ИЗ НАВИГАЦИИ)

**Статус (с 2026-05-26):** файл `HistoryFragment.kt` и layout `history_layout.xml` оставлены в репозитории, но **в `NumberAdapter` больше не используются**. История доступна через swipe внутри `SpectrumFragment` (см. [0.13](#013-двухуровневая-навигация-на-вкладке-спектр-верхний-chart--нижний-swipe-pager)).

Кнопки `buttonLoadHistory` / `buttonHistorySave` / `buttonClearHistory` и сам canvas `historyView` теперь живут внутри Spectrum tab — биндинг в `SpectrumFragment.onViewCreated` + `BottomSwipeAdapter.onBindViewHolder` для page 1.

Команды и логика не менялись: `5u` — запрос истории, `8u` — очистка. `SaveBqMon` экспортирует тот же `drawSPECTER.spectrData`.

---

### 9.3 DoseFragment

**Файл:** `DoseFragment.kt` (~65 строк)  
**Layout:** `dozimetr_layout.xml`

**Назначение:** отображение текущей мощности дозы и гистограммы дозиметра.

**UI-элементы:**
- `dozView` (ImageView) — canvas гистограммы дозиметра
- `txtMAXDoze`, `txtMINDoze` — максимальное и минимальное значение
- `bzDoseHeroValue` — крупное значение мощности дозы
- `bzDoseStatusPill` — пилюля (Normal / L1 / L2 / L3)
- `bzDoseAvgLabel` — среднее CPS
- `buttonClearDoze` — сброс дозиметра (команда 3)

---

### 9.4 BluZMapFragment

**Файл:** `BluZMapFragment.kt` (~691 строка)  
**Layout:** `map_layout.xml`

**Назначение:** GPS-трекинг с визуализацией треков на Yandex MapKit.

#### Управление треками
- **Создать трек:** диалог ввода имени → `dao.insertTrack()` → `dao.activateTrack()`
- **Список треков:** `AlertDialog` со списком активных треков из `dao.getActiveTracks()`
- **Просмотр треков:** выбор из списка → `GO.currentTrack4Show = trackId` → `redrawtMap()`
- **Переименование:** диалог → `dao.editTrack()`
- **Удаление:** `dao.deleteTrack()` (физически не удаляет — помечает `isHidden = true`)

#### Запись трека
- **Record button** → `GO.trackIsRecordeed = true` / `false`
- При включённой записи каждый BLE-фрейм в MainActivity вызывает `recordTrackPoint()`
- **Фоновая запись:** при нажатии Record в фоне — стартует `BleMonitoringService`

#### Экспорт

**KML-формат:**
```xml
<Placemark>
  <styleUrl>#color{n}</styleUrl>
  <Point><coordinates>lon,lat,alt</coordinates></Point>
</Placemark>
```
Цвет точки определяется по CPS: `index = (cps - minCPS) / (maxCPS - minCPS) * 31`

**CSV-формат (столбцы):**  
`timestamp, latitude, longitude, altitude, speed, accuracy, CPS, magnetic_field`

Файлы сохраняются в `/Documents/BluZ/`.

#### Отрисовка карты (`redrawtMap`)
1. `dao.getPointsForTrack(trackId)` — загрузить все точки
2. `dao.getMaxMinForTrack(trackId)` — диапазон CPS
3. `createRainbowColors()` — подготовить 32 иконки цветов
4. Для каждой точки: вычислить цветовой индекс → разместить `PlacemarkMapObject`
5. Вычислить bounding box → `calculateZoomLevel()` → камера на трек

#### Интерактивность карты
- Тап по карте: находит ближайшую точку через `haversineDistance` (если < 30 м)
- Popup `map_hint.xml`: CPS, скорость, высота, время, точность, магнитное поле
- Кнопка "Моё место": `requestFreshLocationModern()` → центр карты на текущей позиции
- Ночной режим карты привязан к теме приложения (см. [0.3](#03-ночной-режим-карты--тема-приложения)): `GO.map?.isNightModeEnabled = !ThemePrefs.isDayTheme(requireContext())`
- Маркер текущего местоположения: красный крест + полупрозрачный серый круг точности (см. [0.7](#07-маркер-текущего-местоположения-на-карте)). GPS-обновления включаются при открытии вкладки (см. [0.8](#08-gps-включается-при-открытии-карты)). Маркеры пересоздаются после `mapObjects.clear()` (см. [0.9](#09-защита-маркера-от-mapobjectsclear)).

#### Жизненный цикл Yandex MapKit
```kotlin
onStart() → MapKitFactory.getInstance().onStart() + mapView.onStart()
onStop()  → MapKitFactory.getInstance().onStop() + mapView.onStop()
```

---

### 9.5 SettingsFragment

**Файл:** `SettingsFragment.kt` (~1300+ строк)  
**Layout:** `setup_layout.xml` — карточный ScrollView с 9 секциями

**Секции настроек:**

**1. Подключение (BLE):** MAC-адрес устройства, кнопка Scan, **Switch «Загружать настройки с BluZ»** (см. [0.20](#020-discovery-режим-сканирования--диалог-выбора-blu_z)). По кнопке Scan открывается кастомный диалог «Поиск BluZ» (`dialog_bz_scan.xml`): countdown 15 сек, динамический список найденных MAC, у каждого — иконка-колокольчик для опознавания прибора (см. [0.21](#021-ping-устройства--опознавание-blu_z-в-эфире))

**2. Отображение спектра:**
- Тип графика: линия / гистограмма
- Цвета: 4 SeekBar (ARGB) для каждого из 4 цветов (линейный, лог, фон-лин, фон-лог) × 2 стиля
- Формат сохранения: BqMon XML / CSV

**3. Детектор:**
- Загрузка χ-вектора из файла (.csv)
- Создание нового детектора
- Выбор / редактирование / удаление активного детектора

**4. Параметры прибора:**
- Квантизация звука/LED: none / ×1 / ×10
- Пороги уровней 1/2/3 (CPS)
- Звук/вибро по уровням (CheckBox)
- Коэффициенты полинома (A, B, C) для 1024/2048/4096 каналов
- CPS→мкР/ч коэффициент
- ВВ, компаратор, точность измерения, разрядность канала, время выборки АЦП

**5. Приложение:**
- `bzSwitchDayTheme` (SwitchCompat — не MaterialSwitch) — тема (управляет также режимом карты, см. [0.3](#03-ночной-режим-карты--тема-приложения))
- Полноэкранный режим (`cbFullScrn`)
- Единицы: мкР/ч / мкЗв/ч (синхронизировано с кликом по dose-readout, см. [0.5](#05-клик-по-доземощности-дозы--переключение-мкрч--мкзвч))
- Формат трека: KML / CSV
- Отступы (padding), зум спектра, уровень лога
- Авто-старт спектрометра

**6. Действия:** в карточке «ДЕЙСТВИЯ» три кнопки:
- `buttonLogs` — открыть `LogActivity`
- `buttonFind` — отправить `sendCommand(6u)` (поиск прибора через звук/вибро)
- `buttonCalibrateBatt` — диалог калибровки АКБ. См. [0.2](#02-калибровка-акб-возвращена-в-settings)

**Переключатель темы:**
```kotlin
bzSwitchDayTheme.setOnCheckedChangeListener { _, isChecked ->
    ThemePrefs.setDayTheme(requireContext(), isChecked)
    view.post { requireActivity().recreate() }
}
```

**Полноэкранный режим:**
```kotlin
cbFullScrn.setOnCheckedChangeListener { _, isChecked ->
    WindowCompat.setDecorFitsSystemWindows(window, !isChecked)
    // + cutout mode для notch-экранов
}
```

**Кнопки действий (floating bar):**
- **Restore** — восстановить параметры из прибора в UI (`GO.readConfigFromDevice(hw)`)
- **Save** — сохранить в SharedPreferences
- **Read** — прочитать конфигурацию из прибора (команда 7)
- **Write** — записать конфигурацию в прибор (команда 6)

---

## 10. Фоновый сервис — BleMonitoringService

**Файл:** `bgService.kt` (~530 строк)  
**Тип:** `ForegroundService` с каналом уведомлений **`"ble_monitor_silent"`** (с 2026-05-26, см. [0.18](#018-тихое-уведомление-blemonitoringservice)). Старый канал `"ble_monitor_channel"` явно удаляется через `deleteNotificationChannel` при создании.

### Возможности

- **BLE-сканирование:** непрерывное, с фильтром по MAC. Автоперезапуск каждые 5 минут (handler.postDelayed).
- **GPS-трекинг:** FusedLocationProviderClient, интервал 10 сек.
- **Магнетометр:** `SensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)`, буфер 20 измерений для усреднения.
- **Уведомление:** **тихое** (`IMPORTANCE_LOW` + `setOnlyAlertOnce(true)` + `setSilent(true)`), обновляется при каждом BLE-фрейме: CPS, мощность дозы, RSSI, magnitude. До первого пакета — прочерки + «Ожидание данных от прибора…» (флаг `hasFirstPacket`, см. [0.19](#019-прочерки-в-уведомлении-до-первого-ble-пакета)). Кнопка «Остановить» в уведомлении.

### Известные ограничения (2026-05-26)

- **Background BLE scan throttling.** Android может троттлить background scan вне foreground — иногда сервис не сразу цепляет advertising-пакеты, нужно ненадолго открыть приложение. Это системное ограничение Android, не баг приложения. Решено не лезть пока не появятся жалобы. Возможные направления при необходимости: сократить restart-интервал scanner (сейчас 5 мин), обрабатывать `onScanFailed` с перезапуском, использовать PendingIntent-scan (`startScan(filters, settings, callbackIntent)` — Android будит сервис при обнаружении).

### Жизненный цикл

```
onCreate()
  ├── Инициализация BluetoothAdapter
  ├── Подписка на SensorManager (магнетометр)
  └── Инициализация FusedLocationClient

onStartCommand(intent)
  ├── Если action == "STOP" → stopSelf()
  ├── startForeground(notificationId, notification)
  ├── startBLEScan()
  └── Запуск корутин: scan-restart, location-updates, track-saving

onDestroy()
  ├── unregisterListener(sensorEventListener)
  ├── stopScan()
  └── removeNotification()
```

### Взаимодействие с базой данных

Каждые 10 сек записывает точку трека: `dao.insertPoint(TrackDetail(trackId = GO.currentTrck, cps, lat, lon, ...))`. Точки привязываются к текущему активному треку `GO.currentTrck`.

---

## 11. Классы отрисовки

Все классы работают с Android `Canvas`/`Bitmap`. Объекты создаются в `App.onCreate()` и живут в GO на протяжении всего жизненного цикла приложения. При пересоздании Activity (поворот) `drawObjectInit` устанавливается в `true` → следующий вызов `init()` пересоздаёт Bitmap под новые размеры View.

---

### 11.1 drawSpecter

**Файл:** `drawSpecter.kt` (~367 строк)

**Поля:**
```
HSize, VSize: Int              — размер canvas
spectrData: DoubleArray(4096)  — исходные данные спектра
tmpSpecterData: DoubleArray    — данные после фильтрации
mlemBuffer: DoubleArray(4096)  — буфер MLEM-компенсации
ResolutionSpectr: Int          — 1024 / 2048 / 4096
koefLin, koefLog: Double       — коэффициенты масштабирования по Y
xSize: Double                  — ширина одного канала в пикселях
flagSMA, flagMEDIAN, flagMLEM  — флаги активных фильтров
```

**`init()`:** создаёт Bitmap только если `GO.drawObjectInit == true` или Bitmap не инициализирован.

**`redrawSpecter(spType, offSetX)`:**
1. Применяет SMA-фильтр (скользящее среднее с окном `GO.windowSMA`)
2. Применяет медианный фильтр (3-точечный)
3. Вычисляет максимум → масштабные коэффициенты `koefLin`, `koefLog`
4. Рисует линейную шкалу (`paintLin`) и логарифмическую (`paintLog`) за один проход
5. Режим "линия": `drawLine` от предыдущей точки к текущей (пропускает нулевые переходы)
6. Режим "гистограмма": вертикальные линии от Y до низа canvas
7. Толщина штриха: 1024→3px, 2048→2px, 4096→1px
8. После отрисовки: вычисляет `compMED` через `DoseCalculator.calculateH10DoseSafe`

**`clearSpecter()`:** заливает canvas чёрным, не сбрасывает данные.

**`resetSpecter()`:** обнуляет `spectrData`, `tmpSpecterData` и заливает canvas.

**`analyzeChiVector()` / `debugDoseCalculation()`:** отладочные методы (вывод в logcat).

---

### 11.2 drawHistory

**Файл:** `drawHistory.kt` (~173 строки)

Аналогична `drawSpecter`, но для исторического спектра:
- `historyData: DoubleArray(4096)` — данные истории
- Нет MLEM-буфера и расчёта дозы
- Нет zoom/pan (`xPosition`, `xZoom` не используются — история отображается полностью)
- Флаг инициализации: `GO.drawObjectInitHistory`

---

### 11.3 drawDozimeter

**Файл:** `drawDozimeter.kt` (~156 строк)

**Поля:**
```
dozimeterData: DoubleArray(512)  — 512 точек гистограммы дозиметра
dozimeterSMA: DoubleArray(512)   — SMA с окном 20
textMax, textMin: TextView       — отображение max/min
offsetX = 50.0f                  — отступ слева (для подписей)
```

**`redrawDozimeter()`:**
1. Находит max/min по массиву данных
2. Вычисляет SMA-20 в `dozimeterSMA`
3. Обновляет `textMax.text` / `textMin.text`
4. Рисует исходный сигнал (`dozPaint`, strokeWidth=2) и SMA-сглаживание (`dozPaintSMA`, strokeWidth=4)
5. Масштабирование: `dozKoef = VSize / (maxY - minY)`; если нет изменения — линия по середине

---

### 11.4 drawCursor

**Файл:** `drawCursor.kt` (~187 строк)

Overlay поверх спектра для указания позиции курсора.

**`init()`:** пересоздаёт bitmap по `cursorView.width × cursorView.height`. Условие пересоздания — флаг `GO.drawCursorObjectInit` (см. [0.10](#010-bitmap-курсора-пересоздаётся-при-повороте-экрана)). При успешной инициализации сбрасывает флаг и обнуляет сохранённую позицию `oldX/oldY`.

**`showCorsor(x, y)`:**
1. Стирает старый курсор (`hideCursor` — использует `PorterDuff.Mode.CLEAR`)
2. Рисует вертикальную линию от `Y=0` до `Y=VSize` (см. [0.11](#011-вертикальный-курсор-до-низа-графика-в-портретной-ориентации))
3. Вычисляет канал: `curChan = (x / GO.drawSPECTER.xSize + GO.xPosition).toInt()`
4. Если есть калибровка (cfA ≠ 0): пересчитывает канал в энергию: `E = cfA·ch² + cfB·ch + cfC`
5. Рисует круг на пересечении с логарифмическим графиком
6. Выводит: число отсчётов + канал/энергия в keV (повёрнуто на 90°)
7. Ищет изотоп в справочнике GO по энергии курсора
8. Если изотоп с `Activity > 0`: вычисляет активность источника:
   - `cntFon = realResolution × (spec[ch-realResolution] + spec[ch+realResolution])`
   - `cntPulse = sum(spec[ch-realResolution..ch+realResolution]) - cntFon`
   - `activ = isotop.Activity × cntPulse / spectrometerTime` (в Бк)

---

### 11.5 drawLogs

**Файл:** `drawLogs.kt` (~139 строк)

Не рисует на Canvas — работает с `TextView` + HTML-форматированием.

**`appendAppLogs(evtText, lev)`:**  
Уровни: 0=Ошибка (красный), 1=Инфо (синий), 2=Предупреждение (жёлтый), 3=Успех (зелёный), 4=Отладка (серый), 5=Отладка2 (пурпурный).  
Добавляет в `GO.appLogBuffer` строку с временной меткой и цветом.  
Вызывает `updateAppLogs()` через `MainScope().launch`.

**`updateLogs()`:**  
Рендерит последние 50 записей из `logData[]` в HTML в `logsText`. Время события вычисляется как `unixNow - (messTm - tm) * 1000`.

---

### 11.6 drawExmple

**Файл:** `drawExmple.kt` (~180 строк)

Превью-канвас для предварительного просмотра настроек цвета в SettingsFragment.

- `exampleData[200]` — линейная функция (0..199)
- `exampleFoneData[200]` — сдвинутая на 50 (50..199)
- Рисует 4 линии: Lin, Log, FoneLin, FoneLog — с теми же цветами/стилями, что и основной спектр
- Используется для предварительного просмотра при изменении цветов

---

## 12. Вычисления дозы — DoseCalculator

**Файл:** `calculateDose.kt` (~92 строки)

```kotlin
object DoseCalculator {
    var chiVectorOrg: DoubleArray = DoubleArray(1024)  // Активный χ-вектор детектора
```

### `calculateH10Dose(chi, spectrum, normalize, acquisitionTimeSec, energyMaxSpecter, energyMaxCHI)`

Вычисляет эквивалентную дозу H*(10) скалярным произведением χ-вектора и спектра.

**Алгоритм:**
1. `ratio = spectrum.size / 1024` — коэффициент ребиннинга (1, 2 или 4)
2. Для каждого канала χ: суммирует `ratio` соседних каналов спектра → `binSum`
3. `dose += chi[i] * binSum`
4. Если `normalize=true` и `spectrumSum > 0`: `dose /= spectrumSum`
5. Если `acquisitionTimeSec > 0`: `dose = dose / acquisitionTimeSec * 3600` (мощность, в час)

**Ограничения:**
- `chi.size` должен быть ровно 1024
- `spectrum.size` должен быть кратен 1024 (1024, 2048, 4096)

### `calculateH10DoseSafe(chi, spectrum, normalize, acquisitionTimeSec, energyMaxSpecter)`

Предварительно очищает спектр: заменяет отрицательные значения и NaN на 0.  
Если `energyMaxSpecter <= 0` — возвращает 0 без вычислений.

---

## 13. Калибровка — Mtrx

**Файл:** `Mtrx.kt` (~70 строк)

Решает систему из 3 уравнений методом определителей Крамера для нахождения коэффициентов квадратного полинома энергетической калибровки: **E = A·ch² + B·ch + C**.

**Использование:**
```kotlin
val mtrx = Mtrx()
mtrx.sysArray[0] = doubleArrayOf(channel1.toDouble(), energy1.toDouble())
mtrx.sysArray[1] = doubleArrayOf(channel2.toDouble(), energy2.toDouble())
mtrx.sysArray[2] = doubleArrayOf(channel3.toDouble(), energy3.toDouble())
mtrx.sysEq()
// Результат: mtrx.cA, mtrx.cB, mtrx.cC
```

Если определитель главной матрицы равен 0 — система не имеет решения, коэффициенты обнуляются (A=0, B=1, C=0 — линейная единичная функция).

---

## 14. Экспорт данных — SaveBqMon

**Файл:** `SaveBqMon.kt` (~262 строки)

**Директория:** `Environment.DIRECTORY_DOCUMENTS/BluZ/`  
Создаётся автоматически, если не существует.

### `saveSpecter()`

Диспетчер: выбирает формат (BqMon XML или CSV) и разрешение из `GO.HWspectrResolution`, вызывает нужный метод.

### `saveHistogramXML(context, spectrData, resolution)`

Формат: BqMon XML (совместим со спектральными программами).

Структура файла:
```xml
<?xml version="1.0"?>
<ResultDataFile>
  <ResultDataList>
    <ResultData>
      <SampleInfo>
        <Location>Lat: {lat} Lng: {lon} Alt: {alt} Speed: {spd}</Location>
        <Time>{ISO8601_startTime}</Time>
      </SampleInfo>
      <DeviceConfigReference>
        <Name>BluZ</Name>
        <Guid>fb3c0393-...</Guid>
      </DeviceConfigReference>
      <StartTime>{ISO8601}</StartTime>
      <EndTime>{ISO8601}</EndTime>
      <EnergySpectrum>
        <NumberOfChannels>{1024|2048|4096}</NumberOfChannels>
        <EnergyCalibration>
          <PolynomialOrder>2</PolynomialOrder>
          <Coefficients>
            <Coefficient>{C}</Coefficient>  <!-- порядок: C, B, A -->
            <Coefficient>{B}</Coefficient>
            <Coefficient>{A}</Coefficient>
          </Coefficients>
        </EnergyCalibration>
        <MeasurementTime>{spectrometerTime}</MeasurementTime>
        <Spectrum>
          <DataPoint>{value}</DataPoint>  <!-- по одному на канал -->
          ...
        </Spectrum>
      </EnergySpectrum>
    </ResultData>
  </ResultDataList>
</ResultDataFile>
```

Геолокация: берётся из `LocationManager.GPS_PROVIDER.getLastKnownLocation()`.  
Имя файла: `yyyyMMdd_HHmmss.xml`.

### `saveHistogramSPE(context, spectrData, resolution)`

Формат: простой CSV — `channel,counts` — совместим с большинством спектральных анализаторов.  
Имя файла: `yyyyMMdd_HHmmss.csv`.

---

## 15. База данных Room

**Файл:** `data/AppDatabase.kt`

```kotlin
@Database(
    entities = [Track::class, TrackDetail::class, DetectorType::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase()
```

Singleton через `getDatabase(context)`. `fallbackToDestructiveMigration(true)` — при несовпадении версии БД пересоздаётся (все данные теряются).

### Сущности

#### Track

```kotlin
@Entity(tableName = "tracks", indices = [Index("is_active")])
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at", defaultValue = "(strftime('%s', 'now'))") val createdAt: Long,
    val isActive: Boolean = false,
    val isHidden: Boolean = false,
    val cps2urh: Float = 0.0f       // Коэффициент перевода для данного трека
)
```

#### TrackDetail

```kotlin
@Entity(
    tableName = "track_details",
    foreignKeys = [ForeignKey(Track::class, ["id"], ["track_id"], onDelete = CASCADE)],
    indices = [Index("track_id"), Index("timestamp"), Index("latitude", "longitude")]
)
data class TrackDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val cps: Float,
    val altitude: Double,
    val speed: Float,
    val magnitude: Double,      // Модуль вектора магнитного поля (мкТл)
    @ColumnInfo(defaultValue = "(strftime('%s', 'now'))") val timestamp: Long = 0
)
```

#### DetectorType

```kotlin
@Entity(tableName = "detectors")
data class DetectorType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val changeAt: Long = System.currentTimeMillis(),
    val chiVector: DoubleArray,     // 1024 коэффициента дозовой чувствительности
    val curActive: Boolean = false
)
```

χ-вектор сериализуется через `DatabaseConverters`: `DoubleArray ↔ ByteArray` (little-endian, `Double.SIZE_BYTES = 8`).

### DosimeterDao — основные запросы

```kotlin
// Треки
@Query("SELECT * FROM tracks WHERE is_hidden = 0 ORDER BY created_at DESC")
fun getActiveTracks(): List<Track>

@Query("SELECT * FROM track_details WHERE track_id = :trackId ORDER BY timestamp ASC")
fun getPointsForTrack(trackId: Long): List<TrackDetail>

@Query("SELECT MIN(cps), MAX(cps) FROM track_details WHERE track_id = :trackId")
fun getMaxMinForTrack(trackId: Long): MinMax

// Детекторы
@Query("SELECT id, name, change_at, cur_active FROM detectors ORDER BY change_at DESC")
fun getAllDetector(): List<DetectorSummary>

@Query("SELECT * FROM detectors WHERE id = :id")
fun getByIdDetector(id: Long): DetectorType
```

---

## 16. GPS и карта

### ContinuousLocationManager

**Файл:** `gpsLocation.kt`

```kotlin
class ContinuousLocationManager(
    private val context: Any?,      // Не используется; FusedLocationClient берёт GO.mainContext
    private val onLocationUpdate: (Location) -> Unit
)
```

**`startLocationUpdates()`:** запрашивает `PRIORITY_HIGH_ACCURACY`, интервал 5 сек.

**`haversineDistance(lat1, lon1, lat2, lon2): Double`:**  
Вычисляет расстояние по поверхности Земли (R = 6 371 000 м) по формуле гаверсинуса.  
Используется в `BluZMapFragment` для нахождения ближайшей точки при тапе на карту.

**`calculateZoomLevel(mapView, latDiff, lonDiff, centerLat): Float`:**  
Вычисляет уровень zoom карты с учётом проекции Меркатора и размеров View. Возвращает значение в диапазоне [1, 21], умноженное на 0.8 (для отступа по краям).

**`calculateZoomFromDiagonal(mapView, diagonalMeters): Float`:**  
Альтернативный расчёт zoom по заданной диагонали в метрах.

### Yandex MapKit

Версия: `4.19.0-lite`. API-ключ передаётся через `BuildConfig.MAPKIT_API_KEY` (из `local.properties`, не коммитится).

Инициализация: `App.onCreate()` → `MapKitFactory.setApiKey() + initialize()`.  
Жизненный цикл: `BluZMapFragment.onStart()/onStop()`.

---

## 17. Вспомогательные классы

### ThemePrefs

**Файл:** `ThemePrefs.kt`

```kotlin
object ThemePrefs {
    private const val PREFS_NAME = "bz_ui_prefs"
    private const val KEY_DAY_THEME = "day_theme"

    fun isDayTheme(context: Context): Boolean
    fun setDayTheme(context: Context, dayTheme: Boolean)  // сохранить + apply
    fun apply(dayTheme: Boolean)  // AppCompatDelegate.setDefaultNightMode
    fun wrapContextWithTheme(base: Context): Context  // createConfigurationContext с uiMode
}
```

**`wrapContextWithTheme`** — критически важен для `FragmentActivity`. Создаёт конфигурационный контекст с явно выставленным `UI_MODE_NIGHT_YES/NO`. Без этого ресурсы (`values-night/`) не подключаются.

### propControl

**Файл:** `propControl.kt`

Обёртка над `SharedPreferences` ("device.properties").

```kotlin
class propControl {
    fun setPropBoolean(name: String, value: Boolean)
    fun getPropBoolean(name: String, default: Boolean): Boolean
    fun setPropStr/Float/Int/Byte(...)
    fun getPropStr/Int/Float/Byte(...)
}
```

### intervalTimer

**Файл:** `intervalTimer.kt`

Запускает `java.util.Timer` с задержкой 2 сек и периодом 10 сек.

```kotlin
internal class MyTimerTask : TimerTask() {
    override fun run() {
        if (GO.allPermissionAccept) {
            if (GO.needTerminate) System.exit(0)
            if (!GO.BTT.connected) {
                GO.BTT.destroyDevice()
                GO.BTT.initLeDevice()  // Автоматическое переподключение
            }
        }
    }
}
```

### buttonColor

**Файл:** `buttonColor.kt`

Утилитарный класс. Методы навигации помечены `@Deprecated` (остались от старой боковой панели).

Активный метод: `setSpecterColor(setCol, Col, fullColor): Int` — изменяет один компонент (A/R/G/B) цвета ARGB.

---

## 18. Тема и стили

### Структура тем

| Каталог | Применение |
|---------|-----------|
| `values/colors.xml` | Светлая тема (дневная) |
| `values-night/colors.xml` | Тёмная тема (ночная, дефолт) |
| `values/themes.xml` | `windowLightStatusBar=true` |
| `values-night/themes.xml` | `windowLightStatusBar=false` |

### Токены дизайн-системы

| Токен | Светлая | Тёмная |
|-------|---------|--------|
| `bz_bg` | #FFFFFF | #0A0D12 |
| `bz_surface` | #F7F8FA | #0F141B |
| `bz_accent` | #006B54 | #00D4AA |
| `bz_on_accent` | #FFFFFF | #0A0D12 |
| `bz_accent_soft` | #E8F5F1 | #003D30 |
| `bz_text` | #050A12 | #E7ECF1 |
| `bz_text_dim` | #6B7280 | #8D97A5 |
| `bz_text_muted` | #9CA3AF | #5A6475 |
| `bz_text_faint` | #D1D5DB | #2E3540 |
| `bz_warn` | #A96B00 | #FFB830 |
| `bz_danger` | #C91414 | #FF4444 |
| `bz_on_danger` | #FFFFFF | #FFFFFF |
| `bz_bt_on` | зелёный | зелёный |
| `bz_bt_warn` | оранжевый | оранжевый |
| `bz_bt_off` | красный | красный |

### Статусные цвет-селекторы (в `res/color/`)

**`bz_nav_icon.xml`:** `state_selected=true` → `bz_accent`, иначе → `bz_text_dim`  
**`bz_nav_text.xml`:** аналогично  
**`bz_switch_thumb.xml`:** `state_checked=true` → `bz_on_accent`, иначе → `bz_text`  
**`bz_switch_track.xml`:** состояния checked/unchecked

### Шрифты (Downloadable Google Fonts)

| Ресурс | Шрифт |
|--------|-------|
| `bz_inter.xml` | Inter Regular — основной UI |
| `bz_inter_bold.xml` | Inter Bold |
| `bz_jetbrains_mono.xml` | JetBrains Mono Regular — числовые значения |
| `bz_jetbrains_mono_bold.xml` | JetBrains Mono Bold |

---

## 19. Ресурсы

### Drawables — фоны кнопок и карточек

| Drawable | Назначение |
|----------|-----------|
| `bg_bz_card` | Фон карточки (rounded 12dp, surface) |
| `bg_bz_pill_bt` | Фон пилюли BT-статуса |
| `bg_bz_chip_accent` | Чип акцентного цвета |
| `bg_bz_chip_danger` | Чип опасности |
| `bg_bz_button_accent` | Прямоугольная кнопка акцента (corners 8dp) |
| `bg_bz_button_danger` | Прямоугольная кнопка опасности (corners 8dp) |
| `bg_bz_button_surface` | Кнопка поверхности |
| `bg_bz_circle_accent` | Круглая кнопка акцента (для Start) |
| `bg_bz_circle_danger` | Круглая кнопка опасности (для Stop) |
| `bg_bz_rail_tab` | Pill-фон для выбранной вкладки Rail |
| `bg_bz_icon_button` | Фон иконочной кнопки |
| `bg_bz_map_header_scrim` | Градиент в заголовке карты |
| `bg_bz_dot` | Точка-индикатор BT |

### Drawables — иконки (vector)

| Иконка | Описание |
|--------|---------|
| `ic_nav_spectrum` | Вкладка "Спектр" |
| `ic_nav_history` | Вкладка "История" |
| `ic_nav_dose` | Вкладка "Доза" |
| `ic_nav_map` | Вкладка "Карта" |
| `ic_nav_settings` | Вкладка "Настройки" |
| `ic_bz_play` | Старт спектрометра |
| `ic_bz_stop` | Стоп спектрометра |
| `ic_bz_save` | Сохранить |
| `ic_bz_clear` | Очистить |
| `ic_bz_radiation` | Символ радиации ☢ |
| `ic_bz_thermometer` | Температура |
| `ic_bz_clock` | Часы |
| `ic_bz_battery` | Батарея |
| `ic_bz_bluetooth` | Bluetooth |
| `ic_bz_warn` | Предупреждение |
| `ic_bz_target` | GPS "Моё место" (vector, белый штрих) |
| `ic_bz_pin` | GPS "Добавить точку" (vector, белый штрих) |

### Строковые ресурсы (strings.xml)

84+ строки. Основные:
- Заголовки вкладок: `nav_spectrum`, `nav_history`, `nav_dose`, `nav_map`, `nav_settings`
- Кнопки: `btn_start`, `btn_stop`, `btn_save`, `btn_clear`, `btn_read`, `btn_write`
- Статусы BT: `bt_connecting`, `bt_connected`, `bt_disconnected`
- Уровни тревоги: `level_1`, `level_2`, `level_3`, `level_normal`
- Единицы: `unit_cps`, `unit_urh`, `unit_usvh`, `unit_kev`

---

## 20. Манифест и разрешения

**Файл:** `AndroidManifest.xml`

**Application:** `android:name=".App"` — кастомный класс Application.

**Разрешения:**

| Разрешение | Назначение |
|-----------|-----------|
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Legacy BLE (< Android 12) |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` | BLE Android 12+ |
| `ACCESS_FINE_LOCATION` | GPS + BLE-сканирование (обязательно для BLE scan) |
| `ACCESS_COARSE_LOCATION` | Грубая геолокация |
| `FOREGROUND_SERVICE` | `BleMonitoringService` |
| `FOREGROUND_SERVICE_LOCATION` | Тип сервиса: геолокация |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Тип сервиса: BLE |
| `POST_NOTIFICATIONS` | Уведомление фонового сервиса (Android 13+) |
| `INTERNET` | Yandex MapKit tiles |

**Компоненты:**
```
MainActivity      — launcher, single-top, portrait+landscape+reverse-portrait
LogActivity       — child of MainActivity
BleMonitoringService — foreground, type=location|connectedDevice
```

**Фичи:**
```
android.hardware.bluetooth_le — required
```

---

## 21. Сборка

**Файл:** `BluZ/build.gradle`

### Варианты сборки

| Вариант | Описание |
|---------|---------|
| `debug` | Отладка, не минифицирован |
| `releaseTest` | Подписан release-ключом, debuggable, не минифицирован — для тестирования на устройстве |
| `release` | Минифицирован (R8), обфусцирован (ProGuard), shrinkResources |

### Подпись

Ключ: `BluZ.jks`, хранится вне репозитория. В `gradle.properties` указан путь (`/home/ed/Documents/...` для Linux, `C:\...` для Windows). **При коммите вернуть путь на Linux.**

### Зависимости

```
androidx.core:core-ktx:1.16.0
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7
androidx.room:room-runtime:2.7.0
androidx.room:room-ktx:2.7.0
com.google.android.gms:play-services-maps:19.2.0
com.google.android.gms:play-services-location:21.3.0
com.yandex.mapkit:mapkit-mobile-sdk-lite:4.19.0-lite
com.google.firebase:firebase-crashlytics-ktx
```

### Конфигурация ресурсов

```gradle
resourceConfigurations += ["ru", "en"]
```

### Файлы, не коммитируемые в git

```
local.properties        — пути SDK, API-ключи
BluZ/src/main/res/values/google_maps_api.xml  — ключ Maps
google-services.json    — Firebase конфигурация
BluZ.jks                — ключ подписи
```

---

## 22. Формат BLE-фрейма (подробно)

Полный байтовый формат пакета, передаваемого прибором (из комментариев в `globalObj.kt`):

```
Байт    Описание
0-2     Заголовок '<B>' (0x3C, 0x42, 0x3E)
3       Тип передачи (0-6)
4       Зарезервировано
5-8     Общее число импульсов (uint32, little-endian)
9-12    Число импульсов за последнюю секунду (uint32)
13-16   Общее время в секундах (uint32)
17-20   Среднее CPS (float)
21-22   Температура (float16, °C)
23-24   Напряжение батареи (float16, В)
25-26   Коэф. полинома A для 1024 каналов (float16)
27-28   Коэф. полинома B для 1024 каналов (float16)
29-30   Коэф. полинома C для 1024 каналов (float16)
31-32   Коэф. пересчёта мкР/ч на CPS (float16)
33      Высокое напряжение (uint8)
34      Порог компаратора (uint8)
35      Уровень 1-го порога (uint8)
36      Уровень 2-го порога (uint8)
37      Уровень 3-го порога (uint8)
38      Битовая конфигурация:
          бит 0 — квантовая индикация светодиодом
          бит 1 — квантовая индикация звуком
          бит 2 — звук уровня 1
          бит 3 — звук уровня 2
          бит 4 — звук уровня 3
          бит 5 — вибро уровня 1
          бит 6 — вибро уровня 2
          бит 7 — вибро уровня 3
39      бит 0 — автозапуск спектрометра
        бит 1 — делитель клика ×10
        бит 2 — делитель LED ×10
40-41   Коэф. A для 2048 каналов (float16)
42-43   Коэф. B для 2048 каналов (float16)
44-45   Коэф. C для 2048 каналов (float16)
46-47   Коэф. A для 4096 каналов (float16)
48-49   Коэф. B для 4096 каналов (float16)
50-51   Коэф. C для 4096 каналов (float16)  ← неточно, уточнить по коду
52-53   Время работы спектрометра (uint16, сек)
54-55   Количество импульсов спектрометра (uint16)
56      Погрешность измерения дозиметра (uint8, %)
57(L)   Разрядность канала АЦП (младший байт)
57(H)   Время выборки АЦП — 3 бита в старшем байте
58      Конец заголовка

Данные (после байта 58):
562-661     Данные лога: 50 × (uint32 timestamp + uint8 action) = 250 байт
662+        Данные спектра (тип 1: 1024 × uint16; тип 2: 2048 × uint16; тип 3: 4096 × uint16)
            Данные истории (тип 4-6: аналогично)
Последние 2 байта:  Контрольная сумма (uint16 — сумма байтов 0..241 по модулю 65536)
```

---

*Документ сгенерирован на основе исходного кода BluZ v1.12. Последнее обновление: 2026-05-24.*
