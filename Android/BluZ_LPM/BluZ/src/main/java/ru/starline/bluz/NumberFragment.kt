package ru.starline.bluz

import FastMLEM
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.map.InputListener
//import com.yandex.mapkit.input.InputListener
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.launch
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Math.toRadians
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private lateinit var hintView: TextView
    var noChange: Boolean = true
    private var mapInputListener: InputListener? = null
    private var allTrackPoints: List<TrackDetail> = emptyList()
    private var hideHintRunnable: Runnable? = null
    private lateinit var mapViewPlan: MapView
    private var cps2urh: Float? = 0f
    private lateinit var paddingTextLeft: EditText
    private lateinit var paddingTextRight: EditText
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
    override fun onDestroy() {
        mapInputListener?.let {
            GO.map?.removeInputListener(it)
        }
        hideHintRunnable?.let { hintView.removeCallbacks(it) }
        super.onDestroy()
    }

    /* Преобразование DP >> PX */
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    /* Формирование треугольного полигона для выгрузки в KML */
    private fun rectPolyGen(lat: Double, lon: Double, radius: Double, altit: Double): String {
        val coords = List(3) { i ->
            val angleDeg = 120.0 * i
            val angleRad = toRadians(angleDeg)
            // Смещение в градусах
            val latOffset = (radius * cos(angleRad)) / 111111.0
            val lonOffset = (radius * sin(angleRad)) / (111111.0 * cos(toRadians(lat)))

            val newLat = lat + latOffset
            val newLon = lon + lonOffset

            // Формат KML: долгота,широта,высота
            String.format(Locale.US, "%.6f,%.6f,%.6f", newLon, newLat, altit)
        }
        return coords.joinToString("\n")
    }

    /*
    * Настройка хинта для показа параметров точки
    */
    private fun setupMapClickListener() {
        // Удаляем старый listener
        mapInputListener?.let { GO.map?.removeInputListener(it) }

        mapInputListener = object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // Скрыть хинт по умолчанию
                hintView.visibility = View.GONE
                hideHintRunnable?.let { hintView.removeCallbacks(it) }
                val clickLat = point.latitude
                val clickLon = point.longitude

                // Ищем ближайшую точку в радиусе 50 метров
                var closestPoint: TrackDetail? = null
                var minDistance = Double.MAX_VALUE

                for (trackPoint in allTrackPoints) { // Перебираем все треки для поиска ближайшего по положению.
                    /* Расчет дистанции между координатами */
                    val dLat = Math.toRadians(trackPoint.latitude - clickLat)
                    val dLon = Math.toRadians(trackPoint.longitude - clickLon)
                    val a = sin(dLat / 2) * sin(dLat / 2) +
                            cos(Math.toRadians(clickLat)) * cos(Math.toRadians(trackPoint.latitude)) *
                            sin(dLon / 2) * sin(dLon / 2)
                    val distance = 6371000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
                    if (distance < minDistance && distance <= 50.0) { // 50 метров
                        minDistance = distance
                        closestPoint = trackPoint
                    }
                }

                if (closestPoint != null) {
                    // Формируем текст хинта
                    val timeStr = SimpleDateFormat("HH:mm:ss\ndd.MM.yyyy", Locale.getDefault())
                        .format(Date(closestPoint.timestamp * 1000))

                    if ( cps2urh == null || cps2urh == 0f) {
                        cps2urh = GO.propCPS2UR
                    }
                    val cpsStr: String
                    if (closestPoint.cps < 0) {
                        cpsStr = "CPS: N/A\n"
                    } else {
                        cpsStr = "CPS: ${"%.2f / %.2f".format(closestPoint.cps, closestPoint.cps * cps2urh!! ) }uRh\n"
                    }
                    hintView.text = buildString {
                        append(cpsStr)
                        append("Speed: ${"%.1f".format(closestPoint.speed * 3.6)} km/h\n")
                        append("Altitude: ${"%.1f".format(closestPoint.altitude)} m\n")
                        append("Magnitude: ${"%.1f".format(closestPoint.magnitude)} uT\n")
                        append("Accuracy: ${"%.1f".format(closestPoint.accuracy)} m\n")
                        append("Time: $timeStr")
                    }

                    // Показываем хинт в фиксированной позиции
                    hintView.post {
                        val params = hintView.layoutParams as ViewGroup.MarginLayoutParams
                        //val x = (mapViewPlan.width - hintView.measuredWidth) / 2
                        //val y = mapViewPlan.height - dpToPx(100)
                        //params.setMargins(x, y, 0, 0)
                        params.setMargins(10, 10, 0, 0)
                        hintView.layoutParams = params
                        hintView.visibility = View.VISIBLE
                        // Скрыть хинт через 10 секунд
                        val newRunnable = Runnable {
                            if (hintView.isVisible) {
                                hintView.visibility = View.GONE
                            }
                        }
                        hideHintRunnable = newRunnable
                        hintView.postDelayed(newRunnable, 10_000)
                    }
                }
            }
            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                hintView.visibility = View.GONE
            }
        }
        GO.map?.addInputListener(mapInputListener!!)
    }

    /* Перерисовка поинтов на карте */
    private fun redrawtMap(trackIdPriv: Long) {
        lifecycleScope.launch {
            val mxMn = GO.dao.getMaxMinForTrack(trackIdPriv)
            val mn = mxMn!!.min
            val mx = mxMn.max
            val dlt = mx - mn
            var kfc = 1.0
            if (dlt > 0) {
                kfc = 31.0 / dlt
            }
            val trcDet = GO.dao.getPointsForTrack(trackIdPriv)
            if (trcDet.isNotEmpty()) {
                // Подготовим переменные для поска границ трека
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLon = Double.MAX_VALUE
                var maxLon = -Double.MAX_VALUE

                for (detLoc in trcDet) {
                    if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                        // Рисуем цветную метку на карте
                        val imp = if (detLoc.cps > 0) {
                            GO.impArr[(kfc * (detLoc.cps - mn)).toInt().coerceIn(0, 31)]
                        } else {
                            GO.impBLACK
                        }
                        GO.map?.mapObjects?.addPlacemark()?.apply {
                            geometry = Point(detLoc.latitude, detLoc.longitude)
                            setIcon(imp!!)
                        }

                        // Ищем крайние координаты
                        minLat = minOf(minLat, detLoc.latitude)
                        maxLat = maxOf(maxLat, detLoc.latitude)
                        minLon = minOf(minLon, detLoc.longitude)
                        maxLon = maxOf(maxLon, detLoc.longitude)
                    }
                }

                // minLat, maxLat, minLon, maxLon определены в основном цикле.
                if (minLat == Double.MAX_VALUE) {
                    // Нет валидных точек
                    return@launch
                }

                if (minLat == maxLat && minLon == maxLon) {
                    // Одна точка - максимальный zoom
                    GO.map?.move(
                        CameraPosition(Point(minLat, minLon), 15f, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                } else {
                    /* Попробуем отобразить весь трек на экране */
                    val southWest = Point(minLat, minLon)
                    val northEast = Point(maxLat, maxLon)
                    val latDiff = abs(northEast.latitude - southWest.latitude)
                    val lonDiff = abs(northEast.longitude - southWest.longitude)
                    val centerLat = (southWest.latitude + northEast.latitude) / 2
                    val zoom = GO.locationManager?.calculateZoomLevel(GO.mapView, latDiff, lonDiff, centerLat) ?: 10f
                    val center = Point(centerLat, (southWest.longitude + northEast.longitude) / 2)
                    GO.map?.move(
                        CameraPosition(center, zoom, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )
                }
                /* Подготовка хинта для отображения данных точки */
                allTrackPoints = trcDet // сохраняем для хинта
                setupMapClickListener()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLocationModern() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && location.time > System.currentTimeMillis() - 5 * 60 * 1000) {
                // Есть актуальная кэшированная локация (не старше 5 минут)
                GO.lastPointLoc = location
            } else {
                // Запрашиваем новую
                requestFreshLocationModern(fusedClient)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestFreshLocationModern(client: FusedLocationProviderClient) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMinUpdateIntervalMillis(0)
            .setMaxUpdateDelayMillis(0)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                GO.lastPointLoc = result.lastLocation!!
                client.removeLocationUpdates(this)
            }
        }

        client.requestLocationUpdates(request, callback, null)
    }

    /*
    *   Отображение конфигурационных параметров в закладке Setup
    */
    fun reloadConfigParameters() {
        GO.propButtonInit = false         // Отключим реакцию в листенерах компонентов
        if (GO.LEMAC.isNotEmpty()) {
            GO.textMACADR.setText(GO.LEMAC)
        }
        when (GO.saveTrackType) {
            0 -> GO.rbKMLType.isChecked = true
            1 -> GO.rbGPXType.isChecked = true
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

        /* Полноэкранный режим */
        GO.cbFullScrn.isChecked = GO.fullScrn

        /* Ночной режим для карты */
        GO.cbNightMapMode.isChecked = GO.nightMapModeEnab

        /* Время выборки АЦП */
        GO.sampleTimeEdit.setText(GO.sampleTime.toString())

        /* Уровень логирования */
        GO.textAppLogLevel.setText(GO.appLogLevel.toString())

        /* Отступ слевого края */
        paddingTextLeft.setText(GO.paddingLeft.toString())

        /* Отступ справого края */
        paddingTextRight.setText(GO.paddingRight.toString())

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
        } else if (GO.pagerFrame == 5) {
            layoutNumber = R.layout.map_layout
        }
        return inflater.inflate(layoutNumber, container, false)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
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
                GO.drawSPECTER.imgView.setOnTouchListener { _, event ->
                    val x: Float = event.x
                    val y: Float = event.y
                    if ((event.action == MotionEvent.ACTION_DOWN)|| (event.action == MotionEvent.ACTION_MOVE)) {
                        if ((GO.drawCURSOR.oldX != x) /*|| (GO.drawCURSOR.oldY != y)*/)  {
                            GO.drawCURSOR.showCorsor(x, y)
                            //Log.i("BluZ-BT", "X: $x, Y: $y")
                        }
                    }
                    true
                }

                val CBSMA: CheckBox = view.findViewById(R.id.cbSMA)
                val CBMEDIAN: CheckBox = view.findViewById(R.id.cbMED)
                val CBMLEM: CheckBox = view.findViewById(R.id.cbMLEM)
                val pbMLEM: ProgressBar = view.findViewById(R.id.pbMLEM)

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
                        GO.txtCompMED.visibility = View.INVISIBLE
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
                        builder.setPositiveButton("Add") { _, _ ->
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
                            .setNegativeButton("Cancel") { _, _ ->
                                // Исполняемый код
                        }
                        builder.create()
                        builder.show()
                    }
                }
                GO.propButtonInit = false
                CBSMA.isChecked = GO.drawSPECTER.flagSMA
                CBMEDIAN.isChecked = GO.drawSPECTER.flagMEDIAN
                CBMLEM.isChecked = GO.drawSPECTER.flagMLEM
                GO.propButtonInit = true

                /* Обработка MLEM */
                CBMLEM.setOnCheckedChangeListener {_, isChecked ->
                    if (isChecked) {
                        Log.i("BluZ-BT", "Start MLEM")
                        if (GO.propButtonInit) {
                            //CBMLEM.isVisible = false
                            //val unfolder = FastMLEM(GO.drawSPECTER.ResolutionSpectr)
                            /* Выполнять будем в фоне */
                            CoroutineScope(Dispatchers.Default).launch {
                                withContext(Dispatchers.Main.immediate) {
                                    CBMLEM.isVisible = false
                                    pbMLEM.max = 25
                                    pbMLEM.progress = 0
                                    pbMLEM.isVisible = true
                                }
                                val unfolder = MLEM(GO.drawSPECTER.ResolutionSpectr)
                                GO.drawSPECTER.mlemBuffer = unfolder.unfoldSpectrum(
                                    GO.drawSPECTER.spectrData,
                                    iterations = 26
                                ) { progress ->
                                    withContext(Dispatchers.Main) {
                                        pbMLEM.progress = progress
                                    }
                                }
                                GO.drawSPECTER.flagMLEM = true /* Массив подготовлен и может прорисовываться */
                                /* Расчитаем МЭД */
                                val spTime = GO.spectrometerTime.toDouble()
                                GO.compMED = unfolder.calculateExposureRate(
                                    GO.drawSPECTER.mlemBuffer,
                                    spTime
                                ).toFloat()
                                /* В основном контексте */
                                withContext(Dispatchers.Main) {
                                    if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                                        GO.drawSPECTER.clearSpecter()
                                        GO.drawSPECTER.redrawSpecter(GO.specterType)
                                    }
                                    pbMLEM.isVisible = false
                                    CBMLEM.isVisible = true
                                }
                            }
                        }
                        Log.i("BluZ-BT", "Finish MLEM")
                    } else {
                        GO.drawSPECTER.flagMLEM = false /* Отключим прорисовку */
                        GO.txtCompMED.visibility = View.INVISIBLE
                    }
                    if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                        GO.drawSPECTER.clearSpecter()
                        GO.drawSPECTER.redrawSpecter(GO.specterType)
                    }
                }

                /* Управление SMA фильтром */
                CBSMA.setOnCheckedChangeListener {_, isChecked ->
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
                CBMEDIAN.setOnCheckedChangeListener {_, isChecked ->
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
                    *                   3 - Сброс дозиметра
                    *                   4 - Сброс логов
                    *                   5 - Запрос истории
                    *                   6 - Поиск прибора - включение звука и вибро
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
                    GO.drawSPECTER.resetSpecter()
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
                GO.drawDOZIMETER.dozView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        GO.drawDOZIMETER.dozView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        GO.drawDOZIMETER.Init()
                        GO.drawDOZIMETER.redrawDozimeter()
                    }
                })

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


                /* Сохранение логов приложения в файл */
                val btnSaveAppLogs : Button = view.findViewById(R.id.buttonSaveAppLog)
                btnSaveAppLogs.setOnClickListener {
                    val simpleDateFormat = SimpleDateFormat("yyyyMMdd'_'HHmmss", Locale.getDefault())
                    val fileName = "AppLog" + simpleDateFormat.format(Date().time)
                    var errFlag = false
                    try {
                        val SDPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                        val direct = File("$SDPath/BluZ")
                        if (!direct.exists()) {         // Нужно проверить наличие каталога.
                            if (direct.mkdir()) {
                                Log.d("BluZ-BT", "SD Path: $SDPath/BluZ")
                                GO.drawLOG.appendAppLogs("Create ok dir: ${direct.toString()}", 3)
                            } else {
                                Log.d("BluZ-BT", "Create dir error.")
                                Toast.makeText(context, "Directory create error.", Toast.LENGTH_LONG).show()
                                GO.drawLOG.appendAppLogs("Create error dir: ${direct.toString()}", 0)
                                errFlag = true
                            }
                        }
                        if (! errFlag) {
                            val myFile = File("$SDPath/BluZ/$fileName.html")
                            if (myFile.createNewFile()) {
                                Log.d("BluZ-BT", "File create Ok.")
                                GO.drawLOG.appendAppLogs("Create ok file: ${myFile.toString()}", 3)
                                val outputStream = FileOutputStream(myFile)
                                outputStream.write(GO.appLogBuffer.toByteArray())
                                outputStream.close()
                                Toast.makeText(context, "Log ${myFile} save complete.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "File create error.", Toast.LENGTH_LONG).show()
                                Log.d("BluZ-BT", "Create file error.")
                            }
                        }



                    } catch (e: Exception) {
                        Log.e("BluZ-BT", "Error: ${e.message}")
                        GO.drawLOG.appendAppLogs("Save: ${e.message}", 0)
                    }
                }

                /* Очистка логов приложения */
                val buttonClearAppLog : Button = view.findViewById(R.id.buttonClearAppLog)
                buttonClearAppLog.setOnClickListener {
                    GO.appLogBuffer = ""
                    GO.drawLOG.updateAppLogs()
                }


                /* Очистка логов */
                val btnCleaarLog: Button = view.findViewById(R.id.buttonClearLog)
                btnCleaarLog.setOnClickListener {
                    GO.BTT.sendCommand(4u)      // Очистка лога.
                    Toast.makeText(GO.mainContext, R.string.resetLogs, Toast.LENGTH_LONG).show()
                }
                GO.drawLOG.logView = view.findViewById(R.id.logScrolView)
                GO.drawLOG.logsText = view.findViewById(R.id.logsText)
                GO.drawLOG.appLogView = view.findViewById(R.id.appScrolView)
                GO.drawLOG.appLogText = view.findViewById(R.id.appLogText)
                if (! GO.drawLOG.logsDrawIsInit) {
                    GO.drawLOG.updateLogs()
                    GO.drawLOG.updateAppLogs()
                    GO.drawLOG.logsDrawIsInit = true
                }

                GO.drawLOG.appLogView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        GO.drawLOG.appLogView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        GO.drawLOG.updateAppLogs()
                        GO.drawLOG.updateLogs()
                    }
                })


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
                GO.cbFullScrn = view.findViewById(R.id.CBFullScreen)
                GO.cbNightMapMode = view.findViewById(R.id.CBNightMode)
                GO.rbGPXType = view.findViewById(R.id.rbGPX)
                GO.rbKMLType = view.findViewById(R.id.rbKML)
                GO.rbTrackFmt = view.findViewById(R.id.RGTrackFormat)
                GO.sampleTimeEdit = view.findViewById(R.id.editSampleTime)
                GO.textAppLogLevel = view.findViewById(R.id.editTextApplucationLog)

                paddingTextLeft  = view.findViewById(R.id.editTextPaddingLeft)
                paddingTextRight = view.findViewById(R.id.editTextPaddingRight)

                reloadConfigParameters()

                /* Изменение отображения режима карты */
                GO.cbNightMapMode.setOnCheckedChangeListener { buttonView, isChecked ->
                    GO.nightMapModeEnab = isChecked
                }

                /* Изменение полноэкранного режима включение / выключение видимости баров */
                GO.cbFullScrn.setOnCheckedChangeListener { buttonView, isChecked ->
                    GO.fullScrn = isChecked
                    val window = requireActivity().window
                    val decorView = window.decorView
                    WindowCompat.setDecorFitsSystemWindows(window, !isChecked)
                    val insetsController = WindowCompat.getInsetsController(window, decorView)

                    if (isChecked) {
                        // Прозрачный статус-бар
                        window.statusBarColor = Color.TRANSPARENT
                        //insetsController.isAppearanceLightStatusBars = false // белые иконки
                        //Разрешаем рисовать ПОД челкой
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    } else {
                        // Возвращаем цвет
                        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.black)
                        //insetsController.isAppearanceLightStatusBars = true // чёрные иконки на светлом фоне
                        // Возвращаем стандартное поведение для челки
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                        }
                    }

                    /*
                    if (isChecked) {
                        // Скрываем НАВИГАЦИОННУЮ панель (не статус-бар!)
                        insetsController.hide(WindowInsets.Type.navigationBars())

                        // Делаем статус-бар прозрачным, но оставляем его (значки времени и т.д.)
                        window.statusBarColor = Color.TRANSPARENT
                        insetsController.isAppearanceLightStatusBars = false // белые иконки

                        // Разрешаем рисовать ПОД челкой
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode =
                                if (isChecked) {
                                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                                } else {
                                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                                }
                        }

                    } else {
                        // Возвращаем всё обратно
                        insetsController.show(WindowInsets.Type.navigationBars())
                        insetsController.show(WindowInsets.Type.statusBars())

                        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.black)
                        insetsController.isAppearanceLightStatusBars = true

                        // Возвращаем стандартное поведение для челки
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                        }
                    }
                    */
                }

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
                    val pdTmpL = paddingTextLeft.text.toString().toIntOrNull() ?: 0
                    val pdTmpR = paddingTextRight.text.toString().toIntOrNull() ?: 0
                    val activity = requireActivity()
                    val mainLayout = activity.findViewById<View>(R.id.main)
                    if ((pdTmpL != GO.paddingLeft) or (pdTmpR != GO.paddingRight)) {
                        /* Отступ от левого края */
                        GO.paddingLeft = pdTmpL

                        /* Отступ от правого края */
                        GO.paddingRight = pdTmpR
                        mainLayout.setPadding(GO.paddingLeft, mainLayout.paddingTop, GO.paddingRight, mainLayout.paddingBottom)
                    }


                    /* Уровень ногирования */
                    GO.appLogLevel = GO.textAppLogLevel.text.toString().toInt()

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

                    GO.sampleTime = GO.sampleTimeEdit.text.toString().toInt()
                    if (GO.sampleTime < 0 || GO.sampleTime> 7) {
                        GO.sampleTime = 0
                    }

                    Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())

                    GO.writeConfigParameters()      // Сохраненние конфигурации.

                    Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
                    if (GO.LEMAC.length == 17 &&  GO.LEMAC[0] != 'X') { // MAC адрес настроен, продолжаем работу.
                        GO.tmFull.startTimer();
                    } else {
                        GO.oneShotBLETimer = false
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
                    *                   1 - |
                    *                   2 - | Врямя выборки
                    *                   3 - |
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
                    //var convVal = ByteArray(4)

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
                    var convVal = ByteBuffer.allocate(4).putInt(GO.propLevel1).array();
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
                    val tmpSampleTime = ((GO.sampleTimeEdit.text.toString().toUInt() and 7u) shl 1).toUByte()
                    GO.BTT.sendBuffer[38] = GO.BTT.sendBuffer[38] or tmpSampleTime
                    GO.BTT.sendCommand(0u)
                }

                /* Radiobuttons для выбора элемента настройки цвета */

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

                /* Переключатель формата для сохранения спектра */
                GO.rbTrackFmt.setOnCheckedChangeListener { _, checkedId -> view.findViewById<RadioButton>(checkedId)?.apply {
                    noChange = false
                    when (checkedId) {
                        GO.rbKMLType.id -> {
                            GO.saveTrackType = 0
                            if (GO.isButtonSaveTrackInitialized) {
                                GO.buttonSaveTrack.text = resources.getString(R.string.textKML)
                            }
                        }
                        GO.rbGPXType.id -> {
                            GO.saveTrackType = 1
                            if (GO.isButtonSaveTrackInitialized) {
                                GO.buttonSaveTrack.text = resources.getString(R.string.textGPX)
                            }
                        }
                    }
                    noChange = true
                }
                }

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
                /*
                * Панель для отображения цвета спектра
                * нужно прорисовать после определения layout
                */
                GO.drawExamp.exampleImgView = view.findViewById(R.id.tvColor)
                GO.drawExamp.exampleImgView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        GO.drawExamp.exampleImgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        GO.drawExamp.init()
                        GO.drawExamp.exampRedraw()
                    }
                })

                /* Кнопка для включения звука и вибро - поиск прибора */
                val buttonFnd = view.findViewById<Button>(R.id.buttonFind)
                buttonFnd.setOnClickListener {
                    GO.BTT.sendCommand(6u)
                }

            } else if (getInt(ARG_OBJECT) == 5) {
                /*
                *
                * Работа с картой и треками
                *
                */
                // Создаём хинт программно

                hintView = TextView(requireContext()).apply {
                    setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                    setBackgroundResource(R.drawable.hint_background)
                    textSize = 12f
                    setTextColor(Color.BLACK)
                    visibility = View.GONE
                }
                mapViewPlan = view.findViewById<MapView>(R.id.mapview)
                mapViewPlan = view.findViewById<MapView>(R.id.mapview)
                mapViewPlan.addView(
                    hintView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )                //(mapViewPlan.parent as ViewGroup).addView(hintView)
                /* Сохранения трека */
                GO.buttonSaveTrack = view.findViewById(R.id.buttonSaveTrack)
                GO.buttonSaveTrack.setOnClickListener {
                    val sdf = SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.getDefault())
                    val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.HALF_UP }
                    val documentsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                    val bluzDir = File("$documentsDir/BluZ")
                    /* Проверяем наличие каталога приложения в Documents */
                    if (!bluzDir.exists()) {
                        bluzDir.mkdirs()        // Создаем если отсутствует
                    }
                    when (GO.saveTrackType) {
                        /* KML формат */
                        0 -> {
                            /* Создадим KML файл для сохранения трека */
                            if (GO.curretnTrcName.isNotEmpty()) {
                                /* Формируем имя файла для сохранения трека */
                                val fileKML = File("$documentsDir/BluZ/${GO.curretnTrcName}.kml")
                                /* Если файл существует - удаляем */
                                if (fileKML.exists()) {
                                    fileKML.delete()
                                }
                                Log.i("BluZ-BT", "File: $fileKML")
                                try {
                                    /* Пробуем создать файл */
                                    if (fileKML.createNewFile()) {
                                        Log.d("BluZ-BT", "File create Ok.")
                                        val outputStream = FileOutputStream(fileKML)
                                        var kmlTmp =
                                            GO.mainContext.resources.openRawResource(R.raw.track_header)
                                                .bufferedReader().use { it.readText() }
                                        val kmlHdr =
                                            kmlTmp.replace("____NAME____", GO.curretnTrcName)
                                                .replace("____DECRIPTION____", "BluZ KML")
                                        /* Записываем заголовок файла */
                                        outputStream.write(kmlHdr.toByteArray())

                                        kmlTmp =
                                            GO.mainContext.resources.openRawResource(R.raw.track_style)
                                                .bufferedReader().use { it.readText() }
                                        /* Записываем стили */
                                        var kmlStl: String = ""
                                        for (iclr in 0..31) {
                                            kmlStl = kmlTmp.replace("____ID____", "Stl$iclr")
                                                .replace("____COLOR____", GO.radClrsKml[iclr])
                                            outputStream.write(kmlStl.toByteArray())
                                        }
                                        kmlStl = kmlTmp.replace("____ID____", "blackStyle")
                                            .replace("____COLOR____", "7f000000")
                                        outputStream.write(kmlStl.toByteArray())
                                        kmlTmp =
                                            GO.mainContext.resources.openRawResource(R.raw.track_point)
                                                .bufferedReader().use { it.readText() }

                                        lifecycleScope.launch {
                                            val maxMin =
                                                GO.dao.getMaxMinForTrack(GO.currentTrack4Show)
                                            val mn = maxMin!!.min
                                            val mx = maxMin.max
                                            val dlt = mx - mn
                                            var kfc = 1.0
                                            if (dlt > 0) {
                                                kfc = 31.0 / dlt
                                            }
                                            Log.d("BluZ-BT", "Max: $mx, Min: $mn, Koef: $kfc")
                                            /* Выбираем точки для текущего трека */
                                            val trcDet =
                                                GO.dao.getPointsForTrack(GO.currentTrack4Show)
                                            if (trcDet.isNotEmpty()) {
                                                for (detLoc in trcDet) {
                                                    if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                                                        var styleStr: String
                                                        /* Если CPS не определен */
                                                        if (detLoc.cps >= 0) {
                                                            styleStr =
                                                                "#Stl" + (kfc * (detLoc.cps - mn)).toInt()
                                                        } else {
                                                            styleStr = "#blackStyle"
                                                        }
                                                        val kmlPnt = kmlTmp.replace(
                                                            "____POINT____",
                                                            "CPS:" + df.format(detLoc.cps) + " / " + df.format(
                                                                detLoc.cps * GO.propCPS2UR
                                                            ) + "uR/h"
                                                        )
                                                            .replace(
                                                                "____STR1____",
                                                                "Time:" + sdf.format(Date(detLoc.timestamp * 1000))
                                                            )
                                                            .replace(
                                                                "____STR2____",
                                                                "Speed:" + df.format(detLoc.speed * 3.6f) + " km/h"
                                                            )
                                                            .replace(
                                                                "____STR3____",
                                                                "Altit:" + df.format(detLoc.altitude)
                                                            )
                                                            .replace(
                                                                "____STR4____",
                                                                "Accur:" + df.format(detLoc.accuracy)
                                                            )
                                                            .replace(
                                                                "____STR5____",
                                                                "Magn:" + df.format(detLoc.magnitude)
                                                            )
                                                            .replace(
                                                                "____LOC____",
                                                                rectPolyGen(
                                                                    detLoc.latitude,
                                                                    detLoc.longitude,
                                                                    detLoc.accuracy.toDouble(),
                                                                    detLoc.altitude
                                                                )
                                                            )
                                                            .replace("____STYLE____", styleStr)
                                                        /* Записываем поинт */
                                                        outputStream.write(kmlPnt.toByteArray())
                                                    }
                                                }
                                                kmlTmp =
                                                    GO.mainContext.resources.openRawResource(R.raw.track_footer)
                                                        .bufferedReader().use { it.readText() }
                                                outputStream.write(kmlTmp.toByteArray())
                                                outputStream.close()
                                                Toast.makeText(
                                                    context,
                                                    "Save complete to: %s".format(fileKML),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } catch (e: IOException) {
                                    Toast.makeText(
                                        context,
                                        "Error save file. ${e.toString()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e("BluZ-BT", "Error: ", e)
                                }
                            } else {
                                Toast.makeText(context, "Track not selected.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        /* CSV формат */
                        1 -> {
                            /* Создадим CSV файл для сохранения трека */
                            if (GO.curretnTrcName.isNotEmpty()) {
                                /* Формируем имя файла для сохранения трека */
                                val fileCSV = File("$documentsDir/BluZ/${GO.curretnTrcName}.csv")
                                /* Если файл существует - удаляем */
                                if (fileCSV.exists()) {
                                    fileCSV.delete()
                                }
                                Log.i("BluZ-BT", "File: $fileCSV")
                                try {
                                    /* Пробуем создать файл */
                                    if (fileCSV.createNewFile()) {
                                        Log.d("BluZ-BT", "File create Ok.")
                                        val outputStream = FileOutputStream(fileCSV)
                                        var csvTmp =
                                            GO.mainContext.resources.openRawResource(R.raw.track_header_csv)
                                                .bufferedReader().use { it.readText() }
                                        /* Записываем заголовок файла */
                                        outputStream.write(csvTmp.toByteArray())

                                        // ___TIME___;___DOSE___;___LAT___;___LNG___;___ALT___;___SPEED___;___CPS___;___ACCUR___;___MAGN___
                                        csvTmp =
                                            GO.mainContext.resources.openRawResource(R.raw.track_detale_csv)
                                                .bufferedReader().use { it.readText() }

                                        lifecycleScope.launch {
                                            /* Выбираем точки для текущего трека */
                                            val trcDet =
                                                GO.dao.getPointsForTrack(GO.currentTrack4Show)
                                            if (trcDet.isNotEmpty()) {
                                                for (detLoc in trcDet) {
                                                    if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                                                        val csvPnt = csvTmp.replace(
                                                            "___TIME___",
                                                            detLoc.timestamp.toString()
                                                        )
                                                            .replace(
                                                                "___DOSE___",
                                                                df.format(detLoc.cps * GO.propCPS2UR)
                                                            )
                                                            .replace(
                                                                "___LAT___",
                                                                detLoc.latitude.toString()
                                                            )
                                                            .replace(
                                                                "___LNG___",
                                                                detLoc.longitude.toString()
                                                            )
                                                            .replace(
                                                                "___ALT___",
                                                                df.format(detLoc.altitude)
                                                            )
                                                            .replace(
                                                                "___SPEED___",
                                                                df.format(detLoc.speed * 3.6f)
                                                            )
                                                            .replace(
                                                                "___CPS___",
                                                                df.format(detLoc.cps)
                                                            )
                                                            .replace(
                                                                "___ACCUR___",
                                                                df.format(detLoc.accuracy)
                                                            )
                                                            .replace(
                                                                "___MAGN___",
                                                                df.format(detLoc.magnitude)
                                                            )
                                                        /* Записываем поинт */
                                                        outputStream.write(csvPnt.toByteArray())
                                                    }
                                                }
                                                outputStream.close()
                                                Toast.makeText(
                                                    context,
                                                    "Save complete to: %s".format(fileCSV),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Track %s is empty. ${GO.curretnTrcName}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } catch (e: IOException) {
                                    Toast.makeText(
                                        context,
                                        "Error save file. ${e.toString()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e("BluZ-BT", "Error: ", e)
                                }
                            } else {
                                Toast.makeText(context, "Track not selected.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }

                when (GO.saveTrackType) {
                    0 -> GO.buttonSaveTrack.text = resources.getString(R.string.textKML)
                    1 -> GO.buttonSaveTrack.text = resources.getString(R.string.textGPX)
                }
                /* Кнопка запуска/остановки записи трека. */
                GO.recordTrc = view.findViewById(R.id.buttonRecord)
                /* Восстановим текущее состояние */
                if (GO.trackIsRecordeed) {
                    GO.recordTrc.text = getString(R.string.stopRec)
                    GO.recordTrc.setTextColor(Color.RED)
                    GO.locationManager?.startLocationUpdates()
                    GO.currentTrack4Show = GO.currentTrck
                    lifecycleScope.launch {
                        /* Получим текущий активный трек для записи */
                        val tmpTrc = GO.dao.getCurrentTrack()
                        if (tmpTrc != null) {
                            GO.currentTrck = tmpTrc
                        } else {
                            /* Если нет активного трека, нужно установить на трек по умолчанию */
                            GO.currentTrck = GO.dao.getFirstTrack()
                            GO.dao.activateTrack(GO.currentTrck)
                        }
                        GO.curretnTrcName = GO.dao.getSelectTrack(GO.currentTrck)
                        cps2urh = GO.dao.getCPS2URH(GO.currentTrck)
                        GO.currentTrackName.text =
                            getString(R.string.current_track_label, GO.curretnTrcName)
                        Log.d(
                            "BluZ-BT",
                            "Track_id: ${GO.currentTrck}, Track_name: ${GO.curretnTrcName}"
                        )
                        redrawtMap(GO.currentTrck)
                    }

                }
                /* Обработка нажатия на кнопку записи трека*/
                GO.recordTrc.setOnClickListener {
                    if (GO.trackIsRecordeed) {
                        /* Здесь останавливаем запись трека */
                        GO.recordTrc.text = getString(R.string.startRec)
                        GO.recordTrc.setTextColor(
                            resources.getColor(
                                R.color.buttonTextColor,
                                GO.mainContext.theme
                            )
                        )
                        GO.trackIsRecordeed = false
                        GO.locationManager?.stopLocationUpdates()
                    } else {
                        /* Здесь начинаем запись трека */
                        GO.recordTrc.text = getString(R.string.stopRec)
                        GO.recordTrc.setTextColor(Color.RED)
                        GO.trackIsRecordeed = true
                        /* Запускаем менеджер обновления и записи данных */
                        GO.locationManager?.startLocationUpdates()
                        GO.currentTrck = GO.currentTrack4Show
                        lifecycleScope.launch {
                            GO.dao.deactivateAllTracks()
                            GO.dao.activateTrack(GO.currentTrack4Show)
                        }
                    }
                }

                /* Название отображаемого трека */
                GO.currentTrackName = view.findViewById(R.id.textCurrentTrack)
                GO.currentTrackName.setOnClickListener {
                    if (GO.curretnTrcName.isNotEmpty()) {
                        val context = requireContext()
                        // Создаём EditText для изменения имени
                        val input = EditText(context)
                        input.setText(GO.curretnTrcName)
                        AlertDialog.Builder(context)
                            .setTitle("Edit track")
                            .setView(input) // добавляем поле ввода
                            .setPositiveButton("Save") { dialog, _ ->
                                val name = input.text.toString().trim()
                                if (name.isEmpty()) {
                                    Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT)
                                        .show()
                                    return@setPositiveButton
                                }

                                // Запускаем корутину для изменения названия трека в БД
                                lifecycleScope.launch {
                                    try {
                                        GO.dao.editTrack(GO.currentTrack4Show, name)
                                        GO.curretnTrcName = name
                                        GO.currentTrackName.text =
                                            getString(R.string.current_track_label, name)
                                    } catch (e: Exception) {
                                        Log.e("TrackDialog", "Error edit track", e)
                                    }
                                }
                                dialog.dismiss()
                            }
                            .setNegativeButton("Close") { dialog, _ ->
                                dialog.cancel()
                            }
                            .setNeutralButton("Delete") { dialog, which ->
                                if (GO.currentTrack4Show > 1) {
                                    /* Если выполняется запись, нужно остановить */
                                    if (GO.trackIsRecordeed) {
                                        /* Здесь останавливаем запись трека */
                                        GO.recordTrc.text = getString(R.string.startRec)
                                        GO.recordTrc.setTextColor(
                                            resources.getColor(
                                                R.color.buttonTextColor,
                                                GO.mainContext.theme
                                            )
                                        )
                                        GO.trackIsRecordeed = false
                                        GO.locationManager?.stopLocationUpdates()
                                    }
                                    lifecycleScope.launch {
                                        try {
                                            GO.dao.deleteTrack(GO.currentTrack4Show)
                                            //GO.currentTrack4Show = GO.dao.getFirstTrack()
                                            //GO.curretnTrcName = GO.dao.getSelectTrack(GO.currentTrack4Show)
                                            //GO.currentTrackName.text =  getString(R.string.current_track_label, GO.curretnTrcName)
                                            GO.currentTrack4Show = 0
                                            GO.curretnTrcName = ""
                                            GO.currentTrackName.text =
                                                getString(R.string.current_track_label, "")
                                        } catch (e: Exception) {
                                            Log.e("TrackDialog", "Error delete track", e)
                                        }
                                    }
                                    GO.map?.mapObjects?.clear()
                                }
                            }
                            .show()
                    }
                }
                /* Создание нового трека */
                val btnNewTrack: Button = view.findViewById(R.id.buttonNewTrack)
                btnNewTrack.setOnClickListener {
                    val context = requireContext()

                    // Создаём EditText для ввода имени
                    val input = EditText(context)
                    input.hint = "Enter Track name"

                    // Диалог создания нового трека
                    AlertDialog.Builder(context)
                        .setTitle("New track")
                        .setView(input) // добавляем поле ввода
                        .setPositiveButton("Create") { dialog, _ ->
                            val name = input.text.toString().trim()
                            if (name.isEmpty()) {
                                Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }

                            GO.map?.mapObjects?.clear()
                            // Запускаем корутину для сохранения в БД
                            lifecycleScope.launch {
                                try {
                                    val newTrack = Track(
                                        id = 0,
                                        name = name,
                                        createdAt = System.currentTimeMillis() / 1000,
                                        isActive = false,
                                        isHidden = false,
                                        GO.propCPS2UR
                                    )
                                    GO.currentTrack4Show = GO.dao.insertTrack(newTrack)
                                    GO.curretnTrcName = name
                                    GO.currentTrackName.text =
                                        getString(R.string.current_track_label, name)
                                    /* Сразу делаем активным если не выполняется запись */
                                    if (!GO.trackIsRecordeed) {
                                        GO.dao.deactivateAllTracks()
                                        GO.dao.activateTrack(GO.currentTrack4Show)
                                        GO.currentTrck = GO.currentTrack4Show
                                    }
                                    // Здесь необходимо обновление списка.
                                    //refreshTrackList()
                                } catch (e: Exception) {
                                    Log.e("TrackDialog", "Error create track", e)
                                }
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Close") { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                }
                /* Выбор трека для отображения */
                val btnTrackList: Button = view.findViewById(R.id.buttonTracks)
                btnTrackList.setOnClickListener {
                    lifecycleScope.launch {
                        val tracks = GO.dao.getAllTracks()
                        val sdf = SimpleDateFormat("[dd.MM.yy HH:mm:ss] ", Locale.getDefault())
                        val names = tracks.map { sdf.format(Date(it.createdAt * 1000)) + it.name }
                            .toTypedArray()

                        /* Изменение стиля диалогового окна,
                         * для уменьшения межстрочных интервалов
                         * и раскраски отдельных полей
                         */
                        val adapter = object : ArrayAdapter<String>(
                            requireContext(),
                            R.layout.item_track_dialog,
                            names
                        ) {
                            override fun getView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                val view = super.getView(position, convertView, parent)
                                val textView = view.findViewById<TextView>(R.id.textView)
                                val fullText = names[position]
                                val dateLength = 20
                                if (fullText.length >= dateLength) {
                                    val spannable = SpannableString(fullText)
                                    val dateColor = ContextCompat.getColor(
                                        requireContext(),
                                        R.color.labelTextColor2
                                    )
                                    spannable.setSpan(
                                        ForegroundColorSpan(dateColor),
                                        0,
                                        dateLength,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    textView.text = spannable
                                } else {
                                    textView.text = fullText
                                }
                                return view
                            }
                        }

                        AlertDialog.Builder(requireContext())
                            .setTitle("Select track")
                            //.setItems(names) { _, which ->
                            .setAdapter(adapter) { _, which ->
                                GO.map?.mapObjects?.clear()                 // Очистим карту от старых объектов.
                                val selectedTrack = tracks[which]
                                GO.curretnTrcName = selectedTrack.name
                                GO.currentTrackName.text =
                                    getString(R.string.current_track_label, selectedTrack.name)
                                GO.currentTrack4Show =
                                    selectedTrack.id     // Запомним текущий отображаемый трек.
                                redrawtMap(GO.currentTrack4Show)
                                /* Нужно остановить запись */
                                if (GO.trackIsRecordeed) {
                                    /* Здесь останавливаем запись трека */
                                    GO.recordTrc.text = getString(R.string.startRec)
                                    GO.recordTrc.setTextColor(
                                        resources.getColor(
                                            R.color.buttonTextColor,
                                            GO.mainContext.theme
                                        )
                                    )
                                    GO.trackIsRecordeed = false
                                    GO.locationManager?.stopLocationUpdates()
                                }
                            }
                            .setNegativeButton("Close", null)
                            .show()
                    }
                }

                /* Добавление поинта */
                val btnAddPoint: ImageButton = view.findViewById(R.id.buttonAddPoint)
                btnAddPoint.setOnClickListener {
                    //val drawable = ContextCompat.getDrawable(GO.mainContext, R.drawable.ic_gps_point)!!.mutate()
                    //DrawableCompat.setTint(drawable, Color.RED)
                    //val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                    //val canvas = Canvas(bitmap)
                    //drawable.setBounds(0, 0, canvas.width, canvas.height)
                    //drawable.draw(canvas)
                    //val imp1 = ImageProvider.fromBitmap(bitmap)

                    //val imageProvider = ImageProvider.fromResource(GO.mainContext, R.drawable.ic_gps_point
                    /*  Test */
                    /*for (iii in 0 until 256) {
                        val placemark = GO.map?.mapObjects?.addPlacemark().apply {
                            this?.geometry = Point(GO.lastPointLoc.latitude + (iii / 50000.0), GO.lastPointLoc.longitude)
                            //this!!.setIcon(GO.imp[iii]!!)
                            this!!.setIcon(GO.impGREEN)
                        }
                    }*/
                    //val placemark = GO.map?.mapObjects?.addPlacemark().apply {
                    //    this?.geometry = Point(GO.lastPointLoc.latitude, GO.lastPointLoc.longitude)
                    //    //this!!.setIcon(GO.imp[iii]!!)
                    //    this!!.setIcon(GO.impGREEN)
                    //}

                }

                /* Показать текущую позицию */
                val btnMapLocate: ImageButton = view.findViewById(R.id.buttonMapLocate)
                btnMapLocate.setOnClickListener {
                    /*
                    Get GPS location
                    */
                    try {
                        //val lm = GO.mainContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        //val loc: Location? = lm.getCurrentLocation(Context.LOCATION_SERVICE, )
                        //val loc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        //if (loc != null) {
                        //    GO.Latitude = loc.latitude
                        //    GO.Longitude = loc.longitude
                        //locationStr = " Lat: ${loc.latitude} Lng: ${loc.longitude} Alt: ${loc.altitude} Speed: ${loc.speed}"
                        //} else {
                        //    Toast.makeText(context, "GPS error.", Toast.LENGTH_SHORT).show()
                        //}

                        if (GO.lastPointLoc.latitude != 0.0 && GO.lastPointLoc.longitude != 0.0) {
                            GO.map?.move(
                                CameraPosition(
                                    Point(
                                        GO.lastPointLoc.latitude,
                                        GO.lastPointLoc.longitude
                                    ),/* zoom = */ 15.0f,/* azimuth = */ 0.0f,/* tilt = */ 0.0f
                                )
                            )
                        }
                        // Остановить запрос
                        //locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
                        //locationCallback = null
                    } catch (e: Exception) {
                        Toast.makeText(context, "GPS write error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                /* Яндекс API */
                GO.createRainbowColors()        // Создание градиента для меток.
                val lm = GO.mainContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                /* Инициализируем карту */
                GO.mapView = view.findViewById(R.id.mapview)
                GO.mapWindow = GO.mapView.mapWindow
                GO.map = GO.mapWindow?.map
                MapKitFactory.getInstance().onStart()
                GO.mapView.onStart()
                //GO.map?.isRotateGesturesEnabled = false         // Отключим поворот карты
                GO.map?.set2DMode(true)
                GO.map?.isNightModeEnabled = GO.nightMapModeEnab
                // Проверяем, включен ли провайдер
                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // Показать диалог с предложением включить GPS
                    Log.d("BluZ-BT", "Provider is disabled.")
                    return
                } else {
                    Log.d("BluZ-BT", "Provider is enabled.")
                }
                val lstLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lstLoc != null) {
                    GO.lastPointLoc = lstLoc
                    // Создаём менеджер
                    GO.locationManager = ContinuousLocationManager(this) { location ->
                        // Этот блок вызывается при каждом обновлении
                        if (isAdded) {
                            activity?.runOnUiThread {
                                // Событие обновления координат, далее проверяем - насколько изменилась дистанция.
                                //val results = FloatArray(1)
                                //Location.distanceBetween(
                                //    location.latitude,
                                //    location.longitude,
                                //    GO.lastPointLoc.latitude,
                                //    GO.lastPointLoc.longitude,
                                //    results
                                //)
                                /* Расчитываем средний cps за интервал между метками*/
                                val cpsAveredge: Float = GO.cpsAVG / GO.cpsIntervalCount
                                Log.i(
                                    "BluZ-BT",
                                    "Lat: ${location.latitude}, Lng: ${location.longitude}, Accuracy: ${location.accuracy}, cps:${cpsAveredge}, CT: ${GO.currentTrck}"
                                )
                            }
                        }
                    }
                } else {
                    getLocationModern()
                }
            }
        }
    }
}