package ru.starline.bluz

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import android.content.res.ColorStateList
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail
import ru.starline.bluz.utils.await
import kotlin.math.round
import kotlin.system.exitProcess
import androidx.core.content.edit
import android.view.ViewGroup.LayoutParams
import java.nio.ByteBuffer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import ru.starline.bluz.GO

public val GO: globalObj = globalObj()


//public lateinit var mainContext: Context
public var PI: Int = 0

/**
 * Главная Activity приложения — центральный класс UI-слоя.
 *
 * **Базовый класс:** [FragmentActivity], НЕ [androidx.appcompat.app.AppCompatActivity].
 * Это требование переключения день/ночь темы через [ThemePrefs.wrapContextWithTheme] в
 * [attachBaseContext] — собственный механизм вместо `AppCompatDelegate.setDefaultNightMode`,
 * который работает только с `AppCompatActivity`.
 *
 * **Ответственность:**
 *  - Инициализация и проверка разрешений (BLE, location, notifications)
 *  - Хостинг [androidx.viewpager2.widget.ViewPager2] с фрагментами через [NumberAdapter]
 *  - Подписка на [BluetoothInterface.deviceFrames] и [BluetoothInterface.status] —
 *    рендеринг StatusStrip, dose-readouts, спектра, дозиметра, лога
 *  - Кастомная навигация (5 табов: Spectrum / Dose / Map / Settings / Exit) через
 *    [setupNavigation]
 *  - Запись точек трека ([recordTrackPoint]) при активной записи
 *  - Запуск фонового сервиса ([BleMonitoringService]) при выходе с активной записью
 *    через [performExit]
 *
 * **Жизненный цикл:**
 *  1. [attachBaseContext] оборачивает Context с принудительным `uiMode` темы
 *  2. [onCreate] — полная инициализация (см. документ DEVELOPER_GUIDE.md раздел 4.1)
 *  3. [onResume] — повторное применение темы, проверка фонового сервиса
 *
 * **Подводный камень.** Все вызовы `ContextCompat.getColor(...)` внутри [applyFrameToUi]
 * и [applyDoseReadouts] должны использовать `this` (Activity-контекст с темой),
 * а НЕ `GO.mainContext` (Application-контекст без переопределения uiMode).
 */
public class MainActivity : FragmentActivity() {

    /**
     * Оборачивает Context через [ThemePrefs.wrapContextWithTheme] — принудительно
     * выставляет `uiMode` в Configuration ПЕРЕД созданием Activity. Без этого тёмная/светлая
     * тема не подключила бы корректные ресурсы из `values-night/`.
     */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(ThemePrefs.wrapContextWithTheme(newBase))
    }

    /**
     * Применяет цвета системных баров (status bar / navigation bar) согласно текущей теме.
     * Цвет — `bz_bg` (белый в светлой / тёмно-синий в тёмной). Также управляет
     * appearance-флагами (`isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars`)
     * для корректного контраста иконок системы.
     *
     * Вызывается из [onResume] и из `OnApplyWindowInsetsListener`.
     */
    private fun applySystemBarsForTheme() {
        val dayTheme = ThemePrefs.isDayTheme(this)
        val bg = ContextCompat.getColor(this, R.color.bz_bg)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = dayTheme
            isAppearanceLightNavigationBars = dayTheme
        }
    }


    val deviceViewModel: DeviceViewModel by viewModels()
    private lateinit var orientationListener: OrientationEventListener
    private var isReversed = false

    /* Отслеживание нажатия на кнопки громкости */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                true // Сообщение для системы, что бы отключить индикатор громкости
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Обработка события "+"
                Toast.makeText(this, "Кнопка +", Toast.LENGTH_SHORT).show()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Обработка события "-"
                Toast.makeText(this, "Кнопка -", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    /* Переход в паузу*/
    override fun onPause() {
        super.onPause()
        orientationListener.disable() // Освобождаем ресурсы
    }


    /* Восстановление приложения */
    override fun onResume() {
        super.onResume()
        applySystemBarsForTheme()
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            applySystemBarsForTheme()
            insets
        }
        // Проверяем разрешения при возврате из настроек
        if (hasPendingPermissionCheck) {
            hasPendingPermissionCheck = false
            checkAndRequestPermissions()
        }
        /* Проверяем запущен севис или нет */
        if (checkSerice()) {
            GO.drawObjectInit = true
            GO.drawDozObjectInit = true
            Log.d("BluZ-BT", "Service is real running")
            GO.drawLOG.appendAppLogs("Service running.", 4)
            stopService(Intent(this, BleMonitoringService::class.java))
            getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {
                putBoolean("is_ble_service_running", false)}
        } else {
            Log.d("BluZ-BT", "Service not running")
            GO.drawLOG.appendAppLogs("Service not running.", 4)
        }
        /* Переключение ориентации экрана */
        //orientationListener.enable()
    }

    /**
     * Проверяет, реально ли запущен [BleMonitoringService] (двойная проверка:
     * флаг в SharedPreferences `app_state` + `ActivityManager.getRunningServices`).
     *
     * @return `true` если сервис активен — приложение должно его остановить при возврате
     *  в foreground (`onResume`), чтобы вернуть управление UI-слою.
     */
    private fun checkSerice() : Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val prefs = getSharedPreferences("app_state", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_ble_service_running", false)) {
            for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
                if (BleMonitoringService::class.java.name == service.service.className) {
                    return true
                }
            }
        }
        return  false
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("BluZ-BT", "Получен результат запроса разрешений: $permissions")

        // Проверяем, все ли разрешения получены
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Log.d("BluZ-BT", "All permission granted!")
            GO.drawLOG.appendAppLogs("Permission granted.", 3)
            GO.allPermissionAccept = true
            GO.startBluetoothTimer()
            // initApplication()
        } else {
            // Проверяем, нужно ли показать объяснение или отправить в настройки
            if (shouldShowRequestPermissionRationale(permissions.keys.toTypedArray())) {
                // Показываем диалог с объяснением
                showPermissionExplanationDialog(permissions.keys.toTypedArray())
            } else {
                // Разрешения отклонены навсегда - отправляем в настройки
                showSettingsDialog()
            }
        }
    }

    /*
    override fun onStart() {
        super.onStart()
        GO.drawSPEC.init()
        GO.drawSPEC.clearSpecter()
    }*/


    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarsForTheme()
        if (savedInstanceState != null) {
            GO.drawLOG.appendAppLogs("Second create !", 0)
        }
        GO.mainContext = applicationContext
        val mainLayout = findViewById<View>(R.id.main)
        /* Использование всего экрана, место занятое челкой, тоже используется. */
        //enableEdgeToEdge()
        //enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color. TRANSPARENT, Color. TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim))

        /* Phase D2: system bars visible. fitsSystemWindows on root inserts padding under status/nav bars. */
        WindowInsetsControllerCompat(window, window.decorView.findViewById(android.R.id.content)).show(WindowInsetsCompat.Type.systemBars())
        GO.adapter = NumberAdapter(this)
        GO.indicatorBT = findViewById(R.id.indicatorBT)
        GO.viewPager = findViewById(R.id.VPMain)
        GO.viewPager.isUserInputEnabled = false             // Отключение прокрутки viewPager2
        GO.viewPager.adapter = GO.adapter
        GO.txtStat1 = findViewById(R.id.textStatistic1)
        GO.txtCompMED = findViewById(R.id.textCOMPMED)

        // Phase B StatusStrip — discrete value views
        GO.bzStatusStrip = findViewById(R.id.statusStrip)
        GO.bzCpsValue = findViewById(R.id.bzCpsValue)
        GO.bzClockValue = findViewById(R.id.bzClockValue)
        GO.bzTempValue = findViewById(R.id.bzTempValue)
        GO.bzBattValue = findViewById(R.id.bzBattValue)
        GO.bzBtDot = findViewById(R.id.bzBtDot)
        GO.bzBtIcon = findViewById(R.id.bzBtIcon)
        GO.bzBtRssi = findViewById(R.id.bzBtRssi)

        /* Обработка сенсора ориентации */
        orientationListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return

                // Определяем, в каком ландшафтном положении устройство
                val shouldReverse = when {
                    orientation in 45..134 -> true   // обычный landscape (левый торец внизу)
                    orientation in 225..314 -> false   // landscape reversed (правый торец внизу)
                    else -> return // игнорируем портрет и промежуточные углы
                }

                if (shouldReverse != isReversed) {
                    isReversed = shouldReverse
                    mainLayout.rotation = if (shouldReverse) 180f else 0f
                    Log.i("BluZ-BT", "Landscape reversed: $shouldReverse")
                }
            }
        }


        GO.txtStat2 = findViewById(R.id.textStatistic2)
        GO.txtStat3 = findViewById(R.id.textStatistic3)
        // textIsotopInfo биндится в SpectrumFragment (живёт внутри chart card)
        restoreStatusStripFromState()
        GO.drawLOG.appendAppLogs("Start app pid=${android.os.Process.myPid()} uid=${android.os.Process.myUid()}", 1)
        lifecycleScope.launch {
            val dao = GO.dao
            /* Создание первого трека */
            if (dao.getActiveTracks().isEmpty()) {
                dao.insertTrack(Track(0, "Default track", System.currentTimeMillis() / 1000))
            }

            /* Получим текущий трек для начала записи. */
            var tmpTrack = dao.getCurrentTrack()
            if (tmpTrack != null) {
                GO.currentTrck = tmpTrack
            } else {
                tmpTrack = dao.getFirstTrack()
                GO.dao.activateTrack(tmpTrack)
                GO.currentTrck = tmpTrack
            }
            Log.i("BluZ-BT", "Current track ID: ${GO.currentTrck}")
        }
        /* Янидекс карта */
        //MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        //MapKitFactory.initialize(this)
        /* Создание массива разноцветных поинтов для отображения на карте */
        val size = 32 // размер иконки в пикселях
        for (ind in 0..31) {
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)

            // Создаём кисть с нужным цветом
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = GO.radClrs[ind]
                style = Paint.Style.FILL
            }

            // Рисуем круг в центре битмапа
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            GO.impArr[ind] = ImageProvider.fromBitmap(bitmap)
        }
        /*
        val drawable = ContextCompat.getDrawable(GO.mainContext, R.drawable.ic_gps_point)!!.mutate()
        var bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        var canvas = Canvas(bitmap)
        for (ind in 0..31) {
            DrawableCompat.setTint(drawable, GO.radClrs[ind])
            bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            GO.impArr[ind] = ImageProvider.fromBitmap(bitmap)
        }
        */
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Рисуем круг в центре битмапа
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        GO.impBLACK = ImageProvider.fromBitmap(bitmap)
        /*
        DrawableCompat.setTint(drawable, Color.BLACK)
        bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        GO.impBLACK = ImageProvider.fromBitmap(bitmap)
        */
        /*
        *   Тексты форматов для сохранения
        */
        GO.saveSpecterType1 = getResources().getString(R.string.textType1)
        GO.saveSpecterType2 = getResources().getString(R.string.textType2)

        /*
        *   Цвета для курсора
        */
        GO.ColorEraseCursor = resources.getColor(R.color.eraseColorCursor, GO.mainContext.theme)
        GO.ColorActiveCursor = resources.getColor(R.color.activeColorCursor, GO.mainContext.theme)

        /*
        *   Проверка и запрос разрешений.
        */
        checkAndRequestPermissions()
        /*
        * Основные кнопки
        */

        setupNavigation()

        /*
        *       Параметры приложения
        */
        GO.PP = propControl()
        GO.readConfigParameters()
        if (GO.fullScrn) {
            enableEdgeToEdge()
        }
        GO.enrgCalc = energyCalculator()
        GO.enrgCalc.init(GO.propCoef4096A, GO.propCoef4096B, GO.propCoef4096C, GO.propCoef4096D, GO.propCoef4096E, GO.spectrResolution)
        //val eLast = GO.enrgCalc.channelToEnergy(4095)
        //Log.d("BluZ-BT", "rS: ${GO.enrgCalc.rS}, eLast: $eLast")

        /* Тип детектора */
        lifecycleScope.launch {
            GO.dao.getByIdDetector(GO.currentDetector)?.let { detectorType ->
                val hasNaN = detectorType.chiVector.any { it.isNaN() }
                val hasInf = detectorType.chiVector.any { it.isInfinite() }
                if (hasNaN || hasInf) {
                    Log.i("BluZ-BT", "CHI vector is wrong.")
                } else {
                    GO.curretnDetectorName = detectorType.name
                    DoseCalculator.chiVectorOrg = detectorType.chiVector
                    GO.drawSPECTER.flagMLEM = true
                    Log.d("BluZ-BT", "Detector type Ok. ${DoseCalculator.chiVectorOrg}")
                }
            } ?: run {
                GO.curretnDetectorName = ""
                Log.d("BluZ-BT", "Detector type wrong.")
            }
        }

        //val activity = requireActivity()
        mainLayout.setPadding(GO.paddingLeft, mainLayout.paddingTop, GO.paddingRight, mainLayout.paddingBottom)
        /* Загрузка справочника изотопов */
        GO.loadIsotop()
        GO.startBluetoothTimer()
        deviceViewModel.observeBle(GO.BTT)

        /* Subscribe to BLE status and device data streams */
        observeBleStatus()
        observeDeviceFrames()
    }

    // --- BLE status observer ---

    /**
     * Подписывается на [BluetoothInterface.status] и обновляет цвет BT-индикатора
     * в StatusStrip:
     *  - Connected → зелёный (`bz_bt_on`)
     *  - Connecting → оранжевый (`bz_bt_warn`)
     *  - Disconnected / Error → красный (`bz_bt_off`)
     *
     * Корутина живёт пока живёт [lifecycleScope] Activity.
     */
    private fun observeBleStatus() {
        lifecycleScope.launch {
            GO.BTT.status.collect { status ->
                val tintRes = when (status) {
                    is BleStatus.Connected    -> R.color.bz_bt_on
                    is BleStatus.Connecting   -> R.color.bz_bt_warn
                    is BleStatus.Disconnected -> R.color.bz_bt_off
                    is BleStatus.InitBLE      -> R.color.bz_bt_init
                    is BleStatus.Error        -> R.color.bz_bt_off
                }
                val tint = ContextCompat.getColor(GO.mainContext, tintRes)
                GO.bzBtDot.backgroundTintList = ColorStateList.valueOf(tint)
                ImageViewCompat.setImageTintList(GO.bzBtIcon, ColorStateList.valueOf(tint))
                if (status !is BleStatus.Connected) {
                    // При отключении сбрасываем индикатор — иначе на экране висят старые «-78» с зелёным цветом.
                    GO.Current_RSSI = -400
                    applyBtRssi()
                }
            }
        }
    }

    /**
     * Обновляет TextView мощности сигнала [globalObj.bzBtRssi] в StatusStrip.
     *
     * Источник — [globalObj.Current_RSSI], который пополняется в `onReadRemoteRssi` (вызывается
     * на каждом успешно собранном BLE-фрейме). Значение `-400` — sentinel «нет данных»
     * (нет связи или RSSI ещё не прочитан), отображается прочерком.
     *
     * Цвет по уровню сигнала: меньше -95 dBm → красный, [-95..-75) → жёлтый,
     * ≥-75 → зелёный. Прочерк (нет данных) — приглушённым `bz_text_dim`.
     */
    fun applyBtRssi() {
        // bzBtRssi инициализируется в onCreate до подписки на BLE-status и до прихода фреймов,
        // поэтому к моменту любого вызова уже не null. Если поменяется порядок инициализации —
        // надо будет обернуть в isInitialized-проверку через сам globalObj (lateinit-property
        // reference недоступен извне класса).
        val rssi = GO.Current_RSSI
        val colorRes: Int
        val text: String
        if (rssi <= -400) {
            text = "—"
            colorRes = R.color.bz_text_dim
        } else {
            text = rssi.toString()
            colorRes = when {
                rssi < -95 -> R.color.bz_bt_off    // красный
                rssi < -75 -> R.color.bz_bt_warn   // жёлтый/оранжевый
                else       -> R.color.bz_bt_on     // зелёный
            }
        }
        GO.bzBtRssi.text = text
        GO.bzBtRssi.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    /**
     * Выставляет текст и цвет [globalObj.bzBattValue] в StatusStrip.
     *
     * Текст определяется флагом [globalObj.showBatteryPercent] через [globalObj.formatBattery].
     * Цвет по уровню напряжения (тот же диапазон что и у RSSI): ≥3.3 В → зелёный,
     * 3.0..3.3 В → жёлтый, <3.0 В → красный. До прихода первого фрейма (`battLevel <= 0`)
     * остаётся placeholder из layout.
     */
    fun applyBatteryDisplay(voltage: Float) {
        if (voltage <= 0f) return
        GO.bzBattValue.text = GO.formatBattery(voltage)
        val colorRes = when {
            voltage < 3.0f -> R.color.bz_bt_off    // красный
            voltage < 3.3f -> R.color.bz_bt_warn   // жёлтый
            else           -> R.color.bz_bt_on     // зелёный
        }
        GO.bzBattValue.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    // --- Device frame observer ---

    /**
     * Подписывается на [BluetoothInterface.deviceFrames]. На каждый собранный фрейм:
     *  1. [applyFrameToState] — копирует поля в [globalObj]
     *  2. [applyFrameToUi] — обновляет UI (StatusStrip, hero, спектр, дозиметр, лог)
     *
     * suspend collect не пропускает фреймы — фреймы приходят примерно раз в секунду.
     */
    private fun observeDeviceFrames() {
        lifecycleScope.launch {
            GO.BTT.deviceFrames.collect { frame ->
                applyFrameToState(frame)
                applyFrameToUi(frame)
            }
        }
    }

    /** Copy data from frame into the global state object (GO). */
    /**
     * Раскладывает поля [DeviceFrame] в [globalObj] (`GO.*`).
     *
     * **Что копируется:**
     *  - Базовые измерения: [GO.PCounter], [GO.pulsePerSec], [GO.cps], [GO.cpsAVG] (накапливается),
     *    [GO.messTm], [GO.tempMC], [GO.battLevel], [GO.overloadFlag]
     *  - `HardwareConfig` → все `GO.HW*` поля (LED/звук/вибро, пороги, коэффициенты полинома,
     *    ВВ, компаратор, время спектрометра)
     *  - Состояние спектрометра ([GO.specterRunning]) — `true` если frame содержит `spectrumData`
     *  - [GO.specterType] и [drawSpecter.ResolutionSpectr] / [drawHistory.ResolutionHistory]
     *    подгоняются под [frame.frameType]
     */
    private fun applyFrameToState(frame: DeviceFrame) {
        GO.PCounter      = frame.totalPulses
        GO.pulsePerSec   = frame.pulsesPerSec
        GO.cps           = frame.avgCps
        GO.cpsAVG       += frame.pulsesPerSec.toFloat()
        GO.cpsIntervalCount++
        GO.messTm        = frame.measurementTime
        GO.tempMC        = frame.temperature
        GO.battLevel     = frame.batteryVoltage
        GO.overloadFlag  = frame.overload

        val hw = frame.hw
        GO.HWpropLedKvant          = hw.ledKvant
        GO.HWpropSoundKvant        = hw.soundKvant
        GO.HWpropSoundLevel1       = hw.soundLevel1
        GO.HWpropSoundLevel2       = hw.soundLevel2
        GO.HWpropSoundLevel3       = hw.soundLevel3
        GO.HWpropVibroLevel1       = hw.vibroLevel1
        GO.HWpropVibroLevel2       = hw.vibroLevel2
        GO.HWpropVibroLevel3       = hw.vibroLevel3
        GO.HWpropAutoStartSpectrometr = hw.autoStartSpectrometer
        GO.HWpropClick10           = hw.click10
        GO.HWpropLed10             = hw.led10
        GO.HWpropLevel1            = hw.level1
        GO.HWpropLevel2            = hw.level2
        GO.HWpropLevel3            = hw.level3
        GO.HWpropCPS2UR            = hw.cps2ur
        GO.HWpropHVoltage          = hw.hVoltage
        GO.HWpropComparator        = hw.comparator
        //GO.HWCoef1024A             = hw.coef1024A
        //GO.HWCoef1024B             = hw.coef1024B
        //GO.HWCoef1024C             = hw.coef1024C
        //GO.HWCoef2048A             = hw.coef2048A
        //GO.HWCoef2048B             = hw.coef2048B
        //GO.HWCoef2048C             = hw.coef2048C
        GO.HWCoef4096A             = hw.coef4096A
        GO.HWCoef4096B             = hw.coef4096B
        GO.HWCoef4096C             = hw.coef4096C
        GO.HWCoef4096D             = hw.coef4096D
        GO.HWCoef4096E             = hw.coef4096E
        GO.HWAqureValue            = hw.acquireValue
        GO.HWBitsChan              = hw.bitsChan
        GO.HWSampleTime            = hw.sampleTime
        GO.spectrometerTime        = hw.spectrometerTime
        GO.spectrometerPulse       = hw.spectrometerPulse

        /* Spectrum resolution metadata */
        when (frame.frameType) {
            1 -> { GO.specterType = 0; GO.drawSPECTER.ResolutionSpectr = 1024; GO.HWspectrResolution = 0 }
            2 -> { GO.specterType = 1; GO.drawSPECTER.ResolutionSpectr = 2048; GO.HWspectrResolution = 1 }
            3 -> { GO.specterType = 2; GO.drawSPECTER.ResolutionSpectr = 4096; GO.HWspectrResolution = 2 }
            4 -> { GO.specterType = 0; GO.drawHISTORY.ResolutionHistory = 1024; GO.HWspectrResolution = 0 }
            5 -> { GO.specterType = 1; GO.drawHISTORY.ResolutionHistory = 2048; GO.HWspectrResolution = 1 }
            6 -> { GO.specterType = 2; GO.drawHISTORY.ResolutionHistory = 4096; GO.HWspectrResolution = 2 }
        }
    }

    /** Restore StatusStrip values from cached GO state. Used after Activity recreate (rotation). */
    /**
     * Восстанавливает значения StatusStrip из кэшированного состояния [globalObj] после
     * пересоздания Activity (поворот, recreate из-за смены темы).
     *
     * Без этого вызова после поворота `bzCpsValue`, `bzTempValue`, `bzBattValue` показали
     * бы placeholder из layout (нули), пока не придёт следующий BLE-фрейм — ~1 секунда
     * визуального мигания.
     *
     * Защита от первого запуска: если `GO.battLevel <= 0f` — фрейм ещё не приходил,
     * placeholder остаётся.
     */
    private fun restoreStatusStripFromState() {
        if (GO.battLevel <= 0f) return  // No frame received yet — leave placeholders

        GO.bzCpsValue.text = GO.pulsePerSec.toString()
        GO.bzCpsValue.setTextColor(
            ContextCompat.getColor(
                this,
                if (GO.overloadFlag) R.color.bz_danger else R.color.bz_accent
            )
        )
        GO.bzStatusStrip.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (GO.overloadFlag) R.color.bz_danger_soft else R.color.bz_surface
            )
        )
        GO.bzTempValue.text = "%d°C".format(GO.tempMC.toInt())
        applyBatteryDisplay(GO.battLevel)
        applyBtRssi()

        GO.showStatistics()  // updates bzClockValue
    }



    /**
     * Переключает единицы измерения дозы (мкР/ч ↔ мкЗв/ч), сохраняет выбор в SharedPreferences
     * (ключ `propUnits`) и сразу перерисовывает dose-readouts через [applyDoseReadouts].
     *
     * Вызывается из click-listener-ов на dose-readout TextView в [SpectrumFragment] и
     * [DoseFragment]. Не зависит от того, какой фрагмент активен.
     */
    fun toggleDoseUnits() {
        GO.unitsMess = if (GO.unitsMess == 1) 0 else 1
        GO.PP.setPropInt(GO.propUnits, GO.unitsMess)
        GO.applyDoseReadouts()
    }

    /**
     * Обновляет UI на основе пришедшего [DeviceFrame]. **Должен вызываться на главном потоке.**
     *
     * **Что делает:**
     *  1. Тинтит BT-индикатор зелёным (good data)
     *  2. Применяет системные бары темы ([applySystemBarsForTheme])
     *  3. StatusStrip: `bzCpsValue`, `bzTempValue`, `bzBattValue`, фон strip (red если overload)
     *  4. [globalObj.showStatistics] — часы, общая статистика, RSSI
     *  5. History hero (integral + avg CPS)
     *  6. [applyDoseReadouts] — все dose-readouts с pill уровня
     *  7. [recordTrackPoint] при активной записи трека
     *  8. Передаёт `dosimeterData` в `drawDOZIMETER` и `historyData` в `drawHISTORY`
     *  9. Передаёт `spectrumData` в `drawSPECTER` (если есть) + обновляет специтр-кнопку
     * 10. Передаёт `logEntries` в `drawLOG`
     *
     * Вызывается из подписчика [BluetoothInterface.deviceFrames] в [observeDeviceFrames].
     */
    private suspend fun applyFrameToUi(frame: DeviceFrame) {
        /* Mark indicator green on good data — new pill */
        val tint = ContextCompat.getColor(this, R.color.bz_bt_on)
        GO.bzBtDot.backgroundTintList = ColorStateList.valueOf(tint)
        ImageViewCompat.setImageTintList(GO.bzBtIcon, ColorStateList.valueOf(tint))

        applySystemBarsForTheme()

        /* StatusStrip discrete values */
        GO.bzCpsValue.text = frame.pulsesPerSec.toString()
        GO.bzCpsValue.setTextColor(
            ContextCompat.getColor(
                this,
                if (frame.overload) R.color.bz_danger else R.color.bz_accent
            )
        )
        GO.bzStatusStrip.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (frame.overload) R.color.bz_danger_soft else R.color.bz_surface
            )
        )
        GO.bzTempValue.text = "%d°C".format(frame.temperature.toInt())
        applyBatteryDisplay(frame.batteryVoltage)
        applyBtRssi()

        GO.showStatistics()

        /* Phase D: history hero readouts */
        GO.bzHistIntegralValue?.text = String.format("%,d", frame.totalPulses.toLong()).replace(',', ' ')
        GO.bzHistAvgCpsValue?.text = "%.2f".format(frame.avgCps)

        GO.applyDoseReadouts()

        /* Track recording */
        if (GO.trackIsRecordeed && GO.currentTrck > 0) {
            recordTrackPoint(frame)
        }

        /* Dosimeter */
        for (i in frame.dosimeterData.indices) {
            GO.drawDOZIMETER.dozimeterData[i] = frame.dosimeterData[i]
        }
        if (GO.initDOZ) {
            GO.drawDOZIMETER.Init()
            if (GO.drawDOZIMETER.dozVSize > 0 && GO.drawDOZIMETER.dozHSize > 0) {
                GO.drawDOZIMETER.clearDozimeter()
                GO.drawDOZIMETER.redrawDozimeter()
            }
        }

        /* Logs */
        frame.logEntries.forEachIndexed { i, entry ->
            GO.drawLOG.logData[i].tm = entry.timestamp
            GO.drawLOG.logData[i].act = entry.action
        }
        if (GO.drawLOG.logsDrawIsInit) GO.drawLOG.updateLogs()

        /* History spectrum */
        frame.historyData?.let { histData ->
            histData.copyInto(GO.drawHISTORY.historyData)
            GO.drawHISTORY.init()
            if (GO.drawHISTORY.VSize > 0 && GO.drawHISTORY.HSize > 0) {
                GO.drawHISTORY.clearHistory()
                GO.drawHISTORY.redrawSpecter(GO.specterType)
            }
        }

        /* Live spectrum */
        frame.spectrumData?.let { specData ->
            GO.specterRunning = true
            applySpecterButton()
            specData.copyInto(GO.drawSPECTER.spectrData)
            GO.drawSPECTER.init()
            if (GO.drawSPECTER.VSize > 0 && GO.drawSPECTER.HSize > 0) {
                GO.drawSPECTER.clearSpecter()
                GO.drawSPECTER.redrawSpecter(GO.specterType)
            }
        } ?: run {
            if (frame.frameType == 0) {
                GO.specterRunning = false
                applySpecterButton()
            }
        }
    }

    /**
     * Рендерит иконку и фон кнопки Start/Stop записи спектра ([GO.btnSpecterSS]) на основе
     * флага [GO.specterRunning]:
     *  - `true` → иконка `ic_bz_stop` + фон `bg_bz_circle_danger` (красный)
     *  - `false` → иконка `ic_bz_play` + фон `bg_bz_circle_accent` (зелёный)
     *
     * Безопасно вызывать многократно. Ничего не делает, если кнопка ещё не привязана
     * (`!GO.btnSpecterSSisInit`).
     */
    fun applySpecterButton() {
        if (!GO.btnSpecterSSisInit) return
        val running = GO.specterRunning
        GO.btnSpecterSS.setImageResource(if (running) R.drawable.ic_bz_stop else R.drawable.ic_bz_play)
        GO.btnSpecterSS.background = ContextCompat.getDrawable(
            this,
            if (running) R.drawable.bg_bz_circle_danger else R.drawable.bg_bz_circle_accent
        )
    }

    // --- GPS + track recording ---

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): android.location.Location? = withContext(Dispatchers.IO) {
        try { fusedLocationClient.lastLocation.await() }
        catch (e: Exception) { Log.w("BluZ-BT", "getLastKnownLocation failed", e); null }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): android.location.Location? = withContext(Dispatchers.IO) {
        try {
            val cts = CancellationTokenSource()
            coroutineScope {
                launch { delay(10_000); cts.cancel() }
                fusedLocationClient.getCurrentLocation(100, cts.token).await()
            }
        } catch (e: Exception) {
            Log.e("BluZ-BT", "getFreshLocation failed", e); null
        }
    }

    /**
     * Записывает одну точку трека в БД при активной записи (`GO.trackIsRecordeed == true`).
     *
     * Координаты берёт из `GO.lastPointLoc` (обновляется в [ContinuousLocationManager]).
     * CPS — из текущего фрейма (`frame.pulsesPerSec`). Магнитное поле здесь не пишется
     * (магнитометр живёт в [BleMonitoringService], если приложение в foreground —
     * пишется `0.0`).
     *
     * Вызывается из [applyFrameToUi].
     */
    private suspend fun recordTrackPoint(frame: DeviceFrame) {
        val location = getLastKnownLocation() ?: getFreshLocation()
        val detail = TrackDetail(
            trackId   = GO.currentTrck,
            latitude  = location?.latitude  ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            accuracy  = location?.accuracy  ?: 0f,
            altitude  = location?.altitude  ?: 0.0,
            speed     = location?.speed     ?: 0f,
            cps       = frame.pulsesPerSec.toFloat(),
            magnitude = 0.0,
            timestamp = System.currentTimeMillis() / 1000
        )
        GO.dao.insertPoint(detail)

        if (location != null) {
            val imp = when {
                frame.pulsesPerSec < GO.propLevel1.toUInt() -> GO.impArr[0]
                frame.pulsesPerSec < GO.propLevel2.toUInt() -> GO.impArr[15]
                frame.pulsesPerSec < GO.propLevel3.toUInt() -> GO.impArr[20]
                else                                        -> GO.impArr[31]
            }
            GO.map?.mapObjects?.addPlacemark()?.apply {
                geometry = Point(location.latitude, location.longitude)
                setIcon(imp!!)
            }
        }
    }

    /**
     * Запрашивает все нужные runtime-разрешения: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
     * (Android 12+), `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS` (Android 13+).
     *
     * Если все уже есть — выставляет `GO.allPermissionAccept = true` и сразу инициализирует
     * BLE. Иначе запускает [requestPermissionLauncher] с диалогом-объяснением для тех
     * разрешений, по которым `shouldShowRequestPermissionRationale` вернул true.
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Проверяем location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Bluetooth permissions для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Проверяем, нужно ли показать объяснение
            //if (shouldShowRequestPermissionRationale(permissionsToRequest.toTypedArray())) {
            // Показываем объяснение перед запросом
            //showInitialPermissionExplanationDialog(permissionsToRequest.toTypedArray())
            //} else {
            // Просто запрашиваем разрешения
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            //}
        } else {
            // Все разрешения уже есть
            GO.allPermissionAccept = true
            GO.drawLOG.appendAppLogs("All permission granted.", 3)
            //GO.startBluetoothTimer()
        }
    }

    // Проверяет, нужно ли показать объяснение для хотя бы одного разрешения
    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
    }

    // Диалог с объяснением перед первым запросом
    private fun showInitialPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this).setTitle("Need permission.").setMessage("For BLE need permissions.").setPositiveButton("Grant") { _, _ ->
            requestPermissionLauncher.launch(permissions)
        }.setNegativeButton("Cancel") { _, _ ->
            finish()
        }.setCancelable(false).show()
    }

    // Диалог с объяснением после отказа
    private fun showPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this).setTitle("Need permission").setMessage("For BLE need permissions.").setPositiveButton("Retry") { _, _ ->
            requestPermissionLauncher.launch(permissions)
        }.setNegativeButton("Cancel") { _, _ ->
            finish()
        }.setCancelable(false).show()
    }

    /**
     * Проверяет наличие всех нужных BLE-разрешений (без запроса). Используется в
     * [performExit] перед стартом фонового сервиса — без разрешений сервис всё равно
     * не сможет ничего сделать.
     */
    private fun hasPermissions(): Boolean {
        val permissionList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT) // тоже нужно на API 31+
        }

        // На всех версиях ниже API 31 и для совместимости — нужна геолокация
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            // или ACCESS_COARSE_LOCATION, если хватает точности
        }

        return permissionList.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // --- Navigation setup (BottomNavigationView / NavigationRailView) ---

    /**
     * Настраивает кастомную bottom-навигацию и (в landscape) side-rail.
     *
     * **5 элементов:** Spectrum / Dose / Map / Settings / **Exit**.
     * Первые 4 — страницы [ViewPager2]. Exit — действие (`performExit()`), не страница.
     *
     * **Wire-up:**
     *  - Привязывает иконку и текст к каждому таб-контейнеру через [BzTabSpec]
     *  - Click-listener: если `index >= pageCount` → [performExit], иначе [ViewPager2.setCurrentItem]
     *  - Регистрирует [androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback] для
     *    синхронизации индикатора и `applyStatVisibility`
     */
    private fun setupNavigation() {
        val bottomNav: LinearLayout? = findViewById(R.id.bottomNav)
        val sideRail: LinearLayout? = findViewById(R.id.sideRail)

        // Таб с индексом >= pageCount — действие (Выход), а не страница ViewPager
        val pageCount = GO.adapter.itemCount  // 4: Spectrum, Dose, Map, Settings

        val tabSpecs = listOf(
            BzTabSpec(R.id.tab_spectrum, R.drawable.ic_nav_spectrum, "Спектр"),
            BzTabSpec(R.id.tab_history, R.drawable.ic_nav_spectrum, "Alarm"),
            BzTabSpec(R.id.tab_dose,     R.drawable.ic_nav_dose,     "Доза"),
            BzTabSpec(R.id.tab_map,      R.drawable.ic_nav_map,      "Карта"),
            BzTabSpec(R.id.tab_settings, R.drawable.ic_nav_settings, "Настр."),
            BzTabSpec(R.id.tab_exit,     R.drawable.ic_bz_exit,      "Выход")
        )

        val railSpecs = listOf(
            BzTabSpec(R.id.rail_spectrum, R.drawable.ic_nav_spectrum, "Спектр"),
            BzTabSpec(R.id.rail_history, R.drawable.ic_nav_spectrum, "Alarm"),
            BzTabSpec(R.id.rail_dose,     R.drawable.ic_nav_dose,     "Доза"),
            BzTabSpec(R.id.rail_map,      R.drawable.ic_nav_map,      "Карта"),
            BzTabSpec(R.id.rail_settings, R.drawable.ic_nav_settings, "Настр."),
            BzTabSpec(R.id.rail_exit,     R.drawable.ic_bz_exit,      "Выход")
        )

        fun wireTabs(container: View?, specs: List<BzTabSpec>) {
            container ?: return
            specs.forEachIndexed { index, spec ->
                val tab = container.findViewById<View>(spec.containerId) ?: return@forEachIndexed
                tab.findViewById<android.widget.ImageView>(R.id.bz_tab_icon).setImageResource(spec.iconRes)
                tab.findViewById<TextView>(R.id.bz_tab_label).text = spec.label
                tab.setOnClickListener {
                    if (index >= pageCount) {
                        // Действие, а не страница — Выход
                        performExit()
                    } else if (GO.viewPager.currentItem != index) {
                        GO.viewPager.setCurrentItem(index, false)
                    }
                }
            }
        }

        wireTabs(bottomNav, tabSpecs)
        wireTabs(sideRail, railSpecs)

        fun applyStatVisibility(page: Int) {
            when (page) {
                1 -> {  // Доза
                    if (GO.initDOZ) {
                        GO.drawDOZIMETER.Init()
                        if (GO.drawDOZIMETER.dozVSize > 0 && GO.drawDOZIMETER.dozHSize > 0) {
                            GO.drawDOZIMETER.clearDozimeter()
                            GO.drawDOZIMETER.redrawDozimeter()
                        }
                    }
                    GO.showStatistics()
                }
                0 -> GO.showStatistics()  // Спектр
                else -> { /* hero readouts handled per-fragment */ }
            }
        }

        GO.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav?.let { syncTabSelection(position, tabSpecs, it, showTopIndicator = true) }
                sideRail?.let { syncTabSelection(position, railSpecs, it, showTopIndicator = false) }
                applyStatVisibility(position)
            }
        })

        bottomNav?.let { syncTabSelection(0, tabSpecs, it, showTopIndicator = true) }
        sideRail?.let { syncTabSelection(0, railSpecs, it, showTopIndicator = false) }
        applyStatVisibility(0)
    }

    private data class BzTabSpec(val containerId: Int, val iconRes: Int, val label: String)

    /**
     * Применяет визуальное состояние «selected» к нужному таб-контейнеру.
     *
     * Внутри каждого таба `duplicateParentState=true` — состояние пробрасывается на icon
     * и label через [color] селекторы (`bz_nav_icon.xml`, `bz_nav_text.xml`).
     *
     * @param activePage Индекс выбранного фрагмента (0..pageCount-1).
     * @param specs Список табов (то же, что в [setupNavigation]).
     * @param container Корневой LinearLayout навигации (bottom или rail).
     * @param showTopIndicator Если `true` — отображает или скрывает верхний accent-индикатор.
     */
    private fun syncTabSelection(activePage: Int, specs: List<BzTabSpec>, container: View, showTopIndicator: Boolean) {
        specs.forEachIndexed { index, spec ->
            val tab = container.findViewById<View>(spec.containerId) ?: return@forEachIndexed
            val active = (index == activePage)
            tab.isSelected = active
            if (showTopIndicator) {
                tab.findViewById<View>(R.id.bz_tab_indicator)?.visibility =
                    if (active) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    /**
     * Выход из приложения. Если активна запись трека ([GO.trackIsRecordeed] == true) и
     * есть нужные разрешения — стартует [BleMonitoringService] в foreground:
     *  1. Пишет в SharedPreferences `app_state`: `device_mac`, `current_track_id`, `cps_2_doze`
     *  2. `startForegroundService(Intent(this, BleMonitoringService::class.java))`
     *
     * Затем останавливает таймер реконнекта, скан и GATT-соединение, и вызывает
     * `finishAndRemoveTask()`.
     *
     * Вызывается из click-listener на табе «Выход» в bottom-nav (см. [setupNavigation]).
     */
    private fun performExit() {
        if (!GO.needTerminate) {
            if (GO.trackIsRecordeed) {
                if (!hasPermissions()) {
                    Toast.makeText(this, "Нужны разрешения для фоновой записи", Toast.LENGTH_LONG).show()
                    return
                }
                if (!checkSerice()) {
                    Intent(this, BleMonitoringService::class.java).also { intent ->
                        try {
                            getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {
                                putString("device_mac", GO.LEMAC)
                                putLong("current_track_id", GO.currentTrck)
                                putFloat("cps_2_doze", GO.propCPS2UR)
                            }
                            startForegroundService(intent)
                        } catch (e: Exception) {
                            Log.e("BluZ-BT", "Не удалось запустить сервис", e)
                        }
                    }
                }
            }
            GO.tmFull.stopTimer()
            GO.oneShotBLETimer = false
            GO.BTT.stopScan()
            GO.BTT.destroyDevice()
        }
        finishAndRemoveTask()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Выйти из BluZ?")
            .setPositiveButton("Выйти") { _, _ -> performExit() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Флаг для отслеживания возврата из настроек
    private var hasPendingPermissionCheck = false

    // Диалог для перехода в настройки
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission disabled.")
            .setMessage("Permission disabled permanently. Need grant necessary permissions.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                hasPendingPermissionCheck = true
                startActivity(intent)
                // Не закрываем сразу, чтобы пользователь мог вернуться
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
