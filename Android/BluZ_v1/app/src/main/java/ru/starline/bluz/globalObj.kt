package ru.starline.bluz

import android.content.ClipData.Item
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2

/**
 * Created by ed on 27,июнь,2024
 */
class globalObj {
    public var LEMAC: String = ""
    public lateinit var mainContext: Context
    public lateinit var drawSPECTER: drawSpecter
    public lateinit var drawCURSOR: drawCursor
    public var drawObjectInit: Boolean = true
    public var pagerFrame: Int = 1
    public lateinit var BTT:  BluetoothInterface
    public var initBT: Boolean = false
    public lateinit var adapter: NumberAdapter
    public lateinit var textMACADR: EditText
    public lateinit var bColor: buttonColor
    public lateinit var indicatorBT: TextView
    public lateinit var viewPager: ViewPager2
    public lateinit var PP: propControl
    public lateinit var scanButton: Button
    public lateinit var btnSpecterSS: Button
    public lateinit var btnReadFromDevice: Button
    public lateinit var btnWriteToDevice: Button

    /*
    *   Цвета для курсора
    */
    public var ColorEraseCursor: Int = 0
    public var ColorActiveCursor: Int = 0

    /*
     *  Цвета для графика типа линия
     */
    public var ColorLin: Int = 0
    public var ColorLog: Int = 0
    public var ColorFone: Int = 0
    public var ColorFoneLg: Int = 0
    /*
     *  Цвета для графика типа гистограмма
     */
    public var ColorLinGisto: Int = 0
    public var ColorLogGisto: Int = 0
    public var ColorFoneGisto: Int = 0
    public var ColorFoneLgGisto: Int = 0

    public var specterType: Int = 0                 // Разрешение спектра полученное из прибора
    public var specterGraphType: Int = 0            // Тип отображаемого спектра 0 - Линия, 1 - Гистограмма
    public var spectrResolution: Int = 0            // Разрешение спектра из настроек
    public var sendCS: UShort = 0u
    public var PCounter: UInt = 0u                  // Всего принято частиц
    public var cps: Float = 0.0f                    // Среднее cps
    public var messTm:UInt = 0u                     // Время измерения
    public var battLevel: Float = 0.0f              // Уровень батареии
    public var tempMC: Float = 0.0f                 // Температура МК
    public var pulsePerSec: UInt = 0u               // CPS за короткий интервал.

    /* Параметры для хранения в приборе */
    public var propSoundKvant: Boolean = false      // Озвучка прихода частицы
    public var propLedKvant: Boolean = true         // Подсветка прихода частицы
    public var propButtonInit: Boolean = false      // Флаг активности изменения состояния переключателей
    public var propLevel1: Int = 0
    public var propLevel2: Int = 0
    public var propLevel3: Int = 0
    public var propSoundLevel1: Boolean = true
    public var propSoundLevel2: Boolean = true
    public var propSoundLevel3: Boolean = true
    public var propVibroLevel1: Boolean = true
    public var propVibroLevel2: Boolean = true
    public var propVibroLevel3: Boolean = true
    public var propAutoStartSpectrometr: Boolean = false
    public var propCPS2UR: Float = 0.0f
    public var propCoefA: Float = 0.0f
    public var propCoefB: Float = 0.0f
    public var propCoefC: Float = 0.0f
    public var propComparator: UShort = 0u
    public var propHVoltage: UShort = 0u
    public var windowSMA: Int = 5
}