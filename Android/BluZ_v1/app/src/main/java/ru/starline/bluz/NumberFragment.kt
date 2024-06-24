package ru.starline.bluz

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment

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
    private var colorLin: Color = Color.argb(255, 128, 128, 128).toColor()
    private var colorLg: Color = Color.argb(255, 0, 0, 0).toColor()
    private var colorFoneLin: Color = Color.argb(255, 0, 0, 0).toColor()
    private lateinit var rbLine: RadioButton
    private lateinit var rbLg: RadioButton
    private lateinit var rbFoneLin: RadioButton
    private lateinit var tvColor: TextView
    private lateinit var selA: SeekBar
    private lateinit var selR: SeekBar
    private lateinit var selG: SeekBar
    private lateinit var selB: SeekBar

    /* Получение цвета для отображения спектров */
    fun setSpecterColor(setCol: Int, Col: Int): Int {
        var tmpColor: Int = 0
        var tmpA: Int
        var tmpR: Int
        var tmpG: Int
        var tmpB: Int
        /* Для какого графика меняем цвет */
        if (rbLine.isChecked) {
            tmpColor = colorLin.toArgb()
        } else if (rbLg.isChecked) {
            tmpColor = colorLg.toArgb()
        } else if (rbFoneLin.isChecked) {
            tmpColor = colorFoneLin.toArgb()
        }

        /* Разложение на составляющие */
        tmpA = Color.alpha(tmpColor)
        tmpR = Color.red(tmpColor)
        tmpG = Color.green(tmpColor)
        tmpB = Color.blue(tmpColor)
            /* Какой цвет меняем */
        when(setCol) {
            0 -> tmpA = Col
            1 -> tmpR = Col
            2 -> tmpG = Col
            3 -> tmpB = Col
        }
        /* Собираем цвет из составляющих */
        tmpColor = Color.argb(tmpA, tmpR, tmpG, tmpB)
        if (rbLine.isChecked) {
            colorLin = Color.valueOf(tmpColor)
        } else if (rbLg.isChecked) {
            colorLg = Color.valueOf(tmpColor)
        } else if (rbFoneLin.isChecked) {
            colorFoneLin = Color.valueOf(tmpColor)
        }
        Log.d("BluZ-BT", "Colors: ")
        Log.d("BluZ-BT", tmpA.toString())
        Log.d("BluZ-BT", tmpR.toString())
        Log.d("BluZ-BT", tmpG.toString())
        Log.d("BluZ-BT", tmpB.toString())

        return tmpColor
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
                        BTT.initLeDevice()
                    } else {
                        btnSpecterSS.setText(getString(R.string.textStartStop))
                        btnSpecterSS.setTextColor(getResources().getColor(R.color.buttonTextColor, mainContext.theme))
                        BTT.destroyDevice()
                    }
                    drawSPEC.init()
                    /* Массив для теста */
                    for (idx in 0..drawSPEC.HSize ) {
                        drawSPEC.spectrData[idx] = Math.log(idx.toDouble())
                    }
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
                //BTT = BluetoothInterface(indicatorBT)
                val btnScanBT: Button = view.findViewById(R.id.buttonScanBT)
                btnScanBT.setOnClickListener {
                    if (btnScanBT.text == getString(R.string.textScan)) {
                        btnScanBT.setText(getString(R.string.textScan2))
                        btnScanBT.setTextColor(getResources().getColor(R.color.Red, mainContext.theme))
                        /*
                        *   Сканирование BT устройств
                        */
                        BTT.startScan(textMACADR, btnScanBT)
                    } else {
                        btnScanBT.setText(getString(R.string.textScan))
                        btnScanBT.setTextColor(getResources().getColor(R.color.buttonTextColor, mainContext.theme))
                        BTT.stopScan()
                    }
                }

                tvColor = view.findViewById(R.id.tvColor)
                rbLine = view.findViewById(R.id.RBLin)
                rbLg = view.findViewById(R.id.RBLg)
                rbFoneLin = view.findViewById(R.id.RBFoneLin)

                /* Установка прозрачности */
                selA = view.findViewById(R.id.seekBarA)
                selA.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        tvColor.setBackgroundColor(setSpecterColor(0, progress))
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка красного цвета */
                selR = view.findViewById(R.id.seekBarR)
                selR.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        tvColor.setBackgroundColor(setSpecterColor(1, progress))
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка зеленого цвета */
                selG = view.findViewById(R.id.seekBarG)
                selG.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        tvColor.setBackgroundColor(setSpecterColor(2, progress))
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка синего цвета */
                selB = view.findViewById(R.id.seekBarB)
                selB.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        tvColor.setBackgroundColor(setSpecterColor(3, progress))
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

            }
        }
    }
}