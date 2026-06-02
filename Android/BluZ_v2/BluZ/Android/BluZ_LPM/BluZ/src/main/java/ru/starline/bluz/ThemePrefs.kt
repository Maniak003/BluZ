package ru.starline.bluz

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Управление выбором день/ночь темы.
 *
 * **Почему свой механизм.** [MainActivity] расширяет [androidx.fragment.app.FragmentActivity],
 * а не [androidx.appcompat.app.AppCompatActivity], поэтому штатный
 * `AppCompatDelegate.setDefaultNightMode` для resources не работает. Решение — оборачивать
 * Context через [wrapContextWithTheme] в `attachBaseContext`, явно выставляя `uiMode` в
 * Configuration. После этого Android корректно подключает ресурсы из `values-night/`.
 *
 * Хранится в отдельных SharedPreferences `bz_ui_prefs`, ключ `day_theme`.
 *
 * **Переключение во время работы** — `ThemePrefs.setDayTheme(ctx, isDay)` + `recreate()` Activity.
 */
object ThemePrefs {
    private const val PREFS_NAME = "bz_ui_prefs"
    private const val KEY_DAY_THEME = "day_theme"

    /** Текущий выбор темы. По умолчанию `false` (тёмная). */
    fun isDayTheme(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DAY_THEME, false)
    }

    /** Сохранить выбор + сразу применить через [apply]. Не пересоздаёт Activity — это
     *  должен сделать caller через `requireActivity().recreate()`. */
    fun setDayTheme(context: Context, dayTheme: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DAY_THEME, dayTheme)
            .apply()
        apply(dayTheme)
    }

    /** Применить [AppCompatDelegate.setDefaultNightMode]. Влияет только на новые/recreate-нутые
     *  Activity и только если они расширяют AppCompatActivity (у нас не так — основной механизм
     *  даёт [wrapContextWithTheme]). */
    fun apply(dayTheme: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dayTheme) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )
    }

    /**
     * Создаёт обёртку Context с принудительно выставленным `uiMode` (NIGHT_YES/NIGHT_NO)
     * в Configuration. Возвращает контекст, через который Android правильно резолвит
     * ресурсы по теме (значения из `values-night/` для тёмной).
     *
     * Вызывать в `MainActivity.attachBaseContext`. См. также DEVELOPER_GUIDE.md раздел 12.
     */
    fun wrapContextWithTheme(base: Context): Context {
        val dayTheme = isDayTheme(base)
        val nightMode = if (dayTheme) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        val config = Configuration(base.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        return base.createConfigurationContext(config)
    }
}
