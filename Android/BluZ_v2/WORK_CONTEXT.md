# BluZ — Рабочий контекст сессии

Последнее обновление: 2026-05-23

---

## Кто работает над проектом

- **Автор кода** — пишет на Linux, оригинальная среда: JDK 17 (Temurin),
  Android Studio
- **Пользователь** — конечный пользователь приложения, Windows 11,
  Android Studio установлен, устройство BluZ в наличии

---

## Текущее состояние

### Что работает
- ✅ Приложение собирается (`BUILD SUCCESSFUL`)
- ✅ Устанавливается на телефон через `adb install`
- ✅ Открывается в обеих ориентациях (portrait + landscape)
- ✅ Карта Яндекс отображает тайлы
- ✅ BLE-слой рефакторирован (SharedFlow / StateFlow)
- ✅ DeviceViewModel подключён
- ✅ APK: `C:\Bluz\BluZ-debug.apk`

### Что в процессе
- 🔄 Дизайн-ревью через Claude Design (промпт готов в `DESIGN_PROMPT.md`)

### Что планируется (Phase 3)
- ⬜ Реализовать новые XML-лэйауты по дизайну
- ⬜ Разбить `NumberFragment.kt` на 6 отдельных фрагментов
- ⬜ Заменить ViewPager2 на Navigation Component + BottomNavigationView
- ⬜ Добавить поддержку WindowInsets для notch и скруглений

---

## Среда сборки (Windows)

| Компонент | Путь / Значение |
|-----------|----------------|
| Android Studio | `C:\Program Files\Android\Android Studio` |
| JDK (JBR) | `C:\Program Files\Android\Android Studio\jbr` — OpenJDK 21.0.10 |
| Android SDK | `C:\Users\motok\AppData\Local\Android\Sdk` |
| adb | `C:\Users\motok\AppData\Local\Android\Sdk\platform-tools\adb.exe` |
| Проект | `C:\Bluz\BluZ\Android\BluZ_LPM` |
| Debug APK | `C:\Bluz\BluZ-debug.apk` |

### Команды для пересборки и установки

```powershell
# Сборка
cd "C:\Bluz\BluZ\Android\BluZ_LPM"
./gradlew assembleDebug

# Копирование APK
Copy-Item "BluZ\build\outputs\apk\debug\BluZ-debug.apk" "C:\Bluz\BluZ-debug.apk" -Force

# Установка
$adb = "C:\Users\motok\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r "C:\Bluz\BluZ-debug.apk"

# Лог крэша
& $adb logcat -d *:E | Select-String "AndroidRuntime|bluz|FATAL"
```

---

## Файлы, изменённые в ходе работы

### Созданные (новые файлы)
| Файл | Назначение |
|------|-----------|
| `BluZ/src/.../DeviceFrame.kt` | Data-классы модели BLE-пакета |
| `BluZ/src/.../DeviceViewModel.kt` | ViewModel с StateFlow<DeviceUiState> |
| `BluZ_LPM/local.properties` | SDK path для Windows (не в git) |
| `BluZ_LPM/BluZ/local.properties` | SDK path + MapKit API key (не в git) |

### Изменённые
| Файл | Изменения |
|------|----------|
| `BluetoothInterface.kt` | Переписан: SharedFlow/StateFlow, нет UI-кода, исправлен writeNext() |
| `MainActivity.kt` | Добавлены: DeviceViewModel, observeBle, Flow-подписки, GPS методы |
| `globalObj.kt` | Удалены: receiveData, sendCS |
| `NumberFragment.kt` | Исправлен MapKit lifecycle (onStart/onStop) |
| `AndroidManifest.xml` | Добавлен INTERNET, убран screenOrientation=landscape |
| `gradle/libs.versions.toml` | Добавлен lifecycle-viewmodel-ktx 2.8.7 |
| `BluZ/build.gradle` | Добавлена зависимость viewmodel; jvmToolchain(21) для Windows |
| `gradle.properties` | Добавлен Windows JDK path (закомментирован Linux path) |

### НЕ изменять (важно для автора)
- `gradle.properties` — при коммите вернуть `org.gradle.java.home` на Linux путь
- `BluZ/build.gradle` — при коммите вернуть `jvmToolchain(17)` и убрать `jvmTarget`
- `local.properties` файлы — в `.gitignore`, не коммитить

---

## Известные проблемы

### Баг в AppDatabase.kt
```kotlin
// Строка: val MIGRATION_2_3 = object : Migration(1, 3) {
// Должно быть: Migration(2, 3)
// Это означает, что если кто-то обновляется с версии 2, миграция не сработает
```
**Статус:** не исправлено, требует осторожности (риск потери данных при обновлении)

### Карта Яндекс
- API-ключ хранится в `BluZ/local.properties` (`MAPKIT_API_KEY=...`), вне git
- Сервис: MapKit — мобильный SDK
- Пакет: `ru.starline.bluz`
- При смене пакета (release подпись) потребуется обновить ключ

---

## Архитектурные решения и обоснования

### Почему SharedFlow, а не LiveData
`SharedFlow` не привязан к Android lifecycle и работает в любом слое.
LiveData требует `LifecycleOwner` и неудобна в BLE-коллбэках.

### Почему extraBufferCapacity = 4 в deviceFrames
Устройство присылает пакет каждую ~секунду. Если UI временно занят
(поворот экрана, garbage collection), буфер из 4 пакетов позволяет
не потерять данные. Больше не нужно — данные не критичные.

### Почему GO.* ещё не убран
Все 6 вкладок в одном `NumberFragment.kt` читают из `GO.*` для
рисования графиков. Убрать god-object можно только после разбиения
на отдельные фрагменты (Phase 3). Пока `DeviceViewModel` дублирует
данные как промежуточный слой.

### Почему jvmToolchain(21) на Windows
Android Studio поставляется с JBR (JetBrains Runtime) JDK 21.
Отдельного JDK 17 нет. Для Android разработки JDK 21 полностью
совместим — D8/R8 транспилирует всё в DEX для целевого minSdk.

---

## Контекст для следующей сессии

Когда дизайн из Claude Design будет готов:

1. **Получить скриншоты/описание** нового UI от пользователя
2. **Создать новые XML-лэйауты** для каждого экрана
3. **Разбить NumberFragment:**
   - `SpectrumFragment.kt`
   - `HistoryFragment.kt`
   - `DosimeterFragment.kt`
   - `LogFragment.kt`
   - `SettingsFragment.kt`
   - `MapFragment.kt`
4. **Добавить Navigation Component:**
   ```groovy
   implementation "androidx.navigation:navigation-fragment-ktx:2.7.7"
   implementation "androidx.navigation:navigation-ui-ktx:2.7.7"
   ```
5. **Каждый фрагмент** подписывается на `deviceViewModel.state`
   через `viewLifecycleOwner.lifecycleScope`
6. **Убрать GO.* зависимости** из фрагментов по мере их переноса

---

## Документация

| Файл | Содержимое |
|------|-----------|
| `C:\Bluz\README.md` | Обзор проекта, стек, структура, сборка |
| `C:\Bluz\ARCHITECTURE.md` | Архитектура, классы, поток данных, БД |
| `C:\Bluz\BLE_PROTOCOL.md` | BLE протокол, формат пакета, декомпрессия |
| `C:\Bluz\REFACTORING_LOG.md` | Журнал всех изменений Phase 1-2 |
| `C:\Bluz\DESIGN_PROMPT.md` | Промпт для Claude Design |
| `C:\Bluz\WORK_CONTEXT.md` | Этот файл — текущий контекст работы |
| `C:\Bluz\BluZ\Android\BluZ_LPM\REFACTORING_NOTES.md` | Детальные технические заметки |
