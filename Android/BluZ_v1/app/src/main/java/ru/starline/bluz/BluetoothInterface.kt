package ru.starline.bluz

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.TextView


/**
 * Created by ed on 20,июнь,2024
 */
class BluetoothInterface {
    public var LEMACADDRESS: String = ""
    val BTM = mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val BTA = BTM.adapter
    val BTS = BTA.bluetoothLeScanner
    lateinit var indBT: TextView
    lateinit var txtMACADDRESS: TextView

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
                    LEMAC = result.device.address
                    //indBT.setBackgroundColor(getResources().getColor(R.color.Green, mainContext.theme) )
                    txtMACADDRESS.text = LEMAC
                    BTS.stopScan(this)
                }
            }
        }
    }

    fun setTextObjects(indicator: TextView, textMAC: TextView) {
        indBT = indicator
        txtMACADDRESS = textMAC

    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        BTS.startScan(leScanCallback)

    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        BTS.startScan(leScanCallback)
    }
}