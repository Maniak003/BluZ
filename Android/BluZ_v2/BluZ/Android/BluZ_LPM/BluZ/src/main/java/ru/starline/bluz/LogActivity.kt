package ru.starline.bluz

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

/**
 * Активность-хост для [LogFragment]. Открывается из [SettingsFragment] кнопкой
 * «Журнал событий» в карточке «ДЕЙСТВИЯ».
 *
 * **Базовый класс:** [AppCompatActivity] (в отличие от [MainActivity]) — отдельная Activity
 * с back-кнопкой, не содержит BLE-логики. Тема работает через [ThemePrefs.wrapContextWithTheme]
 * в [attachBaseContext] и через `AppCompatDelegate` (доступен в AppCompatActivity).
 */
class LogActivity : AppCompatActivity() {

    /** См. документацию у [MainActivity.attachBaseContext]. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(ThemePrefs.wrapContextWithTheme(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val dayTheme = ThemePrefs.isDayTheme(this)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bz_bg)
        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bz_bg)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = dayTheme
            isAppearanceLightNavigationBars = dayTheme
        }

        findViewById<ImageButton>(R.id.buttonLogBack).setOnClickListener {
            finish()
        }
    }
}
