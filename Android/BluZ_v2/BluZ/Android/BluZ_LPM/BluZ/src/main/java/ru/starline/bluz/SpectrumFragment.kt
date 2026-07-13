package ru.starline.bluz

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Matrix
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.math.abs

/**
 * Главная вкладка с гамма-спектром и накопленным историческим спектром.
 *
 * **Layout:** `spectr_layout.xml` (portrait), `layout-land/spectr_layout.xml` (landscape).
 *
 * **Структура:**
 *  - Title row: subtitle [globalObj.bzSpecSubtitle] + «Гамма-спектр»/«История» +
 *    кнопки Save / Start-Stop (в title row, не внизу)
 *  - Hero readouts: МОЩНОСТЬ ДОЗЫ / СРЕДНЯЯ (или CPS если коэф `propCPS2UR == 0`)
 *  - Chart card — FrameLayout с тремя ImageView в стеке:
 *    - `specterView` ([drawSpecter] canvas) — VISIBLE на странице 0
 *    - `historyView` ([drawHistory] canvas) — VISIBLE на странице 1
 *    - `cursorView` ([drawCursor] overlay) — VISIBLE на странице 0, GONE на 1
 *  - Toolbar внутри chart card: SMA / MEDIAN / MLEM / Calibrate / Confirm / Clear
 *  - **Нижний swipe-pager** [androidx.viewpager2.widget.ViewPager2] — две страницы:
 *    - `bz_spec_bottom_meas.xml` — таймер измерения + подсказка
 *    - `bz_spec_bottom_history.xml` — Integral / Ср.CPS + Load/Save/Clear history
 *
 * **Touch на cursorView:**
 *  - 1 палец: перемещение курсора → [drawCursor.showCorsor]
 *  - 2 пальца: pinch-zoom (xZoom 1..5), pan (xPosition)
 *
 * **Калибровка:** кнопка `buttonCalibrate` → пользователь тапает 3 пика → вводит реальные
 * энергии → [Mtrx.sysEq] решает систему 3 уравнений, получает A, B, C коэффициенты полинома.
 *
 * **Page change callback** в onViewCreated синхронизирует видимость элементов chart card
 * при swipe нижнего pager (см. `pager.registerOnPageChangeCallback`).
 *
 * **Привязка View** в onViewCreated, обнуление в onDestroyView — обязательно (все
 * `GO.bzSpec*`, `bzRecClock`, `bzHistIntegralValue`, `txtIsotopInfo`, `bzSpecSubtitle` nullable).
 */
class SpectrumFragment : Fragment() {

    private var currentScaleX = 1f
    private lateinit var btnCalibrate: Button
    private lateinit var btnConfirmCalibrate: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.spectr_layout, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GO.btnSaveBQ = view.findViewById(R.id.buttonSaveBQ)
        GO.txtIsotopInfo = view.findViewById(R.id.textIsotopInfo)
        GO.bzSpecSubtitle = view.findViewById(R.id.bzSpecSubtitle)
        GO.bzSpecPageIsHistory = false
        GO.updateSpecSubtitle()

        GO.drawCURSOR.cursorView = view.findViewById(R.id.cursorView)
        GO.drawSPECTER.imgView = view.findViewById(R.id.specterView)

        //GO.drawSPECTER.imgView.apply {
        //    scaleType = ImageView.ScaleType.MATRIX
        //    imageMatrix = Matrix()
        //    isClickable = false
        //}

        GO.drawSPECTER.imgView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawSPECTER.imgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawObjectInit = true
                    GO.drawSPECTER.init()
                    GO.drawSPECTER.clearSpecter()
                    GO.drawSPECTER.redrawSpecter(GO.specterType)
                }
            }
        )

        GO.drawCURSOR.cursorView.apply {
            isClickable = true
            isFocusable = true
        }

        // Пересоздание bitmap курсора под текущие размеры cursorView (важно после поворота:
        // у нового view другие width/height, старый bitmap не подходит — курсор рисуется криво).
        GO.drawCURSOR.cursorView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawCURSOR.cursorView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawCursorObjectInit = true
                    GO.drawCURSOR.init()
                }
            }
        )

        var lastDist = 0.0f
        var resizeKf = 0.0f
        var xPos = 0.0f

        GO.drawCURSOR.cursorView.setOnTouchListener { _, event ->
            when {
                event.pointerCount == 2 -> {
                    val x1 = event.getX(0)
                    val x2 = event.getX(1)
                    val dist = kotlin.math.hypot(x1 - x2, x2 - x1)
                    if (xPos > 0) {
                        GO.xPosition += (xPos - x1) * 0.5f
                        if (GO.xPosition < 0) {
                            GO.xPosition = 0.0f
                        } else if (0 > GO.drawSPECTER.ResolutionSpectr * GO.drawSPECTER.xSize - GO.drawSPECTER.HSize - GO.xPosition) {
                            GO.xPosition = (GO.drawSPECTER.ResolutionSpectr * GO.drawSPECTER.xSize - GO.drawSPECTER.HSize).toFloat()
                        }
                    }
                    if (lastDist > 0) {
                        resizeKf = kotlin.math.abs(dist / lastDist)
                        GO.xZoom = GO.xZoom * resizeKf
                        if (GO.xZoom < 1) {
                            GO.xZoom = 1.0f
                        } else if (GO.xZoom > 5) {
                            GO.xZoom = 5.0f
                        }
                        GO.drawSPECTER.clearSpecter()
                        GO.drawSPECTER.redrawSpecter(GO.specterType)
                    }
                    lastDist = dist
                    xPos = x1
                    //Log.i("BluZ-BT", "X1: $x1, Resol: ${GO.drawSPECTER.ResolutionSpectr}, HSize: ${GO.drawSPECTER.HSize}, xSize: ${GO.drawSPECTER.xSize}, Xpos: ${GO.xPosition}, Scale: ${GO.xZoom}")
                    true
                }
                event.pointerCount == 1 -> {
                    lastDist = 0.0f
                    xPos = 0.0f
                    val rawX = event.x
                    val rawY = event.y
                    if (event.action == MotionEvent.ACTION_DOWN ||
                        event.action == MotionEvent.ACTION_MOVE) {
                        val correctedX = rawX / currentScaleX.coerceAtLeast(0.001f)
                        if (GO.drawCURSOR.oldX != correctedX) {
                            GO.drawCURSOR.showCorsor(correctedX, rawY)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        val CBSMA: CheckBox = view.findViewById(R.id.cbSMA)
        val CBMEDIAN: CheckBox = view.findViewById(R.id.cbMED)

        var calState: Int = 0
        //val matrx = Mtrx()

        /* Событие окончания калибровки и расчета СЛАУ */
        btnConfirmCalibrate = view.findViewById(R.id.buttonConfirmCalibrate)
        btnConfirmCalibrate.setOnClickListener {
            /* Тест */
            //GO.enrgCalc.addCalibrationPoint(21.0, 37.0)
            //GO.enrgCalc.addCalibrationPoint(61.0, 100.0)
            //GO.enrgCalc.addCalibrationPoint(95.0, 200.0)
            //GO.enrgCalc.addCalibrationPoint(199.0, 500.0)
            //GO.enrgCalc.addCalibrationPoint(278.0, 662.0)

            if (btnConfirmCalibrate.text == "V") {
                /* Данные приняты выполняем расчет и переходим на страницу настроек */
                val points = GO.enrgCalc.getCalibrationPoints()  // Получаем список
                val result = GO.enrgCalc.fitCalibration(points, calState - 1)
                if (result.isValid) {
                    GO.propCoef4096A = 0f
                    GO.propCoef4096B = 0f
                    when (calState) {
                        3 -> {
                            GO.propCoef4096C = result.coefficients[2].toFloat()
                            GO.propCoef4096D = result.coefficients[1].toFloat()
                            GO.propCoef4096E = result.coefficients[0].toFloat()
                        }
                        4 -> {
                            GO.propCoef4096B = result.coefficients[3].toFloat()
                            GO.propCoef4096C = result.coefficients[2].toFloat()
                            GO.propCoef4096D = result.coefficients[1].toFloat()
                            GO.propCoef4096E = result.coefficients[0].toFloat()
                        }
                        5 -> {
                            GO.propCoef4096A = result.coefficients[4].toFloat()
                            GO.propCoef4096B = result.coefficients[3].toFloat()
                            GO.propCoef4096C = result.coefficients[2].toFloat()
                            GO.propCoef4096D = result.coefficients[1].toFloat()
                            GO.propCoef4096E = result.coefficients[0].toFloat()
                        }
                    }
                    GO.enrgCalc.init(GO.propCoef4096A, GO.propCoef4096B, GO.propCoef4096C, GO.propCoef4096D, GO.propCoef4096E, GO.specterType)
                    if (result.isValid) {
                        Log.d("BluZ-BT", "=== ПРОВЕРКА КАЛИБРОВКИ ===")
                        for ((ch, enExpected) in points) {
                            val enCalc = GO.enrgCalc.channelToEnergy(ch.toInt())
                            val err = abs(enCalc - enExpected.toFloat())
                            Log.d("BluZ-BT", "Канал ${ch.toInt()}: ожидаемо=${enExpected}%.1f, получено=${enCalc}%.1f, ошибка=${err}%.3f кэВ")
                        }
                        Log.d("BluZ-BT", "RMS ошибка: ${result.rmsError}%.3f кэВ")
                    }                } else {
                    Log.e("BluZ-BT", "Калибровка не удалась: плохая сходимость или мало точек")
                }
                GO.needCalibrate = true
                GO.txtStat1.visibility = View.INVISIBLE
                GO.txtStat2.visibility = View.INVISIBLE
                GO.txtStat3.visibility = View.INVISIBLE
                GO.txtCompMED.visibility = View.INVISIBLE
                GO.viewPager.setCurrentItem(4, false)
            }
            btnConfirmCalibrate.text = "X"
            btnCalibrate.text = "1"
            calState = 0
            GO.enrgCalc.clearCalibration()
        }

        /* Ввод данных для калибровки, от 3, до 5 значений */
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
                builder.setPositiveButton("Add") { _, _ ->
                    if (inEnergy.text.isNotEmpty()) {
                        if (calState == 0) {
                            GO.enrgCalc.clearCalibration()
                        }
                        val channel = GO.drawCURSOR.curChan.toDouble()
                        val energy = inEnergy.text.toString().toDouble()
                        GO.enrgCalc.addCalibrationPoint(channel, energy)
                        calState++
                        if (calState > 2 && calState < 5) {         // Если 3 и более значения, можно начинать расчитывать коэффициенты
                            btnConfirmCalibrate.text = "V"
                            btnCalibrate.text = (calState + 1).toString()
                        } else if (calState >= 4) {
                            btnConfirmCalibrate.text = "V"
                            btnCalibrate.text = "1"
                        } else {
                            btnCalibrate.text = (calState + 1).toString()
                        }
                    }
                }
                    .setNegativeButton("Cancel") { _, _ -> }
                builder.create()
                builder.show()
            }
        }

        GO.propButtonInit = false
        CBSMA.isChecked = GO.drawSPECTER.flagSMA
        CBMEDIAN.isChecked = GO.drawSPECTER.flagMEDIAN
        GO.propButtonInit = true

        CBSMA.setOnCheckedChangeListener { _, isChecked ->
            if (GO.propButtonInit) {
                GO.drawSPECTER.flagSMA = isChecked
                if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                    GO.drawSPECTER.clearSpecter()
                    GO.drawSPECTER.redrawSpecter(GO.specterType)
                } else {
                    Log.e("BluZ-BT", "drawSPEC is null")
                }
            }
        }

        CBMEDIAN.setOnCheckedChangeListener { _, isChecked ->
            if (GO.propButtonInit) {
                GO.drawSPECTER.flagMEDIAN = isChecked
                if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                    GO.drawSPECTER.clearSpecter()
                    GO.drawSPECTER.redrawSpecter(GO.specterType)
                } else {
                    Log.e("BluZ-BT", "drawSPEC is null")
                }
            }
        }

        GO.btnSpecterSS = view.findViewById(R.id.buttonSpecterSS)
        GO.btnSpecterSSisInit = true

        // Рендер по кэшированному состоянию (переживает поворот). Не ждём первого BLE-фрейма.
        (activity as? MainActivity)?.applySpecterButton()

        if (GO.initBT) {
            GO.drawSPECTER.init()
        } else {
            GO.initBT = true
        }

        GO.btnSpecterSS.setOnClickListener {
            GO.BTT.sendCommand(2u)
            GO.specterRunning = !GO.specterRunning
            (activity as? MainActivity)?.applySpecterButton()
        }

        GO.btnSaveBQ.setOnClickListener {
            val saveBqMon = SaveBqMon()
            saveBqMon.saveSpecter()
            Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
        }

        /* Очистка спектра */
        val btnClearSpecter: ImageButton = view.findViewById(R.id.buttonClearSpectr)
        btnClearSpecter.setOnClickListener {
            //GO.BTT.sendCommand(1u)
            //GO.drawSPECTER.resetSpecter()
            AlertDialog.Builder(view.context)
                .setTitle("Удаление набранного спектра.")
                .setMessage("Вы действительно хотите очистить данные?\nЭто действие нельзя отменить.")
                .setPositiveButton("Очистить") { dialog, _ ->
                    // Выполняем очистку только после подтверждения
                    GO.BTT.sendCommand(1u)
                    GO.drawSPECTER.resetSpecter()
                }
                .setNegativeButton("Отмена", null) // null автоматически закрывает диалог
                .setCancelable(true)               // позволяет закрыть по нажатию вне окна или кнопке Back
                .show()
        }

        // Phase C: register hero readouts so MainActivity.applyFrameToUi can update them
        GO.bzSpecDoseValue = view.findViewById(R.id.bzSpecDoseValue)
        GO.bzSpecAvgValue = view.findViewById(R.id.bzSpecAvgValue)
        GO.bzSpecDoseUnit = view.findViewById(R.id.bzSpecDoseUnit)
        GO.bzSpecAvgUnit = view.findViewById(R.id.bzSpecAvgUnit)
        GO.bzTotalSpectrPulses = view.findViewById(R.id.textTotalCounter)
        val toggleUnits = View.OnClickListener {
            (activity as? MainActivity)?.toggleDoseUnits()
        }
        GO.bzSpecDoseValue?.setOnClickListener(toggleUnits)
        GO.bzSpecAvgValue?.setOnClickListener(toggleUnits)
        GO.bzSpecDoseUnit?.setOnClickListener(toggleUnits)
        GO.bzSpecAvgUnit?.setOnClickListener(toggleUnits)
        GO.bzTotalSpectrPulses?.text = "Counts: " + GO.spectrometerPulse.toString()
        applySpecUnitLabels()
        GO.applyDoseReadouts()


        // Bottom swipe pager: страница 0 — таймер измерения, страница 1 — данные истории
        //val pager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.specBottomPager)
        //pager.adapter = BottomSwipeAdapter()

        // При свайпе нижнего pager переключаем верхний chart card: спектр <-> история
        val specToolbar = view.findViewById<View>(R.id.specToolbar)
        val cursorView = GO.drawCURSOR.cursorView
        val specterView = GO.drawSPECTER.imgView
        //val historyView = GO.drawHISTORY.imgView
        val btnSS = GO.btnSpecterSS
        // В landscape-варианте title/subtitle нет (компактный header), findViewById вернёт null —
        // используем safe-call ниже, иначе при повороте NPE в onPageSelected.
        val subtitleTv: TextView? = view.findViewById(R.id.bzSpecSubtitle)
        //val titleTv: TextView? = view.findViewById(R.id.bzSpecTitle)
        GO.drawSPECTER.clearSpecter()
        GO.drawSPECTER.redrawSpecter(GO.specterType)
    }

    override fun onDestroyView() {
        GO.bzSpecDoseValue = null
        GO.bzSpecAvgValue = null
        GO.bzSpecDoseUnit = null
        GO.bzSpecAvgUnit = null
        GO.bzTotalSpectrPulses = null
        GO.bzRecClock = null
        GO.bzRecLabel = null
        GO.bzHistDuration = null
        GO.bzHistIntegralValue = null
        GO.bzHistAvgCpsValue = null
        GO.txtIsotopInfo = null
        GO.bzSpecSubtitle = null
        GO.bzSpecPageIsHistory = false
        super.onDestroyView()
    }

    /** Адаптер для нижнего ViewPager2 на вкладке Spectrum.
     *  Page 0 — «Идёт измерение» с таймером. Page 1 — числовые показатели истории + кнопки. */
    /**
     * RecyclerView-адаптер для нижнего ViewPager2 (`specBottomPager`).
     *
     * Две страницы:
     *  - position 0 — `bz_spec_bottom_meas.xml`: метка «ИДЁТ ИЗМЕРЕНИЕ · СВАЙП ВЛЕВО …» + clock
     *  - position 1 — `bz_spec_bottom_history.xml`: метка «ИСТОРИЯ · СВАЙП ВПРАВО …» + Integral / Avg CPS + Load/Save/Clear
     *
     * При каждом биндинге заново привязывает `GO.bzRecClock` / `bzHistIntegralValue` /
     * `bzHistAvgCpsValue` к новым view (ViewPager2 пересоздаёт holders при свайпе).
     *
     * См. также page change callback в [onViewCreated], который синхронно переключает chart card.
     */
    private inner class BottomSwipeAdapter
        : androidx.recyclerview.widget.RecyclerView.Adapter<BottomSwipeAdapter.VH>() {

        inner class VH(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int = 2
        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val layoutRes = if (viewType == 0) R.layout.bz_spec_bottom_meas
                            else R.layout.bz_spec_bottom_history
            val v = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            v.layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val v = holder.itemView
            if (position == 0) {
                GO.bzRecLabel = v.findViewById(R.id.bzRecLabel)
                GO.bzRecClock = v.findViewById(R.id.bzRecClock)
                GO.bzRecClock?.setOnClickListener {
                    GO.clockShowSecondsSpec = !GO.clockShowSecondsSpec
                    GO.showStatistics()
                }
                GO.showStatistics()
            } else {
                GO.bzHistDuration = null  // удалён из layout — таймер теперь в верхнем статусбаре
                GO.bzHistIntegralValue = v.findViewById(R.id.bzHistIntegralValue)
                GO.bzHistAvgCpsValue = v.findViewById(R.id.bzHistAvgCpsValue)

                val btnLoad: Button = v.findViewById(R.id.buttonLoadHistory)
                btnLoad.setOnClickListener {
                    GO.BTT.sendCommand(5u)
                    Toast.makeText(GO.mainContext, R.string.historyRequest, Toast.LENGTH_LONG).show()
                }
                val btnSave: Button = v.findViewById(R.id.buttonHistorySave)
                btnSave.setOnClickListener {
                    val saveBqMon = SaveBqMon()
                    saveBqMon.saveSpecter()
                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_LONG).show()
                }
                val btnClear: ImageButton = v.findViewById(R.id.buttonClearHistory)
                btnClear.setOnClickListener {
                    GO.BTT.sendCommand(8u)
                    for (i in 0 until GO.drawHISTORY.ResolutionHistory) {
                        GO.drawHISTORY.historyData[i] = 0.0
                    }
                    GO.drawHISTORY.clearHistory()
                    GO.drawHISTORY.redrawSpecter(GO.specterType)
                }

                v.post {
                    Log.d("BluZ-Layout", "=== HISTORY PAGE DIAGNOSTICS ===")

                    // 1. Печатаем всё дерево view для этой страницы
                    fun printTree(view: View?, indent: String = "") {
                        if (view == null) return
                        val idName = try { view.resources.getResourceEntryName(view.id) } catch (e: Exception) { "unknown" }
                        Log.d("BluZ-Layout", "${indent}${view::class.simpleName}#$idName " +
                                "[${view.width}x${view.height}] vis=${view.visibility} alpha=${view.alpha}")
                        if (view is ViewGroup) {
                            for (i in 0 until view.childCount) {
                                printTree(view.getChildAt(i), indent + "  ")
                            }
                        }
                    }
                    printTree(v)

                    // 2. Конкретные проверки
                    val histHero = v.findViewById<LinearLayout>(R.id.histHero)
                    Log.d("BluZ-Layout", "histHero: found=${histHero != null}, height=${histHero?.height}, vis=${histHero?.visibility}")

                    val btnSave = v.findViewById<Button>(R.id.buttonHistorySave)
                    Log.d("BluZ-Layout", "buttonHistorySave: found=${btnSave != null}, vis=${btnSave?.visibility}, isShown=${btnSave?.isShown}")

                    // 3. Принудительно показываем для теста
                    btnSave?.apply {
                        visibility = View.VISIBLE
                        alpha = 1f
                        bringToFront()
                    }
                    v.invalidate()
                }

                GO.showStatistics()
            }
        }
    }


    /**
     * Обновляет лейблы единиц измерения у hero-readouts (`bzSpecDoseUnit`, `bzSpecAvgUnit`).
     * Учитывает [globalObj.unitsMess]: 1 = мкЗв/ч, иначе мкР/ч.
     *
     * Вызывается в [onViewCreated] для начального состояния. Дальше единицы обновляются
     * через [MainActivity.applyDoseReadouts] на каждом BLE-фрейме.
     */
    private fun applySpecUnitLabels() {
        val unit = if (GO.unitsMess == 1) "мкЗв/ч" else "мкР/ч"
        GO.bzSpecDoseUnit?.text = unit
        GO.bzSpecAvgUnit?.text = unit
        //GO.bzTotalSpectrPulses?.text = GO.spectrometerPulse.toString()
    }
}
