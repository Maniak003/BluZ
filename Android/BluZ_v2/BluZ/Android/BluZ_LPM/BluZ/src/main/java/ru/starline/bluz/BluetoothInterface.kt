package ru.starline.bluz

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round
import kotlin.system.exitProcess

/*
 UUIDs generated from the random UUID generator:
 BLUZ_UUID : 0000fe80-cc7a-482a-984a-7f2ed5b3e58f
 RX_UUID   : 0000fe81-8e22-4541-9d4c-21edae82ed19
 TX_UUID   : 0000fe82-8e22-4541-9d4c-21edae82ed19
*/

/**
 * BLE-стек приложения: подключение к прибору BluZ через GATT, сборка фреймов из MTU-чанков,
 * проверка CRC и эмиссия [DeviceFrame] подписчикам.
 *
 * **UUIDs:**
 *  - Service: `0000fe80-cc7a-482a-984a-7f2ed5b3e58f`
 *  - RX (notify, приём от прибора): `0000fe81-8e22-4541-9d4c-21edae82ed19`
 *  - TX (write, отправка команд): `0000fe82-8e22-4541-9d4c-21edae82ed19`
 *
 * **Реактивные потоки:**
 *  - [deviceFrames]: [SharedFlow] — горячий поток собранных и проверенных фреймов
 *  - [status]: [StateFlow] — текущее состояние подключения ([BleStatus])
 *
 * **Жизненный цикл соединения** (см. [initLeDevice]):
 *  1. `connectGatt(autoConnect=false)` → onConnectionStateChange(CONNECTED)
 *  2. `requestMtu(251)` → onMtuChanged
 *  3. `discoverServices` → onServicesDiscovered
 *  4. `setCharacteristicNotification(RX, true)` — готово к приёму
 *
 * **Формат фрейма** — см. DEVELOPER_GUIDE.md раздел 5.2 или комментарии в [globalObj].
 * Многопакетный (MTU 251, payload 248): первый байт каждого MTU-пакета — порядковый номер
 * (0 = старт нового фрейма). [processIncomingPacket] собирает их в общий буфер и при
 * получении ожидаемого финального пакета эмитит [DeviceFrame].
 *
 * **Команды отправки** — [sendCommand]. Командные коды 0..8 описаны в DEVELOPER_GUIDE.md
 * раздел 5.3. Для команд с параметрами заполняется [sendBuffer] заранее.
 *
 * **Reconnect.** [GO.tmFull] (`intervalTimer`) каждые 10 секунд проверяет [connected];
 * если false → `destroyDevice() + initLeDevice()`.
 *
 * **Discovery-режим** (поиск всех приборов в эфире) — отдельный API [scanForDevices].
 * Старый [startScan] оставлен для совместимости.
 */
class BluetoothInterface {
    private var LEMACADDRESS: String = ""
    private val BTM = GO.mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val BTA = BTM.adapter
    private val BTS = BTA.bluetoothLeScanner
    lateinit var txtMACADDRESS: EditText
    private var gatt: BluetoothGatt? = null
    private var device: BluetoothDevice? = null
    private var writeBuffer: ArrayList<ByteArray>? = null
    private val MAX_MTU: Int = 251
    private val payloadSize: Int = MAX_MTU - 3
    private var writePending = false
    var connected: Boolean = false

    private val BLUETOOTH_LE_CCCD = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val BLUETOOTH_BLUZ_SERVICE = java.util.UUID.fromString("0000fe80-cc7a-482a-984a-7f2ed5b3e58f")
    private val BLUETOOTH_BLUZ_CHAR_R = java.util.UUID.fromString("0000fe81-8e22-4541-9d4c-21edae82ed19")
    private val BLUETOOTH_BLUZ_CHAR_W = java.util.UUID.fromString("0000fe82-8e22-4541-9d4c-21edae82ed19")
    private var rdCharacteristic: BluetoothGattCharacteristic? = null
    private var wrCharacteristic: BluetoothGattCharacteristic? = null

    @OptIn(ExperimentalUnsignedTypes::class)
    var sendBuffer = UByteArray(255)

    /** Emits a fully assembled and checksum-verified device data frame. */
    private val _deviceFrames = MutableSharedFlow<DeviceFrame>(extraBufferCapacity = 4)
    val deviceFrames: SharedFlow<DeviceFrame> = _deviceFrames

    /** BLE connection state observable by the UI layer. */
    private val _status = MutableStateFlow<BleStatus>(BleStatus.Disconnected)
    val status: StateFlow<BleStatus> = _status

    // --- Scan ---

    val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            GO.drawLOG.appendAppLogs("Scan callback.", 5)
            if (result.device == null || TextUtils.isEmpty(result.device.name)) return
            Log.d("BluZ-BT", "${result.device.name} ${result.device.address}")
            if (result.device.name == GO.propCfgBLEDeviceName) {
                LEMACADDRESS = result.device.address
                GO.drawLOG.appendAppLogs("Found MAC: $LEMACADDRESS.", 4)
                MainScope().launch {
                    withContext(Dispatchers.Main) {
                        txtMACADDRESS.setText(LEMACADDRESS)
                        GO.scanButton.setTextColor(GO.mainContext.resources.getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.scanButton.setText(GO.mainContext.getString(R.string.textScan))
                    }
                }
                BTS.stopScan(this)
            }
        }
    }

    /**
     * **Legacy.** Сканирует BLE-эфир до первого устройства с именем `"BluZ"`, подставляет
     * найденный MAC в `textMAC` и останавливает скан. Используется ТОЛЬКО старым кодом —
     * новый UI выбора прибора работает через [scanForDevices] (накопительный режим).
     *
     * @param textMAC EditText в Settings, куда подставляется MAC. Хранится в [txtMACADDRESS]
     *  для использования в `leScanCallback`.
     */
    @SuppressLint("MissingPermission")
    fun startScan(textMAC: EditText) {
        txtMACADDRESS = textMAC
        GO.drawLOG.appendAppLogs("Request scan LE.", 3)
        GO.tmFull.stopTimer()
        destroyDevice()
        connected = false
        MainScope().launch {
            withContext(Dispatchers.Main) {
                textMAC.setText(GO.mainContext.getString(R.string.defaultMAC))
            }
        }
        if (!BTA.isEnabled) {
            GO.drawLOG.appendAppLogs("BLE disable.", 0)
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(GO.mainContext, "BlueTooth disable ?", Toast.LENGTH_LONG).show()
                    GO.scanButton.setTextColor(GO.mainContext.resources.getColor(R.color.buttonTextColor, GO.mainContext.theme))
                    GO.scanButton.text = GO.mainContext.getString(R.string.textScan)
                    launch {
                        kotlinx.coroutines.delay(3000L)
                        exitProcess(-1)
                    }
                }
            }
        } else {
            GO.drawLOG.appendAppLogs("Start scan LE.", 3)
            BTS.startScan(leScanCallback)
            Log.d("BluZ-BT", "LE scanning.")
        }
    }

    /** Останавливает [leScanCallback] (используется парно с [startScan]). */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        BTS.stopScan(leScanCallback)
        GO.drawLOG.appendAppLogs("Stop scan.", 1)
    }

    /** Discovery-режим: ищет ВСЕ устройства с именем `BluZ` в течение `durationMs`.
     *  - `onFound(mac)` вызывается при каждом НОВОМ обнаружении (без дублей).
     *  - `onComplete()` — когда сканирование закончилось (по таймауту или отмене через `cancel()` на возвращённой Job).
     *  Все callback-и приходят на главный поток. */
    @SuppressLint("MissingPermission")
    fun scanForDevices(
        durationMs: Long,
        onFound: (String) -> Unit,
        onComplete: () -> Unit
    ): kotlinx.coroutines.Job {
        val found = linkedSetOf<String>()
        val discoveryCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                if (result.device == null || TextUtils.isEmpty(result.device.name)) return
                if (result.device.name == GO.propCfgBLEDeviceName) {
                    val mac = result.device.address
                    if (found.add(mac)) {
                        GO.drawLOG.appendAppLogs("Discovery: found $mac", 4)
                        MainScope().launch { onFound(mac) }
                    }
                }
            }
        }
        if (!BTA.isEnabled) {
            GO.drawLOG.appendAppLogs("BLE disable.", 0)
            return MainScope().launch { onComplete() }
        }
        GO.tmFull.stopTimer()
        destroyDevice()
        connected = false
        GO.drawLOG.appendAppLogs("Start discovery scan ($durationMs ms).", 3)
        BTS.startScan(discoveryCallback)
        val job = MainScope().launch {
            try {
                kotlinx.coroutines.delay(durationMs)
            } finally {
                try { BTS.stopScan(discoveryCallback) } catch (e: Exception) {
                    GO.drawLOG.appendAppLogs("stopScan error: ${e.message}", 2)
                }
                GO.drawLOG.appendAppLogs("Discovery finished. ${found.size} device(s).", 3)
                onComplete()
            }
        }
        return job
    }

    // --- Connect ---

    /**
     * Подключается к прибору по `GO.LEMAC` через GATT.
     *
     * **Шаги:**
     *  1. Валидация MAC (длина 17, не начинается с 'X')
     *  2. `bluetoothAdapter.getRemoteDevice(mac).connectGatt(autoConnect=false)`
     *  3. Дальше работает [gattCallback]: onConnectionStateChange → requestMtu(251) →
     *     onMtuChanged → discoverServices → onServicesDiscovered → setCharacteristicNotification
     *  4. Эмитит в [_status]: `Connecting` → `Connected`
     *
     * **Подводный камень.** Если уже было соединение (например при ping другого MAC) —
     * сначала вызвать [destroyDevice]. Иначе будет race.
     */
    @SuppressLint("MissingPermission")
    fun initLeDevice() {
        if (GO.LEMAC.length != 17 || GO.LEMAC[0] == 'X') {
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(GO.mainContext, "MAC address not setting.\nScan BT device.", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        writeBuffer = ArrayList()
        Log.d("BluZ-BT", "Accept connect...")
        GO.drawLOG.appendAppLogs("Accept connect.", 1)
        if (!BTA.isEnabled) {
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(GO.mainContext, "BlueTooth disable ?\nProgram terminated.", Toast.LENGTH_LONG).show()
                }
            }
            GO.needTerminate = true
            return
        }
        device = BTA.getRemoteDevice(GO.LEMAC.uppercase())
        GO.drawLOG.appendAppLogs("MAC:${GO.LEMAC}, Status: ${BTA.state}", 1)
        if (device == null) {
            Log.i("BluZ-BT", "Error: Device: ${GO.LEMAC} not connected.")
            GO.drawLOG.appendAppLogs("Device not connect.", 0)
            return
        }
        Log.i("BluZ-BT", "Try gatt connect.")
        GO.drawLOG.appendAppLogs("Try gatt connect.", 1)
        //_status.tryEmit(BleStatus.Connecting)
        gatt = device!!.connectGatt(GO.mainContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) {
            Log.i("BluZ-BT", "Error: Gatt create failed.")
            GO.drawLOG.appendAppLogs("Gatt create failed.", 0)
        }
    }

    // --- GATT callback ---

    @OptIn(ExperimentalUnsignedTypes::class)
    private val gattCallback = object : BluetoothGattCallback() {
        /* Accumulation state for multi-MTU frames */
        private var idxArray: Int = 0
        private var numberMTU: Int = 0
        private var dataType: Int = 0
        private var checkSum: UShort = 0u
        private var spectrumResolution: Int = 1024
        private var historyResolution: Int = 1024
        private val receiveData = UByteArray(9760)

        /* Per-frame header fields parsed from the first MTU packet */
        private var totalPulses: UInt = 0u
        private var pulsesPerSec: UInt = 0u
        private var measurementTime: UInt = 0u
        private var avgCps: Float = 0f
        private var temperature: Float = 0f
        private var batteryVoltage: Float = 0f

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("BluZ-BT", "Gatt connect success.")
                    GO.drawLOG.appendAppLogs("Gatt success. pid=${android.os.Process.myPid()}", 3)
                    if (!gatt.discoverServices()) {
                        Log.e("BluZ-BT", "Error: Discover service failed.")
                        GO.drawLOG.appendAppLogs("Discover service failed.", 0)
                    }
                    if (!gatt.requestMtu(MAX_MTU)) {
                        Log.e("BluZ-BT", "MTU set failed.")
                        GO.drawLOG.appendAppLogs("MTU set failed.", 0)
                    } else {
                        GO.initBT = true
                    }
                    if (!gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                        Log.e("BluZ-BT", "High priority set failed.")
                    }
                    _status.tryEmit(BleStatus.Connecting)
                    gatt.readRemoteRssi()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connected = false
                    writePending = false
                    GO.configDataReady = false
                    Log.i("BluZ-BT", "Disconnect.")
                    GO.drawLOG.appendAppLogs("Disconnect.", 1)
                    _status.tryEmit(BleStatus.Disconnected)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BluZ-BT", "servicesDiscovered, status $status")
            GO.drawLOG.appendAppLogs("servicesDiscovered, status $status", 1)
            writePending = false
            var charFlag = false
            for (gattService in gatt.services) {
                GO.drawLOG.appendAppLogs("${gattService.uuid}", 4)
                for (ch in gattService.characteristics) {
                    GO.drawLOG.appendAppLogs("-${ch.uuid} prop=${ch.properties}", 4)
                }
                if (gattService.uuid == BLUETOOTH_BLUZ_SERVICE) {
                    wrCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_W)
                    rdCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_R)
                    GO.drawLOG.appendAppLogs("gatt Characteristics Ok.", 3)
                    charFlag = true
                }
            }
            if (!charFlag) GO.drawLOG.appendAppLogs("Characteristics not found.", 0)
            if (!gatt.requestMtu(MAX_MTU)) {
                GO.drawLOG.appendAppLogs("Error set MTU.", 0)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.characteristic !== rdCharacteristic) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BluZ-BT", "Error: Write characteristic failed.")
                GO.drawLOG.appendAppLogs("Write characteristic failed.", 0)
            } else {
                Log.i("BluZ-BT", "Connect success.")
                GO.drawLOG.appendAppLogs("Write characteristic Ok.", 3)
                connected = true
                _status.tryEmit(BleStatus.Connected)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (!connected || wrCharacteristic == null) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluZ-BT", "write finished, status=$status")
                return
            }
            if (characteristic === wrCharacteristic) {
                Log.d("BluZ-BT", "write finished, status=$status")
                writeNext()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BluZ-BT", "mtu size $mtu, status $status")
            GO.drawLOG.appendAppLogs("mtu $mtu status $status", 3)
            if (wrCharacteristic == null) {
                GO.drawLOG.appendAppLogs("characteristic not writable - 1", 0)
                return
            }
            val writeProps = wrCharacteristic!!.properties
            if ((writeProps and (BluetoothGattCharacteristic.PROPERTY_WRITE +
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                GO.drawLOG.appendAppLogs("characteristic not writable - 2", 0)
                return
            }
            if (!gatt.setCharacteristicNotification(rdCharacteristic, true)) {
                GO.drawLOG.appendAppLogs("No notification for read characteristic", 0)
                return
            }
            val readDescriptor = rdCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD) ?: run {
                GO.drawLOG.appendAppLogs("No BLUETOOTH_LE_CCCD descriptor", 0)
                return
            }
            val readProps = rdCharacteristic!!.properties
            val descriptorValue = when {
                (readProps and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 -> {
                    GO.drawLOG.appendAppLogs("Enable read indication", 3)
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                (readProps and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 -> {
                    GO.drawLOG.appendAppLogs("Enable read notification", 3)
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                else -> {
                    GO.drawLOG.appendAppLogs("No ind/notify for read char ($readProps)", 0)
                    return
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = gatt.writeDescriptor(readDescriptor, descriptorValue)
                if (result != 0) GO.drawLOG.appendAppLogs("Failed to write descriptor", 4)
            } else {
                @Suppress("DEPRECATION")
                readDescriptor.value = descriptorValue
                if (!gatt.writeDescriptor(readDescriptor)) {
                    GO.drawLOG.appendAppLogs("Failed to write descriptor", 0)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            GO.Current_RSSI = if (status == BluetoothGatt.GATT_SUCCESS) rssi else -400
        }

        /** Receives a notification packet, assembles multi-MTU frame, parses and emits DeviceFrame. */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic !== rdCharacteristic) return
            @Suppress("DEPRECATION")
            val data = characteristic.value
            if (data == null || data.isEmpty()) return
            processIncomingPacket(data, gatt)
        }

        @SuppressLint("MissingPermission")
        /**
         * Собирает многопакетный BLE-фрейм из MTU-чанков, проверяет CRC, парсит поля и
         * эмитит [DeviceFrame] в [_deviceFrames].
         *
         * **Алгоритм:**
         *  1. Первый байт каждого MTU-пакета — порядковый номер (0 = старт нового фрейма)
         *  2. Полезная нагрузка (`data[1..]`) копируется в накопительный буфер по смещению,
         *     зависящему от номера пакета
         *  3. По достижению ожидаемой длины (зависит от [DeviceFrame.frameType]) проверяется
         *     CRC (uint16 = `sum(bytes[0..len-3]) & 0xFFFF`, хранится в последних 2 байтах)
         *  4. Парсится header (58 байт), dosimeterData (512 байт), log (50×5 байт),
         *     spectrum/history (resolution×2 байт)
         *  5. [decodeHardwareConfig] извлекает [HardwareConfig] из header'а
         *  6. `_deviceFrames.tryEmit(frame)` или suspend-emit — на главном потоке
         *
         * **Восстановление от сбоев.** Если порядковый номер не совпадает с ожидаемым —
         * текущая сборка отбрасывается, ждём следующего пакета с номером 0.
         */
        private fun processIncomingPacket(data: ByteArray, gatt: BluetoothGatt) {
            GO.drawLOG.appendAppLogs("onCharacteristicChanged", 5)
            /* First packet of a new frame starts with <B> (60, 66, 62) */
            if (data[0].toUByte() == 60.toUByte()
                && data[1].toUByte() == 66.toUByte()
                && data[2].toUByte() == 62.toUByte()
            ) {
                dataType = data[3].toInt()
                idxArray = 0
                checkSum = 0u
                when (dataType) {
                    0 -> { numberMTU = 6;  spectrumResolution = 0; historyResolution = 0 }
                    1 -> { numberMTU = 16; spectrumResolution = 1024; historyResolution = 0 }
                    2 -> { numberMTU = 23; spectrumResolution = 2048; historyResolution = 0 }
                    3 -> { numberMTU = 40; spectrumResolution = 4096; historyResolution = 0 }
                    4 -> { numberMTU = 16; spectrumResolution = 0; historyResolution = 1024 }
                    5 -> { numberMTU = 23; spectrumResolution = 0; historyResolution = 2048 }
                    6 -> { numberMTU = 40; spectrumResolution = 0; historyResolution = 4096 }
                }
                totalPulses = ByteBuffer.wrap(data, 10, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
                pulsesPerSec = ByteBuffer.wrap(data, 14, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
                measurementTime = (data[18].toUByte().toUInt()
                    or (data[19].toUByte().toUInt() shl 8)
                    or (data[20].toUByte().toUInt() shl 16)
                    or (data[21].toUByte().toUInt() shl 24))
                avgCps = round(java.lang.Float.intBitsToFloat(
                    ByteBuffer.wrap(data, 22, 4).order(ByteOrder.LITTLE_ENDIAN).int) * 100) / 100
                temperature = round(java.lang.Float.intBitsToFloat(
                    ByteBuffer.wrap(data, 26, 4).order(ByteOrder.LITTLE_ENDIAN).int))
                batteryVoltage = round(java.lang.Float.intBitsToFloat(
                    ByteBuffer.wrap(data, 30, 4).order(ByteOrder.LITTLE_ENDIAN).int) * 100) / 100
                Log.d("BluZ-BT", "Frame start: type=$dataType mtu=$numberMTU pulses=$totalPulses")
            }

            numberMTU--
            /* Copy payload bytes (skip 5-byte footer; skip last 7 bytes on the final MTU for checksum) */
            val endOfData = if (numberMTU == 0) data.size - 7 else data.size - 5
            var idx = 0
            while (idx <= endOfData) {
                val b = data[idx++].toUByte()
                checkSum = (checkSum + b).toUShort()
                receiveData[idxArray++] = b
            }

            if (numberMTU > 0) return   // wait for remaining MTU packets

            /* Verify checksum */
            val receivedCS = (data[242].toUByte().toUInt() or (data[243].toUByte().toUInt() shl 8)).toUShort()
            if (receivedCS != checkSum) {
                Log.e("BluZ-BT", "CS mismatch: got $receivedCS calculated $checkSum")
                GO.drawLOG.appendAppLogs("CS incorrect: $receivedCS vs $checkSum", 0)
                return
            }
            GO.drawLOG.appendAppLogs("CS correct: $checkSum", 3)
            gatt.readRemoteRssi()

            /* Decode hardware config from receiveData */
            val hw = decodeHardwareConfig(receiveData)

            /* Decode dosimeter histogram (512 uint16 values, offset 100 bytes) */
            val dosimeterData = DoubleArray(512) { i ->
                val base = 100 + i * 2
                (receiveData[base].toUInt() or (receiveData[base + 1].toUInt() shl 8)).toDouble()
            }

            /* Decode log entries (50 entries, 6 bytes each, offset 1124 bytes) */
            val logEntries = (0 until 50).map { i ->
                val base = 1124 + i * 6
                val tm = receiveData[base].toUInt() or
                        (receiveData[base + 1].toUInt() shl 8) or
                        (receiveData[base + 2].toUInt() shl 16) or
                        (receiveData[base + 3].toUInt() shl 24)
                val act = (receiveData[base + 4] + receiveData[base + 5]).toUByte()
                LogEntry(tm, act)
            }

            /* Decode spectrum / history (offset 1424 bytes) */
            val mult = hw.bitsChan.toDouble() / 65535.0 * ln(2.0)
            val spectrumData: DoubleArray? = if (spectrumResolution > 0) {
                DoubleArray(spectrumResolution) { i ->
                    val base = 1424 + i * 2
                    val compressed = (receiveData[base].toUInt() and 0xFFu) or
                            ((receiveData[base + 1].toUInt() and 0xFFu) shl 8)
                    exp(compressed.toDouble() * mult) - 1.0
                }
            } else null

            val historyData: DoubleArray? = if (historyResolution > 0) {
                DoubleArray(historyResolution) { i ->
                    val base = 1424 + i * 2
                    round(exp(
                        ((receiveData[base].toUInt() or (receiveData[base + 1].toUInt() shl 8)).toDouble()) * mult
                    ) - 1.0)
                }
            } else null

            val overload = (receiveData[97].toUByte() and 8.toUByte()) != 0.toUByte()

            val frame = DeviceFrame(
                frameType = dataType,
                totalPulses = totalPulses,
                pulsesPerSec = pulsesPerSec,
                avgCps = avgCps,
                measurementTime = measurementTime,
                temperature = temperature,
                batteryVoltage = batteryVoltage,
                dosimeterData = dosimeterData,
                logEntries = logEntries,
                spectrumData = spectrumData,
                historyData = historyData,
                hw = hw,
                overload = overload
            )

            GO.configDataReady = true
            _deviceFrames.tryEmit(frame)
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        /**
         * Извлекает [HardwareConfig] из header'а собранного фрейма.
         *
         * Парсит коэффициенты полинома (float16 — конвертируется в Float), битовую
         * конфигурацию (LED/звук/вибро на каждом из 3 уровней), значения АЦП-порогов
         * (`comparator` — `((d[53] shl 8) or d[52]) and 0x3FFu` — 10-битное unsigned),
         * время и счётчик спектрометра.
         *
         * @param d Уже собранный буфер фрейма как [UByteArray].
         * @return [HardwareConfig] для подстановки в [DeviceFrame.hw].
         */
        private fun decodeHardwareConfig(d: UByteArray): HardwareConfig {
            val flags0 = d[60].toUInt()
            val flags1 = d[61].toUInt()
            val specTime = ((d[89].toULong() shl 24) or (d[88].toULong() shl 16) or
                    (d[87].toULong() shl 8) or d[86].toULong()).toUInt()
            val specPulse = ((d[93].toULong() shl 24) or (d[92].toULong() shl 16) or
                    (d[91].toULong() shl 8) or d[90].toULong()).toUInt()
            var bitsChan = d[96].toUByte()
            if (bitsChan < 16u || bitsChan > 32u) bitsChan = 20u
            var sampleTime = d[97].toUByte() and 7.toUByte()
            if (sampleTime > 7u) sampleTime = 1u
            return HardwareConfig(
                ledKvant = (flags0 and 1u) != 0u,
                soundKvant = (flags0 and 2u) != 0u,
                soundLevel1 = (flags0 and 4u) != 0u,
                soundLevel2 = (flags0 and 8u) != 0u,
                soundLevel3 = (flags0 and 16u) != 0u,
                vibroLevel1 = (flags0 and 32u) != 0u,
                vibroLevel2 = (flags0 and 64u) != 0u,
                vibroLevel3 = (flags0 and 128u) != 0u,
                autoStartSpectrometer = (flags1 and 1u) != 0u,
                click10 = (flags1 and 2u) != 0u,
                led10 = (flags1 and 4u) != 0u,
                level1 = (d[54].toUInt() or (d[55].toUInt() shl 8)).toInt(),
                level2 = (d[56].toUInt() or (d[57].toUInt() shl 8)).toInt(),
                level3 = (d[58].toUInt() or (d[59].toUInt() shl 8)).toInt(),
                cps2ur = ByteBuffer.wrap(d.asByteArray(), 46, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                hVoltage = (((d[51].toUInt() shl 8) or d[50].toUInt()) and 0x3FFu).toUShort(),
                comparator = (((d[53].toUInt() shl 8) or d[52].toUInt()) and 0x3FFu).toUShort(),
                //coef1024A = ByteBuffer.wrap(d.asByteArray(), 34, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                //coef1024B = ByteBuffer.wrap(d.asByteArray(), 38, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                //coef1024C = ByteBuffer.wrap(d.asByteArray(), 42, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                //coef2048A = ByteBuffer.wrap(d.asByteArray(), 62, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                coef4096D = ByteBuffer.wrap(d.asByteArray(), 66, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                coef4096E = ByteBuffer.wrap(d.asByteArray(), 70, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                coef4096A = ByteBuffer.wrap(d.asByteArray(), 74, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                coef4096B = ByteBuffer.wrap(d.asByteArray(), 78, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                coef4096C = ByteBuffer.wrap(d.asByteArray(), 82, 4).order(ByteOrder.LITTLE_ENDIAN).float,
                acquireValue = ((d[95].toUInt() shl 8) or d[94].toUInt()).toUShort(),
                bitsChan = bitsChan,
                sampleTime = sampleTime,
                spectrometerTime = specTime,
                spectrometerPulse = specPulse
            )
        }
    }

    // --- Destroy ---

    /**
     * Принудительный разрыв GATT-соединения: `gatt?.disconnect() + gatt?.close()`,
     * обнуление references, очистка очереди записи. Эмитит `BleStatus.Disconnected`.
     *
     * Вызывается из [intervalTimer] при потере связи (для последующего реконнекта)
     * и из [SettingsFragment.pingDevice] при смене целевого MAC.
     */
    @SuppressLint("MissingPermission")
    fun destroyDevice() {
        GO.drawLOG.appendAppLogs("BLE destroy.", 4)
        connected = false
        _status.tryEmit(BleStatus.Disconnected)
        if (gatt != null) {
            GO.drawLOG.appendAppLogs("gatt close.", 4)
            gatt!!.disconnect()
            gatt!!.close()
            gatt = null
            device = null
            writeBuffer?.clear()
        }
    }

    // --- Write queue (fixed: no double-remove bug) ---

    /**
     * Достаёт следующий чанк из [writeBuffer] и шлёт через `wrCharacteristic.setValue + gatt.writeCharacteristic`.
     *
     * Очерёдность критична: BLE поддерживает только одну операцию записи за раз.
     * Очередной [writeNext] вызывается из `onCharacteristicWrite` callback (после подтверждения).
     */
    @SuppressLint("MissingPermission")
    private fun writeNext() {
        val data: ByteArray?
        synchronized(writeBuffer!!) {
            data = if (writeBuffer!!.isNotEmpty()) {
                writePending = true
                writeBuffer!!.removeAt(0)
            } else {
                writePending = false
                null
            }
        }
        data?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt!!.writeCharacteristic(wrCharacteristic!!, it, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            } else {
                @Suppress("DEPRECATION")
                wrCharacteristic!!.value = it
                @Suppress("DEPRECATION")
                if (!gatt!!.writeCharacteristic(wrCharacteristic)) {
                    Log.e("BluZ-BT", "Write Characteristic error")
                }
            }
            Log.d("BluZ-BT", "write started from next, len=${it.size}")
        }
    }

    /** Sends a command packet to the device, calculating checksum. */
    @OptIn(ExperimentalUnsignedTypes::class)
    /**
     * Отправляет управляющую команду прибору. Заполняет header `sendBuffer`:
     *  - `[0..2]` = `<S>` (0x3C 0x53 0x3E) — маркер начала
     *  - `[3]` = `cmd` — код команды
     *  - `[4..]` = параметры (заполняются caller-ом ДО вызова)
     *  - `[242..243]` = CRC (sum от 0..241, little-endian uint16)
     *
     * Шлёт через [write] (с разбиением на MTU-чанки).
     *
     * **Коды команд** (см. также DEVELOPER_GUIDE.md раздел 5.3):
     *  - 0 — Стоп, 1 — Очистить спектр, 2 — Старт/стоп спектрометра, 3 — Сброс дозиметра
     *  - 4 — Очистить логи, 5 — Запрос истории, 6 — Найти прибор (звук+вибро)
     *  - 7 — Передать реальное напряжение АКБ (параметр: float в `sendBuffer[4..7]`)
     *  - 8 — Очистить историю
     */
    fun sendCommand(cmd: UByte) {
        sendBuffer[0] = '<'.code.toUByte()
        sendBuffer[1] = 'S'.code.toUByte()
        sendBuffer[2] = '>'.code.toUByte()
        sendBuffer[3] = cmd
        var cs: UShort = 0u
        for (i in 0..241) cs = (cs + sendBuffer[i]).toUShort()
        sendBuffer[242] = (cs and 255u).toUByte()
        sendBuffer[243] = ((cs.toUInt() shr 8) and 255u).toUByte()
        write(sendBuffer.toByteArray())
    }

    /**
     * Разбивает массив байт на MTU-чанки (по 248 байт = MTU-3 для GATT-заголовка)
     * и кладёт в [writeBuffer]. Если запись ещё не идёт ([writePending] == false) — стартует
     * [writeNext]. Дальше очередь обрабатывается через `onCharacteristicWrite` callback.
     *
     * @throws IOException если соединение не установлено или TX-характеристика null.
     */
    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        if (!connected || wrCharacteristic == null) return
        var data0: ByteArray?
        synchronized(writeBuffer!!) {
            data0 = if (data.size <= payloadSize) data else data.copyOfRange(0, payloadSize)
            if (!writePending && writeBuffer!!.isEmpty()) {
                writePending = true
            } else {
                writeBuffer!!.add(data0!!)
                Log.d("BluZ-BT", "write queued, len=${data0!!.size}")
                data0 = null
            }
            if (data.size > payloadSize) {
                var i = 1
                while (i * payloadSize < data.size) {
                    val from = i * payloadSize
                    val to = minOf(from + payloadSize, data.size)
                    writeBuffer!!.add(data.copyOfRange(from, to))
                    i++
                }
            }
        }
        data0?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt!!.writeCharacteristic(wrCharacteristic!!, it, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            } else {
                @Suppress("DEPRECATION")
                wrCharacteristic!!.setValue(it)
                @Suppress("DEPRECATION")
                if (!gatt!!.writeCharacteristic(wrCharacteristic)) {
                    Log.d("BluZ-BT", "Write Characteristic error")
                }
            }
            Log.d("BluZ-BT", "write started, len=${it.size}")
        }
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("BluZ-BT", "Broadcast.")
        }
    }
}
