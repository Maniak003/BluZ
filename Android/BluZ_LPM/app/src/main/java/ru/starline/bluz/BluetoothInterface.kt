package ru.starline.bluz

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.Global.getString
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import kotlinx.coroutines.DefaultExecutor.thread
import java.io.IOException
import java.util.Arrays
import java.util.UUID
import ru.starline.bluz.globalObj
import kotlin.concurrent.thread
import kotlin.math.round
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import java.lang.Math.pow
import java.lang.Math.sqrt
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.system.exitProcess


/*
 The following 128bits UUIDs have been generated from the random UUID
 generator:
 0000FE80CC7A482A984A7F2ED5B3E58F: Service 128bits UUID
 0000FE818E2245419D4C21EDAE82ED19: Characteristic 128bits UUID
 0000FE828E2245419D4C21EDAE82ED19: Characteristic 128bits UUID

#define COPY_BLUZ_UUID(uuid_struct)     COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x80,0xcc,0x7a,0x48,0x2a,0x98,0x4a,0x7f,0x2e,0xd5,0xb3,0xe5,0x8f)
#define COPY_RX_UUID(uuid_struct)       COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x81,0x8e,0x22,0x45,0x41,0x9d,0x4c,0x21,0xed,0xae,0x82,0xed,0x19)
#define COPY_TX_UUID(uuid_struct)       COPY_UUID_128(uuid_struct,0x00,0x00,0xfe,0x82,0x8e,0x22,0x45,0x41,0x9d,0x4c,0x21,0xed,0xae,0x82,0xed,0x19)

BLUZ_UUID : 0000fe80-cc7a-482a-984a-7f2ed5b3e58f
RX_UUID   : 0000fe81-8e22-4541-9d4c-21edae82ed19
TX_UUID   : 0000fe82-8e22-4541-9d4c-21edae82ed19
*/

/**
 * Created by ed on 20,июнь,2024
 */
class BluetoothInterface(tv: TextView) {
    private var LEMACADDRESS: String = ""
    private val BTM = GO.mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val BTA = BTM.adapter
    private val BTS = BTA.bluetoothLeScanner
    private var indBT: TextView = tv
    lateinit var txtMACADDRESS: EditText
    //lateinit var startButton: Button
    //private var delegate: BLUZDelegate = BLUZDelegate()
    //private var delegate: DeviceDelegate = null
    private var gatt: BluetoothGatt? = null
    private var device: BluetoothDevice? = null
    private var writeBuffer: ArrayList<ByteArray>? = null
    private val MAX_MTU: Int = 251
    private val payloadSize: Int = MAX_MTU - 3
    private var writePending = false
    var connected: Boolean = false
    private val BLUETOOTH_LE_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val BLUETOOTH_BLUZ_SERVICE: UUID = UUID.fromString("0000fe80-cc7a-482a-984a-7f2ed5b3e58f")
    private val BLUETOOTH_BLUZ_CHAR_R: UUID = UUID.fromString("0000fe81-8e22-4541-9d4c-21edae82ed19")
    private val BLUETOOTH_BLUZ_CHAR_W: UUID = UUID.fromString("0000fe82-8e22-4541-9d4c-21edae82ed19")
    private var rdCharacteristic: BluetoothGattCharacteristic? = null
    private var wrCharacteristic:BluetoothGattCharacteristic? = null
    @OptIn(ExperimentalUnsignedTypes::class)
    public var receiveBuffer = UByteArray(255)
    @OptIn(ExperimentalUnsignedTypes::class)
    public var sendBuffer = UByteArray(255)
    public var testIdx: Int = 0
    public var testIdx2: Int = 0

    /*
    *      Обработчик окончания сканирования, здесь получим результат
    */
    val leScanCallback = object: ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.getDevice() == null || TextUtils.isEmpty(result.getDevice().getName())) {

            } else {
                Log.d("BluZ-BT", result.device.name + " " + result.device.address)
                Log.d("BluZ-BT", result.scanRecord.toString())
                if (result.device.name == "BluZ") {
                    LEMACADDRESS = result.device.address
                    txtMACADDRESS.setText(result.device.address)
                    GO.adapter.fragment.let {
                        GO.scanButton.setTextColor(GO.mainContext.getResources().getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.scanButton.setText(GO.mainContext.getString(R.string.textScan))
                    }
                    BTS.stopScan(this)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(textMAC: EditText/*, startBTN: Button*/) {
        val scanFilter: ScanFilter = ScanFilter.Builder().setDeviceName(GO.propCfgBLEDeviceName).build()
        val scanSetting: ScanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val filterList = ArrayList<ScanFilter>()
        filterList.add(scanFilter)
        //GO.adapter.fragment.let {
        //    it.btnSpecterSS
        //}
        txtMACADDRESS = textMAC // Установить текст для редактирования
        //startButton = startBTN
        MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
            withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                textMAC.setText(GO.mainContext.getString(R.string.defaultMAC))
            }
        }
        //BTS.startScan(leScanCallback)
        if (!BTA.isEnabled()) {
            MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                    Toast.makeText(GO.mainContext, "BlueTooth disable ?\nProgram terminate.", Toast.LENGTH_LONG ).show()
                    GO.adapter.fragment.let {
                        GO.scanButton.setTextColor(GO.mainContext.getResources().getColor(R.color.buttonTextColor, GO.mainContext.theme))
                        GO.scanButton.setText(GO.mainContext.getString(R.string.textScan))
                    }
                    MainScope().launch {
                        delay(3000L)
                        //finishAndRemoveTask()
                        exitProcess(-1)
                    }
                }
            }
        } else {
            BTS.startScan(filterList, scanSetting, leScanCallback)
            Log.d("BluZ-BT", "LE scanning.")
        }
    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        BTS.stopScan(leScanCallback)
    }

    /*
     *    Start BLE.
     */
    @SuppressLint("MissingPermission")
    fun initLeDevice() {
        if ((GO.LEMAC.length == 17) &&  GO.LEMAC[0] != 'X') {
            MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                    //GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop)
                    //GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.buttonTextColor))
                }
            }
            writeBuffer = ArrayList() // Буфер для передачи.
            Log.d("BluZ-BT", "Accept connect...")
            if (!BTA.isEnabled()) {
                MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                    withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                        Toast.makeText(GO.mainContext, "BlueTooth disable ?\nProgram terminated.", Toast.LENGTH_LONG ).show()
                    }
                }
                GO.needTerminate = true
            }
            device = BTA.getRemoteDevice(GO.LEMAC) // Подключаемся по MAC адресу.
            Log.d("BluZ-BT", "Status: " + BTA.getState());
            if (device == null) {
                var tmpmac = GO.LEMAC
                Log.i("BluZ-BT", "Error: Device: $tmpmac not connected.")
                return
            } else {
                Log.i("BluZ-BT", "Try gatt connect.");
                gatt = device!!.connectGatt(GO.mainContext,false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                if (gatt == null) {
                    Log.i("BluZ-BT", "Error: Gatt create failed.");
                    //finish()
                }
            }
        } else {
            MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                    Toast.makeText(GO.mainContext,"MAC address not setting.\nScan BT device.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Сюда попадаем после завершения работы connectGatt
    @OptIn(ExperimentalUnsignedTypes::class)
    private val gattCallback = object : BluetoothGattCallback() {
        private var idxArray: Int = 0
        private var numberMTU: Int = 0
        private var dataType: Int = 0
        private var indexData: Int = 0
        private var endOfData: Int = 0
        private var checkSumm: UShort = 0u
        private var tmpBattery: Float = 0.0f
        private var tmpTemperature: Float = 0.0f
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    /* Прибор подключен, меняем цвет индикатора. */
                    MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                        withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                            tv.setBackgroundColor(GO.mainContext.getColor(R.color.Yellow))
                            //if (GO.btnSpecterSSisInit) {
                                //GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop2)
                                //GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.Red))
                            //}
                        }
                    }
                    Log.i("BluZ-BT", "Gatt connect success.")
                    if (!gatt.discoverServices()) {
                        Log.e("BluZ-BT", "Error: Discover service failed.")
                        //finish()
                    }
                    if (!gatt.requestMtu(MAX_MTU)) {  // Изменяем MTU
                        Log.e("BluZ-BT", "MTU set failed.")
                        //finish()
                    } else {
                        GO.initBT = true
                    }
                    /* Ускоряем обмен данными */
                    if( !gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                        Log.e("BluZ-BT", "Hi priority set failed.")
                    } else {
                        Log.i("BluZ-BT", "Hi priority set Ok.")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    /* Прибор отключен, меняем цвет индикатора */
                    MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                        withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                            tv.setBackgroundColor(GO.mainContext.getColor(R.color.Red))
                            if (GO.btnSpecterSSisInit) {
                                //GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop)
                                //GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.buttonTextColor))
                            }
                        }
                    }
                    connected = false
                    writePending = false
                    GO.configDataReady = false
                    Log.i("BluZ-BT", "Disconnect.")
                }
            }
        }

        // Сюда попадаем после выполнения gatt.discoverServices
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BluZ-BT", "servicesDiscovered, status $status")
            //var sync = true
            writePending = false
            Log.d("BluZ-BT", "Set gatt Characteristics.")
            for (gattService in gatt.services) {
                if (gattService.uuid == BLUETOOTH_BLUZ_SERVICE) {
                    wrCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_W)
                    rdCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_R)
                }
            }
            if (!gatt.requestMtu(MAX_MTU)) Log.d("BluZ-BT", "Error set MTU.")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.characteristic === rdCharacteristic) {
                Log.i("BluZ-BT", "writing read characteristic descriptor finished, status=$status")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BluZ-BT", "Error: Write characteristic failed.")
                } else {
                    Log.i("BluZ-BT", "Connect success.")
                    connected = true
                }
            }
        }

        override fun onCharacteristicWrite( gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int ) {
            if ( !connected || wrCharacteristic == null) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluZ-BT", "write finished, status=$status");
                return
            }
            //delegate!!.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic === wrCharacteristic) { // NOPMD - test object identity
                Log.d("BluZ-BT", "write finished, status=$status");
                writeNext()
            }
        }

        @SuppressLint("MissingPermission")
        @Deprecated("Deprecated in Java")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BluZ-BT", "mtu size $mtu, status $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var payloadSize: Int = mtu - 3
                Log.d("BluZ-BT", "payload size $payloadSize")
            }
            if (wrCharacteristic == null) {
                Log.e("BluZ-BT", "Error: characteristic not writable - 1")
                return
            } else {
                val writeProperties = wrCharacteristic!!.properties
                if ((writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE +
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0 ) {
                    Log.e("BluZ-BT", "Error: characteristic not writable - 2")
                    return
                }
            }
            if (!gatt.setCharacteristicNotification(rdCharacteristic, true)) {
                Log.e("BluZ-BT", "Error: no notification for read characteristic")
                return
            }
            val readDescriptor = rdCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD)
            if (readDescriptor == null) {
                Log.e("BluZ-BT", "Error: no BLUETOOTH_LE_CCCD descriptor for read characteristic")
                return
            }
            val readProperties = rdCharacteristic!!.properties
            if ((readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                Log.d("BluZ-BT", "enable read indication")
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            } else if ((readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                Log.d("BluZ-BT", "enable read notification")
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                Log.e(
                    "BluZ-BT",
                    "Error: no indication/notification for read characteristic ($readProperties)"
                )
                return
            }

            Log.d("BluZ-BT", "writing read characteristic descriptor")
            if (!gatt.writeDescriptor(readDescriptor)) {
                Log.w("BluZ-BT", "Characteristic CCCD descriptor not writable")
            }
            // continues asynchronously in onDescriptorWrite()
        }

        /*
        *      Прием данных
        */
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            //delegate!!.onCharacteristicChanged(gatt, characteristic)
            if (characteristic == rdCharacteristic) { // NOPMD - test object identity
                //val data = readCharacteristic!!.value
                val data = rdCharacteristic!!.value
                /*
                     Заполнение массива
                */
                if (data.isNotEmpty()) {
                    /*
                    *   Ищем стартовую последовательность <B> (60, 66, 62)
                    *   Разбираем заголовок
                    */
                    //testIdx2 = 0;
                    if ((data[0].toUByte() == 60.toUByte())
                       && (data[1].toUByte() == 66.toUByte())
                       && (data[2].toUByte() == 62.toUByte())) {
                        testIdx = 0
                        dataType = data[3].toInt()      // Тип передаваемых данных
                        idxArray = 0                    // Индекс результирующего массива с данными
                        indexData = 100                 // Смещение на начало данных - длина заголовка в байтах
                        when (dataType) {
                            /* Данные дозиметра и лога */
                            0 -> {
                                numberMTU = 6      // Количество пакетов (MTU) для передачи
                                endOfData = data.size - 5
                            }
                            /* Данные дозиметра, лога и спектра 1024 */
                            1 -> {
                                numberMTU = 16     // Количество пакетов (MTU) для передачи
                                GO.specterType = 0 // Определяет размер по горизонтали
                                GO.drawSPECTER.ResolutionSpectr = 1024
                                endOfData = data.size - 5
                            }
                            /* Данные дозиметра, лога и спектра 2048 */
                            2 -> {
                                numberMTU = 23     // Количество пакетов (MTU) для передачи
                                GO.specterType = 1
                                GO.drawSPECTER.ResolutionSpectr = 2048
                                endOfData = data.size - 5
                            }
                            /* Данные дозиметра, лога и спектра 4096 */
                            3 -> {
                                numberMTU = 40     // Количество пакетов (MTU) для передачи
                                GO.specterType = 2
                                GO.drawSPECTER.ResolutionSpectr = 4096
                                endOfData = data.size - 5
                            }
                        }
                        GO.PCounter = (data[10].toUByte() + data[11].toUByte() * 256u + data[12].toUByte() * 65536u + data[13].toUByte() * 16777216u).toUInt()
                        GO.pulsePerSec  = (data[14].toUByte() + data[15].toUByte() * 256u + data[16].toUByte() * 65536u + data[17].toUByte() * 16777216u).toUInt()
                        GO.messTm =  (data[18].toUByte() + data[19].toUByte() * 256u + data[20].toUByte() * 65536u + data[21].toUByte() * 16777216u).toUInt()
                        var tmpInt = (data[22].toUByte() + data[23].toUByte() * 256u + data[24].toUByte() * 65536u + data[25].toUByte() * 16777216u).toUInt()
                        GO.cps = round(java.lang.Float.intBitsToFloat(tmpInt.toInt()) * 100) / 100
                        tmpInt = (data[26].toUByte() + data[27].toUByte() * 256u + data[28].toUByte() * 65536u + data[29].toUByte() * 16777216u).toUInt()
                        GO.tempMC = round(java.lang.Float.intBitsToFloat(tmpInt.toInt()))
                        tmpInt = (data[30].toUByte() + data[31].toUByte() * 256u + data[32].toUByte() * 65536u + data[33].toUByte() * 16777216u).toUInt()
                        GO.battLevel = round(java.lang.Float.intBitsToFloat(tmpInt.toInt()) * 100) / 100
                        var pulseCounter = GO.PCounter
                        Log.d("BluZ-BT", "Start detect. Size: $numberMTU, Type: $dataType, Pulse: $pulseCounter, Temp:" + GO.tempMC.toString() + ", Volt:" + GO.battLevel .toString())

                        /*
                           * Значения заголовка и формат данных для передачи
                           *
                           * Управляющие данные uint8_t
                           * 0,1,2 - Маркер начала <B>
                           * 3 -  Тип передачи
                           *    0    - Данные дозиметра и лог (6 MTU)
                           *    1    - Данные дозиметра, лог и спектр 1024 (14 MTU)
                           *    2    - Данные дозиметра, лог и спектр 2048 (23 MTU)
                           *    3    - Данные дозиметра, лог и спектр 4096 (39 MTU)
                           *    4	 - Данные дозиметра, лог и исторический спектр 1024
                           *    5	 - Данные дозиметра, лог и исторический спектр 2048
                           *    6	 - Данные дозиметра, лог и исторический спектр 4096
                           * 4,5,6,7 - Зарезервировано
                           *
                           * Статистика и когфигурация uint16_t
                           *
                           * 5, 6   - Общее число импульсов от начала измерения uint32_t
                           * 7, 8   - Число импульсов за последнюю секунду. uint32_t
                           * 9, 10  - Общее время в секундах uint32_t
                           * 11, 12 - Среднее количество имльсов в секунду. float
                           * 13, 14 - Температура в гр. цельсия
                           * 15, 16 - Напряжение батареи в вольтах
                           * 17, 18 - Коэффициент полинома A для 1024 каналов
                           * 19, 20 - Коэффициент полинома B для 1024 каналов
                           * 21, 22 - Коэффициент полинома C для 1024 каналов
                           * 23, 24 - Коэффициент пересчета uRh/cps
                           * 25     - Значение высокого напряжения
                           * 26     - Значение порога компаратора
                           * 27     - Уровень первого порога
                           * 28     - Уровень второго порога
                           * 29     - Уровень третьего порога
                           * 30     - Битовая конфигурация
                           *     0 - Индикация кванта светодиодом
                           *     1 - Индикация кванта звуком
                           *     2 - Звук первого уровня
                           *     3 - Звук второго уровня
                           *     4 - Звук третьего уровня
                           *     5 - Вибро первого уровня
                           *     6 - Вибро второго уровня
                           *     7 - Вибро третьего уровня
                           *     8 - Автозапуск спектрометра при включении
                           * 31, 32 - Коэффициент полинома A для 2048 каналов
                           * 33, 34 - Коэффициент полинома B для 2048 каналов
                           * 35, 36 - Коэффициент полинома C для 2048 каналов
                           * 37, 38 - Коэффициент полинома A для 4096 каналов
                           * 39, 40 - Коэффициент полинома B для 4096 каналов
                           * 41, 42 - Коэффициент полинома C для 4096 каналов
                           * 43, 44 - Время работы спектрометра в секундах
                           * 45, 46 - Количество импульсов от спектрометра
                           * 49  - Конец заголовка
                           *
                           * 50  - Данные дозиметра
                           * 572 - Данные лога
                           * 652 - Данные спектра
                           *
                           * 652 или 1676 или 2700 или 4748 - Контрольная сумма.
                         *
                         */

                        checkSumm = 0u
                    }
                    numberMTU--
                    indexData = 0                       // Заголовок прочитан, данные начинаются с начала.
                    endOfData = if (numberMTU == 0) {           // Последний буфер, не считаем последние 2 байта
                        data.size - 7
                    } else {
                        data.size - 5
                    }
                    /*
                    Log.d("BluZ-BT", "Receive: " + data.size.toString()+ " real size: " + testIdx2.toString() + " "
                            + data[48] + " idx:" + idxArray + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[240] + " " + data[241] + " " + data[242].toUByte() + " " + data[243].toUByte()
                            + " numMTU: " + numberMTU.toString() + " indexData: " + indexData.toString() + " endOfData: " + endOfData.toString())
                    */
                    var idx = indexData
                    var d00: UByte
                    while (idx <= endOfData) {
                        d00 = data[idx++].toUByte()
                        checkSumm =  (checkSumm + d00).toUShort()
                        GO.receiveData[idxArray++] = d00
                    }

                    //Log.d("BluZ-BT", "Receive: " + data.size.toString()+ " real size: " + testIdx2.toString() + " "
                    //        + data[48] + " idx:" + idxArray + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[240] + " " + data[241] + " " + data[242].toUByte() + " " + data[243].toUByte()
                    //        + " numMTU: " + numberMTU.toString() + " indexData: " + indexData.toString() + " endOfData: " + endOfData.toString())


                    if (numberMTU == 0) {       // Прием последнего блока
                        var tmpCS: UShort
                        tmpCS = (data[242].toUByte() + (data[243].toUByte() * 256u)).toUShort()
                        if (tmpCS == checkSumm /*|| true*/) {
                            GO.configDataReady = true
                            Log.d("BluZ-BT", "CS - correct: $checkSumm")
                            /* Накопление массива закончено можно вызывать обновление экрана */
                            MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                                withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                                    /* Контрольная сумма совпала, меняем цвет индикатора */
                                    tv.setBackgroundColor(GO.mainContext.getColor(R.color.Green))
                                    
                                    /* Количество импульсов и время набора спектра */
                                    GO.spectrometerTime = GO.receiveData[86] + GO.receiveData[87] * 256u + GO.receiveData[88] * 65536u + GO.receiveData[89] * 16777216u
                                    GO.spectrometerPulse = GO.receiveData[90] + GO.receiveData[91] * 256u + GO.receiveData[92] * 65536u + GO.receiveData[93] * 16777216u
                                    /*
                                    *  Вывод статистики
                                    *  Перевод в дни, часы, минуты, секунды
                                    */
                                    GO.showStatistics()

                                    /* Перевод CPS в uRh */
                                    var doze: Float =  Math.round(GO.pulsePerSec.toFloat() * GO.propCPS2UR * 100.0f) / 100.0f
                                    var avgDoze : Float = (Math.round(GO.cps * GO.propCPS2UR * 100.0f) / 100.0f).toFloat()
                                    GO.txtStat3.setText("CPS:${GO.pulsePerSec} ($doze uRh) Avg:$avgDoze uRh")

                                    /*
                                    *   Загрузка данных спектрометра дозиметра и логов из массивов.
                                    */
                                    var iii = 100        // Начало данных дозиметра (байты).
                                    var d0 : UByte
                                    var d1 : UByte
                                    var d2 : UByte
                                    var d3 : UByte
                                    var jjj = 0
                                    while (jjj < 512) {  // Перегружаем данные дозиметра
                                        d0 = GO.receiveData[iii++]
                                        d1 = GO.receiveData[iii++]
                                        GO.drawDOZIMETER.dozimeterData[jjj++] = (d0 + (d1 * 256u)).toDouble()
                                    }
                                    if (GO.initDOZ) {
                                        GO.drawDOZIMETER.Init()
                                        if (GO.drawDOZIMETER.dozVSize > 0 &&  GO.drawDOZIMETER.dozHSize > 0) {
                                            GO.drawDOZIMETER.clearDozimeter()
                                            GO.drawDOZIMETER.redrawDozimeter()
                                        }
                                    }

                                    /*
                                    *   Логи
                                    */
                                    iii = 1124                                                  // Смещение от начала буфера.
                                    jjj = 0
                                    while (jjj < 50) {                                         // Перегружаем данные логов
                                        d0 = GO.receiveData[iii++]                              // 0 байт временной метки
                                        d1 = GO.receiveData[iii++]                              // 1 байт временной метки
                                        d2 = GO.receiveData[iii++]                              // 2 байт временной метки
                                        d3 = GO.receiveData[iii++]                              // 3 байт временной метки
                                        GO.drawLOG.logData[jjj].tm = d0.toUInt() + (d1.toUInt() shl 8) + (d2.toUInt() shl 16) + (d3.toUInt() shl 24)
                                        GO.drawLOG.logData[jjj++].act = (GO.receiveData[iii++] + GO.receiveData[iii++]).toUByte()  // id события
                                    }
                                    if (GO.drawLOG.logsDrawIsInit) {
                                        GO.drawLOG.updateLogs()
                                    }
                                    /*
                                    *   Чтение параметров прибора
                                    */
                                    GO.HWpropLedKvant = ((GO.receiveData[60] and 1.toUByte()) != 0.toUByte())
                                    GO.HWpropSoundKvant = ((GO.receiveData[60] and 2.toUByte()) != 0.toUByte())
                                    GO.HWpropSoundLevel1 = ((GO.receiveData[60] and 4.toUByte()) != 0.toUByte())
                                    GO.HWpropSoundLevel2 = ((GO.receiveData[60] and 8.toUByte()) != 0.toUByte())
                                    GO.HWpropSoundLevel3 = ((GO.receiveData[60] and 16.toUByte()) != 0.toUByte())
                                    GO.HWpropVibroLevel1 = ((GO.receiveData[60] and 32.toUByte()) != 0.toUByte())
                                    GO.HWpropVibroLevel2 = ((GO.receiveData[60] and 64.toUByte()) != 0.toUByte())
                                    GO.HWpropVibroLevel3 = ((GO.receiveData[60] and 128.toUByte()) != 0.toUByte())
                                    GO.HWpropAutoStartSpectrometr = ((GO.receiveData[61] and 1.toUByte()) != 0.toUByte())
                                    GO.HWpropLevel1 = (GO.receiveData[54] + (GO.receiveData[55] * 256u)).toInt()
                                    GO.HWpropLevel2 = (GO.receiveData[56] + (GO.receiveData[57] * 256u)).toInt()
                                    GO.HWpropLevel3 = (GO.receiveData[58] + (GO.receiveData[59] * 256u)).toInt()

                                    GO.HWpropCPS2UR = java.lang.Float.intBitsToFloat((GO.receiveData[46] + (GO.receiveData[47] * 256u)  + (GO.receiveData[48] * 65536u) + (GO.receiveData[49] * 16777216u)).toInt())
                                    GO.HWpropHVoltage = (GO.receiveData[50] + (GO.receiveData[51] * 256u)).toUShort()
                                    GO.HWpropComparator = (GO.receiveData[52] + (GO.receiveData[53] * 256u)).toUShort()

                                    //GO.HWspectrResolution =
                                    /* Коэффициенты полинома для 1024 */
                                    GO.HWCoef1024A = java.lang.Float.intBitsToFloat((GO.receiveData[34] + (GO.receiveData[35] * 256u)  + (GO.receiveData[36] * 65536u) + (GO.receiveData[37] * 16777216u)).toInt())
                                    GO.HWCoef1024B = java.lang.Float.intBitsToFloat((GO.receiveData[38] + (GO.receiveData[39] * 256u)  + (GO.receiveData[40] * 65536u) + (GO.receiveData[41] * 16777216u)).toInt())
                                    GO.HWCoef1024C = java.lang.Float.intBitsToFloat((GO.receiveData[42] + (GO.receiveData[43] * 256u)  + (GO.receiveData[44] * 65536u) + (GO.receiveData[45] * 16777216u)).toInt())
                                    /* Коэффициенты полинома для 2048 */
                                    GO.HWCoef2048A = java.lang.Float.intBitsToFloat((GO.receiveData[62] + (GO.receiveData[63] * 256u)  + (GO.receiveData[64] * 65536u) + (GO.receiveData[65] * 16777216u)).toInt())
                                    GO.HWCoef2048B = java.lang.Float.intBitsToFloat((GO.receiveData[66] + (GO.receiveData[67] * 256u)  + (GO.receiveData[68] * 65536u) + (GO.receiveData[69] * 16777216u)).toInt())
                                    GO.HWCoef2048C = java.lang.Float.intBitsToFloat((GO.receiveData[70] + (GO.receiveData[71] * 256u)  + (GO.receiveData[72] * 65536u) + (GO.receiveData[73] * 16777216u)).toInt())
                                    /* Коэффициенты полинома для 4096 */
                                    GO.HWCoef4096A = java.lang.Float.intBitsToFloat((GO.receiveData[74] + (GO.receiveData[75] * 256u)  + (GO.receiveData[76] * 65536u) + (GO.receiveData[77] * 16777216u)).toInt())
                                    GO.HWCoef4096B = java.lang.Float.intBitsToFloat((GO.receiveData[78] + (GO.receiveData[79] * 256u)  + (GO.receiveData[80] * 65536u) + (GO.receiveData[81] * 16777216u)).toInt())
                                    GO.HWCoef4096C = java.lang.Float.intBitsToFloat((GO.receiveData[82] + (GO.receiveData[83] * 256u)  + (GO.receiveData[84] * 65536u) + (GO.receiveData[85] * 16777216u)).toInt())
                                    /* Количество импульсов до усреднения для дозиметра */
                                    GO.HWAqureValue = (GO.receiveData[94] + (GO.receiveData[95] * 256u)).toUShort()
                                    //Log.d("BluZ-BT", "L:${GO.receiveData[94]}, H:${GO.receiveData[95]}")
                                    /*
                                    *   Спектр
                                    */
                                    if (dataType > 0) {             // Данные спектрометра
                                        if (GO.btnSpecterSSisInit) {
                                                /* Кнопка для включени/выключения спектрометра */
                                                GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop2)
                                                GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.Red))
                                                /*
                                        *   Смещение от начала буфера
                                        *   HEADER_OFFSET 50
                                        *   SIZE_DOZIMETR_BUFER 512
                                        *   LOG_BUFER_SIZE 50
                                        *   LOG_OFFSET = HEADER_OFFSET + SIZE_DOZIMETR_BUFER = (uint16_t)562 - 1124 байта
                                        *   SPECTER_OFFSET = LOG_OFFSET + LOG_BUFER_SIZE * 3 = (uint16_t)712 - 1424 байта
                                        */
                                                iii = 1424                  // Смещение в байтах от начала буфера.
                                                jjj = 0
                                                //var sm_test = 0.0
                                                //var max_test = 0.0
                                                //var tmp_test = 0.0
                                                //var idx_test = 0
                                                val koefChan = 20.0 / 65535.0
                                                while (jjj < GO.drawSPECTER.ResolutionSpectr) {
                                                    d0 = GO.receiveData[iii++]      // Выбираем младший байт
                                                    d1 = GO.receiveData[iii++]      // Выбираем старший байт
                                                    /* Логарифмическое сжатие */
                                                    GO.drawSPECTER.spectrData[jjj++] = round(2.0.pow((d0 + d1 * 256u).toDouble() * koefChan)) - 1
                                                }
                                                //Log.d("BluZ-BT", "SpectrSUMM:$sm_test, MAX:$max_test ($idx_test)")
                                                GO.drawSPECTER.init()
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
                                    } else {
                                        /* Кнопка для включени/выключения спектрометра */
                                        if (GO.btnSpecterSSisInit) {
                                            GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop)
                                            GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.buttonTextColor))
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("BluZ-BT", "CS - incorrect. Found: $tmpCS calculate: $checkSumm")
                            //Log.e("BluZ-BT", "data[242]: " + data[242].toUByte() + " data[243]: " + data[243].toUByte())
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun destroyDevice() {
        connected = false
        MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
            withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                indBT.setBackgroundColor(GO.mainContext.getColor(R.color.Red))
            }
        }
        if (gatt == null) {
            //Toast.makeText(getBaseContext(), "BlueTooth disabled ?.", Toast.LENGTH_LONG).show();
            //finish()
        } else {
            gatt!!.disconnect()
            gatt!!.close()
            device = null
            writeBuffer!!.clear()
        }
    }

    // Передача данных в ассинхронном режиме.
    @SuppressLint("MissingPermission")
    private fun writeNext() {
        var data: ByteArray? = writeBuffer!!.removeAt(0)

        synchronized(writeBuffer!!) {
            if (!writeBuffer!!.isEmpty() /*&& delegate!!.canWrite()*/) {
                writePending = true
                data = writeBuffer!!.removeAt(0)
            } else {
                writePending = false
                data = null
            }
        }
        data?.let {
            wrCharacteristic!!.value = it
            if (!gatt!!.writeCharacteristic(wrCharacteristic)) {
                Log.e("BluZ-BT", "Write Characteristic error")
                // onSerialIoError(IOException("write failed"))
            } else {
                Log.d("BluZ-BT", "write started from next, len=${it.size}")
            }
        }
    }

    /*
    * Передача комманды в прибор
    * 0, 1, 2       -   Заголовок <S>
    * 3             -   Код команды
    * 242, 243      -   Контрольная сумма
    */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun sendCommand(cmd: UByte) {
        GO.BTT.sendBuffer[0] = '<'.code.toUByte()
        GO.BTT.sendBuffer[1] = 'S'.code.toUByte()
        GO.BTT.sendBuffer[2] = '>'.code.toUByte()
        GO.BTT.sendBuffer[3] = cmd

        /*
        *   Подсчет контрольной суммы
        */
        GO.sendCS = 0u
        Log.d("BluZ-BT", "calcCS: " + GO.sendCS.toString() + " Buffer size:  " + GO.BTT.sendBuffer.size.toString())
        for (iii in 0..241) {
            GO.sendCS = (GO.sendCS + GO.BTT.sendBuffer[iii]).toUShort()
        }
        Log.d("BluZ-BT", "calcCS: " + GO.sendCS.toString())
        GO.BTT.sendBuffer[242] = (GO.sendCS and 255u).toUByte()
        GO.BTT.sendBuffer[243] = ((GO.sendCS.toUInt() shr 8) and 255u).toUByte()

        write(GO.BTT.sendBuffer.toByteArray())
    }

        /*
         *  Передача данных
         */
    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        if ( !connected || wrCharacteristic == null) return
        /*if ( !connected || wrCharacteristic == null) {
            throw IOException("not connected")
        }*/
        var data0: ByteArray?
        synchronized(writeBuffer!!) {
            data0 = if (data.size <= payloadSize) {
                data
            } else {
                Arrays.copyOfRange(data, 0, payloadSize)
            }
            if (!writePending && writeBuffer!!.isEmpty() /*&& delegate!!.canWrite()*/) {
                writePending = true
            } else {
                writeBuffer!!.add(data0!!)
                Log.d("BluZ-BT", "write queued, len=" + data0!!.size)
                data0 = null
            }
            if (data.size > payloadSize) {
                for (i in 1 until (data.size + payloadSize - 1) / payloadSize) {
                    val from: Int = i * payloadSize
                    val to = Math.min(from + payloadSize, data.size)
                    writeBuffer!!.add(Arrays.copyOfRange(data, from, to))
                    Log.d("BluZ-BT", "write queued, len=" + (to - from))
                }
            }
        }
        if (data0 != null) {
            wrCharacteristic!!.setValue(data0)
            if (!gatt!!.writeCharacteristic(wrCharacteristic)) {
                Log.d("BluZ-BT", "Write Characteristic error")
            } else {
                Log.d("BluZ-BT", "write started, len=" + data0!!.size)
            }
        }
    }

    // Broadcast приемник
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //val action = intent.action
            Log.i("BluZ-BT", "Broadcast.")
        }
    }
}