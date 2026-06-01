# BluZ — Портативный гамма-спектрометр

Мобильное Android-приложение для работы с устройством BluZ:
сцинтилляционный детектор с GPS-картированием радиационного фона.

GitHub исходного проекта: https://github.com/Maniak003/BluZ

---

## Что умеет приложение

- **Гамма-спектрометрия** — отображение живого спектра в реальном времени (1024 / 2048 / 4096 каналов)
- **Дозиметрия** — гистограмма 512 каналов, мощность дозы в мкР/ч или мкЗв/ч
- **История** — накопленный спектр за время измерения
- **Журнал** — аппаратный лог событий устройства
- **GPS-картирование** — запись трека с цветными метками уровня радиации на карте Яндекс
- **BLE-подключение** — работает по Bluetooth Low Energy с устройством BluZ

---

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin |
| Минимальный SDK | 27 (Android 8.1) |
| Target SDK | 36 |
| BLE | Android BluetoothGatt API |
| Карты | Yandex MapKit 4.19.0-lite |
| GPS | Google FusedLocationProvider |
| База данных | Room (SQLite) |
| Реактивность | Kotlin Coroutines + Flow |
| ViewModel | AndroidX Lifecycle 2.8.7 |
| Навигация | ViewPager2 + FragmentStateAdapter |
| Build | Gradle 9.3 / AGP 8.12.3 / Kotlin 2.2.21 |

---

## Структура проекта

```
BluZ_LPM/
├── BluZ/
│   └── src/main/
│       ├── java/ru/starline/bluz/
│       │   ├── MainActivity.kt          — главная Activity
│       │   ├── globalObj.kt             — глобальное состояние (god-object)
│       │   ├── BluetoothInterface.kt    — BLE-слой, SharedFlow<DeviceFrame>
│       │   ├── DeviceFrame.kt           — модель данных BLE-пакета
│       │   ├── DeviceViewModel.kt       — ViewModel, StateFlow<DeviceUiState>
│       │   ├── SpectrumFragment.kt      — вкладка «Спектр» (+ внутр. свайп История)
│       │   ├── DoseFragment.kt          — вкладка «Доза»
│       │   ├── BluZMapFragment.kt       — вкладка «Карта» (Yandex MapKit)
│       │   ├── SettingsFragment.kt      — вкладка «Настройки» + автокалибровка
│       │   ├── LogFragment.kt           — журнал событий (хостится в LogActivity)
│       │   ├── LogActivity.kt           — отдельная Activity для журнала
│       │   ├── AutoCalibrator.kt        — расчёт калибровки по Ra-226 (поиск пиков)
│       │   ├── AutoCalibrationController.kt — корутинная машина фаз автокалибровки
│       │   ├── NumberAdapter.kt         — ViewPager2 адаптер (4 вкладки)
│       │   ├── YandexMapActivity.kt     — App класс, инициализация MapKit
│       │   ├── ThemePrefs.kt            — переключение день/ночь темы
│       │   ├── bgService.kt             — BleMonitoringService (foreground service)
│       │   ├── gpsLocation.kt           — ContinuousLocationManager, расчёт zoom
│       │   ├── drawSpecter.kt           — Canvas-рисование спектра
│       │   ├── drawHistory.kt           — Canvas-рисование истории
│       │   ├── drawDozimeter.kt         — Canvas-рисование дозиметра
│       │   ├── drawLogs.kt              — Canvas-рисование лога
│       │   ├── drawCursor.kt            — курсор на графике спектра
│       │   ├── drawExmple.kt            — пример/образец графика
│       │   ├── propControl.kt           — управление SharedPreferences
│       │   ├── buttonColor.kt           — утилита цвета кнопок
│       │   ├── calculateDose.kt         — расчёт дозы
│       │   ├── intervalTimer.kt         — таймер интервалов
│       │   ├── SaveBqMon.kt             — сохранение данных
│       │   ├── Mtrx.kt                  — матричные вычисления
│       │   └── data/
│       │       ├── AppDatabase.kt       — Room database (v3)
│       │       ├── dao/DosimeterDao.kt  — DAO треков и точек
│       │       └── entity/
│       │           ├── Track.kt         — сущность трека
│       │           ├── TrackDetail.kt   — точка трека (GPS + CPS)
│       │           └── detectorType.kt  — тип детектора
│       ├── res/                         — ресурсы (layouts, drawables, values)
│       └── AndroidManifest.xml
├── gradle/libs.versions.toml            — версионный каталог зависимостей
├── build.gradle                         — конфигурация модуля
├── local.properties                     — пути SDK (не в git)
└── gradle.properties                    — настройки Gradle daemon
```

---

## Сборка

### Требования

- Android Studio (содержит JBR / JDK 21)
- Android SDK с Platform 36 и Build Tools 36 (устанавливается автоматически)
- Файл `BluZ/local.properties` с ключом Yandex MapKit (не в git)

### Windows

Файлы для локальной сборки (создать вручную, **не коммитить**):

**`BluZ_LPM/local.properties`**:
```properties
sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
```

**`BluZ_LPM/BluZ/local.properties`**:
```properties
sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
MAPKIT_API_KEY=<ваш_ключ>
```

**`BluZ_LPM/gradle.properties`** — заменить строку JDK:
```properties
#org.gradle.java.home=/usr/lib/jvm/temurin-17-jdk-amd64
org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr
```

**`BluZ_LPM/BluZ/build.gradle`** — изменить toolchain:
```groovy
kotlin { jvmToolchain(21) }
kotlinOptions { jvmTarget = '17' }
```

### Linux (оригинальная среда автора)

```bash
# local.properties создаётся Android Studio автоматически
echo "MAPKIT_API_KEY=<ваш_ключ>" >> BluZ/local.properties
./gradlew assembleDebug
```

### Установка на устройство

```bash
adb install -r BluZ/build/outputs/apk/debug/BluZ-debug.apk
```

---

## Yandex MapKit API Key

1. Зарегистрироваться на https://developer.tech.yandex.ru/
2. Создать ключ, подключить сервис **"MapKit — мобильный SDK"**
3. Добавить ограничение по пакету: `ru.starline.bluz`
4. Вставить ключ в `BluZ/local.properties` → `MAPKIT_API_KEY=...`

**Важно:** без разрешения `android.permission.INTERNET` в манифесте тайлы не загружаются.

---

## BLE-устройство BluZ

- Подключается по Bluetooth Low Energy
- Сервис UUID: `0000fe80-cc7a-482a-984a-7f2ed5b3e58f`
- Характеристика RX (нотификации): `0000fe81-8e22-4541-9d4c-21edae82ed19`
- Характеристика TX (запись): `0000fe82-8e22-4541-9d4c-21edae82ed19`
- MTU: 251 байт (запрашивается при подключении)
- Пакет данных: 6–40 MTU-фрагментов в зависимости от типа

Подробнее о протоколе — см. `BLE_PROTOCOL.md`.
