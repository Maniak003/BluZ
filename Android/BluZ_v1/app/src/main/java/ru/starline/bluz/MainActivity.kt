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

public val GO: globalObj = globalObj()
public const val propADDRESS: String = "Address"
public const val propColorSpecterLin: String = "ColorLin"
public const val propColorSpecterLog: String = "ColorLog"
public const val propColorSpecterFone: String = "ColorFone"
public const val propLevel1: String = "Level1"
public const val propLevel2: String = "Level2"
public const val propLevel3: String = "Level3"
public const val propCPS2UR: String = "CPS2UR"
public const val propCoefA: String = "CoeffA"
public const val propCoefB: String = "CoeffB"
public const val propCoefC: String = "CoeffC"
public const val propIndicator: String = "Indicator"
public const val propHV: String = "HVoltage"
public const val propComparator: String = "Comparator"


//public lateinit var mainContext: Context
public var PI: Int = 0

public class MainActivity : FragmentActivity() {

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
        GO.drawSPECTER = drawSpecter()
        GO.adapter = NumberAdapter(this)
        GO.indicatorBT = findViewById(R.id.indicatorBT)
        GO.viewPager = findViewById(R.id.VPMain)
        GO.viewPager.isUserInputEnabled = false             // Отключение прокрутки viewPager2
        GO.viewPager.adapter = GO.adapter
        GO.bColor = buttonColor()

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
            GO.viewPager.setCurrentItem(0, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnSpecter)
        }

        /* Окно с историей */
        var btnHistory: Button = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            GO.viewPager.setCurrentItem(1, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnHistory)
        }

        /* Окно дозиметра */
        var btnDozimeter: Button = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            GO.viewPager.setCurrentItem(2, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnDozimeter)
        }

        /* Окно c логами */
        var btnLog: Button = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            GO.viewPager.setCurrentItem(3, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnLog)
        }

        /* Окно с настройками */
        var btnSetup: Button = findViewById(R.id.buttonSetup)
        btnSetup.setOnClickListener {
            GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnSetup)
        }
        GO.bColor.initColor(btnSpecter, btnHistory, btnDozimeter, btnLog, btnSetup)
        GO.bColor.resetToDefault()
        GO.bColor.setToActive(btnSpecter)  // Активная закладка.

        /*
        *       Параметры приложения
        */
        GO.PP = propControl()
        GO.LEMAC = GO.PP.getPropStr(propADDRESS)
        GO.ColorLin = GO.PP.getPropInt(propColorSpecterLin)
        GO.ColorLog = GO.PP.getPropInt(propColorSpecterLog)
        GO.ColorFone = GO.PP.getPropInt(propColorSpecterFone)
        GO.BTT = BluetoothInterface(GO.indicatorBT)
        /*
        *       Параметры прибора
        */
        GO.propLevel1 = GO.PP.getPropFloat(propLevel1)
        GO.propLevel2 = GO.PP.getPropFloat(propLevel2)
        GO.propLevel3 = GO.PP.getPropFloat(propLevel3)
        GO.propCoefA = GO.PP.getPropFloat(propCoefA)
        GO.propCoefB = GO.PP.getPropFloat(propCoefB)
        GO.propCoefC = GO.PP.getPropFloat(propCoefC)
        GO.propCPS2UR = GO.PP.getPropFloat(propCPS2UR)
        GO.propIndicator = GO.PP.getPropByte(propIndicator)
        GO.propHVoltage = GO.PP.getPropInt(propHV).toUShort()
        GO.propComparator = GO.PP.getPropInt(propComparator).toUShort()

        Log.d("BluZ-BT", "mac addr: " + GO.LEMAC)
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