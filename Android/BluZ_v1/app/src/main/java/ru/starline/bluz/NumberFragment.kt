package ru.starline.bluz

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt

const val ARG_OBJECT = "oblect"

class NumberFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layoutNumber: Int = 1
        if (pagerFrame == 0) {
            layoutNumber = R.layout.spectr_layout
        } else if (pagerFrame == 1) {
            layoutNumber = R.layout.history_layout
        } else if (pagerFrame == 2) {
            layoutNumber = R.layout.dozimetr_layout
        } else if (pagerFrame == 3) {
            layoutNumber = R.layout.log_layout
        } else if (pagerFrame == 4) {
            layoutNumber = R.layout.setup_layout
        }
        return inflater.inflate(layoutNumber, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            if (getInt(ARG_OBJECT) == 0) {  // Спектр
                /*
                * Объекты закладки спектр
                */
                drawSPEC = drawSpecter(view.findViewById(R.id.specterView))

                /* Старт набора спектра */
                val btnSpecterSS: Button = view.findViewById(R.id.buttonSpecterSS)
                btnSpecterSS.setOnClickListener {
                    if (btnSpecterSS.text == getString(R.string.textStartStop)) {
                        btnSpecterSS.setText(getString(R.string.textStartStop2))
                        btnSpecterSS.setTextColor(getResources().getColor(R.color.Red, mainContext.theme))
                    } else {
                        btnSpecterSS.setText(getString(R.string.textStartStop))
                        btnSpecterSS.setTextColor(getResources().getColor(R.color.buttonTextColor, mainContext.theme))
                    }
                    drawSPEC.init()
                    drawSPEC.testLine()
                }

                /* Сохранение спектра в файл */
                val btnSaveBQ: Button = view.findViewById(R.id.buttonSaveBQ)
                btnSaveBQ.setOnClickListener {
                    Toast.makeText(mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                    drawSPEC.init()
                    drawSPEC.clearSpecter()
                }
            } else if (getInt(ARG_OBJECT) == 1) {   // История
            /*
            *   Обекты закладки история
            */
                /* Сохранение спектра в файл */
                val btnHistorySave: Button = view.findViewById(R.id.buttonHistorySave)
                btnHistorySave.setOnClickListener {
                    Toast.makeText(mainContext, R.string.saveComplete, Toast.LENGTH_LONG).show()
                }

            } else if (getInt(ARG_OBJECT) == 2) {   // Дозиметр
            /*
            *   Обекты закладки дозиметра
            */
                /* Сброс дозиметра */
                val btnClearDose: Button = view.findViewById(R.id.buttonClearDoze)
                btnClearDose.setOnClickListener {
                    Toast.makeText(mainContext, R.string.resetDosimeter, Toast.LENGTH_LONG).show()
                }
            } else if (getInt(ARG_OBJECT) == 3) {   // Логи
            /*
            *   Обекты закладки логов
            */
                /* Очистка логов */
                val btnCleaarLog: Button = view.findViewById(R.id.buttonClearLog)
                btnCleaarLog.setOnClickListener {
                    Toast.makeText(mainContext, R.string.resetLogs, Toast.LENGTH_LONG).show()
                }

            } else if (getInt(ARG_OBJECT) == 4) {   // Настройки
                /*
                *   Обекты закладки настроек
                */
                /* Сохранение параметров */
                val btnSaveSetup: Button = view.findViewById(R.id.buttonSaveSetup)
                textMACADR = view.findViewById(R.id.textMACADDR)
                if (! LEMAC.isEmpty()) {
                    textMACADR.setText(LEMAC)
                }
                btnSaveSetup.setOnClickListener {
                    /* Сохраняем MAC адрес */
                    LEMAC = textMACADR.text.toString()
                    PP.setPropStr(propADDRESS, LEMAC)
                    Log.d("BluZ-BT", "mac addr_: ")
                    Log.d("BluZ-BT", LEMAC)
                    Toast.makeText(mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                }

                /* Сканирование bluetooth устройств */
                val btnScanBT: Button = view.findViewById(R.id.buttonScanBT)
                val BTT = BluetoothInterface()
                btnScanBT.setOnClickListener {
                    if (btnScanBT.text == getString(R.string.textScan)) {
                        btnScanBT.setText(getString(R.string.textScan2))
                        btnScanBT.setTextColor(getResources().getColor(R.color.Red, mainContext.theme))
                        /*
                        *   Сканирование BT устройств
                        */
                        BTT.startScan(indicatorBT, textMACADR, btnScanBT)
                    } else {
                        btnScanBT.setText(getString(R.string.textScan))
                        btnScanBT.setTextColor(getResources().getColor(R.color.buttonTextColor, mainContext.theme))
                        BTT.stopScan()
                    }
                    //indicatorBT.setBackgroundColor(getResources().getColor(R.color.Green, mainContext.theme))
                }
            }
        }
    }
}