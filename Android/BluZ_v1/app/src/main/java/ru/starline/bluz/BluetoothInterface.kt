package ru.starline.bluz

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.util.Arrays
import java.util.UUID

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

val BLUETOOTH_LE_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
val BLUETOOTH_BLUZ_SERVICE: UUID = UUID.fromString("0000fe80-cc7a-482a-984a-7f2ed5b3e58f")
val BLUETOOTH_BLUZ_CHAR_R: UUID = UUID.fromString("0000fe81-8e22-4541-9d4c-21edae82ed19")
val BLUETOOTH_BLUZ_CHAR_W: UUID = UUID.fromString("0000fe82-8e22-4541-9d4c-21edae82ed19")
private var readCharacteristic: BluetoothGattCharacteristic? = null
private var writeCharacteristic:BluetoothGattCharacteristic? = null

/**
 * Created by ed on 20,июнь,2024
 */
class BluetoothInterface(tv: TextView) {
    var LEMACADDRESS: String = ""
    val BTM = mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val BTA = BTM.adapter
    val BTS = BTA.bluetoothLeScanner
    var indBT: TextView = tv
    lateinit var txtMACADDRESS: EditText
    lateinit var startButton: Button
    private var delegate: BLUZDelegate = BLUZDelegate()
    //private var delegate: DeviceDelegate = null
    var gatt: BluetoothGatt? = null
    var device: BluetoothDevice? = null
    private var writeBuffer: ArrayList<ByteArray>? = null
    val MAX_MTU: Int = 244
    private var writePending = false
    var connected: Boolean = false
    private val payloadSize: Int = MAX_MTU
    /*
    *      Обработчик окончания сканирования, здесь получим результат
    */
    val leScanCallback = object: ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //Log.d("BluZ-BT", "onscanresult...")
            super.onScanResult(callbackType, result)
            if (result.getDevice() == null || TextUtils.isEmpty(result.getDevice().getName())) {

            } else {
                Log.d("BluZ-BT", result.device.name)
                Log.d("BluZ-BT", result.device.address)
                Log.d("BluZ-BT", result.scanRecord.toString())
                if (result.device.name == "BluZ") {
                    LEMACADDRESS = result.device.address
                    txtMACADDRESS.setText(result.device.address)
                    //indBT.setBackgroundColor(mainContext.getResources().getColor(R.color.Green, mainContext.theme) )
                    startButton.setTextColor(mainContext.getResources().getColor(R.color.buttonTextColor, mainContext.theme))
                    startButton.setText(mainContext.getString(R.string.textScan))
                    BTS.stopScan(this)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(textMAC: EditText, startBTN: Button) {
        txtMACADDRESS = textMAC // Установить тект для редактирования
        startButton = startBTN
        textMAC.setText(mainContext.getString(R.string.defaultMAC))
        BTS.startScan(leScanCallback)
    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        BTS.stopScan(leScanCallback)
    }

    /*
     * delegate device specific behaviour to inner class
     */

    class DeviceDelegate {
        fun connectCharacteristics(s: BluetoothGattService?): Boolean { return true }
        // following methods only overwritten for Telit devices
        fun onDescriptorWrite(g: BluetoothGatt?, d: BluetoothGattDescriptor?, status: Int) { }
        fun onCharacteristicChanged(g: BluetoothGatt?, c: BluetoothGattCharacteristic?) { }
        fun onCharacteristicWrite(g: BluetoothGatt?, c: BluetoothGattCharacteristic?, status: Int ) { }
        fun canWrite(): Boolean { return true }
        fun disconnect() { }
    }

/*
    class BLUZDelegate : DeviceDelegate() {
        fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
            Log.i("BluZ-BT", "Service BLUZ")
            readCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_R)
            writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_W)
            return true
        }
    }
*/
class BLUZDelegate  {
    fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
        Log.i("BluZ-BT", "Service BLUZ")
        readCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_R)
        writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_BLUZ_CHAR_W)
        return true
    }
    fun disconnect() { }
    fun onDescriptorWrite(g: BluetoothGatt?, d: BluetoothGattDescriptor?, status: Int) { }
    fun onCharacteristicChanged(g: BluetoothGatt?, c: BluetoothGattCharacteristic?) { }
    fun onCharacteristicWrite(g: BluetoothGatt?, c: BluetoothGattCharacteristic?, status: Int ) { }
    fun canWrite(): Boolean { return true }
}

    /*
     *    Start BLE.
     */
    @SuppressLint("MissingPermission")
    fun initLeDevice() {
        writeBuffer = ArrayList() // Буфер для передачи.
        Log.d("BluZ-BT", "Accept connect...")
        if (!BTA.isEnabled()) {
            //Log.d(TAG, "Bluetooth disabled. Exit.");
            Toast.makeText(mainContext, "BlueTooth disable ? \nProgram terminated.", Toast.LENGTH_LONG).show()
            //finish()
        }
        device = BTA.getRemoteDevice(LEMAC) // Подключаемся по MAC адресу.
        Log.d("BluZ-BT", "Status: " + BTA.getState());
        if (device == null) {
            Log.i("BluZ-BT", "Error: Device: $LEMAC not connected.")
            return
        } else {
            //Log.i(TAG, "Try gatt connect.");
            gatt = device!!.connectGatt(mainContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                Log.i("BluZ-BT", "Error: Gatt create failed.");
                //finish()
            }
        }
    }

    // Сюда попадаем после завершения работы connectGatt
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    tv.setBackgroundColor(mainContext.getColor(R.color.Green))
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
                    tv.setBackgroundColor(mainContext.getColor(R.color.Red))

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
            var sync = true
            writePending = false
            Log.d("BluZ-BT", "Set gatt Characteristics.")
            for (gattService in gatt.services) {
                if (gattService.uuid == BLUETOOTH_BLUZ_SERVICE) {
                    delegate = BLUZDelegate()
                }
                if (delegate != null) {
                    sync = delegate.connectCharacteristics(gattService)
                    break
                }
            }
            if (sync) {
                if (!gatt.requestMtu(MAX_MTU)) Log.d("BluZ-BT", "Error set MTU.")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
            delegate!!.onDescriptorWrite(gatt, descriptor, status)
            if (descriptor.characteristic === readCharacteristic) {
                Log.d("BluZ-BT", "writing read characteristic descriptor finished, status=$status")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BluZ-BT", "Error: Write characteristic failed.")
                } else {
                    Log.i("BluZ-BT", "Connect success.")
                    connected = true
                    //if (DA != null) {
                    //    DA.connectIndicator()
                    //}
                    Log.d("BluZ-BT", "Write descriptor Ok.")
                }
            }
        }

        override fun onCharacteristicWrite( gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int ) {
            if ( !connected || writeCharacteristic == null) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluZ-BT", "write finished, status=" + status);
                return
            }
            delegate!!.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic === writeCharacteristic) { // NOPMD - test object identity
                Log.d("BluZ-BT", "write finished, status=" + status);
                writeNext()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BluZ-BT", "mtu size $mtu, status = $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var payloadSize: Int = mtu - 3
                Log.d("BluZ-BT", "payload size $payloadSize")
            }
            if (writeCharacteristic == null) {
                Log.e("BluZ-BT", "Error: characteristic not writable - 1")
                return
            } else {
                val writeProperties = writeCharacteristic!!.properties
                if ((writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE +
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0 ) {
                    Log.e("BluZ-BT", "Error: characteristic not writable - 2")
                    return
                }
            }
            if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
                Log.e("BluZ-BT", "Error: no notification for read characteristic")
                return
            }
            val readDescriptor = readCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD)
            if (readDescriptor == null) {
                Log.d("BluZ-BT", "Error: no CCCD descriptor for read characteristic")
                return
            }
            val readProperties = readCharacteristic!!.properties
            if ((readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                Log.d("BluZ-BT", "enable read indication")
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            } else if ((readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                Log.d("BluZ-BT", "enable read notification")
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                Log.d(
                    "BluZ-BT",
                    "Error: no indication/notification for read characteristic ($readProperties)"
                )
                return
            }
            Log.d("BluZ-BT", "writing read characteristic descriptor")
            if (!gatt.writeDescriptor(readDescriptor)) {
                Log.d("BluZ-BT", "Error: read characteristic CCCD descriptor not writable")
            }
            // continues asynchronously in onDescriptorWrite()
        }

        /*
        *      Прием данных
        */
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            delegate!!.onCharacteristicChanged(gatt, characteristic)
            if (characteristic == readCharacteristic) { // NOPMD - test object identity
                val data = readCharacteristic!!.value
                /*
                     Заполнение массива
                */
                // Ищем стартовую последовательность <B>.
                if (data.isNotEmpty()) {
                    Log.d("BluZ-BT", "Receive: " + data.size.toString())
                }
            }
        }

    }


    @SuppressLint("MissingPermission")
    fun destroyDevice() {
        indBT.setBackgroundColor(mainContext.getColor(R.color.Red))
        if (gatt == null) {
            //Toast.makeText(getBaseContext(), "BlueTooth disabled ?.", Toast.LENGTH_LONG).show();
            //finish()
        } else {
            gatt!!.disconnect()
            gatt!!.close()
            delegate?.disconnect()
            //delegate = null
            device = null
            writeBuffer!!.clear()
        }
    }

    // Передача данных в ассинхронном режиме.
    @SuppressLint("MissingPermission")
    private fun writeNext() {
        val data: ByteArray?
        //synchronized(writeBuffer) {
            if (!writeBuffer!!.isEmpty() && delegate!!.canWrite()) {
                writePending = true
                data = writeBuffer!!.removeAt(0)
            } else {
                writePending = false
                data = null
            }
        //}
        data?.let {
            writeCharacteristic!!.value = it
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                Log.d("BluZ-BT", "Write Characteristic error")
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
        if ( !connected || writeCharacteristic == null) {
            throw IOException("not connected")
        }
        var data0: ByteArray?
        synchronized(writeBuffer!!) {
            data0 = if (data.size <= payloadSize) {
                data
            } else {
                Arrays.copyOfRange(data, 0, payloadSize)
            }
            if (!writePending && writeBuffer!!.isEmpty() && delegate!!.canWrite()) {
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
            writeCharacteristic!!.setValue(data0)
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                Log.d("BluZ-BT", "Write Characteristic error")
            } else {
                Log.d("BluZ-BT", "write started, len=" + data0!!.size)
            }
        }
        // continues asynchronously in onCharacteristicWrite()
    }

    // Broadcast приемник
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.i("BluZ-BT", "Broadcast.")
        }
    }


    fun connect() {
        initLeDevice()
    }
}