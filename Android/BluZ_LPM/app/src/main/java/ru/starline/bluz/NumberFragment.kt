package ru.starline.bluz

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

const val ARG_OBJECT = "oblect"

class NumberFragment : Fragment() {
    private  lateinit var rbGroup: RadioGroup
    private lateinit var rbLine: RadioButton
    private lateinit var rbLg: RadioButton
    private lateinit var rbFoneLin: RadioButton
    private lateinit var rbFoneLg: RadioButton
    private lateinit var selA: SeekBar
    private lateinit var selR: SeekBar
    private lateinit var selG: SeekBar
    private lateinit var selB: SeekBar
    private lateinit var rgTypeSpec: RadioGroup
    private lateinit var btnCalibrate: Button
    private lateinit var btnConfirmCalibrate: Button
    private lateinit var rbResolution: RadioGroup
    var noChange: Boolean = true
/*
    override fun onResume() {
        super.onResume()
        Log.d("BluZ-BT", "View: Resume.")
    }

    override fun onPause() {
        super.onPause()
        Log.d("BluZ-BT", "View: Pause.")
    }

    override fun onStop() {
        super.onStop()
        Log.d("BluZ-BT", "View: Stop.")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BluZ-BT", "View: Destroy.")

    }
*/

    /*
    *   Отображение конфигурационных параметров в закладке Setup
    */
    fun reloadConfigParameters() {
        GO.propButtonInit = false                               // Отключим реакцию в листенерах компонентов
        if (GO.LEMAC.isNotEmpty()) {
            GO.textMACADR.setText(GO.LEMAC)
        }
        when (GO.saveSpecterType) {
            0 -> {
                GO.rbSpctTypeBq.isChecked = true
            }
            1 -> {
                GO.rbSpctTypeSPE.isChecked = true
            }
        }
        when (GO.specterGraphType) {
            0 -> GO.rbLineSpectr.isChecked = true
            1 -> GO.rbGistogramSpectr.isChecked = true
        }
        /* Заполненние элементов управления из текущей конфигурации */
        GO.cbSoundKvant.isChecked = GO.propSoundKvant              // Звук прихода частицы
        GO.cbLedKvant.isChecked = GO.propLedKvant                  // Подсветка прихода частицы
        GO.cbSpectrometr.isChecked = GO.propAutoStartSpectrometr   // Запуск набора спектра при включении прибора
        GO.editRejectChann.setText(GO.rejectChann.toString())      // Количество не отображаемых каналов от начала

        /* Разрешение в конфигурации */
        when (GO.spectrResolution) {
            0 -> {
                GO.rbResolution1024.isChecked = true
            }
            1 -> {
                GO.rbResolution2048.isChecked = true
            }
            2 -> {
                GO.rbResolution4096.isChecked = true
            }
            else -> {
                GO.rbResolution1024.isChecked = true
            }
        }

        /* Значения порогов */
        GO.editLevel1.setText(GO.propLevel1.toString())
        GO.editLevel2.setText(GO.propLevel2.toString())
        GO.editLevel3.setText(GO.propLevel3.toString())

        /* Разрешения звука для порогов */
        GO.cbSoundLevel1.isChecked = GO.propSoundLevel1
        GO.cbSoundLevel2.isChecked = GO.propSoundLevel2
        GO.cbSoundLevel3.isChecked = GO.propSoundLevel3

        /* Разрешения вибро для порогов */
        GO.cbVibroLevel1.isChecked = GO.propVibroLevel1
        GO.cbVibroLevel2.isChecked = GO.propVibroLevel2
        GO.cbVibroLevel3.isChecked = GO.propVibroLevel3

        /* Значения коэффициентов полинома */
        val df = DecimalFormat(GO.acuricyPatern, DecimalFormatSymbols(Locale.US))
        var tmpA: String = ""
        var tmpB: String = ""
        var tmpC: String = ""
        when (GO.spectrResolution) {
            0 -> {
                tmpA = df.format(GO.propCoef1024A)
                tmpB = df.format(GO.propCoef1024B)
                tmpC = df.format(GO.propCoef1024C)
            }
            1 -> {
                tmpA = df.format(GO.propCoef2048A)
                tmpB = df.format(GO.propCoef2048B)
                tmpC = df.format(GO.propCoef2048C)
            }
            2 -> {
                tmpA = df.format(GO.propCoef4096A)
                tmpB = df.format(GO.propCoef4096B)
                tmpC = df.format(GO.propCoef4096C)
            }
        }

        GO.editPolinomA.setText(tmpA)
        GO.editPolinomB.setText(tmpB)
        GO.editPolinomC.setText(tmpC)
        /* CPS в uRh */
        GO.editCPS2Rh.setText(GO.propCPS2UR.toString())

        /* Значения DAC */
        GO.editHVoltage.setText(GO.propHVoltage.toString())
        GO.editComparator.setText(GO.propComparator.toString())

        /* SMA window */
        GO.editSMA.setText(GO.windowSMA.toString())
        GO.propButtonInit = true                   // Включим реакцию в листенерах компонентов

        /* Точность усреднения для дозиметра, количество импульсов */
        GO.aqureEdit.setText(GO.aqureValue.toString())

        /* Количество бит в канале */
        GO.bitsChannelEdit.setText(GO.bitsChannel.toString())
    }

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

    @OptIn(ExperimentalUnsignedTypes::class)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            //var ps = getInt(ARG_OBJECT)
            //Log.d("BluZ-BT", "Position: $ps ")
            if (getInt(ARG_OBJECT) == 0) {  // Спектр
                /*
                * Объекты закладки спектр
                */
                GO.btnSaveBQ = view.findViewById(R.id.buttonSaveBQ)

                GO.drawCURSOR.cursorView = view.findViewById(R.id.cursorView)

                GO.drawSPECTER.imgView = view.findViewById(R.id.specterView)
                GO.drawSPECTER.imgView.setOnTouchListener { v, event ->
                    val x: Float = event.x
                    val y: Float = event.y
                    if ((event.getAction() == MotionEvent.ACTION_DOWN)|| (event.getAction() == MotionEvent.ACTION_MOVE)) {
                        if ((GO.drawCURSOR.oldX != x) /*|| (GO.drawCURSOR.oldY != y)*/)  {
                            GO.drawCURSOR.showCorsor(x, y)
                            //Log.i("BluZ-BT", "X: $x, Y: $y")
                        }
                    }
                    true
                }

                val CBSMA: CheckBox = view.findViewById(R.id.cbSMA)
                val CBMEDIAN: CheckBox = view.findViewById(R.id.cbMED)

                /*
                *   Калибровка
                */
                var calState: Int = 0
                val matrx = Mtrx()
                btnConfirmCalibrate = view.findViewById(R.id.buttonConfirmCalibrate)
                btnConfirmCalibrate.setOnClickListener {
                    if (btnConfirmCalibrate.text == "V") {
                        GO.needCalibrate = true
                        /* Отключаем показ статистики для перехода в настройки */
                        GO.txtStat1.visibility = View.INVISIBLE
                        GO.txtStat2.visibility = View.INVISIBLE
                        GO.txtStat3.visibility = View.INVISIBLE
                        GO.viewPager.setCurrentItem(4, false)
                        GO.bColor.resetToDefault()
                        GO.bColor.setToActive(GO.btnSetup)
                    } else {
                        calState = 0
                    }
                    btnConfirmCalibrate.text = "X"
                    btnCalibrate.text = "1"
                }
                /* Обработка кнопки ввода данных для калибровки */
                btnCalibrate = view.findViewById(R.id.buttonCalibrate)
                btnCalibrate.setOnClickListener {
                    btnConfirmCalibrate.text = "X"
                    if (GO.drawCURSOR.drawCursorInit) {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(it.context)
                        builder.setTitle("Enter energy for channel " + GO.drawCURSOR.curChan.toString() + ", point: " + (calState + 1).toString())
                        val inEnergy = EditText(context)
                        inEnergy.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        inEnergy.inputType = InputType.TYPE_CLASS_NUMBER
                        builder.setView(inEnergy)
                        builder.setPositiveButton("Add") { dialog, which ->
                            if (inEnergy.text.isNotEmpty()) {
                                matrx.sysArray[calState][1] = inEnergy.text.toString().toDouble()
                                matrx.sysArray[calState][0] = GO.drawCURSOR.curChan.toDouble()
                                calState++
                                if (calState > 2) {
                                    calState = 0
                                    btnConfirmCalibrate.text = "V"
                                    btnCalibrate.text = "1"
                                    matrx.sysEq()
                                    when (GO.specterType) {
                                        0 -> {  // 1024
                                            GO.propCoef1024A = matrx.cA
                                            GO.propCoef1024B = matrx.cB
                                            GO.propCoef1024C = matrx.cC
                                        }

                                        1 -> {  // 2048
                                            GO.propCoef2048A = matrx.cA
                                            GO.propCoef2048B = matrx.cB
                                            GO.propCoef2048C = matrx.cC
                                        }

                                        2 -> {  // 4096
                                            GO.propCoef4096A = matrx.cA
                                            GO.propCoef4096B = matrx.cB
                                            GO.propCoef4096C = matrx.cC
                                        }
                                    }
                                } else {
                                    btnCalibrate.text = (calState + 1).toString()
                                }
                            }
                        }
                            .setNegativeButton("Cancel") { dialog, which ->
                                // Исполняемый код
                        }
                        builder.create()
                        builder.show()
                    }
                }
                GO.propButtonInit = false
                CBSMA.isChecked = GO.drawSPECTER.flagSMA
                CBMEDIAN.isChecked = GO.drawSPECTER.flagMEDIAN
                GO.propButtonInit = true

                /* Управление SMA фильтром */
                CBSMA.setOnCheckedChangeListener {buttonView, isChecked ->
                    if (GO.propButtonInit) {
                        GO.drawSPECTER.flagSMA = isChecked
                        if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                            /* specterType: 0 - 1024, 1 - 2048, 2 - 4096 */
                            //Log.d("BluZ-BT", "call drawSPEC")
                            GO.drawSPECTER.clearSpecter()
                            GO.drawSPECTER.redrawSpecter(GO.specterType)
                        } else {
                            //GO.drawObjectInit = true
                            Log.e("BluZ-BT", "drawSPEC is null")
                        }
                    }
                }

                /* Управление медианным фильтром */
                CBMEDIAN.setOnCheckedChangeListener {buttonView, isChecked ->
                    if (GO.propButtonInit) {
                        GO.drawSPECTER.flagMEDIAN = isChecked
                        if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                            /* specterType: 0 - 1024, 1 - 2048, 2 - 4096 */
                            //Log.d("BluZ-BT", "call drawSPEC")
                            GO.drawSPECTER.clearSpecter()
                            GO.drawSPECTER.redrawSpecter(GO.specterType)
                        } else {
                            //GO.drawObjectInit = true
                            Log.e("BluZ-BT", "drawSPEC is null")
                        }
                    }
                }

                /* Старт набора спектра */
                GO.btnSpecterSS = view.findViewById(R.id.buttonSpecterSS)
                GO.btnSpecterSSisInit = true
                /* Проверяем была ли инициализация ранее */
                if (GO.initBT) {
                    if (GO.BTT.connected) {         // Восстанавливаем кнопку запуска
                        GO.btnSpecterSS.text = getString(R.string.textStartStop2)
                        GO.btnSpecterSS.setTextColor(resources.getColor(R.color.Red, GO.mainContext.theme))
                    }
                    GO.drawSPECTER.init()
                } else {
                    GO.initBT = true
                }

                /* Обработка нажатия на кнопку Start/Stop */
                GO.btnSpecterSS.setOnClickListener {
                    /* Передача нажатия на кнопку Start/Stop спектрометра. */

                    /*
                    * Формат буфера для передачи
                    *
                    * 0,1,2         - Маркер <S>
                    * 3             - Режим
                    *                   0 - Настройки
                    *                   1 - Очистка спектра в приборе
                    *                   2 - Включение/Выключение спектрометра
                    *
                    * 242, 243      - Контрольная сумма
                    */
                    /* Передача данных в прибор */
                    GO.BTT.sendCommand(2u)

                    /* Изменение статуса кнопки */

                    if (GO.btnSpecterSS.text == getString(R.string.textStartStop)) {
                        GO.btnSpecterSS.text = getString(R.string.textStartStop2)
                        GO.btnSpecterSS.setTextColor(resources.getColor(R.color.Red, GO.mainContext.theme))
                    } else {
                        GO.btnSpecterSS.text = getString(R.string.textStartStop)
                        GO.btnSpecterSS.setTextColor(resources.getColor(R.color.buttonTextColor, GO.mainContext.theme))
                    }
                }

                /* Сохранение спектра в файл */
                when (GO.saveSpecterType) {
                    0 -> {
                        GO.btnSaveBQ.text = GO.saveSpecterType1
                    }
                    1 -> {
                        GO.btnSaveBQ.text = GO.saveSpecterType2
                    }
                }

                GO.btnSaveBQ.setOnClickListener {
                    val saveBqMon = SaveBqMon()
                    saveBqMon.saveSpecter()
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                }
                /* Кнопка загрузки данных */

                /* Кнопка очистки буфера спектра */
                val btnClearSpecter : Button = view.findViewById(R.id.buttonClearSpectr)
                btnClearSpecter.setOnClickListener {
                    GO.BTT.sendCommand(1u)      // Очистка буфера спектрометра.
                    GO.drawSPECTER.clearSpecter()
                }

            } else if (getInt(ARG_OBJECT) == 1) {   // История
            /*
            *   Обекты закладки история
            */
                GO.drawHISTORY.imgView = view.findViewById(R.id.historyView)
                /* Запрос данных истории из прибора */
                val btnHistoryLoad: Button = view.findViewById(R.id.buttonLoadHistory)
                btnHistoryLoad.setOnClickListener {
                    GO.BTT.sendCommand(5u)      // Запрос исторического спектра
                    Toast.makeText(GO.mainContext, R.string.historyRequest, Toast.LENGTH_LONG).show()
                }
                /* Сохранение исторического спектра в файл */
                val btnHistorySave: Button = view.findViewById(R.id.buttonHistorySave)
                btnHistorySave.setOnClickListener {
                    val saveBqMon = SaveBqMon()
                    saveBqMon.saveSpecter()
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_LONG).show()
                }

            } else if (getInt(ARG_OBJECT) == 2) {   // Дозиметр
            /*
            *   Обекты закладки дозиметра
            */
                GO.drawDOZIMETER.textMax = view.findViewById(R.id.txtMAXDoze)
                GO.drawDOZIMETER.textMin = view.findViewById(R.id.txtMINDoze)
                GO.drawDOZIMETER.dozView = view.findViewById(R.id.dozView)

                if (! GO.initDOZ) {
                    GO.initDOZ = true
                    GO.drawDOZIMETER.Init()
                }

                /* Сброс дозиметра */
                val btnClearDose: Button = view.findViewById(R.id.buttonClearDoze)
                btnClearDose.setOnClickListener {
                    GO.BTT.sendCommand(3u)      // Очистка буфера дозиметра.
                    Toast.makeText(GO.mainContext, R.string.resetDosimeter, Toast.LENGTH_LONG).show()
                }

                /*
                *   Обекты закладки логов
                */
            } else if (getInt(ARG_OBJECT) == 3) {   // Логи
                /* Очистка логов */
                val btnCleaarLog: Button = view.findViewById(R.id.buttonClearLog)
                btnCleaarLog.setOnClickListener {
                    GO.BTT.sendCommand(4u)      // Очистка лога.
                    Toast.makeText(GO.mainContext, R.string.resetLogs, Toast.LENGTH_LONG).show()
                }
                GO.drawLOG.logView = view.findViewById(R.id.logScrolView)
                GO.drawLOG.logsText = view.findViewById(R.id.logsText)
                if (! GO.drawLOG.logsDrawIsInit) {
                    GO.drawLOG.updateLogs()
                    GO.drawLOG.logsDrawIsInit = true
                }

                /*
                *   Обекты закладки настроек
                */
            } else if (getInt(ARG_OBJECT) == 4) {   // Настройки
                GO.rbGistogramSpectr = view.findViewById(R.id.rbGistogram)
                GO.rbLineSpectr = view.findViewById(R.id.rbLine)
                GO.cbSoundKvant = view.findViewById(R.id.CBsoundKvant)
                GO.cbLedKvant = view.findViewById(R.id.CBledKvant)
                //GO.cbMarker = view.findViewById(R.id.CBSpectrometer)
                GO.editPolinomA = view.findViewById(R.id.editPolA)
                GO.editPolinomB = view.findViewById(R.id.editPolB)
                GO.editPolinomC = view.findViewById(R.id.editPolC)
                GO.editLevel1 = view.findViewById(R.id.editLevel1)
                GO.editLevel2 = view.findViewById(R.id.editLevel2)
                GO.editLevel3 = view.findViewById(R.id.editLevel3)
                GO.cbSoundLevel1 = view.findViewById(R.id.CBSoudLevel1)
                GO.cbSoundLevel2 = view.findViewById(R.id.CBSoudLevel2)
                GO.cbSoundLevel3 = view.findViewById(R.id.CBSoudLevel3)
                GO.cbVibroLevel1 = view.findViewById(R.id.CBVibroLevel1)
                GO.cbVibroLevel2 = view.findViewById(R.id.CBVibroLevel2)
                GO.cbVibroLevel3 = view.findViewById(R.id.CBVibroLevel3)
                GO.editCPS2Rh = view.findViewById(R.id.editCPS2RH)
                GO.rbResolution1024 = view.findViewById(R.id.RBResol1024)
                GO.rbResolution2048 = view.findViewById(R.id.RBResol2048)
                GO.rbResolution4096 = view.findViewById(R.id.RBResol4096)
                GO.editHVoltage = view.findViewById(R.id.editHvoltage)
                GO.editComparator = view.findViewById(R.id.editComparator)
                GO.cbSpectrometr = view.findViewById(R.id.CBSpectrometer)
                GO.editSMA = view.findViewById(R.id.editTextSMAWindow)
                GO.editRejectChann = view.findViewById(R.id.editTextRejectCann)
                GO.rbSpctTypeBq = view.findViewById(R.id.rbBqMon)
                GO.rbSpctTypeSPE = view.findViewById(R.id.rbSPE)
                GO.rbSpctType = view.findViewById(R.id.rbSpctType)
                GO.textMACADR = view.findViewById(R.id.textMACADDR)
                GO.aqureEdit = view.findViewById(R.id.editAquracy)
                GO.bitsChannelEdit = view.findViewById(R.id.editBitsChannel)

                reloadConfigParameters()

                /* Измененние коэффициента A для разных разрешений */
                GO.editPolinomA.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if(noChange) {
                            if (GO.editPolinomA.text.isNotEmpty()) {
                                try {
                                    if (GO.rbResolution1024.isChecked) {
                                        GO.propCoef1024A =
                                            GO.editPolinomA.text.toString().toFloat()
                                    } else if (GO.rbResolution2048.isChecked) {
                                        GO.propCoef2048A = GO.editPolinomA.text.toString().toFloat()
                                    } else if (GO.rbResolution4096.isChecked) {
                                        GO.propCoef4096A = GO.editPolinomA.text.toString().toFloat()
                                    }
                                } catch (e: NumberFormatException) {}
                            }
                        }
                    }
                })

                /* Измененние коэффициента B для разных разрешений */
                GO.editPolinomB.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if(noChange) {
                            if (GO.editPolinomB.text.isNotEmpty()) {
                                try {
                                if (GO.rbResolution1024.isChecked) {
                                    GO.propCoef1024B = GO.editPolinomB.text.toString().toFloat()
                                } else if (GO.rbResolution2048.isChecked) {
                                    GO.propCoef2048B = GO.editPolinomB.text.toString().toFloat()
                                } else if (GO.rbResolution4096.isChecked) {
                                    GO.propCoef4096B = GO.editPolinomB.text.toString().toFloat()
                                }
                                } catch (e: NumberFormatException) {}
                            }
                        }
                    }
                })

                /* Измененние коэффициента C для разных разрешений */
                GO.editPolinomC.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if(noChange) {
                            if (GO.editPolinomC.text.isNotEmpty()) {
                                try {
                                if (GO.rbResolution1024.isChecked) {
                                    GO.propCoef1024C = GO.editPolinomC.text.toString().toFloat()
                                } else if (GO.rbResolution2048.isChecked) {
                                    GO.propCoef2048C = GO.editPolinomC.text.toString().toFloat()
                                } else if (GO.rbResolution4096.isChecked) {
                                    GO.propCoef4096C = GO.editPolinomC.text.toString().toFloat()
                                }
                                } catch (e: NumberFormatException) {}
                            }
                        }
                    }
                })

                /*
                 *  Чтение конфигурации из файла
                 */
                var btnRestoreSetup: Button = view.findViewById(R.id.buttonRestoreSetup)
                btnRestoreSetup.setOnClickListener {
                    //Log.d("BluZ-BT", "Restore from config.")
                    GO.readConfigParameters()
                    reloadConfigParameters()
                }
                /*
                 * Сохранение параметров в конфигурационном файле смартфона
                 */
                val btnSaveSetup: Button = view.findViewById(R.id.buttonSaveSetup)

                btnSaveSetup.setOnClickListener {
                    /* Тип файла для сохранения спектра */
                    if (GO.rbSpctTypeBq.isChecked) {
                        GO.saveSpecterType = 0
                    } else {
                        GO.saveSpecterType = 1
                    }
                    GO.rejectChann = GO.editRejectChann.text.toString().toInt();
                    /* Тип графика для отображения спектра */
                    if (GO.rbLineSpectr.isChecked) {                                       // Сохраним тип графика для вывода спектра
                        GO.specterGraphType = 0
                    } else {
                        GO.specterGraphType = 1
                    }
                    /* Звуковое сопровождение регистрации частицы */
                    GO.propSoundKvant = GO.cbSoundKvant.isChecked
                    /* Световое сопровождение регистрации частицы */
                    GO.propLedKvant = GO.cbLedKvant.isChecked
                    /* Запуск спектрометра при включении прибора (потребление 380uA) */
                    GO.propAutoStartSpectrometr = GO.cbSpectrometr.isChecked

                    GO.propLevel1 = GO.editLevel1.text.toString().toInt()              // Значение первого порога из редактора
                    GO.propLevel2 = GO.editLevel2.text.toString().toInt()              // Значение второго порога из редактора
                    GO.propLevel3 = GO.editLevel3.text.toString().toInt()              // Значение третьего порога из редактора

                    GO.propSoundLevel1 = GO.cbSoundLevel1.isChecked
                    GO.propSoundLevel2 = GO.cbSoundLevel2.isChecked
                    GO.propSoundLevel3 = GO.cbSoundLevel3.isChecked

                    GO.propVibroLevel1 = GO.cbVibroLevel1.isChecked
                    GO.propVibroLevel2 = GO.cbVibroLevel2.isChecked
                    GO.propVibroLevel3 = GO.cbVibroLevel3.isChecked
                    /* Коэффициент пересчета CPS в uRh */
                    GO.propCPS2UR = GO.editCPS2Rh.text.toString().toFloat()

                    /* Коэффициенты для полинома преобразования канала в энергию */
                    /*
                    GO.propCoef1024A = GO.editPolinomA.text.toString().toFloat()
                    GO.propCoef1024B = GO.editPolinomB.text.toString().toFloat()
                    GO.propCoef1024C = GO.editPolinomC.text.toString().toFloat()
                    GO.propCoef2048A = GO.editPolinomA.text.toString().toFloat()
                    GO.propCoef2048B = GO.editPolinomB.text.toString().toFloat()
                    GO.propCoef2048C = GO.editPolinomC.text.toString().toFloat()
                    GO.propCoef4096A = GO.editPolinomA.text.toString().toFloat()
                    GO.propCoef4096B = GO.editPolinomB.text.toString().toFloat()
                    GO.propCoef4096C = GO.editPolinomC.text.toString().toFloat()
                    */

                    /* SMA window сформируем нечетное число */
                    GO.windowSMA = (GO.editSMA.text.toString().toInt() / 2).toInt() * 2 + 1
                    /* Уровень высокого напряжения */
                    GO.propHVoltage = GO.editHVoltage.text.toString().toUShort()
                    /* Уровень компаратора */
                    GO.propComparator = GO.editComparator.text.toString().toUShort()

                    if (GO.rbResolution1024.isChecked) {           // Разрешение прибора.
                        GO.spectrResolution = 0
                    } else if (GO.rbResolution2048.isChecked) {
                        GO.spectrResolution = 1
                    } else if (GO.rbResolution4096.isChecked) {
                        GO.spectrResolution = 2
                    } else {
                        GO.spectrResolution = 0
                    }

                    /* Точность усреднения для дозиметра, количество импульсов */
                    GO.aqureValue = GO.aqureEdit.text.toString().toInt()

                    /* Количество бит в канале */
                    GO.bitsChannel = GO.bitsChannelEdit.text.toString().toInt()
                    if (GO.bitsChannel < 16 || GO.bitsChannel > 32) {
                        GO.bitsChannel = 20
                    }

                    Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())

                    GO.writeConfigParameters()      // Сохраненние конфигурации.

                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                    if (GO.LEMAC.length == 17 &&  GO.LEMAC[0] != 'X') { // MAC адрес настроен, продолжаем работу.
                        GO.tmFull.startTimer();
                    } else {
                        GO.tmFull.stopTimer()
                    }
                }

                /* Сканирование bluetooth устройств */
                GO.scanButton = view.findViewById(R.id.buttonScanBT)
                GO.scanButton.setOnClickListener {
                    if (GO.scanButton.text == getString(R.string.textScan)) {
                        GO.scanButton.setText(getString(R.string.textScan2))
                        GO.scanButton.setTextColor(getResources().getColor(R.color.Red, GO.mainContext.theme))
                        /*
                        *   Сканирование BT устройств
                        */
                        GO.BTT.startScan(GO.textMACADR)
                    } else {
                        GO.scanButton.setText(getString(R.string.textScan))
                        GO.scanButton.setTextColor(getResources().getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.BTT.stopScan()
                    }
                }

                /* Чтение настроек из прибора */
                GO.btnReadFromDevice = view.findViewById(R.id.buttonReadFromDevice)
                GO.btnReadFromDevice.setOnClickListener {
                    if (GO.configDataReady) {
                        GO.readConfigFormDevice()
                        reloadConfigParameters()
                    }
                }

                /* Запись настроек в прибор */
                GO.btnWriteToDevice = view.findViewById(R.id.buttonWriteToDevice)
                GO.btnWriteToDevice.setOnClickListener {

                    /*
                    * Формат буфера для передачи
                    *
                    * 0,1,2         - Маркер <S>
                    * 3             - Режим
                    *                   0 - Настройки
                    *                   1 - Очистка спектра в приборе
                    * 4,5,6,7       - Первый порог в uR
                    * 8,9,10,11     - Второй порог в uR
                    * 12,13,14,15   - Третий порог в uR
                    * 16,17,18,19   - Коэффициент пересчета CPS в uR
                    * 20            - Битовые флаги управления светодиодом, звуком и вибро
                    *                   0 - Светодиодная индикация прихода частицы (1 - включена, 0 - выключена)
                    *                   1 - Звуковое сопровождение прихода частицы  (1 - включено, 0 - выключено)
                    *                   2 - Звуковая сигнализация 1 порог   (1 - включено, 0 - выключено)
                    *                   3 - Звуковая сигнализация 2 порог   (1 - включено, 0 - выключено)
                    *                   4 - Звуковая сигнализация 3 порог   (1 - включено, 0 - выключено)
                    *                   5 - Вибро сигнализация 1 порог   (1 - включено, 0 - выключено)
                    *                   6 - Вибро сигнализация 2 порог   (1 - включено, 0 - выключено)
                    *                   7 - Вибро сигнализация 3 порог   (1 - включено, 0 - выключено)
                    * 21,22,23,24   - Коэффициент A полинома преобразования канала в энергию для 1024.
                    * 25,26,27,28   - Коэффициент B полинома преобразования канала в энергию для 1024.
                    * 29,30,31,32   - Коэффициент C полинома преобразования канала в энергию для 1024.
                    * 33,34         - Уровень высокого напряжения
                    * 35,36         - Уровень компаратора
                    * 37            - Разрешение спектра 0 - 1024, 1 - 2048, 2 - 4096
                    * 38            - Битовые флаги управления прибором
                    *                   0 - Автоматический запуск набора спектра (1 - будет так же как в DoZer)
                    *                   1 -
                    *                   2 -
                    *                   3 -
                    *                   4 -
                    *                   5 -
                    *                   6 -
                    *                   7 -
                    * 39,40,41,42   - Коэффициент A полинома преобразования канала в энергию для 2048.
                    * 43,44,45,46   - Коэффициент B полинома преобразования канала в энергию для 2048.
                    * 47,48,49,50   - Коэффициент C полинома преобразования канала в энергию для 2048.
                    * 51,52,53,54   - Коэффициент A полинома преобразования канала в энергию для 4096.
                    * 55,56,57,58   - Коэффициент B полинома преобразования канала в энергию для 4096.
                    * 59,60,61,62   - Коэффициент C полинома преобразования канала в энергию для 4096.
                    * 63,64         - Точность усреднения дозиметра, количество импульсов.
                    * 65            - Разрядность канала (16 - 31 бит)
                    *
                    * 242, 243      - Контрольная сумма
                    */
                    /* Сохраненние настроек */
                    var convVal = ByteArray(4)

                    /* Перед сохранением загрузим текушие параметры из редактора */
                    /* Звуковое сопровождение регистрации частицы */
                    GO.propSoundKvant = GO.cbSoundKvant.isChecked
                    /* Световое сопровождение регистрации частицы */
                    GO.propLedKvant = GO.cbLedKvant.isChecked
                    /* Запуск спектрометра при включении прибора (потребление 380uA) */
                    GO.propAutoStartSpectrometr = GO.cbSpectrometr.isChecked

                    GO.propLevel1 = GO.editLevel1.text.toString().toInt()              // Значение первого порога из редактора
                    GO.propLevel2 = GO.editLevel2.text.toString().toInt()              // Значение второго порога из редактора
                    GO.propLevel3 = GO.editLevel3.text.toString().toInt()              // Значение третьего порога из редактора

                    GO.propSoundLevel1 = GO.cbSoundLevel1.isChecked
                    GO.propSoundLevel2 = GO.cbSoundLevel2.isChecked
                    GO.propSoundLevel3 = GO.cbSoundLevel3.isChecked

                    GO.propVibroLevel1 = GO.cbVibroLevel1.isChecked
                    GO.propVibroLevel2 = GO.cbVibroLevel2.isChecked
                    GO.propVibroLevel3 = GO.cbVibroLevel3.isChecked

                    /* Коэффициент пересчета CPS в uRh */
                    GO.propCPS2UR = GO.editCPS2Rh.text.toString().toFloat()
                    /* Уровень высокого напряжения */
                    GO.propHVoltage = GO.editHVoltage.text.toString().toUShort()
                    /* Уровень компаратора */
                    GO.propComparator = GO.editComparator.text.toString().toUShort()

                    if (GO.rbResolution1024.isChecked) {           // Разрешение прибора.
                        GO.spectrResolution = 0
                    } else if (GO.rbResolution2048.isChecked) {
                        GO.spectrResolution = 1
                    } else if (GO.rbResolution4096.isChecked) {
                        GO.spectrResolution = 2
                    } else {
                        GO.spectrResolution = 0
                    }

                    /* Подготовка массива для передачи */
                    /* Первый порог */
                    convVal = ByteBuffer.allocate(4).putInt(GO.propLevel1).array();
                    GO.BTT.sendBuffer[4] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[5] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[6] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[7] = convVal[3].toUByte()

                    /* Второй порог */
                    convVal = ByteBuffer.allocate(4).putInt(GO.propLevel2).array();
                    GO.BTT.sendBuffer[8] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[9] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[10] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[11] = convVal[3].toUByte()

                    /* Третий порог */
                    convVal = ByteBuffer.allocate(4).putInt(GO.propLevel3).array();
                    GO.BTT.sendBuffer[12] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[13] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[14] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[15] = convVal[3].toUByte()

                    /* Коэффициент пересчета cps в uRh */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCPS2UR).array();
                    GO.BTT.sendBuffer[16] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[17] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[18] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[19] = convVal[3].toUByte()

                    GO.BTT.sendBuffer[20] = 0u                  // Очистим флаги управления индикацией
                    /* Светодиод сопровождает приход частицы */
                    if (GO.cbLedKvant.isChecked) {
                        GO.BTT.sendBuffer[20] = 1u
                    }
                    /* Звук сопровождает приход частицы */
                    if (GO.cbSoundKvant.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000010.toUByte()
                    }
                    /* Звуковая сигнализация первого порога */
                    if (GO.cbSoundLevel1.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000100.toUByte()
                    }
                    /* Звуковая сигнализация второго порога */
                    if (GO.cbSoundLevel2.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00001000.toUByte()
                    }
                    /* Звуковая сигнализация третьего порога */
                    if (GO.cbSoundLevel3.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00010000.toUByte()
                    }
                    /* Вибро первого порога */
                    if (GO.cbVibroLevel1.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00100000.toUByte()
                    }
                    /* Вибро второго порога */
                    if (GO.cbVibroLevel2.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b01000000.toUByte()
                    }
                    /* Вибро третьего порога */
                    if (GO.cbVibroLevel3.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b10000000.toUByte()
                    }

                    /* Коэффициент A полинома для 1024 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024A).array();
                    GO.BTT.sendBuffer[21] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[22] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[23] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[24] = convVal[3].toUByte()

                    /* Коэффициент B полинома для 1024 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024B).array();
                    GO.BTT.sendBuffer[25] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[26] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[27] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[28] = convVal[3].toUByte()

                    /* Коэффициент C полинома для 1024 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024C).array();
                    GO.BTT.sendBuffer[29] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[30] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[31] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[32] = convVal[3].toUByte()

                    /* Коэффициент A полинома для 2048 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048A).array();
                    GO.BTT.sendBuffer[39] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[40] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[41] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[42] = convVal[3].toUByte()

                    /* Коэффициент B полинома для 2048 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048B).array();
                    GO.BTT.sendBuffer[43] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[44] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[45] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[46] = convVal[3].toUByte()

                    /* Коэффициент C полинома для 2048 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048C).array();
                    GO.BTT.sendBuffer[47] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[48] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[49] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[50] = convVal[3].toUByte()

                    /* Коэффициент A полинома для 4096 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096A).array();
                    GO.BTT.sendBuffer[51] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[52] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[53] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[54] = convVal[3].toUByte()

                    /* Коэффициент B полинома для 4096 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096B).array();
                    GO.BTT.sendBuffer[55] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[56] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[57] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[58] = convVal[3].toUByte()

                    /* Коэффициент C полинома для 4096 */
                    convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096C).array();
                    GO.BTT.sendBuffer[59] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[60] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[61] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[62] = convVal[3].toUByte()

                    /* Количество усредняемых импульсов для дозиметра */
                    GO.BTT.sendBuffer[63] = (GO.aqureEdit.text.toString().toUShort() and 255u).toUByte()
                    GO.BTT.sendBuffer[64] = ((GO.aqureEdit.text.toString().toInt() shr 8).toUShort() and 255u).toUByte()

                    /* Разрадность канала */
                    GO.BTT.sendBuffer[65] = GO.bitsChannelEdit.text.toString().toUByte()

                    /* Уровень высокого напряжения */
                    GO.BTT.sendBuffer[33] = (GO.propHVoltage and 255u).toUByte()
                    GO.BTT.sendBuffer[34] = ((GO.propHVoltage.toUInt() shr 8) and 255u).toUByte()

                    /* Уровень компаратора */
                    GO.BTT.sendBuffer[35] = (GO.propComparator and 255u).toUByte()
                    GO.BTT.sendBuffer[36] = ((GO.propComparator.toUInt() shr 8) and 255u).toUByte()

                    /* Разрешение спектра */
                    if (GO.rbResolution1024.isChecked) {
                        GO.BTT.sendBuffer[37] = 0u
                    } else if (GO.rbResolution2048.isChecked) {
                        GO.BTT.sendBuffer[37] = 1u
                    } else if (GO.rbResolution4096.isChecked) {
                        GO.BTT.sendBuffer[37] = 2u
                    } else {
                        GO.BTT.sendBuffer[37] = 0u
                    }

                    /* Запуск спектрометра при включении прибора */
                    if (GO.cbSpectrometr.isChecked) {
                        GO.BTT.sendBuffer[38] = 1u
                    } else {
                        GO.BTT.sendBuffer[38] = 0u
                    }
                    GO.BTT.sendCommand(0u)
                }

                /* Radiobuttons для выбора элемента настройки цвета */
                /*
                * Панель для отображения цвета
                */
                GO.drawExamp.exampleImgView = view.findViewById(R.id.tvColor)

                /* Установка цветов по умолчанию если не нашли в конфигурации */
                if (GO.ColorDosimeter == 0) {
                    GO.ColorDosimeter = resources.getColor(R.color.ColorDosimeter, GO.mainContext.theme)
                }
                if (GO.ColorDosimeterSMA == 0) {
                    GO.ColorDosimeterSMA = resources.getColor(R.color.ColorDosimeterSMA, GO.mainContext.theme)
                }
                if (GO.ColorLin == 0) {
                    GO.ColorLin = resources.getColor(R.color.specterColorLin, GO.mainContext.theme)
                }
                if (GO.ColorLog == 0) {
                    GO.ColorLog = resources.getColor(R.color.specterColorLog, GO.mainContext.theme)
                }
                if (GO.ColorFone == 0) {
                    GO.ColorFone = resources.getColor(R.color.specterColorFone, GO.mainContext.theme)
                }
                if (GO.ColorFoneLg == 0) {
                    GO.ColorFoneLg = resources.getColor(R.color.specterColorFoneLg, GO.mainContext.theme)
                }
                if (GO.ColorLinGisto == 0) {
                    GO.ColorLinGisto = resources.getColor(R.color.specterColorLinGisto, GO.mainContext.theme)
                }
                if (GO.ColorLogGisto == 0) {
                    GO.ColorLogGisto = resources.getColor(R.color.specterColorLogGisto, GO.mainContext.theme)
                }
                if (GO.ColorFoneGisto == 0) {
                    GO.ColorFoneGisto = resources.getColor(R.color.specterColorFoneGisto, GO.mainContext.theme)
                }
                if (GO.ColorFoneLgGisto == 0) {
                    GO.ColorFoneLgGisto = resources.getColor(R.color.specterColorFoneLgGisto, GO.mainContext.theme)
                }

                rbLine = view.findViewById(R.id.RBLin)
                rbLg = view.findViewById(R.id.RBLg)
                rbFoneLin = view.findViewById(R.id.RBFoneLin)
                rbFoneLg = view.findViewById(R.id.RBFoneLg)
                rbGroup = view.findViewById(R.id.rbTypeGroup)
                rgTypeSpec = view.findViewById(R.id.rgTypeSpectr)
                rbResolution = view.findViewById(R.id.RGResolution)

                /*
                *   Выбор типа сохранения спектра
                */
                GO.rbSpctType.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                    noChange = false
                    when (checkedId) {
                        GO.rbSpctTypeBq.id -> {
                            GO.btnSaveBQ.text = GO.saveSpecterType1
                            GO.saveSpecterType = 0
                        }
                        GO.rbSpctTypeSPE.id -> {
                            GO.btnSaveBQ.text = GO.saveSpecterType2
                            GO.saveSpecterType = 1
                        }
                    }
                    noChange = true
                }
                }

                /*
                * Выбор разрешения
                */
                rbResolution.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        val df = DecimalFormat(GO.acuricyPatern, DecimalFormatSymbols(Locale.US))
                        var tmpA: String = ""
                        var tmpB: String = ""
                        var tmpC: String = ""
                        when (checkedId) {
                            GO.rbResolution1024.id -> {
                                tmpA = df.format(GO.propCoef1024A)
                                tmpB = df.format(GO.propCoef1024B)
                                tmpC = df.format(GO.propCoef1024C)
                            }
                            GO.rbResolution2048.id -> {
                                tmpA = df.format(GO.propCoef2048A)
                                tmpB = df.format(GO.propCoef2048B)
                                tmpC = df.format(GO.propCoef2048C)
                            }
                            GO.rbResolution4096.id -> {
                                tmpA = df.format(GO.propCoef4096A)
                                tmpB = df.format(GO.propCoef4096B)
                                tmpC = df.format(GO.propCoef4096C)
                            }
                        }
                        GO.editPolinomA.setText(tmpA)
                        GO.editPolinomB.setText(tmpB)
                        GO.editPolinomC.setText(tmpC)
                        noChange = true
                    }
                }
                /*
                * Выбор типа отображения спектра. Линейный, гистограмма
                */
                rgTypeSpec.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        if (checkedId == GO.rbLineSpectr.id) {
                            GO.specterGraphType = 0
                            if (rbLine.isChecked) {                                   // Цвет для линейного графика
                                //tvColor.setBackgroundColor(GO.ColorLin)
                                selA.setProgress(Color.alpha(GO.ColorLin), false)
                                selR.setProgress(Color.red(GO.ColorLin), false)
                                selG.setProgress(Color.green(GO.ColorLin), false)
                                selB.setProgress(Color.blue(GO.ColorLin), false)
                            } else if (rbLg.isChecked) {                              // Цвет для логарифмического графика
                                //tvColor.setBackgroundColor(GO.ColorLog)
                                selA.setProgress(Color.alpha(GO.ColorLog), false)
                                selR.setProgress(Color.red(GO.ColorLog), false)
                                selG.setProgress(Color.green(GO.ColorLog), false)
                                selB.setProgress(Color.blue(GO.ColorLog), false)
                            } else if (rbFoneLin.isChecked) {                         // Цвет для линейного графика фона
                                //tvColor.setBackgroundColor(GO.ColorFone)
                                selA.setProgress(Color.alpha(GO.ColorFone), false)
                                selR.setProgress(Color.red(GO.ColorFone), false)
                                selG.setProgress(Color.green(GO.ColorFone), false)
                                selB.setProgress(Color.blue(GO.ColorFone), false)
                            } else if (rbFoneLg.isChecked) {                          // Цвет для логарифмического графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneLg)
                                selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                                selR.setProgress(Color.red(GO.ColorFoneLg), false)
                                selG.setProgress(Color.green(GO.ColorFoneLg), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                            }
                        } else if (checkedId == GO.rbGistogramSpectr.id) {
                            GO.specterGraphType = 1
                            if (rbLine.isChecked) {                                   // Цвет для линейного графика
                                //tvColor.setBackgroundColor(GO.ColorLinGisto)
                                selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                                selR.setProgress(Color.red(GO.ColorLinGisto), false)
                                selG.setProgress(Color.green(GO.ColorLinGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                            } else if (rbLg.isChecked) {                              // Цвет для логарифмического графика
                                //tvColor.setBackgroundColor(GO.ColorLogGisto)
                                selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                                selR.setProgress(Color.red(GO.ColorLogGisto), false)
                                selG.setProgress(Color.green(GO.ColorLogGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                            } else if (rbFoneLin.isChecked) {                         // Цвет для линейного графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                            } else if (rbFoneLg.isChecked) {                          // Цвет для логарифмического графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                            }
                        }
                    GO.drawExamp.exampRedraw()
                    noChange = true
                    }
                }

                /*
                 *  Выбор графика для изменения
                 *  Установка текущих значений на ползунках
                 */
                rbGroup.setOnCheckedChangeListener  { _, checkedId ->
                    view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        if (GO.rbLineSpectr.isChecked) {
                            if (checkedId == rbLine.id) {                                   // Цвет для линейного графика
                                //tvColor.setBackgroundColor(GO.ColorLin)
                                selA.setProgress(Color.alpha(GO.ColorLin), false)
                                selR.setProgress(Color.red(GO.ColorLin), false)
                                selG.setProgress(Color.green(GO.ColorLin), false)
                                selB.setProgress(Color.blue(GO.ColorLin), false)
                            } else if (checkedId == rbLg.id) {                              // Цвет для логарифмического графика
                                //tvColor.setBackgroundColor(GO.ColorLog)
                                selA.setProgress(Color.alpha(GO.ColorLog), false)
                                selR.setProgress(Color.red(GO.ColorLog), false)
                                selG.setProgress(Color.green(GO.ColorLog), false)
                                selB.setProgress(Color.blue(GO.ColorLog), false)
                            } else if (checkedId == rbFoneLin.id) {                         // Цвет для линейного графика фона
                                //tvColor.setBackgroundColor(GO.ColorFone)
                                selA.setProgress(Color.alpha(GO.ColorFone), false)
                                selR.setProgress(Color.red(GO.ColorFone), false)
                                selG.setProgress(Color.green(GO.ColorFone), false)
                                selB.setProgress(Color.blue(GO.ColorFone), false)
                            } else if (checkedId == rbFoneLg.id) {                          // Цвет для логарифмического графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneLg)
                                selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                                selR.setProgress(Color.red(GO.ColorFoneLg), false)
                                selG.setProgress(Color.green(GO.ColorFoneLg), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                            }
                        } else if (GO.rbGistogramSpectr.isChecked) {
                            if (checkedId == rbLine.id) {                                   // Цвет для линейного графика
                                //tvColor.setBackgroundColor(GO.ColorLinGisto)
                                selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                                selR.setProgress(Color.red(GO.ColorLinGisto), false)
                                selG.setProgress(Color.green(GO.ColorLinGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                            } else if (checkedId == rbLg.id) {                              // Цвет для логарифмического графика
                                //tvColor.setBackgroundColor(GO.ColorLogGisto)
                                selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                                selR.setProgress(Color.red(GO.ColorLogGisto), false)
                                selG.setProgress(Color.green(GO.ColorLogGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                            } else if (checkedId == rbFoneLin.id) {                         // Цвет для линейного графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                            } else if (checkedId == rbFoneLg.id) {                          // Цвет для логарифмического графика фона
                                //tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                            }
                        }
                        GO.drawExamp.exampRedraw()
                        noChange = true
                    }
                }

                /* Установка прозрачности  A - канал */
                selA = view.findViewById(R.id.seekBarA)
                selA.setProgress(Color.alpha(GO.ColorLin), false)
                selA.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar, progress: Int, fromUser: Boolean ) {
                        if (noChange) {
                            if (GO.rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(0, progress, GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(0, progress, GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(0, progress, GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLg)
                                }
                            } else if (GO.rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLgGisto)
                                }
                            }
                            GO.drawExamp.exampRedraw()
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
                            if (GO.rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(1, progress, GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(1, progress, GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(1, progress, GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLg)
                                }
                            } else if (GO.rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLgGisto)
                                }
                            }
                            GO.drawExamp.exampRedraw()
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
                            if(GO.rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(2, progress, GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(2, progress, GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(2, progress, GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLg)
                                }
                            } else if (GO.rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLgGisto)
                                }
                            }
                            GO.drawExamp.exampRedraw()
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
                            if (GO.rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(3, progress, GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(3, progress, GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(3, progress, GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneLg)
                                }
                            } else if (GO.rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneLgGisto)
                                }
                            }
                            GO.drawExamp.exampRedraw()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }
        }
    }
}