package ru.starline.bluz

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.material.internal.ViewUtils.getContentView
import kotlin.system.exitProcess
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import java.nio.charset.Charset
import java.util.UUID

public var pagerFrame: Int = 1
public lateinit var viewPager: ViewPager2
public lateinit var indicatorBT: TextView
public lateinit var mainContext: Context
public lateinit var bColor: buttonColor
public lateinit var textMACADR: EditText
public var LEMAC: String = ""
public const val propADDRESS: String = "Address"
public lateinit var PP: propControl
public lateinit var drawSPEC: drawSpecter
public lateinit var BTT:  BluetoothInterface

class MainActivity : FragmentActivity() {
    private lateinit var adapter: NumberAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        mainContext = applicationContext

        indicatorBT = findViewById(R.id.indicatorBT)

        viewPager = findViewById(R.id.VPMain)
        viewPager.isUserInputEnabled = false
        adapter = NumberAdapter(this)
        viewPager.adapter = adapter

        /*
        *   Проверка и запрос разрешений.
        */
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            val permissionsRq = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
            ActivityCompat.requestPermissions(this, permissionsRq, 0)
        }

        /*
        * Основные кнопки
        */

        /* Завершение приложения */
        var btnExit: Button = findViewById(R.id.buttonExit)
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
        var btnSpecter: Button = findViewById(R.id.buttonSpecter)
        btnSpecter.setOnClickListener {
            viewPager.setCurrentItem(0, false)
            bColor.resetToDefault()
            bColor.setToActive(btnSpecter)
        }

        /* Окно с историей */
        var btnHistory: Button = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            viewPager.setCurrentItem(1, false)
            bColor.resetToDefault()
            bColor.setToActive(btnHistory)
        }

        /* Окно дозиметра */
        var btnDozimeter: Button = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            viewPager.setCurrentItem(2, false)
            bColor.resetToDefault()
            bColor.setToActive(btnDozimeter)
        }

        /* Окно c kjufvb */
        var btnLog: Button = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            viewPager.setCurrentItem(3, false)
            bColor.resetToDefault()
            bColor.setToActive(btnLog)
        }

        /* Окно с настройками */
        var btnSetup: Button = findViewById(R.id.buttonSetup)
        btnSetup.setOnClickListener {
            viewPager.setCurrentItem(4, false)
            bColor.resetToDefault()
            bColor.setToActive(btnSetup)
        }
        bColor = buttonColor()
        bColor.initColor(btnSpecter, btnHistory, btnDozimeter, btnLog, btnSetup)
        bColor.resetToDefault()
        bColor.setToActive(btnSpecter)  // Активная закладка.

        /*
        *       Параметры приложения
        */

        PP = propControl()
        LEMAC = PP.getPropStr(propADDRESS)
        if ((LEMAC.length == 17) && ! LEMAC.contentEquals("XX")) {
            BTT = BluetoothInterface(indicatorBT)
        }
        Log.d("BluZ-BT", "mac addr: " + LEMAC)
    }
}



/*
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

 */