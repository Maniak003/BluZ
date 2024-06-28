package ru.starline.bluz

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.widget.Button

/**
 * Created by ed on 21,июнь,2024
 */
class buttonColor {
    lateinit var bSpectr: Button
    lateinit var bHistory: Button
    lateinit var bDosimetr: Button
    lateinit var bLog: Button
    lateinit var bSetup: Button

    /* Настройка указателей на кнопки */
    fun initColor(b1: Button, b2: Button, b3: Button, b4: Button, b5: Button) {
        bSpectr = b1
        bHistory = b2
        bDosimetr = b3
        bLog = b4
        bSetup = b5
    }

    /* Установим цвета по умолчанию для всех кнопок */
    fun resetToDefault() {
        bSpectr.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bHistory.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bDosimetr.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bLog.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
        bSetup.setBackgroundTintList(ColorStateList.valueOf(GO.mainContext.getResources().getColor(R.color.buttonColor, GO.mainContext.theme)))
    }

    /* Установка активной кнопки */
    fun setToActive(b: Button) {
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