package ru.starline.bluz

import android.graphics.Color
import android.widget.Button
import android.widget.ImageButton

/**
 * Created by ed on 21,июнь,2024
 */
/**
 * Утилита для манипуляций с ARGB-цветом по компонентам и (legacy) изменение цвета кнопок.
 *
 * **Legacy.** Большинство методов помечены `@Deprecated` — раньше использовались для
 * подсветки активной вкладки в старой sidebar-навигации. После переработки на
 * BottomNavigationView (Phase 3) нужный функционал даёт state-list color selectors
 * (`bz_nav_icon.xml`, `bz_nav_text.xml` в res/color/).
 *
 * Активно используется только [setSpecterColor].
 */
class buttonColor {
    private var isInit = false

    @Deprecated("Old sidebar nav — no longer used with BottomNavigationView")
    fun initColor(vararg buttons: ImageButton) { isInit = false }

    @Deprecated("Old sidebar nav — no longer used with BottomNavigationView")
    fun resetToDefault() { /* no-op */ }

    @Deprecated("Old sidebar nav — no longer used with BottomNavigationView")
    fun setToActive(b: ImageButton) { /* no-op */ }

    fun resetActive(b: Button) { /* no-op */ }
    /**
     * Меняет один компонент (A/R/G/B) в ARGB-цвете.
     *
     * @param setCol Какой компонент изменить: 0=alpha, 1=red, 2=green, 3=blue.
     * @param Col Новое значение компонента (0..255).
     * @param fullColor Исходный ARGB-цвет.
     * @return Новый ARGB-цвет.
     *
     * Используется в SeekBar-обработчиках настроек цветов спектра.
     */
    fun setSpecterColor(setCol: Int, Col: Int, fullColor: Int): Int {
        var tmpColor: Int
        var tmpA: Int
        var tmpR: Int
        var tmpG: Int
        var tmpB: Int

        /* Разложение на составляющие */
        tmpA = Color.alpha(fullColor)
        tmpR = Color.red(fullColor)
        tmpG = Color.green(fullColor)
        tmpB = Color.blue(fullColor)
        /* Какой цвет меняем */
        when(setCol) {
            0 -> tmpA = Col
            1 -> tmpR = Col
            2 -> tmpG = Col
            3 -> tmpB = Col
        }
        /* Собираем цвет из составляющих */
        tmpColor = Color.argb(tmpA, tmpR, tmpG, tmpB)
        //Log.d("BluZ-BT", "Colors: A: $tmpA, R: $tmpR, G: $tmpG, B: $tmpB")
        return tmpColor
    }
}