package ru.starline.bluz

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.MapKitFactory
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.starline.bluz.data.entity.Track
import kotlin.system.exitProcess
import androidx.core.content.edit

public val GO: globalObj = globalObj()


//public lateinit var mainContext: Context
public var PI: Int = 0

public class MainActivity : FragmentActivity() {
    override fun onResume() {
        super.onResume()
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
            stopService(Intent(this, BleMonitoringService::class.java))
            getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {
                putBoolean("is_ble_service_running", false)}
        } else {
            Log.d("BluZ-BT", "Service not running")
        }
    }

    /* Проверка активности сервиса */
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GO.mainContext = applicationContext
        /* Использование всего экрана, место занятое челкой, тоже используется. */
        //enableEdgeToEdge()
        //enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color. TRANSPARENT, Color. TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim))

        //WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window,window.decorView.findViewById(android.R.id.content)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())

            // When the screen is swiped up at the bottom
            // of the application, the navigationBar shall
            // appear for some time
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        GO.drawSPECTER = drawSpecter()
        GO.drawDOZIMETER = drawDozimeter()
        GO.drawHISTORY = drawHistory()
        GO.drawLOG = drawLogs()
        GO.drawCURSOR = drawCursor()
        GO.drawExamp = drawExmple()
        GO.adapter = NumberAdapter(this)
        GO.indicatorBT = findViewById(R.id.indicatorBT)
        GO.viewPager = findViewById(R.id.VPMain)
        GO.viewPager.isUserInputEnabled = false             // Отключение прокрутки viewPager2
        GO.viewPager.adapter = GO.adapter
        GO.bColor = buttonColor()
        GO.txtStat1 = findViewById(R.id.textStatistic1)
        GO.txtStat2 = findViewById(R.id.textStatistic2)
        GO.txtStat3 = findViewById(R.id.textStatistic3)
        GO.txtIsotopInfo = findViewById(R.id.textIsotopInfo)
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
        }        /*
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

        /* Завершение приложения */
        var btnExit: ImageButton = findViewById(R.id.buttonExit)
        btnExit.setOnClickListener {
            /* Выполняется запись трека, нужно запустить сервис */
            if (GO.trackIsRecordeed) {
                // Проверяем разрешения перед запуском сервиса
                if (!hasPermissions()) {
                    Toast.makeText(this, "Нужны разрешения для фоновой записи", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val prefs = getSharedPreferences("app_state", Context.MODE_PRIVATE)
                if (checkSerice()) {
                    Log.i("BluZ-BT", "Service already running.")
                } else {
                    Log.i("BluZ-BT", "Service need start.")

                    // Подготавливаем интент для сервиса
                    Intent(this, BleMonitoringService::class.java).also { intent ->
                        try {
                            val prefs = getSharedPreferences("app_state", Context.MODE_PRIVATE)
                            prefs.edit {
                                putString("device_mac", GO.LEMAC)
                                putLong("current_track_id", GO.currentTrck)
                                putFloat("cps_2_doze", GO.propCPS2UR)
                            }
                            startForegroundService(intent)
                            Log.i("BluZ-BT", "BleMonitoringService run.")
                        } catch (e: Exception) {
                            Log.e("BluZ-BT", "Не удалось запустить сервис", e)
                            Toast.makeText(this, "Ошибка запуска сервиса", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
            /* Отключаем таймер проверки BLE соединения */
            GO.tmFull.stopTimer()
            GO.BTT.stopScan()
            GO.BTT.destroyDevice()

            // Закрываем активность корректно
            finishAndRemoveTask()

            /* Запись трека не выполняется - просто завершаем приложение */
            //activity.finish()
            //finishAffinity()
            //System.exit(0)
            //finish()
            //System.out.close()
            //finishAndRemoveTask()
            //exitProcess(-1)
        }

        /* Окно со спектром */
        var btnSpecter: ImageButton = findViewById(R.id.buttonSpecter)
        btnSpecter.setOnClickListener {
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.VISIBLE
            GO.viewPager.setCurrentItem(0, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnSpecter)
            /* Вывод статитики для дозиметра и спектрометра */
            GO.showStatistics()
        }

        /* Окно с историей */
        var btnHistory: ImageButton = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            /* Показ статистики */
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.VISIBLE
            GO.viewPager.setCurrentItem(1, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnHistory)
            /* Вывод статитики для дозиметра и спектрометра */
            GO.showStatistics()
        }

        /* Окно дозиметра */
        var btnDozimeter: ImageButton = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            /* Показ статистики */
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(2, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnDozimeter)
            if (GO.initDOZ) {
                GO.drawDOZIMETER.Init()
                if (GO.drawDOZIMETER.dozVSize > 0 &&  GO.drawDOZIMETER.dozHSize > 0) {
                    GO.drawDOZIMETER.clearDozimeter()
                    GO.drawDOZIMETER.redrawDozimeter()
                }
            }
            /* Вывод статитики для дозиметра и спектрометра */
            GO.showStatistics()
        }

        /* Окно c логами */
        var btnLog: ImageButton = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            /* Скрыть статистику */
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(3, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnLog)
            if (GO.drawLOG.logsDrawIsInit) {
                GO.drawLOG.updateLogs()
            }
        }

        /* Окно с настройками */
        GO.btnSetup = findViewById(R.id.buttonSetup)
        GO.btnSetup.setOnClickListener {
            /* Скрыть статистику */
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnSetup)
            /*
            * TODO -- Нужно разобраться с этой ерундой.
            */
            lifecycleScope.launch {
                delay(100L)
                GO.drawExamp.exampRedraw()
            }
        }

        /* Окно с картой */
        GO.btnMap = findViewById(R.id.buttonMap)
        GO.btnMap.setOnClickListener {
            /* Скрыть статистику */
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(5, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnMap)
        }

        GO.bColor.initColor(btnSpecter, btnHistory, btnDozimeter, btnLog, GO.btnSetup, GO.btnMap)
        GO.bColor.resetToDefault()
        GO.bColor.setToActive(btnSpecter)  // Активная закладка.

        /*
        *       Параметры приложения
        */
        GO.PP = propControl()
        GO.readConfigParameters()
        if (GO.fullScrn) {
            enableEdgeToEdge()
        }
        /* Загрузка справочника изотопов */
        GO.loadIsotop()
        //if (GO.allPermissionAccept) {
        GO.startBluetoothTimer()
        //}
    }

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

    // Флаг для отслеживания возврата из настроек
    private var hasPendingPermissionCheck = false

    // Диалог для перехода в настройки
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission disabled.")
            .setMessage("Permission disabled permanently. Need grant necessary permissions in setup.")
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
