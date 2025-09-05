package ru.starline.bluz

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


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
        enableEdgeToEdge()
        //enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color. TRANSPARENT, Color. TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window,
            window.decorView.findViewById(android.R.id.content)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())

            // When the screen is swiped up at the bottom
            // of the application, the navigationBar shall
            // appear for some time
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

        /* Янидекс карта */
        MapKitFactory.setApiKey("API-YANDEX-KEY")
        MapKitFactory.initialize(this)


        /*
        *   Тексты форматов для сохранения
        */
        GO.saveSpecterType1 = getResources().getString(R.string.textType1)
        GO.saveSpecterType2 = getResources().getString(R.string.textType2)

        /*
        *   Цвета для курсора
        */
        GO.ColorEraseCursor = getResources().getColor(R.color.eraseColorCursor, GO.mainContext.theme)
        GO.ColorActiveCursor = getResources().getColor(R.color.activeColorCursor, GO.mainContext.theme)

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
            //activity.finish()
            //finishAffinity()
            //System.exit(0)
            //finish()
            //System.out.close()
            finishAndRemoveTask()
            exitProcess(-1)
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
        AlertDialog.Builder(this)
            .setTitle("Need permission.")
            .setMessage("For BLE need permissions.")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // Диалог с объяснением после отказа
    private fun showPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("Need permission")
            .setMessage("For BLE need permissions.")
            .setPositiveButton("Retry") { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
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


