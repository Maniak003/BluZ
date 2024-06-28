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
    public var drawObjectInit: Boolean = true

}