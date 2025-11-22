package ru.starline.bluz

import android.graphics.Color
import android.icu.util.TimeZone
import android.text.Html
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    public lateinit var appLogView: ScrollView
    public lateinit var appLogText: TextView
    private var sdf: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US)

    /*
    *   События приложения
    * lev:
    * 0 - Ошибка
    * 1 - Информация
    * 2 - Предупреждение
    * 3 - Успешно
    * 4 - Отладка
    * 5 - Отдадка 2
    *
    */
    fun appendAppLogs(evtText: String, lev: Int) {
        if (GO.appLogLevel >= lev) {
            val colorEvt: String = when (lev) {
                0 -> " <font color=#FF0000>"
                1 -> " <font color=#0000FF>"
                2 -> " <font color=#FFFF00>"
                3 -> " <font color=#00FF00>"
                4 -> " <font color=#3F3F3F>"
                5 -> " <font color=#FF00FF>"
                else -> " <font color=#7F7F7F>"
            }
            val s = sdf.format(Date().time) + colorEvt + evtText + "</font><br>"
            GO.appLogBuffer = GO.appLogBuffer + s
            updateAppLogs()
        }
    }
    fun updateAppLogs() {
        if (!this::appLogText.isInitialized) {
            return
        }
        if (!this::appLogView.isInitialized) {
            return
        }
        MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
            withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                appLogText.text = Html.fromHtml(GO.appLogBuffer, GO.appLogBuffer.length)
            }
        }
    }
    /*
    *   Обновление логов
    *
    *	0 -     Отсутствие события
    *	1 -     Включение прибора
    *	2 -     превышение уровня 1
    *	3 -     превышение уровня 2
    *	4 -     превышение уровня 3
    *	5 -     нормальный уровень
    *   6 -     сброс дозиметра
    *   7 -     сброс спектрометра
    *   8 -     запись данных во флеш
    *   9 -     запуск спектрометра
    *   10 -    останов спектрометра
    *   11 -    Очистка лога
    *   12 -    Сброс спектрометра при переключении разрешения
    *   13 -    Перегрузка
    */
    fun updateLogs() {
        logsText.text = ""
        //var TZ: TimeZone = TimeZone.getDefault()
        val unixTime = Date().time
        //Log.i("BluZ-BT", "UT: $unixTime")
        var s: String = ""
        for (idx: Int in 0 until LOG_BUFFER_SIZE) {
            if (logData[idx].act > 0u) {
                var eventStr: String = ""
                when (logData[idx].act.toInt() ) {
                    1 -> eventStr = "Turn on"
                    2 -> eventStr = "<font color=#FFBF00>Level 1"
                    3 -> eventStr = "<font color=#C80000>Level 2"
                    4 -> eventStr = "<font color=#B02EE8>Level 3"
                    5 -> eventStr = "<font color=#1AFF00>Normal"
                    6 -> eventStr = "Dosimeter reset"
                    7 -> eventStr = "Spectrometer reset"
                    8 -> eventStr = "Write to flash"
                    9 -> eventStr = "Start spectrometer"
                    10 -> eventStr = "Stop spectrometer"
                    11 -> eventStr = "Clear logs"
                    12 -> eventStr = "Change resolution"
                    13 -> eventStr = "<font color=#FF2EE8>Overload"
                    else -> eventStr = "Unknown: ${logData[idx].act.toInt()}"
                }
                //Log.i("BluZ-BT", "LT: " + GO.messTm.toString())
                var logTime = unixTime - (GO.messTm.toInt() - logData[idx].tm.toLong()) * 1000
                s = s + sdf.format(logTime) + " " + eventStr + "</font><br>"
            }
        }
        logsText.text = Html.fromHtml(s, s.length)
    }
}