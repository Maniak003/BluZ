package ru.starline.bluz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import kotlin.system.exitProcess

public val GO: globalObj = globalObj()
public const val propADDRESS: String = "Address"
public const val BLEDeviceName: String = "BluZ"
public const val propSpectrGraphType: String = "SpecterGraphType"
public const val propColorDozimeter: String = "ColorDozimeter"
public const val propColorSpecterLin: String = "ColorLin"
public const val propColorSpecterLog: String = "ColorLog"
public const val propColorSpecterFone: String = "ColorFone"
public const val propColorSpecterFoneLg: String = "ColorFoneLg"
public const val propColorSpecterLinGisto: String = "ColorLinGisto"
public const val propColorSpecterLogGisto: String = "ColorLogGisto"
public const val propColorSpecterFoneGisto: String = "ColorFoneGisto"
public const val propColorSpecterFoneLgGisto: String = "ColorFoneLgGisto"
public const val propLevel1: String = "Level1"
public const val propLevel2: String = "Level2"
public const val propLevel3: String = "Level3"
public const val propSoundLevel1: String = "soundLevel1"
public const val propSoundLevel2: String = "soundLevel2"
public const val propSoundLevel3: String = "soundLevel3"
public const val propVibroLevel1: String = "vibroLevel1"
public const val propVibroLevel2: String = "vibroLevel2"
public const val propVibroLevel3: String = "vibroLevel3"
public const val propCPS2UR: String = "CPS2UR"
public const val propCoefA: String = "CoeffA"
public const val propCoefB: String = "CoeffB"
public const val propCoefC: String = "CoeffC"
public const val propCoef2048A: String = "Coeff2048A"
public const val propCoef2048B: String = "Coeff2048B"
public const val propCoef2048C: String = "Coeff2048C"
public const val propCoef4096A: String = "Coeff4096A"
public const val propCoef4096B: String = "Coeff4096B"
public const val propCoef4096C: String = "Coeff4096C"
public const val propHV: String = "HVoltage"
public const val propComparator: String = "Comparator"
public const val propSoundKvant: String = "SoundKvant"
public const val propLedKvant: String = "LedKvant"
public const val propResolution: String = "Resolution"
public const val propStartSpectrometr: String = "AutoStartSpectrometr"
public const val propSMAWindow: String = "SMAWindow"
public const val propRejectCann: String = "RejectConn"


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
        GO.drawDOZIMETER = drawDozimeter()
        GO.drawCURSOR = drawCursor()
        GO.adapter = NumberAdapter(this)
        GO.indicatorBT = findViewById(R.id.indicatorBT)
        GO.viewPager = findViewById(R.id.VPMain)
        GO.viewPager.isUserInputEnabled = false             // Отключение прокрутки viewPager2
        GO.viewPager.adapter = GO.adapter
        GO.bColor = buttonColor()

        /*
        *   Цвета для курсора
        */
        GO.ColorEraseCursor = getResources().getColor(R.color.eraseColorCursor, GO.mainContext.theme)
        GO.ColorActiveCursor = getResources().getColor(R.color.activeColorCursor, GO.mainContext.theme)

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
            /* Все нужные разрешения имеются */
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
            GO.viewPager.setCurrentItem(0, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnSpecter)
        }

        /* Окно с историей */
        var btnHistory: ImageButton = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            GO.viewPager.setCurrentItem(1, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnHistory)
        }

        /* Окно дозиметра */
        var btnDozimeter: ImageButton = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            GO.viewPager.setCurrentItem(2, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnDozimeter)
        }

        /* Окно c логами */
        var btnLog: ImageButton = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            GO.viewPager.setCurrentItem(3, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnLog)
        }

        /* Окно с настройками */
        GO.btnSetup = findViewById(R.id.buttonSetup)
        GO.btnSetup.setOnClickListener {
            GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnSetup)
        }
        GO.bColor.initColor(btnSpecter, btnHistory, btnDozimeter, btnLog, GO.btnSetup)
        GO.bColor.resetToDefault()
        GO.bColor.setToActive(btnSpecter)  // Активная закладка.

        /*
        *       Параметры приложения
        */
        GO.PP = propControl()
        GO.LEMAC = GO.PP.getPropStr(propADDRESS)
        /* Цвета для дозиметра */
        GO.ColorDosimeter = GO.PP.getPropInt(propColorDozimeter)
        if (GO.ColorDosimeter == 0) {
            GO.ColorDosimeter = resources.getColor(R.color.ColorDosimeter, GO.mainContext.theme)
        }

        /* Цвета для линейного графика */
        GO.ColorLin = GO.PP.getPropInt(propColorSpecterLin)
        GO.ColorLog = GO.PP.getPropInt(propColorSpecterLog)
        GO.ColorFone = GO.PP.getPropInt(propColorSpecterFone)
        GO.ColorFoneLg = GO.PP.getPropInt(propColorSpecterFoneLg)
        /* Цвета для гистограммы */
        GO.ColorLinGisto = GO.PP.getPropInt(propColorSpecterLinGisto)
        GO.ColorLogGisto = GO.PP.getPropInt(propColorSpecterLogGisto)
        GO.ColorFoneGisto = GO.PP.getPropInt(propColorSpecterFoneGisto)
        GO.ColorFoneLgGisto = GO.PP.getPropInt(propColorSpecterFoneLgGisto)

        /* Тип графика спектра: линейный, гистограмма */
        GO.specterGraphType = GO.PP.getPropInt(propSpectrGraphType)
        GO.rejectChann = GO.PP.getPropInt(propRejectCann)
        //Log.d("BluZ-BT", "Reject chann: " +GO.rejectCann )
        GO.BTT = BluetoothInterface(GO.indicatorBT)
        /*
        *       Параметры прибора
        */
        GO.propSoundKvant = GO.PP.getPropBoolean(propSoundKvant)
        GO.propLedKvant = GO.PP.getPropBoolean(propLedKvant)
        GO.propAutoStartSpectrometr = GO.PP.getPropBoolean(propStartSpectrometr)
        GO.propSoundLevel1 = GO.PP.getPropBoolean(propSoundLevel1)
        GO.propSoundLevel2 = GO.PP.getPropBoolean(propSoundLevel2)
        GO.propSoundLevel3 = GO.PP.getPropBoolean(propSoundLevel3)
        GO.propVibroLevel1 = GO.PP.getPropBoolean(propVibroLevel1)
        GO.propVibroLevel2 = GO.PP.getPropBoolean(propVibroLevel2)
        GO.propVibroLevel3 = GO.PP.getPropBoolean(propVibroLevel3)
        GO.propLevel1 = GO.PP.getPropInt(propLevel1)
        GO.propLevel2 = GO.PP.getPropInt(propLevel2)
        GO.propLevel3 = GO.PP.getPropInt(propLevel3)
        GO.propCoefA = GO.PP.getPropFloat(propCoefA)
        GO.propCoefB = GO.PP.getPropFloat(propCoefB)
        GO.propCoefC = GO.PP.getPropFloat(propCoefC)
        GO.propCoef2048A = GO.PP.getPropFloat(propCoef2048A)
        GO.propCoef2048B = GO.PP.getPropFloat(propCoef2048B)
        GO.propCoef2048C = GO.PP.getPropFloat(propCoef2048C)
        GO.propCoef4096A = GO.PP.getPropFloat(propCoef4096A)
        GO.propCoef4096B = GO.PP.getPropFloat(propCoef4096B)
        GO.propCoef4096C = GO.PP.getPropFloat(propCoef4096C)
        GO.propCPS2UR = GO.PP.getPropFloat(propCPS2UR)
        GO.propHVoltage = GO.PP.getPropInt(propHV).toUShort()
        GO.propComparator = GO.PP.getPropInt(propComparator).toUShort()
        GO.spectrResolution = GO.PP.getPropInt(propResolution)
        GO.windowSMA = GO.PP.getPropInt(propSMAWindow)
        if (GO.windowSMA < 3) {
            GO.windowSMA = 3
        }

        GO.needTerminate = false
        Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())

        if (GO.LEMAC.length == 17 &&  GO.LEMAC[0] != 'X') { // MAC адрес настроен, продолжаем работу.
            GO.tmFull.startTimer();
        } else {                                            // MAC адрес не настроен, переходим к настройкам
            GO.viewPager.setCurrentItem(1, false)
            GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnSetup)
            Toast.makeText(GO.mainContext, "MAC address not set.\nScan your device.", Toast.LENGTH_LONG ).show()
        }
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