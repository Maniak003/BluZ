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
    public var ColorLin: Int = 0
    public var ColorLog: Int = 0
    public var ColorFone: Int = 0
    public var specterType: Int = 0

    /* Параметры для хранения в приборе */
    public var propLevel1: Float = 0.0f
    public var propLevel2: Float = 0.0f
    public var propLevel3: Float = 0.0f
    public var propCPS2UR: Float = 0.0f
    public var propCoefA: Float = 0.0f
    public var propCoefB: Float = 0.0f
    public var propCoefC: Float = 0.0f
    public var propIndicator: UByte = 0u
    public var propComparator: UInt = 0u
    public var propHVoltage: UInt = 0u


}