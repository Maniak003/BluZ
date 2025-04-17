package ru.starline.bluz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import kotlin.system.exitProcess

public val GO: globalObj = globalObj()


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
        GO.drawLOG = drawLogs()
        GO.drawCURSOR = drawCursor()
        GO.adapter = NumberAdapter(this)
        GO.indicatorBT = findViewById(R.id.indicatorBT)
        GO.viewPager = findViewById(R.id.VPMain)
        GO.viewPager.isUserInputEnabled = false             // Отключение прокрутки viewPager2
        GO.viewPager.adapter = GO.adapter
        GO.bColor = buttonColor()
        GO.txtStat1 = findViewById(R.id.textStatistic1)
        GO.txtStat2 = findViewById(R.id.textStatistic2)
        GO.txtStat3 = findViewById(R.id.textStatistic3)
        GO.txtIsotopInfo = findViewById(R.id.textIsotopInfo)

        /*
        *   Тексты форматов для сохранения
        */
        GO.saveSpecterType1 = getResources().getString(R.string.textType1)
        GO.saveSpecterType2 = getResources().getString(R.string.textType2)

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
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.VISIBLE
            GO.viewPager.setCurrentItem(0, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnSpecter)
        }

        /* Окно с историей */
        var btnHistory: ImageButton = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.VISIBLE
            GO.viewPager.setCurrentItem(1, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnHistory)
        }

        /* Окно дозиметра */
        var btnDozimeter: ImageButton = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            GO.txtStat1.visibility = View.VISIBLE
            GO.txtStat2.visibility = View.VISIBLE
            GO.txtStat3.visibility = View.VISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(2, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnDozimeter)
            if (GO.initDOZ) {
                GO.drawDOZIMETER.Init()
                if (GO.drawDOZIMETER.dozVSize > 0 &&  GO.drawDOZIMETER.dozHSize > 0) {
                    GO.drawDOZIMETER.clearDozimeter()
                    GO.drawDOZIMETER.redrawDozimeter()
                }
            }
        }

        /* Окно c логами */
        var btnLog: ImageButton = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(3, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(btnLog)
            if (GO.drawLOG.logsDrawIsInit) {
                GO.drawLOG.updateLogs()
            }
        }

        /* Окно с настройками */
        GO.btnSetup = findViewById(R.id.buttonSetup)
        GO.btnSetup.setOnClickListener {
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
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
        GO.readConfigParameters()
        /* Загрузка справочника изотопов */
        GO.loadIsotop()
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