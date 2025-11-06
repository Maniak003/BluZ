package ru.starline.bluz

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.widget.Button
import android.widget.ImageButton

/**
 * Created by ed on 21,июнь,2024
 */
class buttonColor {
    lateinit var bSpectr: ImageButton
    lateinit var bHistory: ImageButton
    lateinit var bDosimetr: ImageButton
    lateinit var bLog: ImageButton
    lateinit var bSetup: ImageButton
    lateinit var bMap: ImageButton

    /* Настройка указателей на кнопки */
    fun initColor(b1: ImageButton, b2: ImageButton, b3: ImageButton, b4: ImageButton, b5: ImageButton, b6: ImageButton) {
        bSpectr = b1
        bHistory = b2
        bDosimetr = b3
        bLog = b4
        bSetup = b5
        bMap = b6
    }

    /* Установим цвета по умолчанию для всех кнопок */
    fun resetToDefault() {
        bSpectr.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bHistory.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bDosimetr.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bLog.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bSetup.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bMap.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
    }

    /* Установка активной кнопки */
    fun setToActive(b: ImageButton) {
        b.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.activeButtonColor, GO.mainContext.theme)))
    }

    fun resetActive(b: Button) {
        b.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
    }
    /* Получение цвета для отображения спектров */
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