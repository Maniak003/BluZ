package ru.starline.bluz

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView.OnCheckedChangeListener
import ru.starline.bluz.globalObj
import ru.starline.bluz.MainActivity

const val ARG_OBJECT = "oblect"

class NumberFragment : Fragment() {
    public lateinit var btnSpecterSS: Button
    private  lateinit var rbGroup: RadioGroup
    private lateinit var rbLine: RadioButton
    private lateinit var rbLg: RadioButton
    private lateinit var rbFoneLin: RadioButton
    private lateinit var tvColor: TextView
    private lateinit var selA: SeekBar
    private lateinit var selR: SeekBar
    private lateinit var selG: SeekBar
    private lateinit var selB: SeekBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var layoutNumber: Int = 1
        if (GO.pagerFrame == 0) {
            layoutNumber = R.layout.spectr_layout
        } else if (GO.pagerFrame == 1) {
            layoutNumber = R.layout.history_layout
        } else if (GO.pagerFrame == 2) {
            layoutNumber = R.layout.dozimetr_layout
        } else if (GO.pagerFrame == 3) {
            layoutNumber = R.layout.log_layout
        } else if (GO.pagerFrame == 4) {
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
                GO.drawSPECTER = drawSpecter(view.findViewById(R.id.specterView), view.findViewById(R.id.textStatistics1), view.findViewById(R.id.textStatistics2))
                //GO.drawSPEC.init()
                //GO.drawSPEC.clearSpecter()

                /* Старт набора спектра */
                btnSpecterSS = view.findViewById(R.id.buttonSpecterSS)
                btnSpecterSS.setOnClickListener {
                    if (btnSpecterSS.text == getString(R.string.textStartStop)) {
                        btnSpecterSS.setText(getString(R.string.textStartStop2))
                        btnSpecterSS.setTextColor(getResources().getColor(R.color.Red, GO.mainContext.theme))
                        GO.BTT.initLeDevice()
                    } else {
                        btnSpecterSS.setText(getString(R.string.textStartStop))
                        btnSpecterSS.setTextColor(getResources().getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.BTT.destroyDevice()
                    }
                }

                /* Сохранение спектра в файл */
                val btnSaveBQ: Button = view.findViewById(R.id.buttonSaveBQ)
                btnSaveBQ.setOnClickListener {
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                    GO.drawSPECTER.init()
                    GO.drawSPECTER.clearSpecter()
                }
            } else if (getInt(ARG_OBJECT) == 1) {   // История
            /*
            *   Обекты закладки история
            */
                /* Сохранение спектра в файл */
                val btnHistorySave: Button = view.findViewById(R.id.buttonHistorySave)
                btnHistorySave.setOnClickListener {
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_LONG).show()
                }

            } else if (getInt(ARG_OBJECT) == 2) {   // Дозиметр
            /*
            *   Обекты закладки дозиметра
            */
                /* Сброс дозиметра */
                val btnClearDose: Button = view.findViewById(R.id.buttonClearDoze)
                btnClearDose.setOnClickListener {
                    Toast.makeText(GO.mainContext, R.string.resetDosimeter, Toast.LENGTH_LONG).show()
                }
            } else if (getInt(ARG_OBJECT) == 3) {   // Логи
            /*
            *   Обекты закладки логов
            */
                /* Очистка логов */
                val btnCleaarLog: Button = view.findViewById(R.id.buttonClearLog)
                btnCleaarLog.setOnClickListener {
                    Toast.makeText(GO.mainContext, R.string.resetLogs, Toast.LENGTH_LONG).show()
                }

            } else if (getInt(ARG_OBJECT) == 4) {   // Настройки
                /*
                *   Обекты закладки настроек
                */
                /* Сохранение параметров */
                val btnSaveSetup: Button = view.findViewById(R.id.buttonSaveSetup)
                GO.textMACADR = view.findViewById(R.id.textMACADDR)
                if (! GO.LEMAC.isEmpty()) {
                    GO.textMACADR.setText(GO.LEMAC)
                }
                btnSaveSetup.setOnClickListener {
                    /* Сохраняем MAC адрес */
                    GO.LEMAC = GO.textMACADR.text.toString()
                    GO.PP.setPropStr(propADDRESS, GO.LEMAC)     // Сохраним MAC адрес устройства
                    GO.PP.setPropInt(propColorSpecterLin, GO.ColorLin)     // Сохраним цвет линейного графика
                    GO.PP.setPropInt(propColorSpecterLog, GO.ColorLog)     // Сохраним цвет логарифмического графика
                    GO.PP.setPropInt(propColorSpecterFone, GO.ColorFone)    // Сохранним цвет графика фона
                    Log.d("BluZ-BT", "mac addr: ")
                    Log.d("BluZ-BT", GO.LEMAC)
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                }

                /* Сканирование bluetooth устройств */
                //BTT = BluetoothInterface(indicatorBT)
                GO.scanButton = view.findViewById(R.id.buttonScanBT)
                GO.scanButton.setOnClickListener {
                    if (GO.scanButton.text == getString(R.string.textScan)) {
                        GO.scanButton.setText(getString(R.string.textScan2))
                        GO.scanButton.setTextColor(getResources().getColor(R.color.Red, GO.mainContext.theme))
                        /*
                        *   Сканирование BT устройств
                        */
                        GO.BTT.startScan(GO.textMACADR/*, btnScanBT*/)
                    } else {
                        GO.scanButton.setText(getString(R.string.textScan))
                        GO.scanButton.setTextColor(getResources().getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.BTT.stopScan()
                    }
                }

                /* Radiobuttons для выбора элемента настройки цвета */
                /*
                * TODO -- Нужно сделать imageView с тремя графиками для наглядности
                * Панель для отображения цвета
                */
                tvColor = view.findViewById(R.id.tvColor)
                if (GO.ColorLin == 0) {
                    GO.ColorLin = getResources().getColor(R.color.specterColorLin, GO.mainContext.theme)
                }
                if (GO.ColorLog == 0) {
                    GO.ColorLog = getResources().getColor(R.color.specterColorLog, GO.mainContext.theme)
                }
                if (GO.ColorFone == 0) {
                    GO.ColorFone = getResources().getColor(R.color.specterColorFone, GO.mainContext.theme)
                }
                var noChange: Boolean = true
                tvColor.setBackgroundColor(GO.ColorLin)
                rbLine = view.findViewById(R.id.RBLin)
                rbLg = view.findViewById(R.id.RBLg)
                rbFoneLin = view.findViewById(R.id.RBFoneLin)
                rbGroup = view.findViewById(R.id.rbTypeGroup)

                /* Выбор графика для изменения */
                rbGroup.setOnCheckedChangeListener  { _, checkedId ->
                    view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        if (checkedId == rbLine.id) {               // Цвет для линейного графика
                            tvColor.setBackgroundColor(GO.ColorLin)
                            selA.setProgress(Color.alpha(GO.ColorLin), false)
                            selR.setProgress(Color.red(GO.ColorLin), false)
                            selG.setProgress(Color.green(GO.ColorLin), false)
                            selB.setProgress(Color.blue(GO.ColorLin), false)
                        } else if (checkedId == rbLg.id) {          // Цвет для логарифмического графика
                            tvColor.setBackgroundColor(GO.ColorLog)
                            selA.setProgress(Color.alpha(GO.ColorLog), false)
                            selR.setProgress(Color.red(GO.ColorLog), false)
                            selG.setProgress(Color.green(GO.ColorLog), false)
                            selB.setProgress(Color.blue(GO.ColorLog), false)
                        } else if (checkedId == rbFoneLin.id) {     // Цвет для графика фона
                            tvColor.setBackgroundColor(GO.ColorFone)
                            selA.setProgress(Color.alpha(GO.ColorFone), false)
                            selR.setProgress(Color.red(GO.ColorFone), false)
                            selG.setProgress(Color.green(GO.ColorFone), false)
                            selB.setProgress(Color.blue(GO.ColorFone), false)
                        }
                        noChange = true
                    }
                }

                /* Установка прозрачности */
                selA = view.findViewById(R.id.seekBarA)
                selA.setProgress(Color.alpha(GO.ColorLin), false)
                selA.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        if (noChange) {
                            if (rbLine.isChecked) {
                                GO.ColorLin = GO.bColor.setSpecterColor(0, progress, GO.ColorLin)
                                tvColor.setBackgroundColor(GO.ColorLin)
                            } else if (rbLg.isChecked) {
                                GO.ColorLog = GO.bColor.setSpecterColor(0, progress, GO.ColorLog)
                                tvColor.setBackgroundColor(GO.ColorLog)
                            } else if (rbFoneLin.isChecked) {
                                GO.ColorFone = GO.bColor.setSpecterColor(0, progress, GO.ColorFone)
                                tvColor.setBackgroundColor(GO.ColorFone)
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка красного цвета */
                selR = view.findViewById(R.id.seekBarR)
                selR.setProgress(Color.red(GO.ColorLin), false)
                selR.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        if (noChange) {
                            if (rbLine.isChecked) {
                                GO.ColorLin = GO.bColor.setSpecterColor(1, progress, GO.ColorLin)
                                tvColor.setBackgroundColor(GO.ColorLin)
                            } else if (rbLg.isChecked) {
                                GO.ColorLog = GO.bColor.setSpecterColor(1, progress, GO.ColorLog)
                                tvColor.setBackgroundColor(GO.ColorLog)
                            } else if (rbFoneLin.isChecked) {
                                GO.ColorFone = GO.bColor.setSpecterColor(1, progress, GO.ColorFone)
                                tvColor.setBackgroundColor(GO.ColorFone)
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка зеленого цвета */
                selG = view.findViewById(R.id.seekBarG)
                selG.setProgress(Color.green(GO.ColorLin), false)
                selG.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        if (noChange) {
                            if (rbLine.isChecked) {
                                GO.ColorLin = GO.bColor.setSpecterColor(2, progress, GO.ColorLin)
                                tvColor.setBackgroundColor(GO.ColorLin)
                            } else if (rbLg.isChecked) {
                                GO.ColorLog = GO.bColor.setSpecterColor(2, progress, GO.ColorLog)
                                tvColor.setBackgroundColor(GO.ColorLog)
                            } else if (rbFoneLin.isChecked) {
                                GO.ColorFone = GO.bColor.setSpecterColor(2, progress, GO.ColorFone)
                                tvColor.setBackgroundColor(GO.ColorFone)
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                /* Установка синего цвета */
                selB = view.findViewById(R.id.seekBarB)
                selB.setProgress(Color.blue(GO.ColorLin), false)
                selB.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        if (noChange) {
                            if (rbLine.isChecked) {
                                GO.ColorLin = GO.bColor.setSpecterColor(3, progress, GO.ColorLin)
                                tvColor.setBackgroundColor(GO.ColorLin)
                            } else if (rbLg.isChecked) {
                                GO.ColorLog = GO.bColor.setSpecterColor(3, progress, GO.ColorLog)
                                tvColor.setBackgroundColor(GO.ColorLog)
                            } else if (rbFoneLin.isChecked) {
                                GO.ColorFone = GO.bColor.setSpecterColor(3, progress, GO.ColorFone)
                                tvColor.setBackgroundColor(GO.ColorFone)
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

            }
        }
    }
}