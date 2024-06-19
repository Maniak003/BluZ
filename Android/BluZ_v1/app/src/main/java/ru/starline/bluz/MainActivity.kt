package ru.starline.bluz

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import android.widget.Button
import android.widget.TextView
import com.google.android.material.internal.ViewUtils.getContentView
import kotlin.system.exitProcess

public var pagerFrame: Int = 1
public lateinit var viewPager: ViewPager2
public lateinit var btnSpecterSS: Button
public lateinit var mainContext: Context
public lateinit var indicatorBT: TextView

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
        }

        /* Окно с историей */
        var btnHistory: Button = findViewById(R.id.buttonHistory)
        btnHistory.setOnClickListener {
            viewPager.setCurrentItem(1, false)
        }

        /* Окно дозиметра */
        var btnDozimeter: Button = findViewById(R.id.buttonDosimeter)
        btnDozimeter.setOnClickListener {
            viewPager.setCurrentItem(2, false)
        }

        /* Окно c kjufvb */
        var btnLog: Button = findViewById(R.id.buttonLog)
        btnLog.setOnClickListener {
            viewPager.setCurrentItem(3, false)
        }

        /* Окно с настройками */
        var btnSetup: Button = findViewById(R.id.buttonSetup)
        btnSetup.setOnClickListener {
            viewPager.setCurrentItem(4, false)
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