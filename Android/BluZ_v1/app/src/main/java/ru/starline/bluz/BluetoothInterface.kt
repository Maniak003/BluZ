package ru.starline.bluz

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/**
 * Created by ed on 20,июнь,2024
 */
class BluetoothInterface {
    var LEMACADDRESS: String = ""
    val BTM = mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val BTA = BTM.adapter
    val BTS = BTA.bluetoothLeScanner
    lateinit var indBT: TextView
    lateinit var txtMACADDRESS: EditText
    lateinit var startButton: Button

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
    fun startScan(indicator: TextView, textMAC: EditText, startBTN: Button) {
        txtMACADDRESS = textMAC
        indBT = indicator
        startButton = startBTN
        textMAC.setText(mainContext.getString(R.string.defaultMAC))
        BTS.startScan(leScanCallback)

    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        BTS.stopScan(leScanCallback)
    }
}