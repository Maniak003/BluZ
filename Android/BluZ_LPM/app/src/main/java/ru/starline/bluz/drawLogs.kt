package ru.starline.bluz

import android.icu.util.TimeZone
import android.text.Html
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Created by ed on 27,февраль,2025
 */
class drawLogs {
    data class LG(
        var tm: UInt,
        var act: UByte
        )

    val LOG_BUFFER_SIZE = 50
    public var logsDrawIsInit: Boolean = false
    public val logData = Array(LOG_BUFFER_SIZE) { LG(0u, 0u) }
    public lateinit var logView : ScrollView
    public lateinit var logsText: TextView

    /*
    *   Обновление логов
    *
    *	0 - Отсутствие события
    *	1 - Включение прибора
    *	2 - превышение уровня 1
    *	3 - превышение уровня 2
    *	4 - превышение уровня 3
    *	5 - нормальный уровень
    */
    fun updateLogs() {
        logsText.text = ""
        var TZ: TimeZone = TimeZone.getDefault()
        val unixTime = Date().time
        Log.i("BluZ-BT", "UT: $unixTime")
        var s: String = ""
        for (idx: Int in 0 until LOG_BUFFER_SIZE) {
            if (logData[idx].act > 0u) {
                var eventStr: String = "Unknown"
                when (logData[idx].act.toInt() ) {
                    1 -> eventStr = "Turn on"
                    2 -> eventStr = "<font color=#FFBF00>Level 1"
                    3 -> eventStr = "<font color=#C80000>Level 2"
                    4 -> eventStr = "<font color=#B02EE8>Level 3"
                    5 -> eventStr = "<font color=#1AFF00>Normal"
                }
                Log.i("BluZ-BT", "LT: " + GO.messTm.toString())
                var logTime = unixTime - (GO.messTm.toInt() - logData[idx].tm.toLong()) * 1000
                var sdf: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                s = s + sdf.format(logTime) + " " + eventStr + "</font><br>"
            }
        }
        logsText.text = Html.fromHtml(s, s.length)
    }
}