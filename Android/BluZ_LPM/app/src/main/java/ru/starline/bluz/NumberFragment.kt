package ru.starline.bluz

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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
import java.nio.ByteBuffer

const val ARG_OBJECT = "oblect"

class NumberFragment : Fragment() {
    private  lateinit var rbGroup: RadioGroup
    private lateinit var rbLine: RadioButton
    private lateinit var rbLg: RadioButton
    private lateinit var rbFoneLin: RadioButton
    private lateinit var rbFoneLg: RadioButton
    private lateinit var tvColor: TextView
    private lateinit var selA: SeekBar
    private lateinit var selR: SeekBar
    private lateinit var selG: SeekBar
    private lateinit var selB: SeekBar
    private lateinit var rgTypeSpec: RadioGroup
    private lateinit var btnCalibrate: Button
    private lateinit var btnConfirmCalibrate: Button
    private lateinit var rbResolution: RadioGroup
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

                /*
                *   Калибровка
                */
                var calState: Int = 0
                val matrx = Mtrx()
                btnConfirmCalibrate = view.findViewById(R.id.buttonConfirmCalibrate)
                btnConfirmCalibrate.setOnClickListener {
                    if (btnConfirmCalibrate.text == "V") {
                        /* Отключаем показ статистики */
                        GO.txtStat1.visibility = View.INVISIBLE
                        GO.txtStat2.visibility = View.INVISIBLE
                        GO.txtStat3.visibility = View.INVISIBLE
                        GO.viewPager.setCurrentItem(4, false)
                        GO.bColor.resetToDefault()
                        GO.bColor.setToActive(GO.btnSetup)
                    }
                    btnConfirmCalibrate.text = "X"
                    btnCalibrate.text = "1"
                }
                btnCalibrate = view.findViewById(R.id.buttonCalibrate)
                btnCalibrate.setOnClickListener {
                    if (GO.drawCURSOR.drawCursorInit) {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(it.context)
                        builder.setTitle("Enter energy for chanal " + GO.drawCURSOR.curChan.toString() + ", point: " + (calState + 1).toString())
                        val inEnergy = EditText(context)
                        inEnergy.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        inEnergy.inputType = InputType.TYPE_CLASS_NUMBER
                        builder.setView(inEnergy)
                        builder.setPositiveButton("Add") { dialog, which ->
                            matrx.sysArray[calState][1] = inEnergy.text.toString().toDouble()
                            matrx.sysArray[calState][0] = GO.drawCURSOR.curChan.toDouble()
                            calState++
                            if (calState > 2) {
                                calState = 0
                                btnConfirmCalibrate.text = "V"
                                btnCalibrate.text = "1"
                                matrx.sysEq()
                                when (GO.spectrResolution) {
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
                            .setNegativeButton("Cancel") { dialog, which ->
                                // Do something else.
                        }
                        builder.create()
                        builder.show()
                    }
                }
                GO.propButtonInit = false
                CBSMA.isChecked = GO.drawSPECTER.flagSMA
                GO.propButtonInit = true

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
                val btnSaveBQ: Button = view.findViewById(R.id.buttonSaveBQ)
                btnSaveBQ.setOnClickListener {
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
                val rbGistogramSpectr: RadioButton = view.findViewById(R.id.rbGistogram)
                val rbLineSpectr: RadioButton = view.findViewById(R.id.rbLine)
                val cbSoundKvant: CheckBox = view.findViewById(R.id.CBsoundKvant)
                val cbLedKvant: CheckBox = view.findViewById(R.id.CBledKvant)
                val cbMarker: CheckBox = view.findViewById(R.id.CBSpectrometer)
                val editPolinomA: EditText = view.findViewById(R.id.editPolA)
                val editPolinomB: EditText = view.findViewById(R.id.editPolB)
                val editPolinomC: EditText = view.findViewById(R.id.editPolC)
                val editLevel1: EditText = view.findViewById(R.id.editLevel1)
                val editLevel2: EditText = view.findViewById(R.id.editLevel2)
                val editLevel3: EditText = view.findViewById(R.id.editLevel3)
                val cbSoundLevel1: CheckBox = view.findViewById(R.id.CBSoudLevel1)
                val cbSoundLevel2: CheckBox = view.findViewById(R.id.CBSoudLevel2)
                val cbSoundLevel3: CheckBox = view.findViewById(R.id.CBSoudLevel3)
                val cbVibroLevel1: CheckBox = view.findViewById(R.id.CBVibroLevel1)
                val cbVibroLevel2: CheckBox = view.findViewById(R.id.CBVibroLevel2)
                val cbVibroLevel3: CheckBox = view.findViewById(R.id.CBVibroLevel3)
                val editCPS2Rh: EditText = view.findViewById(R.id.editCPS2RH)
                val rbResolution1024: RadioButton = view.findViewById(R.id.RBResol1024)
                val rbResolution2048: RadioButton = view.findViewById(R.id.RBResol2048)
                val rbResolution4096: RadioButton = view.findViewById(R.id.RBResol4096)
                val editHVoltage: EditText = view.findViewById(R.id.editHvoltage)
                val editComparator: EditText = view.findViewById(R.id.editComparator)
                val cbSpectrometr: CheckBox = view.findViewById(R.id.CBSpectrometer)
                val editSMA: EditText = view.findViewById(R.id.editTextSMAWindow)
                val editRejectChann: EditText = view.findViewById(R.id.editTextRejectCann)

                when (GO.specterGraphType) {
                    0 -> rbLineSpectr.isChecked = true
                    1 -> rbGistogramSpectr.isChecked = true
                }
                /* Заполненние элементов управления из текущей конфигурации */
                GO.propButtonInit = false                               // Отключим реакцию в листенерах компонентов
                cbSoundKvant.isChecked = GO.propSoundKvant              // Звук прихода частицы
                cbLedKvant.isChecked = GO.propLedKvant                  // Подсветка прихода частицы
                cbSpectrometr.isChecked = GO.propAutoStartSpectrometr   // Запуск набора спектра при включении прибора
                editRejectChann.setText(GO.rejectChann.toString())      // Количество не отображаемых каналов от начала

                /* Разрешение в конфигурации */
                when (GO.spectrResolution) {
                    0 -> {
                        rbResolution1024.isChecked = true
                    }
                    1 -> {
                        rbResolution2048.isChecked = true
                    }
                    2 -> {
                        rbResolution4096.isChecked = true
                    }
                    else -> {
                        rbResolution1024.isChecked = true
                    }
                }

                /* Значения порогов */
                editLevel1.setText(GO.propLevel1.toString())
                editLevel2.setText(GO.propLevel2.toString())
                editLevel3.setText(GO.propLevel3.toString())

                /* Разрешения звука для порогов */
                cbSoundLevel1.isChecked = GO.propSoundLevel1
                cbSoundLevel2.isChecked = GO.propSoundLevel2
                cbSoundLevel3.isChecked = GO.propSoundLevel3

                /* Разрешения вибро для порогов */
                cbVibroLevel1.isChecked = GO.propVibroLevel1
                cbVibroLevel2.isChecked = GO.propVibroLevel2
                cbVibroLevel3.isChecked = GO.propVibroLevel3

                /* Значения коэффициентов полинома */
                when (GO.spectrResolution) {
                    0 -> {
                        editPolinomA.setText(GO.propCoef1024A.toString())
                        editPolinomB.setText(GO.propCoef1024B.toString())
                        editPolinomC.setText(GO.propCoef1024C.toString())
                    }
                    1 -> {
                        editPolinomA.setText(GO.propCoef2048A.toString())
                        editPolinomB.setText(GO.propCoef2048B.toString())
                        editPolinomC.setText(GO.propCoef2048C.toString())
                    }
                    2 -> {
                        editPolinomA.setText(GO.propCoef4096A.toString())
                        editPolinomB.setText(GO.propCoef4096B.toString())
                        editPolinomC.setText(GO.propCoef4096C.toString())
                    }
                }

                /* CPS в uRh */
                editCPS2Rh.setText(GO.propCPS2UR.toString())

                /* Значения DAC */
                editHVoltage.setText(GO.propHVoltage.toString())
                editComparator.setText(GO.propComparator.toString())

                /* SMA window */
                editSMA.setText(GO.windowSMA.toString())

                GO.propButtonInit = true                   // Включим реакцию в листенерах компонентов

                /*
                 *
                 * Сохранение параметров в конфигурационном файле смартфона
                 *
                 */
                val btnSaveSetup: Button = view.findViewById(R.id.buttonSaveSetup)

                GO.textMACADR = view.findViewById(R.id.textMACADDR)
                if (GO.LEMAC.isNotEmpty()) {
                    GO.textMACADR.setText(GO.LEMAC)
                }
                btnSaveSetup.setOnClickListener {
                    /* Сохраняем MAC адрес */
                    GO.LEMAC = GO.textMACADR.text.toString()
                    GO.rejectChann = editRejectChann.text.toString().toInt();
                    //Log.d("BluZ-BT", "Reject chann: " + GO.rejectChann )
                    GO.PP.setPropInt(propRejectCann, GO.rejectChann)                    // Сохраним количество не отображаемых каналов
                    GO.PP.setPropStr(propADDRESS, GO.LEMAC)                             // Сохраним MAC адрес устройства
                    GO.PP.setPropInt(propColorSpecterLin, GO.ColorLin)                  // Сохраним цвет линейного графика
                    GO.PP.setPropInt(propColorSpecterLog, GO.ColorLog)                  // Сохраним цвет логарифмического графика
                    GO.PP.setPropInt(propColorSpecterFone, GO.ColorFone)                // Сохраним цвет графика фона
                    GO.PP.setPropInt(propColorSpecterFoneLg, GO.ColorFoneLg)            // Сохраним цвет логарифмического графика фона
                    GO.PP.setPropInt(propColorSpecterLinGisto, GO.ColorLinGisto)        // Сохраним цвет линейного графика гистограммы
                    GO.PP.setPropInt(propColorSpecterLogGisto, GO.ColorLogGisto)        // Сохраним цвет логарифмического графика  гистограммы
                    GO.PP.setPropInt(propColorSpecterFoneGisto, GO.ColorFoneGisto)      // Сохраним цвет графика фона гистограммы
                    GO.PP.setPropInt(propColorSpecterFoneLgGisto, GO.ColorFoneLgGisto)  // Сохраним цвет логарифмического графика фона гистограммы
                    GO.PP.setPropInt(propColorDozimeter, GO.ColorDosimeter)             // Сохраним цвет дозиметра
                    GO.PP.setPropInt(propColorDozimeterSMA, GO.ColorDosimeterSMA)       // Сохраним цвет дозиметра
                    if (rbLineSpectr.isChecked) {                                       // Сохраним тип графика для вывода спектра
                        GO.specterGraphType = 0
                    } else {
                        GO.specterGraphType = 1
                    }
                    GO.PP.setPropInt(propSpectrGraphType, GO.specterGraphType)

                    GO.propSoundKvant = cbSoundKvant.isChecked
                    GO.PP.setPropBoolean(propSoundKvant, GO.propSoundKvant)

                    GO.propLedKvant = cbLedKvant.isChecked
                    GO.PP.setPropBoolean(propLedKvant, GO.propLedKvant)

                    GO.propAutoStartSpectrometr = cbSpectrometr.isChecked
                    GO.PP.setPropBoolean(propStartSpectrometr, GO.propAutoStartSpectrometr)

                    GO.propLevel1 = editLevel1.text.toString().toInt()              // Значение первого порога из редактора
                    GO.propLevel2 = editLevel2.text.toString().toInt()              // Значение второго порога из редактора
                    GO.propLevel3 = editLevel3.text.toString().toInt()              // Значение третьего порога из редактора
                    GO.PP.setPropInt(propLevel1, GO.propLevel1)                     // Значение первого порога
                    GO.PP.setPropInt(propLevel2, GO.propLevel2)                     // Значение второго порога
                    GO.PP.setPropInt(propLevel3, GO.propLevel3)                     // Значение третьего порога

                    GO.propSoundLevel1 = cbSoundLevel1.isChecked
                    GO.propSoundLevel2 = cbSoundLevel2.isChecked
                    GO.propSoundLevel3 = cbSoundLevel3.isChecked
                    GO.PP.setPropBoolean(propSoundLevel1, GO.propSoundLevel1)       // Звук первого порога
                    GO.PP.setPropBoolean(propSoundLevel2, GO.propSoundLevel2)       // Звук второго порога
                    GO.PP.setPropBoolean(propSoundLevel3, GO.propSoundLevel3)       // Звук третьего порога

                    GO.propVibroLevel1 = cbVibroLevel1.isChecked
                    GO.propVibroLevel2 = cbVibroLevel2.isChecked
                    GO.propVibroLevel3 = cbVibroLevel3.isChecked
                    GO.PP.setPropBoolean(propVibroLevel1, GO.propVibroLevel1)       // Вибро первого порога
                    GO.PP.setPropBoolean(propVibroLevel2, GO.propVibroLevel2)       // Вибро второго порога
                    GO.PP.setPropBoolean(propVibroLevel3, GO.propVibroLevel3)       // Вибро третьего порога

                    GO.propCPS2UR = editCPS2Rh.text.toString().toFloat()
                    GO.PP.setPropFloat(propCPS2UR, GO.propCPS2UR)                   // Коэффициент пересчета cps в uRh

                    when (GO.spectrResolution) {
                        0 -> {
                            GO.propCoef1024A = editPolinomA.text.toString().toFloat()
                            GO.propCoef1024B = editPolinomB.text.toString().toFloat()
                            GO.propCoef1024C = editPolinomC.text.toString().toFloat()
                            }
                        1 -> {
                            GO.propCoef2048A = editPolinomA.text.toString().toFloat()
                            GO.propCoef2048B = editPolinomB.text.toString().toFloat()
                            GO.propCoef2048C = editPolinomC.text.toString().toFloat()
                        }
                        2 -> {
                            GO.propCoef4096A = editPolinomA.text.toString().toFloat()
                            GO.propCoef4096B = editPolinomB.text.toString().toFloat()
                            GO.propCoef4096C = editPolinomC.text.toString().toFloat()
                        }
                    }
                    GO.PP.setPropFloat(propCoef1024A, GO.propCoef1024A)             // A - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef1024B, GO.propCoef1024B)             // B - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef1024C, GO.propCoef1024C)             // C - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef2048A, GO.propCoef2048A)             // A - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef2048B, GO.propCoef2048B)             // B - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef2048C, GO.propCoef2048C)             // C - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef4096A, GO.propCoef4096A)             // A - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef4096B, GO.propCoef4096B)             // B - полинома пересчета канала в энергию
                    GO.PP.setPropFloat(propCoef4096C, GO.propCoef4096C)             // C - полинома пересчета канала в энергию


                    /* SMA window сформируем нечетное число */
                    GO.windowSMA = (editSMA.text.toString().toInt() / 2).toInt() * 2 + 1
                    GO.PP.setPropInt(propSMAWindow, GO.windowSMA)

                    GO.propHVoltage = editHVoltage.text.toString().toUShort()
                    GO.PP.setPropInt(propHV, GO.propHVoltage.toInt())               // Уровень высокого напряжения

                    GO.propComparator = editComparator.text.toString().toUShort()
                    GO.PP.setPropInt(propComparator, GO.propComparator.toInt())     // Уровень Компаратора

                    if (rbResolution1024.isChecked) {           // Разрешение прибора.
                        GO.spectrResolution = 0
                    } else if (rbResolution2048.isChecked) {
                        GO.spectrResolution = 1
                    } else if (rbResolution4096.isChecked) {
                        GO.spectrResolution = 2
                    } else {
                        GO.spectrResolution = 0
                    }
                    Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())
                    GO.PP.setPropInt(propResolution, GO.spectrResolution)
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
                    *
                    * 242, 243      - Контрольная сумма
                    */
                    /* Сохраненние настроек */
                    var convVal = ByteArray(4)
                    /*
                    * EEE754
                    var convVal = ByteBuffer.allocate(4).putFloat(testD).array();
                    GO.BTT.sendBuffer[3] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[4] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[5] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[6] = convVal[3].toUByte()
                    */

                    /* Первый порог */
                    convVal = ByteBuffer.allocate(4).putInt(GO.propLevel1).array();
                    GO.BTT.sendBuffer[4] = convVal[0].toUByte()
                    GO.BTT.sendBuffer[5] = convVal[1].toUByte()
                    GO.BTT.sendBuffer[6] = convVal[2].toUByte()
                    GO.BTT.sendBuffer[7] = convVal[3].toUByte()
                    //convVal.

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
                    if (cbLedKvant.isChecked) {
                        GO.BTT.sendBuffer[20] = 1u
                    }
                    /* Звук сопровождает приход частицы */
                    if (cbSoundKvant.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000010.toUByte()
                    }
                    /* Звуковая сигнализация первого порога */
                    if (cbSoundLevel1.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000100.toUByte()
                    }
                    /* Звуковая сигнализация второго порога */
                    if (cbSoundLevel2.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00001000.toUByte()
                    }
                    /* Звуковая сигнализация третьего порога */
                    if (cbSoundLevel3.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00010000.toUByte()
                    }
                    /* Вибро первого порога */
                    if (cbVibroLevel1.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00100000.toUByte()
                    }
                    /* Вибро второго порога */
                    if (cbVibroLevel2.isChecked) {
                        GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b01000000.toUByte()
                    }
                    /* Вибро третьего порога */
                    if (cbVibroLevel3.isChecked) {
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

                    /* Уровень высокого напряжения */
                    GO.BTT.sendBuffer[33] = (GO.propHVoltage and 255u).toUByte()
                    GO.BTT.sendBuffer[34] = ((GO.propHVoltage.toUInt() shr 8) and 255u).toUByte()

                    /* Уровень компаратора */
                    GO.BTT.sendBuffer[35] = (GO.propComparator and 255u).toUByte()
                    GO.BTT.sendBuffer[36] = ((GO.propComparator.toUInt() shr 8) and 255u).toUByte()

                    /* Разрешение спектра */
                    if (rbResolution1024.isChecked) {
                        GO.BTT.sendBuffer[37] = 0u
                    } else if (rbResolution2048.isChecked) {
                        GO.BTT.sendBuffer[37] = 1u
                    } else if (rbResolution4096.isChecked) {
                        GO.BTT.sendBuffer[37] = 2u
                    } else {
                        GO.BTT.sendBuffer[37] = 0u
                    }

                    /* Запуск спектрометра при включении прибора */
                    if (cbSpectrometr.isChecked) {
                        GO.BTT.sendBuffer[38] = 1u
                    } else {
                        GO.BTT.sendBuffer[38] = 0u
                    }

                    GO.BTT.sendCommand(0u)
                }

                /* Radiobuttons для выбора элемента настройки цвета */
                /*
                * TODO -- Нужно сделать imageView с четырьмя графиками для наглядности
                * Панель для отображения цвета
                */
                tvColor = view.findViewById(R.id.tvColor)

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

                var noChange: Boolean = true
                tvColor.setBackgroundColor(GO.ColorLin)
                rbLine = view.findViewById(R.id.RBLin)
                rbLg = view.findViewById(R.id.RBLg)
                rbFoneLin = view.findViewById(R.id.RBFoneLin)
                rbFoneLg = view.findViewById(R.id.RBFoneLg)
                rbGroup = view.findViewById(R.id.rbTypeGroup)
                rgTypeSpec = view.findViewById(R.id.rgTypeSpectr)
                rbResolution = view.findViewById(R.id.RGResolution)

                /*
                * Выбор разрешения
                */
                rbResolution.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        when (checkedId) {
                            rbResolution1024.id -> {
                                editPolinomA.setText(GO.propCoef1024A.toString())
                                editPolinomB.setText(GO.propCoef1024B.toString())
                                editPolinomC.setText(GO.propCoef1024C.toString())
                            }
                            rbResolution2048.id -> {
                                editPolinomA.setText(GO.propCoef2048A.toString())
                                editPolinomB.setText(GO.propCoef2048B.toString())
                                editPolinomC.setText(GO.propCoef2048C.toString())
                            }
                            rbResolution4096.id -> {
                                editPolinomA.setText(GO.propCoef4096A.toString())
                                editPolinomB.setText(GO.propCoef4096B.toString())
                                editPolinomC.setText(GO.propCoef4096C.toString())
                            }
                        }
                        noChange = true
                    }
                }
                /*
                * Выбор типа отображения спектра. Линейный, гистограмма
                */
                rgTypeSpec.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                        noChange = false
                        if (checkedId == rbLineSpectr.id) {
                            GO.specterGraphType = 0
                            if (rbLine.isChecked) {                                   // Цвет для линейного графика
                                tvColor.setBackgroundColor(GO.ColorLin)
                                selA.setProgress(Color.alpha(GO.ColorLin), false)
                                selR.setProgress(Color.red(GO.ColorLin), false)
                                selG.setProgress(Color.green(GO.ColorLin), false)
                                selB.setProgress(Color.blue(GO.ColorLin), false)
                            } else if (rbLg.isChecked) {                              // Цвет для логарифмического графика
                                tvColor.setBackgroundColor(GO.ColorLog)
                                selA.setProgress(Color.alpha(GO.ColorLog), false)
                                selR.setProgress(Color.red(GO.ColorLog), false)
                                selG.setProgress(Color.green(GO.ColorLog), false)
                                selB.setProgress(Color.blue(GO.ColorLog), false)
                            } else if (rbFoneLin.isChecked) {                         // Цвет для линейного графика фона
                                tvColor.setBackgroundColor(GO.ColorFone)
                                selA.setProgress(Color.alpha(GO.ColorFone), false)
                                selR.setProgress(Color.red(GO.ColorFone), false)
                                selG.setProgress(Color.green(GO.ColorFone), false)
                                selB.setProgress(Color.blue(GO.ColorFone), false)
                            } else if (rbFoneLg.isChecked) {                          // Цвет для логарифмического графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneLg)
                                selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                                selR.setProgress(Color.red(GO.ColorFoneLg), false)
                                selG.setProgress(Color.green(GO.ColorFoneLg), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                            }
                        } else if (checkedId == rbGistogramSpectr.id) {
                            GO.specterGraphType = 1
                            if (rbLine.isChecked) {                                   // Цвет для линейного графика
                                tvColor.setBackgroundColor(GO.ColorLinGisto)
                                selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                                selR.setProgress(Color.red(GO.ColorLinGisto), false)
                                selG.setProgress(Color.green(GO.ColorLinGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                            } else if (rbLg.isChecked) {                              // Цвет для логарифмического графика
                                tvColor.setBackgroundColor(GO.ColorLogGisto)
                                selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                                selR.setProgress(Color.red(GO.ColorLogGisto), false)
                                selG.setProgress(Color.green(GO.ColorLogGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                            } else if (rbFoneLin.isChecked) {                         // Цвет для линейного графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                            } else if (rbFoneLg.isChecked) {                          // Цвет для логарифмического графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                            }
                        }
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
                        if (rbLineSpectr.isChecked) {
                            if (checkedId == rbLine.id) {                                   // Цвет для линейного графика
                                tvColor.setBackgroundColor(GO.ColorLin)
                                selA.setProgress(Color.alpha(GO.ColorLin), false)
                                selR.setProgress(Color.red(GO.ColorLin), false)
                                selG.setProgress(Color.green(GO.ColorLin), false)
                                selB.setProgress(Color.blue(GO.ColorLin), false)
                            } else if (checkedId == rbLg.id) {                              // Цвет для логарифмического графика
                                tvColor.setBackgroundColor(GO.ColorLog)
                                selA.setProgress(Color.alpha(GO.ColorLog), false)
                                selR.setProgress(Color.red(GO.ColorLog), false)
                                selG.setProgress(Color.green(GO.ColorLog), false)
                                selB.setProgress(Color.blue(GO.ColorLog), false)
                            } else if (checkedId == rbFoneLin.id) {                         // Цвет для линейного графика фона
                                tvColor.setBackgroundColor(GO.ColorFone)
                                selA.setProgress(Color.alpha(GO.ColorFone), false)
                                selR.setProgress(Color.red(GO.ColorFone), false)
                                selG.setProgress(Color.green(GO.ColorFone), false)
                                selB.setProgress(Color.blue(GO.ColorFone), false)
                            } else if (checkedId == rbFoneLg.id) {                          // Цвет для логарифмического графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneLg)
                                selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                                selR.setProgress(Color.red(GO.ColorFoneLg), false)
                                selG.setProgress(Color.green(GO.ColorFoneLg), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                            }
                        } else if (rbGistogramSpectr.isChecked) {
                            if (checkedId == rbLine.id) {                                   // Цвет для линейного графика
                                tvColor.setBackgroundColor(GO.ColorLinGisto)
                                selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                                selR.setProgress(Color.red(GO.ColorLinGisto), false)
                                selG.setProgress(Color.green(GO.ColorLinGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                            } else if (checkedId == rbLg.id) {                              // Цвет для логарифмического графика
                                tvColor.setBackgroundColor(GO.ColorLogGisto)
                                selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                                selR.setProgress(Color.red(GO.ColorLogGisto), false)
                                selG.setProgress(Color.green(GO.ColorLogGisto), false)
                                selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                            } else if (checkedId == rbFoneLin.id) {                         // Цвет для линейного графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                            } else if (checkedId == rbFoneLg.id) {                          // Цвет для логарифмического графика фона
                                tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                                selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                                selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                                selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                            }
                        }
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
                            if (rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(0, progress, GO.ColorLin)
                                    tvColor.setBackgroundColor(GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(0, progress, GO.ColorLog)
                                    tvColor.setBackgroundColor(GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(0, progress, GO.ColorFone)
                                    tvColor.setBackgroundColor(GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLg)
                                    tvColor.setBackgroundColor(GO.ColorFoneLg)
                                }
                            } else if (rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLinGisto)
                                    tvColor.setBackgroundColor(GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLogGisto)
                                    tvColor.setBackgroundColor(GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLgGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                }
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
                            if (rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(1, progress, GO.ColorLin)
                                    tvColor.setBackgroundColor(GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(1, progress, GO.ColorLog)
                                    tvColor.setBackgroundColor(GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(1, progress, GO.ColorFone)
                                    tvColor.setBackgroundColor(GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLg)
                                    tvColor.setBackgroundColor(GO.ColorFoneLg)
                                }
                            } else if (rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLinGisto)
                                    tvColor.setBackgroundColor(GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLogGisto)
                                    tvColor.setBackgroundColor(GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLgGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                }
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
                            if(rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(2, progress, GO.ColorLin)
                                    tvColor.setBackgroundColor(GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(2, progress, GO.ColorLog)
                                    tvColor.setBackgroundColor(GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(2, progress, GO.ColorFone)
                                    tvColor.setBackgroundColor(GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLg)
                                    tvColor.setBackgroundColor(GO.ColorFoneLg)
                                }
                            } else if (rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLinGisto)
                                    tvColor.setBackgroundColor(GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLogGisto)
                                    tvColor.setBackgroundColor(GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLgGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                }
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
                            if (rbLineSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLin = GO.bColor.setSpecterColor(3, progress, GO.ColorLin)
                                    tvColor.setBackgroundColor(GO.ColorLin)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLog = GO.bColor.setSpecterColor(3, progress, GO.ColorLog)
                                    tvColor.setBackgroundColor(GO.ColorLog)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFone = GO.bColor.setSpecterColor(3, progress, GO.ColorFone)
                                    tvColor.setBackgroundColor(GO.ColorFone)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLg = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneLg)
                                    tvColor.setBackgroundColor(GO.ColorFoneLg)
                                }
                            } else if (rbGistogramSpectr.isChecked) {
                                if (rbLine.isChecked) {
                                    GO.ColorLinGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLinGisto)
                                    tvColor.setBackgroundColor(GO.ColorLinGisto)
                                } else if (rbLg.isChecked) {
                                    GO.ColorLogGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLogGisto)
                                    tvColor.setBackgroundColor(GO.ColorLogGisto)
                                } else if (rbFoneLin.isChecked) {
                                    GO.ColorFoneGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneGisto)
                                } else if (rbFoneLg.isChecked) {
                                    GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLgGisto)
                                    tvColor.setBackgroundColor(GO.ColorFoneLgGisto)
                                }
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