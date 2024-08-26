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
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
        val scanFilter: ScanFilter = ScanFilter.Builder().setDeviceName(BLEDeviceName).build()
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
        BTS.startScan(filterList, scanSetting, leScanCallback)
        Log.d("BluZ-BT", "LE scanning.")
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
                //Log.i(TAG, "Try gatt connect.");
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
                    MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                        withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                            tv.setBackgroundColor(GO.mainContext.getColor(R.color.Green))
                            GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop2)
                            GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.Red))
                        }
                    }
                    Log.i("BluZ-BT", "Gatt connect success.")
                    if (!gatt.discoverServices()) {
                        Log.e("BluZ-BT", "Error: Discover service failed.")
                        //finish()
                    }
                    if (!gatt.requestMtu(MAX_MTU)) {  // Изменяем MTU
                        Log.i("BluZ-BT", "MTU set failed.")
                        //finish()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                        withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                            tv.setBackgroundColor(GO.mainContext.getColor(R.color.Red))
                            GO.btnSpecterSS.text = GO.mainContext.getString(R.string.textStartStop)
                            GO.btnSpecterSS.setTextColor(GO.mainContext.getColor(R.color.buttonTextColor))
                        }
                    }
                    connected = false
                    writePending = false
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
                    testIdx2 = 0;
                    if ((data[0].toUByte() == 60.toUByte())
                       && (data[1].toUByte() == 66.toUByte())
                       && (data[2].toUByte() == 62.toUByte())) {
                        testIdx = 0
                        idxArray = 0                    // Индекс результирующего массива с данными
                        numberMTU = data[3].toInt()     // Количество пакетов для передачи
                        dataType = data[4].toInt()      // Тип передаваемых данных
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
                         * 0,1,2 - Маркер начала <B>
                         * 3 -  количество MTU
                         *    1    - Параметры прибора
                         *    3    - Даные лога
                         *    4    - Данные дозиметра
                         *    9    - Спектр 1024 разрядов
                         *    17   - Спектр 2048 разрядов
                         *    34   - Спектр 4096 разрядов
                         *
                         * 4 - Тип передаваемых данных
                         *    0   - Текущий спектр
                         *    1   - Исторический спектр
                         *
                         * 5, 6   - Общее число импульсов от начала измерения uint32_t
                         * 7, 8   - Число импульсов за последнюю секунду. uint32_t
                         * 9, 10  - Общее время в секундах uint32_t
                         * 11, 12 - Среднее количество имльсов в секунду. float
                         * 13, 14 - Температура в гр. цельсия
                         * 15, 16 - Напряжение батареи в вольтах
                         *
                         */

                        /* Определим размер заголовка */
                        when(numberMTU) {
                            1 -> {                      // Параметры прибора
                                indexData = 8           // Размер заголовка
                            }
                            3 -> {                      // Логи
                                indexData = 8
                            }
                            9 -> {                      // Спектр с разрешением 1024
                                indexData = 55
                                endOfData = data.size - 5
                                GO.specterType = 0         // Определяет размер по горизонтали
                            }
                            17 -> {                     // Спектр с разрешением 2048
                                indexData = 55
                                endOfData = data.size - 5
                                GO.specterType = 1
                            }
                            34 -> {                     // Спектр с разрешением 4096
                                indexData = 55
                                endOfData = data.size - 5
                                GO.specterType = 2
                            }
                        }
                        /* Считаем контрольную сумму заголовка*/
                        checkSumm = 0u
                        for (idxH in 0 ..<indexData) {
                            checkSumm = (checkSumm + data[idxH].toUByte()).toUShort()
                            receiveBuffer[idxH] = data[idxH].toUByte()
                            testIdx++
                            testIdx2++
                        }
                        numberMTU--                     // Считаем количество передач
                    } else {
                        numberMTU--
                        indexData = 0
                        endOfData = if (numberMTU == 0) {           // Последний буфер, не считаем последние 2 байта
                            data.size - 7
                        } else {
                            data.size - 5
                        }
                    }
                    /*
                    Log.d("BluZ-BT", "Receive: " + data.size.toString()+ " real size: " + testIdx2.toString() + " "
                            + data[48] + " idx:" + idxArray + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[240] + " " + data[241] + " " + data[242].toUByte() + " " + data[243].toUByte()
                            + " numMTU: " + numberMTU.toString() + " indexData: " + indexData.toString() + " endOfData: " + endOfData.toString())
                    */
                    when(dataType) {
                        /* Данные спектра */
                        0, 1 -> {
                            /* Перегружаем данные в массив для спектра */
                            var idx = indexData
                            var d0: UByte
                            var d1: UByte
                            while (idx <= endOfData) {
                                d0 = data[idx++].toUByte()
                                d1 = data[idx++].toUByte()
                                checkSumm =  (checkSumm + d0 + d1).toUShort()
                                /* Накопление массива спектра */
                                GO.drawSPECTER.spectrData[idxArray++] = (d0 + d1 * 256u).toDouble()
                                testIdx+=2
                                testIdx2+=2
                            }
                        }
                    }
                    /*
                    Log.d("BluZ-BT", "Receive: " + data.size.toString()+ " real size: " + testIdx2.toString() + " "
                            + data[48] + " idx:" + idxArray + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[240] + " " + data[241] + " " + data[242].toUByte() + " " + data[243].toUByte()
                            + " numMTU: " + numberMTU.toString() + " indexData: " + indexData.toString() + " endOfData: " + endOfData.toString())
                    */

                    if (numberMTU == 0) {       // Прием последнего блока
                        var tmpCS: UShort
                        tmpCS = (data[242].toUByte() + (data[243].toUByte() * 256u)).toUShort()
                        Log.d("BluZ-BT", "Total data size: $testIdx")
                        if (tmpCS == checkSumm) {
                            Log.d("BluZ-BT", "CS - correct: $checkSumm")
                            /* Накопление массива закончено можно вызывать обновление экрана */

                            MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
                                withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
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
            val action = intent.action
            Log.i("BluZ-BT", "Broadcast.")
        }
    }
}