package ru.starline.bluz

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.starline.bluz.BuildConfig

/**
 * Точка входа в приложение — кастомный [Application] класс.
 *
 * Регистрируется в [AndroidManifest.xml] через `android:name=".App"`.
 *
 * **Что делает в onCreate (раньше любой Activity):**
 *  1. Применяет тему через [ThemePrefs.apply] — нужно ДО создания Activity, чтобы
 *     `AppCompatDelegate.setDefaultNightMode` сработал
 *  2. Инициализирует Yandex MapKit с API-ключом из `BuildConfig.MAPKIT_API_KEY`
 *  3. **Создаёт singleton draw-объекты** ([drawSpecter], [drawHistory], [drawDozimeter],
 *     [drawLogs], [drawCursor], [drawExmple]) и кладёт их в [globalObj] (`GO`).
 *
 * **Почему draw-объекты живут здесь, а не в [MainActivity].** Activity пересоздаётся при
 * повороте экрана; данные спектра ([drawSpecter.spectrData], 4096 значений) и истории —
 * не должны пропадать. Application живёт всё время существования процесса.
 *
 * Bitmap-буферы внутри draw-объектов пересоздаются при первом `init()` через флаги
 * `GO.drawObjectInit` / `drawObjectInitHistory` / `drawDozObjectInit` / `drawCursorObjectInit`
 * (фрагменты ставят их в true после получения новых размеров view).
 *
 * **Имя файла:** исторически `YandexMapActivity.kt`. Реальной Activity здесь нет.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        ThemePrefs.apply(ThemePrefs.isDayTheme(this))

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)

        GO.drawSPECTER = drawSpecter()
        GO.drawDOZIMETER = drawDozimeter()
        GO.drawHISTORY = drawHistory()
        GO.drawLOG = drawLogs()
        GO.drawCURSOR = drawCursor()
        GO.drawHistoryCURSOR = drawHistoryCursor()
        GO.drawExamp = drawExmple()
    }
}