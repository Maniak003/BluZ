package ru.starline.bluz

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Math.toRadians
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

/**
 * Вкладка карты с GPS-трекингом радиационного фона.
 *
 * **SDK:** Yandex MapKit `4.19.0-lite`. API-ключ из `BuildConfig.MAPKIT_API_KEY`.
 *
 * **Жизненный цикл MapView** — критично:
 *  - `onStart`: `MapKitFactory.getInstance().onStart()` + `mapViewPlan.onStart()`
 *  - `onStop`: то же в обратном порядке. Иначе утечки и/или краши.
 *
 * **Треки и точки:**
 *  - [data.entity.Track] — название, дата создания, флаги isActive/isHidden, `cps2urh`
 *  - [data.entity.TrackDetail] — точка трека (lat/lon/cps/magnitude/...), FK на Track
 *  - Управление через `dao` ([data.dao.DosimeterDao]): create, list, edit, delete (soft),
 *    activate, getPointsForTrack, getMaxMinForTrack
 *
 * **Отрисовка** ([redrawtMap]):
 *  1. Загрузка точек выбранного трека из БД
 *  2. Подготовка палитры цветов через [globalObj.createRainbowColors] — 32 [ImageProvider]
 *     с цветами от синего до красного (нормализация по min/max CPS трека)
 *  3. Для каждой точки `addPlacemark` с цветной иконкой
 *  4. Расчёт bounding box и применение camera position
 *
 * **Маркер «моё место»:**
 *  - [myLocationCross] — красный крест 40×40 px (Bitmap → ImageProvider)
 *  - [myLocationAccuracy] — серый полупрозрачный круг радиусом `location.accuracy`
 *  - Обновляются в [updateMyLocationOnMap] на каждый GPS callback
 *  - **Важно:** `mapObjects.clear()` (при create/delete/select track) уничтожает их →
 *    [resetMyLocationMarkers] обнуляет ссылки + проверка `isValid` в начале update-метода
 *
 * **GPS-трекинг:**
 *  - `ContinuousLocationManager` запускается в [onStart], останавливается в [onStop]
 *    (если нет активной записи — иначе нужен для записи)
 *  - При активной записи (`GO.trackIsRecordeed`) [MainActivity.recordTrackPoint] вставляет
 *    точку при каждом BLE-фрейме
 *  - При выходе из приложения [MainActivity.performExit] стартует [BleMonitoringService]
 *    для фоновой записи
 *
 * **Экспорт:** KML (с цветной палитрой) или CSV в `/Documents/BluZ/`.
 *
 * **Ночной режим карты** = тема приложения: `map.isNightModeEnabled = !ThemePrefs.isDayTheme(...)`.
 */
class BluZMapFragment : Fragment() {

    private var mapInputListener: InputListener? = null
    private var allTrackPoints: List<TrackDetail> = emptyList()
    private var hideHintRunnable: Runnable? = null
    private lateinit var mapViewPlan: MapView
    private var cps2urh: Float? = 0f
    private lateinit var hintView: TextView

    // Маркер текущего местоположения: красный крест + круг точности
    private var myLocationCross: PlacemarkMapObject? = null
    private var myLocationAccuracy: CircleMapObject? = null
    private val myLocationCrossImage: ImageProvider by lazy { createMyLocationCrossImage() }

    /**
     * Старт MapView. Обязательно дёргать `MapKitFactory.getInstance().onStart()` +
     * `mapViewPlan.onStart()` — иначе карта не отрисует тайлы.
     *
     * Также запускает GPS-обновления через [globalObj.locationManager] — пока вкладка
     * видна, маркер «моё место» обновляется.
     */
    override fun onStart() {
        super.onStart()
        if (::mapViewPlan.isInitialized) {
            MapKitFactory.getInstance().onStart()
            mapViewPlan.onStart()
        }
        // Включаем GPS-обновления пока вкладка карты видна
        GO.locationManager?.startLocationUpdates()
    }

    /**
     * Стоп MapView и MapKit. **Не останавливает GPS** если идёт активная запись трека
     * ([globalObj.trackIsRecordeed]) — пользователь, скорее всего, переключился на другую
     * вкладку и хочет, чтобы запись продолжалась.
     */
    override fun onStop() {
        super.onStop()
        if (::mapViewPlan.isInitialized) {
            mapViewPlan.onStop()
            MapKitFactory.getInstance().onStop()
        }
        // Не останавливаем при активной записи трека — сервис продолжает в фоне через свой клиент,
        // но и фрагмент-локальный поток нужен MainActivity.recordTrackPoint, который использует GO.lastPointLoc
        if (!GO.trackIsRecordeed) {
            GO.locationManager?.stopLocationUpdates()
        }
    }

    /**
     * Сброс ссылок на MapObject ([myLocationCross], [myLocationAccuracy]). Они привязаны
     * к старому [com.yandex.mapkit.mapview.MapView], при пересоздании view (поворот) будут
     * созданы заново в [updateMyLocationOnMap].
     */
    override fun onDestroyView() {
        // Сброс ссылок на MapObject — они привязаны к старому MapView и будут пересозданы
        myLocationCross = null
        myLocationAccuracy = null
        super.onDestroyView()
    }

    /**
     * Снимает [mapInputListener] с карты и убирает pending Runnable для скрытия hint-popup.
     * Без этого после уничтожения фрагмента остались бы висящие callback-и → утечки.
     */
    override fun onDestroy() {
        mapInputListener?.let {
            GO.map?.removeInputListener(it)
        }
        if (::hintView.isInitialized) {
            hideHintRunnable?.let { hintView.removeCallbacks(it) }
        }
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.map_layout, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hintView = TextView(requireContext()).apply {
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundResource(R.drawable.hint_background)
            textSize = 12f
            setTextColor(Color.BLACK)
            visibility = View.GONE
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        mapViewPlan = view.findViewById<MapView>(R.id.mapview)

        val rootLayout = view.findViewById<ViewGroup>(R.id.map_Layout)
        rootLayout.addView(
            hintView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        /*
        mapViewPlan.addView(
            hintView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )*/

        GO.buttonSaveTrack = view.findViewById(R.id.buttonSaveTrack)
        GO.buttonSaveTrack.setOnClickListener {
            val sdf = SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.getDefault())
            val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.HALF_UP }
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            val bluzDir = File("$documentsDir/BluZ")
            if (!bluzDir.exists()) {
                bluzDir.mkdirs()
            }
            when (GO.saveTrackType) {
                0 -> {
                    if (GO.curretnTrcName.isNotEmpty()) {
                        val fileKML = File("$documentsDir/BluZ/${GO.curretnTrcName}.kml")
                        if (fileKML.exists()) {
                            fileKML.delete()
                        }
                        Log.i("BluZ-BT", "File: $fileKML")
                        try {
                            if (fileKML.createNewFile()) {
                                Log.d("BluZ-BT", "File create Ok.")
                                val outputStream = FileOutputStream(fileKML)
                                var kmlTmp = GO.mainContext.resources.openRawResource(R.raw.track_header)
                                    .bufferedReader().use { it.readText() }
                                val kmlHdr = kmlTmp.replace("____NAME____", GO.curretnTrcName)
                                    .replace("____DECRIPTION____", "BluZ KML")
                                outputStream.write(kmlHdr.toByteArray())

                                kmlTmp = GO.mainContext.resources.openRawResource(R.raw.track_style)
                                    .bufferedReader().use { it.readText() }
                                var kmlStl = ""
                                for (iclr in 0..31) {
                                    kmlStl = kmlTmp.replace("____ID____", "Stl$iclr")
                                        .replace("____COLOR____", GO.radClrsKml[iclr])
                                    outputStream.write(kmlStl.toByteArray())
                                }
                                kmlStl = kmlTmp.replace("____ID____", "blackStyle")
                                    .replace("____COLOR____", "7f000000")
                                outputStream.write(kmlStl.toByteArray())
                                kmlTmp = GO.mainContext.resources.openRawResource(R.raw.track_point)
                                    .bufferedReader().use { it.readText() }

                                lifecycleScope.launch {
                                    val maxMin = GO.dao.getMaxMinForTrack(GO.currentTrack4Show)
                                    val mn = maxMin!!.min
                                    val mx = maxMin.max
                                    val dlt = mx - mn
                                    var kfc = 1.0
                                    if (dlt > 0) {
                                        kfc = 31.0 / dlt
                                    }
                                    Log.d("BluZ-BT", "Max: $mx, Min: $mn, Koef: $kfc")
                                    val trcDet = GO.dao.getPointsForTrack(GO.currentTrack4Show)
                                    if (trcDet.isNotEmpty()) {
                                        for (detLoc in trcDet) {
                                            if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                                                val styleStr = if (detLoc.cps >= 0) {
                                                    "#Stl" + (kfc * (detLoc.cps - mn)).toInt()
                                                } else {
                                                    "#blackStyle"
                                                }
                                                val uK = when (GO.unitsMess) {
                                                    1 -> 0.01f
                                                    else -> 1.0f
                                                }
                                                val kmlPnt = kmlTmp.replace(
                                                    "____POINT____",
                                                    "CPS:" + df.format(detLoc.cps) + " / " + df.format(
                                                        detLoc.cps * GO.propCPS2UR * uK
                                                    ) + when (GO.unitsMess) {
                                                        1 -> "uSv/h"
                                                        else -> "uR/h"
                                                    }
                                                )
                                                    .replace("____STR1____", "Time:" + sdf.format(Date(detLoc.timestamp * 1000)))
                                                    .replace("____STR2____", "Speed:" + df.format(detLoc.speed * 3.6f) + " km/h")
                                                    .replace("____STR3____", "Altit:" + df.format(detLoc.altitude))
                                                    .replace("____STR4____", "Accur:" + df.format(detLoc.accuracy))
                                                    .replace("____STR5____", "Magn:" + df.format(detLoc.magnitude))
                                                    .replace(
                                                        "____LOC____",
                                                        rectPolyGen(detLoc.latitude, detLoc.longitude, detLoc.accuracy.toDouble(), detLoc.altitude)
                                                    )
                                                    .replace("____STYLE____", styleStr)
                                                outputStream.write(kmlPnt.toByteArray())
                                            }
                                        }
                                        kmlTmp = GO.mainContext.resources.openRawResource(R.raw.track_footer)
                                            .bufferedReader().use { it.readText() }
                                        outputStream.write(kmlTmp.toByteArray())
                                        outputStream.close()
                                        Toast.makeText(context, "Save complete to: %s".format(fileKML), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            Toast.makeText(context, "Error save file. ${e.toString()}", Toast.LENGTH_SHORT).show()
                            Log.e("BluZ-BT", "Error: ", e)
                        }
                    } else {
                        Toast.makeText(context, "Track not selected.", Toast.LENGTH_SHORT).show()
                    }
                }
                1 -> {
                    if (GO.curretnTrcName.isNotEmpty()) {
                        val fileCSV = File("$documentsDir/BluZ/${GO.curretnTrcName}.csv")
                        if (fileCSV.exists()) {
                            fileCSV.delete()
                        }
                        Log.i("BluZ-BT", "File: $fileCSV")
                        try {
                            if (fileCSV.createNewFile()) {
                                Log.d("BluZ-BT", "File create Ok.")
                                val outputStream = FileOutputStream(fileCSV)
                                var csvTmp = GO.mainContext.resources.openRawResource(R.raw.track_header_csv)
                                    .bufferedReader().use { it.readText() }
                                outputStream.write(csvTmp.toByteArray())
                                csvTmp = GO.mainContext.resources.openRawResource(R.raw.track_detale_csv)
                                    .bufferedReader().use { it.readText() }
                                lifecycleScope.launch {
                                    val trcDet = GO.dao.getPointsForTrack(GO.currentTrack4Show)
                                    if (trcDet.isNotEmpty()) {
                                        for (detLoc in trcDet) {
                                            if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                                                val csvPnt = csvTmp
                                                    .replace("___TIME___", detLoc.timestamp.toString())
                                                    .replace("___DOSE___", df.format(detLoc.cps * GO.propCPS2UR))
                                                    .replace("___LAT___", detLoc.latitude.toString())
                                                    .replace("___LNG___", detLoc.longitude.toString())
                                                    .replace("___ALT___", df.format(detLoc.altitude))
                                                    .replace("___SPEED___", df.format(detLoc.speed * 3.6f))
                                                    .replace("___CPS___", df.format(detLoc.cps))
                                                    .replace("___ACCUR___", df.format(detLoc.accuracy))
                                                    .replace("___MAGN___", df.format(detLoc.magnitude))
                                                outputStream.write(csvPnt.toByteArray())
                                            }
                                        }
                                        outputStream.close()
                                        Toast.makeText(context, "Save complete to: %s".format(fileCSV), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Track %s is empty. ${GO.curretnTrcName}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            Toast.makeText(context, "Error save file. ${e.toString()}", Toast.LENGTH_SHORT).show()
                            Log.e("BluZ-BT", "Error: ", e)
                        }
                    } else {
                        Toast.makeText(context, "Track not selected.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        when (GO.saveTrackType) {
            0 -> GO.buttonSaveTrack.text = resources.getString(R.string.textKML)
            1 -> GO.buttonSaveTrack.text = resources.getString(R.string.textGPX)
        }

        GO.recordTrc = view.findViewById(R.id.buttonRecord)
        if (GO.trackIsRecordeed) {
            GO.recordTrc.text = getString(R.string.stopRec)
            GO.recordTrc.setTextColor(Color.RED)
            GO.locationManager?.startLocationUpdates()
            GO.currentTrack4Show = GO.currentTrck
            lifecycleScope.launch {
                val tmpTrc = GO.dao.getCurrentTrack()
                if (tmpTrc != null) {
                    GO.currentTrck = tmpTrc
                } else {
                    GO.currentTrck = GO.dao.getFirstTrack()
                    GO.dao.activateTrack(GO.currentTrck)
                }
                GO.curretnTrcName = GO.dao.getSelectTrack(GO.currentTrck)
                cps2urh = GO.dao.getCPS2URH(GO.currentTrck)
                GO.currentTrackName.text = getString(R.string.current_track_label, GO.curretnTrcName)
                Log.d("BluZ-BT", "Track_id: ${GO.currentTrck}, Track_name: ${GO.curretnTrcName}")
                redrawtMap(GO.currentTrck)
            }
        }

        GO.recordTrc.setOnClickListener {
            if (GO.trackIsRecordeed) {
                GO.recordTrc.text = getString(R.string.startRec)
                GO.recordTrc.setTextColor(
                    resources.getColor(R.color.buttonTextColor, GO.mainContext.theme)
                )
                GO.trackIsRecordeed = false
                GO.locationManager?.stopLocationUpdates()
            } else {
                GO.recordTrc.text = getString(R.string.stopRec)
                GO.recordTrc.setTextColor(Color.RED)
                GO.trackIsRecordeed = true
                GO.locationManager?.startLocationUpdates()
                GO.currentTrck = GO.currentTrack4Show
                lifecycleScope.launch {
                    GO.dao.deactivateAllTracks()
                    GO.dao.activateTrack(GO.currentTrack4Show)
                }
            }
        }

        GO.currentTrackName = view.findViewById(R.id.textCurrentTrack)
        GO.currentTrackName.setOnClickListener {
            if (GO.curretnTrcName.isNotEmpty()) {
                val context = requireContext()
                val input = EditText(context)
                input.setText(GO.curretnTrcName)
                AlertDialog.Builder(context)
                    .setTitle("Edit track")
                    .setView(input)
                    .setPositiveButton("Save") { dialog, _ ->
                        val name = input.text.toString().trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        lifecycleScope.launch {
                            try {
                                GO.dao.editTrack(GO.currentTrack4Show, name)
                                GO.curretnTrcName = name
                                GO.currentTrackName.text = getString(R.string.current_track_label, name)
                            } catch (e: Exception) {
                                Log.e("TrackDialog", "Error edit track", e)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }
                    .setNeutralButton("Delete") { _, _ ->
                        if (GO.currentTrack4Show > 1) {
                            if (GO.trackIsRecordeed) {
                                GO.recordTrc.text = getString(R.string.startRec)
                                GO.recordTrc.setTextColor(
                                    resources.getColor(R.color.buttonTextColor, GO.mainContext.theme)
                                )
                                GO.trackIsRecordeed = false
                                GO.locationManager?.stopLocationUpdates()
                            }
                            lifecycleScope.launch {
                                try {
                                    GO.dao.deleteTrack(GO.currentTrack4Show)
                                    GO.currentTrack4Show = 0
                                    GO.curretnTrcName = ""
                                    GO.currentTrackName.text = getString(R.string.current_track_label, "")
                                } catch (e: Exception) {
                                    Log.e("TrackDialog", "Error delete track", e)
                                }
                            }
                            GO.map?.mapObjects?.clear()
                            resetMyLocationMarkers()
                        }
                    }
                    .show()
            }
        }

        val btnNewTrack: Button = view.findViewById(R.id.buttonNewTrack)
        btnNewTrack.setOnClickListener {
            val context = requireContext()
            val input = EditText(context)
            input.hint = "Enter Track name"
            AlertDialog.Builder(context)
                .setTitle("New track")
                .setView(input)
                .setPositiveButton("Create") { dialog, _ ->
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    GO.map?.mapObjects?.clear()
                    resetMyLocationMarkers()
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
                            GO.currentTrackName.text = getString(R.string.current_track_label, name)
                            if (!GO.trackIsRecordeed) {
                                GO.dao.deactivateAllTracks()
                                GO.dao.activateTrack(GO.currentTrack4Show)
                                GO.currentTrck = GO.currentTrack4Show
                            }
                        } catch (e: Exception) {
                            Log.e("TrackDialog", "Error create track", e)
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }
                .show()
        }

        val btnTrackList: Button = view.findViewById(R.id.buttonTracks)
        btnTrackList.setOnClickListener {
            lifecycleScope.launch {
                val tracks = GO.dao.getAllTracks()
                val sdf = SimpleDateFormat("[dd.MM.yy HH:mm:ss] ", Locale.getDefault())
                val names = tracks.map { sdf.format(Date(it.createdAt * 1000)) + it.name }.toTypedArray()
                val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.item_track_dialog, names) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getView(position, convertView, parent)
                        val textView = v.findViewById<TextView>(R.id.textView)
                        val fullText = names[position]
                        val dateLength = 20
                        if (fullText.length >= dateLength) {
                            val spannable = SpannableString(fullText)
                            val dateColor = ContextCompat.getColor(requireContext(), R.color.labelTextColor2)
                            spannable.setSpan(ForegroundColorSpan(dateColor), 0, dateLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            textView.text = spannable
                        } else {
                            textView.text = fullText
                        }
                        return v
                    }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Select track")
                    .setAdapter(adapter) { _, which ->
                        GO.map?.mapObjects?.clear()
                        resetMyLocationMarkers()
                        val selectedTrack = tracks[which]
                        GO.curretnTrcName = selectedTrack.name
                        GO.currentTrackName.text = getString(R.string.current_track_label, selectedTrack.name)
                        GO.currentTrack4Show = selectedTrack.id
                        redrawtMap(GO.currentTrack4Show)
                        if (GO.trackIsRecordeed) {
                            GO.recordTrc.text = getString(R.string.startRec)
                            GO.recordTrc.setTextColor(
                                resources.getColor(R.color.buttonTextColor, GO.mainContext.theme)
                            )
                            GO.trackIsRecordeed = false
                            GO.locationManager?.stopLocationUpdates()
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }

        val btnAddPoint: ImageButton = view.findViewById(R.id.buttonAddPoint)
        btnAddPoint.setOnClickListener { }

        val btnMapLocate: ImageButton = view.findViewById(R.id.buttonMapLocate)
        btnMapLocate.setOnClickListener {
            try {
                if (GO.lastPointLoc.latitude != 0.0 && GO.lastPointLoc.longitude != 0.0) {
                    GO.map?.move(
                        CameraPosition(
                            Point(GO.lastPointLoc.latitude, GO.lastPointLoc.longitude),
                            15.0f, 0.0f, 0.0f
                        )
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "GPS write error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        GO.createRainbowColors()
        val lm = GO.mainContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        GO.mapView = view.findViewById(R.id.mapview)
        GO.mapWindow = GO.mapView.mapWindow
        GO.map = GO.mapWindow?.map
        GO.map?.set2DMode(true)
        GO.map?.isNightModeEnabled = !ThemePrefs.isDayTheme(requireContext())

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("BluZ-BT", "Provider is disabled.")
            return
        } else {
            Log.d("BluZ-BT", "Provider is enabled.")
        }

        // Создаём locationManager всегда — он нужен и для отображения текущей позиции,
        // и для записи трека. Запуск/остановка обновлений — в onStart/onStop.
        GO.locationManager = ContinuousLocationManager(this) { location ->
            if (isAdded) {
                activity?.runOnUiThread {
                    GO.lastPointLoc = location
                    updateMyLocationOnMap(location)
                    val cpsAveredge: Float = GO.cpsAVG / GO.cpsIntervalCount
                    Log.i(
                        "BluZ-BT",
                        "Lat: ${location.latitude}, Lng: ${location.longitude}, Accuracy: ${location.accuracy}, cps:${cpsAveredge}, CT: ${GO.currentTrck}"
                    )
                }
            }
        }
        // Стартуем обновления сразу (onStart мог отработать до создания locationManager)
        GO.locationManager?.startLocationUpdates()

        val lstLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lstLoc != null) {
            GO.lastPointLoc = lstLoc
            updateMyLocationOnMap(lstLoc)
        } else {
            getLocationModern()
        }
    }

    /** Конвертирует dp в пиксели через текущую плотность экрана. */
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    /**
     * Генерирует KML-полигон из 3 точек, описывающий круг точности GPS вокруг центра
     * (lat, lon) с заданным радиусом в метрах.
     *
     * Точки расставлены через 120° (треугольник). Используется при экспорте трека в KML —
     * для каждой точки рисуется такой треугольник, чтобы пользователь видел погрешность.
     *
     * Формула пересчёта метры → градусы: `latOffset = radius·cos(θ) / 111111`,
     * `lonOffset = radius·sin(θ) / (111111·cos(lat))` (поправка на широту).
     */
    private fun rectPolyGen(lat: Double, lon: Double, radius: Double, altit: Double): String {
        val coords = List(3) { i ->
            val angleDeg = 120.0 * i
            val angleRad = toRadians(angleDeg)
            val latOffset = (radius * cos(angleRad)) / 111111.0
            val lonOffset = (radius * sin(angleRad)) / (111111.0 * cos(toRadians(lat)))
            val newLat = lat + latOffset
            val newLon = lon + lonOffset
            String.format(Locale.US, "%.6f,%.6f,%.6f", newLon, newLat, altit)
        }
        return coords.joinToString("\n")
    }

    /**
     * Привязывает [com.yandex.mapkit.map.InputListener] к карте.
     *
     * **onMapTap:** ищет ближайшую точку трека к месту тапа через [ContinuousLocationManager.haversineDistance].
     * Если точка ближе 30 метров — показывает popup `map_hint.xml` с её данными
     * (CPS, скорость, высота, время, accuracy, magnetic).
     *
     * **onMapLongTap:** не используется в текущей версии.
     */
    private fun setupMapClickListener() {
        mapInputListener?.let { GO.map?.removeInputListener(it) }
        mapInputListener = object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                hintView.visibility = View.GONE
                hideHintRunnable?.let { hintView.removeCallbacks(it) }
                val clickLat = point.latitude
                val clickLon = point.longitude
                var closestPoint: TrackDetail? = null
                var minDistance = Double.MAX_VALUE

                for (trackPoint in allTrackPoints) {
                    val dLat = Math.toRadians(trackPoint.latitude - clickLat)
                    val dLon = Math.toRadians(trackPoint.longitude - clickLon)
                    val a = sin(dLat / 2) * sin(dLat / 2) +
                            cos(Math.toRadians(clickLat)) * cos(Math.toRadians(trackPoint.latitude)) *
                            sin(dLon / 2) * sin(dLon / 2)
                    val distance = 6371000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
                    if (distance < minDistance && distance <= 50.0) {
                        minDistance = distance
                        closestPoint = trackPoint
                    }
                }

                if (closestPoint != null) {
                    val timeStr = SimpleDateFormat("HH:mm:ss\ndd.MM.yyyy", Locale.getDefault())
                        .format(Date(closestPoint.timestamp * 1000))
                    if (cps2urh == null || cps2urh == 0f) {
                        cps2urh = GO.propCPS2UR
                    }
                    val cpsStr: String = if (closestPoint.cps < 0) {
                        "CPS: N/A\n"
                    } else {
                        when (GO.unitsMess) {
                            1 -> "CPS: ${"%.2f / %.3f".format(closestPoint.cps, closestPoint.cps * cps2urh!! * 0.01f)}uSv/h\n"
                            else -> "CPS: ${"%.2f / %.2f".format(closestPoint.cps, closestPoint.cps * cps2urh!!)}uR/h\n"
                        }
                    }
                    hintView.text = buildString {
                        append(cpsStr)
                        append("Speed: ${"%.1f".format(closestPoint.speed * 3.6)} km/h\n")
                        append("Altitude: ${"%.1f".format(closestPoint.altitude)} m\n")
                        append("Magnitude: ${"%.1f".format(closestPoint.magnitude)} uT\n")
                        append("Accuracy: ${"%.1f".format(closestPoint.accuracy)} m\n")
                        append("Time: $timeStr")
                    }


                    hintView.post {
                        val params = hintView.layoutParams as ViewGroup.MarginLayoutParams
                        params.setMargins(10, 50, 0, 0)
                        hintView.layoutParams = params
                        hintView.visibility = View.VISIBLE
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

    /**
     * Перерисовывает все точки выбранного трека на карте.
     *
     *  1. `dao.getMaxMinForTrack(trackId)` — диапазон CPS для нормализации цветов
     *  2. `dao.getPointsForTrack(trackId)` — все точки
     *  3. Для каждой — выбор иконки из `GO.impArr[32]` (цветовой градиент по CPS)
     *  4. `map.mapObjects.addPlacemark` с этой иконкой
     *  5. Расчёт bounding box → `map.move(CameraPosition)` с зумом через [ContinuousLocationManager.calculateZoomLevel]
     *
     * @param trackIdPriv ID трека для отрисовки (обычно [globalObj.currentTrack4Show]).
     */
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
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLon = Double.MAX_VALUE
                var maxLon = -Double.MAX_VALUE

                for (detLoc in trcDet) {
                    if (detLoc.latitude != 0.0 && detLoc.longitude != 0.0) {
                        val imp = if (detLoc.cps > 0) {
                            GO.impArr[(kfc * (detLoc.cps - mn)).toInt().coerceIn(0, 31)]
                        } else {
                            GO.impBLACK
                        }
                        GO.map?.mapObjects?.addPlacemark()?.apply {
                            geometry = Point(detLoc.latitude, detLoc.longitude)
                            setIcon(imp!!)
                        }
                        minLat = minOf(minLat, detLoc.latitude)
                        maxLat = maxOf(maxLat, detLoc.latitude)
                        minLon = minOf(minLon, detLoc.longitude)
                        maxLon = maxOf(maxLon, detLoc.longitude)
                    }
                }

                if (minLat == Double.MAX_VALUE) return@launch

                if (minLat == maxLat && minLon == maxLon) {
                    GO.map?.move(
                        CameraPosition(Point(minLat, minLon), 15f, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                } else {
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
                allTrackPoints = trcDet
                setupMapClickListener()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    /**
     * Получает локацию через `FusedLocationProviderClient.lastLocation`. Если результат
     * старше 5 минут — fallback на [requestFreshLocationModern] (свежий fix с таймаутом 10 сек).
     *
     * Используется при первой инициализации карты.
     */
    private fun getLocationModern() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && location.time > System.currentTimeMillis() - 5 * 60 * 1000) {
                GO.lastPointLoc = location
                updateMyLocationOnMap(location)
            } else {
                requestFreshLocationModern(fusedClient)
            }
        }
    }

    /** Рисует bitmap красного креста для маркера текущего местоположения. */
    /**
     * Рисует Bitmap 40×40 px с красным крестом (вертикальная + горизонтальная линии,
     * толщина 2 px, antialiased) и оборачивает в [com.yandex.runtime.image.ImageProvider]
     * через `ImageProvider.fromBitmap`.
     *
     * Lazy-initialized — создаётся при первом обращении к [myLocationCrossImage].
     */
    private fun createMyLocationCrossImage(): ImageProvider {
        val size = 40
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            isAntiAlias = true
        }
        val c = size / 2f
        canvas.drawLine(c, 0f, c, size.toFloat(), paint)
        canvas.drawLine(0f, c, size.toFloat(), c, paint)
        return ImageProvider.fromBitmap(bmp)
    }

    /** Обновляет маркер текущего местоположения на карте. Безопасно вызывать многократно. */
    /**
     * Создаёт или обновляет маркер «моё место» (красный крест + круг точности) на карте.
     *
     * **Защита от уничтоженных объектов** — если `myLocationCross.isValid == false`
     * (после `mapObjects.clear()`), обнуляет ссылку и пересоздаёт. Без этой проверки
     * `cross.geometry = point` падал бы с native crash `weak_ptr expired`.
     *
     * Круг точности рисуется только если `location.hasAccuracy() && accuracy > 0f` —
     * `CircleMapObject` с `fillColor = 0x40808080` (серый 25% alpha), без обводки.
     */
    private fun updateMyLocationOnMap(location: Location) {
        val map = GO.map ?: return
        val point = Point(location.latitude, location.longitude)

        // Если предыдущий маркер был уничтожен через mapObjects.clear() — нативный
        // weak_ptr истёк, isValid вернёт false. Обнуляем ссылку и пересоздадим заново.
        if (myLocationCross?.isValid == false) myLocationCross = null
        if (myLocationAccuracy?.isValid == false) myLocationAccuracy = null

        val cross = myLocationCross
        if (cross == null) {
            myLocationCross = map.mapObjects.addPlacemark(point, myLocationCrossImage)
        } else {
            cross.geometry = point
        }

        if (location.hasAccuracy() && location.accuracy > 0f) {
            val circle = Circle(point, location.accuracy)
            val acc = myLocationAccuracy
            if (acc == null) {
                myLocationAccuracy = map.mapObjects.addCircle(circle).apply {
                    strokeColor = Color.TRANSPARENT
                    strokeWidth = 0f
                    fillColor = 0x40808080.toInt()  // серый 25% alpha
                }
            } else {
                acc.geometry = circle
            }
        }
    }

    /** Сбрасывает ссылки на маркер местоположения. Вызывать после mapObjects.clear(). */
    /**
     * Обнуляет ссылки на [myLocationCross] и [myLocationAccuracy]. Вызывать ОБЯЗАТЕЛЬНО
     * после `GO.map.mapObjects.clear()` (которое уничтожает все MapObject) — иначе
     * следующий [updateMyLocationOnMap] упадёт с native crash.
     *
     * Вызывается в трёх местах: при создании, удалении и выборе трека.
     */
    private fun resetMyLocationMarkers() {
        myLocationCross = null
        myLocationAccuracy = null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    /**
     * Запрашивает свежий fix через `requestLocationUpdates` с `PRIORITY_HIGH_ACCURACY`
     * и `maxUpdates=1`. Получив первое значение — снимает listener и обновляет
     * `GO.lastPointLoc` + маркер на карте.
     */
    private fun requestFreshLocationModern(client: FusedLocationProviderClient) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMinUpdateIntervalMillis(0)
            .setMaxUpdateDelayMillis(0)
            .setMaxUpdates(1)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    GO.lastPointLoc = loc
                    updateMyLocationOnMap(loc)
                }
                client.removeLocationUpdates(this)
            }
        }
        client.requestLocationUpdates(request, callback, null)
    }
}
