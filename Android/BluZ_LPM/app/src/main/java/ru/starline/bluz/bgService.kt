package ru.starline.bluz

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import ru.starline.bluz.data.AppDatabase
import ru.starline.bluz.data.entity.TrackDetail
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationRequest
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import await
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.NonCancellable.cancel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.util.size
import androidx.core.content.edit

class BleMonitoringService : Service() {
    // MAC-адрес BluZ
    companion object {
        var TARGET_DEVICE_MAC = ""
        var cps2doze = 0f
        private const val LOCATION_PRIORITY_HIGH_ACCURACY = 100
        private const val LOCATION_PRIORITY_BALANCED = 102
        private const val LOCATION_PRIORITY_LOW_POWER = 104
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var scanner: BluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val dao by lazy { database.dosimeterDao() }

    private var activeTrackId: Long? = null

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let  { scanResult ->
                if (scanResult.device.address == TARGET_DEVICE_MAC) {
                    saveToTrack(scanResult)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluZ-BT", "Scan failed: $errorCode")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BluZ-BT", "BleMonitoringService: onCreate called")
        val prefs = getSharedPreferences("app_state", Context.MODE_PRIVATE)
        TARGET_DEVICE_MAC = prefs.getString("device_mac", "") ?: ""
        activeTrackId = prefs.getLong("current_track_id", 0)
        cps2doze = prefs.getFloat("cps_2_doze", 0f)
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter
        scanner = bluetoothAdapter.bluetoothLeScanner
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /* Проверим нужно ли завершить сервис. */
        if (intent?.action == "STOP_SERVICE") {
            getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {
                putBoolean("is_ble_service_running", false)
                putString("device_mac", "")
                putLong("current_track_id", 0)
            }
            Log.d("BluZ-BT", "Получена команда на остановку из уведомления")
            stopSelf()
            return START_NOT_STICKY
        }

        // Создаём уведомление сразу (фоновые сервисы должны быстро показать foreground-статус)
        createNotificationChannel()
        updateNotification(0f)
        val initialNotification = updateNotification(0f)
        startForeground(1, initialNotification)
        //getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {putBoolean("is_ble_service_running", true)}
        //activeTrackId = GO.currentTrck
        Log.d("BluZ-BT", "Using trackId: $activeTrackId")
        /* Установим флаг в shared для индикации, что сервис запущен и сохраним текущий трек */
        getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {
            putBoolean("is_ble_service_running", true)
        }

        // Запускаем фоновую задачу: проверка трека + разрешений + старт сканирования
        scope.launch {
            try {
                // Проверяем разрешения до вызова startBleScan()
                if (!hasPermissions()) {
                    Log.e("BluZ-BT", "Required permissions are missing. Cannot start scan.")
                    stopSelf()
                    return@launch
                }
                // Запускаем сканирование BLE
                startBleScan()

            } catch (e: SecurityException) {
                Log.e("BluZ-BT", "Security error when starting scan", e)
                stopSelf()
            } catch (e: Exception) {
                Log.e("BluZ-BT", "Unexpected error in onStartCommand", e)
                stopSelf()
            }
        }
        /* Запускаем сервис, так, что бы его не убила система */
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun saveToTrack(result: ScanResult) {
        val trackId = activeTrackId ?: return

        scope.launch(Dispatchers.IO)  {
            try {
                // Получаем GPS данные.
                val location = getLastKnownLocation() ?: getFreshLocation()
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0
                val accuracy = location?.accuracy ?: 0f
                val altitude = location?.altitude ?: 0.0
                val speed = location?.speed ?: 0f

                // Извлекаем CPS из manufacturer data
                val cps = extractCpsFromScanResult(result) ?: 0f

                // Уровень сигнала
                val rssi = result.rssi

                // Готовим данные для сохранения в базу.
                val detail = TrackDetail(
                    trackId = trackId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    altitude = altitude,
                    speed = speed,
                    cps = cps,
                    magnitude = 0.0, // Нужно добавить данные магнитометра.
                    timestamp = System.currentTimeMillis()
                )

                // Сохраняем точку
                dao.insertPoint(detail)
                updateNotification(cps)

                Log.d("BluZ-BT","Saved: RSSI=$rssi, CPS=$cps, Lat=$latitude, Lon=$longitude, Accur=$accuracy")

            } catch (e: Exception) {
                Log.e("BluZ-BT", "Failed to save track point", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        try {
            if (::scanner.isInitialized && hasPermissions()) {
                scanner.stopScan(scanCallback)
                Log.d("BluZ-BT", "BLE scan stopped gracefully")
            }
        } catch (e: SecurityException) {
            Log.w("BluZ-BT", "Failed to stop scan due to missing permissions", e)
            // Игнорируем — сервис всё равно завершается
        } catch (e: Exception) {
            Log.e("BluZ-BT", "Unexpected error while stopping scan", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        /* Отметим в shared? что сервис не запущен */
        getSharedPreferences("app_state", Context.MODE_PRIVATE).edit {putBoolean("is_ble_service_running", false)}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ble_monitor_channel",
            "Track is recorded.",
            //NotificationManager.IMPORTANCE_LOW
            //NotificationManager.IMPORTANCE_DEFAULT
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scanning for specific BLE device"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "ble_monitor_channel")
            .setContentTitle("BLE Monitoring Active")
            .setContentText("Tracking device: $TARGET_DEVICE_MAC")
            //.setSmallIcon(R.drawable.ic_bluetooth_searching)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        try {
            scanner.stopScan(scanCallback)
            Log.d("BluZ-BT", "BLE scan stopped")
        } catch (e: SecurityException) {
            Log.e("BluZ-BT", "Failed to stop scan", e)
        } catch (e: Exception) {
            // На некоторых устройствах stopScan может кидать NPE, если сканирование ещё не запущено
            Log.w("BluZ-BT", "Error stopping scan", e)
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ])
    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val mac = TARGET_DEVICE_MAC
        Log.d("BluZ-BT", "Starting BLE scan for $mac")
        if (!hasPermissions()) {
            Log.e("BluZ-BT", "Permissions missing")
            stopSelf()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w("BluZ-BT", "Bluetooth is disabled")
            stopSelf()
            return
        }

        val filter = ScanFilter.Builder()
            .setDeviceAddress(mac)
            .build()

        val settings = ScanSettings.Builder()
            //.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // Экономный режим, но много пропусков.
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            Log.d("BluZ-BT", "BLE scan started for MAC: $mac")
        } catch (e: SecurityException) {
            Log.e("BluZ-BT", "Failed to start scan due to permission", e)
            stopSelf()
        }
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

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): android.location.Location? = withContext(Dispatchers.IO) {
        try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.w("BluZ-BT", "Failed to get last location", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): android.location.Location? = withContext(Dispatchers.IO) {
        return@withContext try {
            // Создаём CancellationToken для отмены запроса
            val cancellationTokenSource = CancellationTokenSource()

            // Устанавливаем таймаут в 10 секунд
            coroutineScope {
                launch {
                    delay(10000)
                    cancellationTokenSource.cancel()
                }

                // Запрашиваем свежее местоположение с высокой точностью
                fusedLocationClient.getCurrentLocation(
                    LOCATION_PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()            }
        } catch (e: Exception) {
            Log.e("BluZ-BT", "Failed to get fresh location", e)
            null
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun extractCpsFromScanResult(result: ScanResult): Float? {
        val scanRecord = result.scanRecord ?: return null
        val manufacturerData = scanRecord.manufacturerSpecificData

        for (i in 0 until manufacturerData.size) {
            val companyId = manufacturerData.keyAt(i)
            if (companyId == 0x0030) {
                val userData = manufacturerData.valueAt(i) // 4 ,байта - CPS

                // Ожидаются минимум 4 байта для CPS
                if (userData.size >= 4) {
                    try {
                        val cpsValue = bytesToUInt(userData[0], userData[1], userData[2], userData[3])
                        return cpsValue.toFloat()
                    } catch (e: IndexOutOfBoundsException) {
                        Log.w("BluZ-BT", "CPS data too short", e)
                    }
                } else {
                    Log.w("BluZ-BT", "User data too short: ${userData.size} bytes")
                }
            }
        }
        return null
    }

    // Помогает собрать 4 байта в int (little-endian)
    private fun bytesToUInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): UInt {
        return (b0.toUInt() and 0xFFu) or
                ((b1.toUInt() and 0xFFu) shl 8) or
                ((b2.toUInt() and 0xFFu) shl 16) or
                ((b3.toUInt() and 0xFFu) shl 24)
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(cps: Float): Notification {
        val formattedCps = "CPS:$cps / ${cps * cps2doze}uR" // CPS в уведомлении.
        val stopIntent = createStopServiceIntent()

        val notification = NotificationCompat.Builder(this, "ble_monitor_channel")
            .setContentTitle("Track is recorded.")
            .setContentText(formattedCps)
            .setStyle(NotificationCompat.BigTextStyle().bigText(formattedCps)) // Для крупного текста
            //.setSmallIcon(R.drawable.ic_bluetooth_notification)
            .setSmallIcon(R.drawable.ic_radiation_24)
            //.setSmallIcon(R.drawable.ic_cps_display)
            .setContentIntent(createPendingIntent())
            //.setOngoing(true)
            //.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(cps.toInt())
            .addAction(
                R.drawable.ic_stop, // иконка (например, круг с чертой)
                "Остановить",
                stopIntent
            )
            .build()

        // Обновляем уведомление с тем же ID
        NotificationManagerCompat.from(this).notify(1, notification)
        return notification
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /* intent для остановки сервиса */
    private fun createStopServiceIntent(): PendingIntent {
        val intent = Intent(this, BleMonitoringService::class.java)
        intent.action = "STOP_SERVICE"
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}