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
/**
 * Управление логами приложения и прибора. **Не рисует на Canvas** — работает с TextView
 * через HTML-форматирование.
 *
 * **Application log** ([appendAppLogs]) — буфер строк со временем и цветовой кодировкой
 * в [globalObj.appLogBuffer]:
 *  - 0 — ошибка (красный)
 *  - 1 — info (синий)
 *  - 2 — warning (жёлтый)
 *  - 3 — success (зелёный)
 *  - 4 — debug (серый)
 *  - 5 — trace (пурпурный)
 *
 * Фильтрация — уровень показа задаётся [globalObj.appLogLevel] (0..5).
 *
 * **Hardware log** ([updateLogs]) — 50 последних событий прибора (массив [logData]).
 * Заполняется в [MainActivity.applyFrameToUi] из [DeviceFrame.logEntries]. Время
 * записи вычисляется относительно [globalObj.messTm].
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
    /**
     * Добавляет строку в [globalObj.appLogBuffer] со временем и цветом по уровню `lev`.
     * Игнорирует если `lev > GO.appLogLevel` (фильтрация по настройке).
     *
     * Сразу вызывает [updateAppLogs] — TextView обновляется немедленно.
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
    /** Перерисовывает app-log TextView из [globalObj.appLogBuffer] (parse HTML).
     *  Автоскролл вниз через `post { fullScroll(FOCUS_DOWN) }`. */
    fun updateAppLogs() {
        if (!this::appLogText.isInitialized) {
            return
        }
        if (!this::appLogView.isInitialized) {
            return
        }
        MainScope().launch {                    // Конструкция необходима для модификации чужого контекста
            //withContext(Dispatchers.Main) {     // Иначе перестает переключаться ViewPage2
                appLogText.text = Html.fromHtml(GO.appLogBuffer, Html.FROM_HTML_MODE_COMPACT)
            // Прокрутка вниз после отрисовки
            appLogView.post {
                appLogView.fullScroll(ScrollView.FOCUS_DOWN)
                //appLogView.smoothScrollTo(0, appLogView.getChildAt(0)?.height ?: 0)
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
    *   13 -    Изменение количества бит в канале
    *   14 -    Перегрузка
    *   15 -    Калибровка аккумулятора
    */
    /**
     * Перерисовывает hardware-log TextView из массива [logData] (50 [LG] записей).
     *
     * Для каждого ненулевого `act` (код события 1..18) выбирается имя события + цвет:
     *  - Turn on, Level 1/2/3, Normal, Dosimeter/Spectrometer reset, Write to flash,
     *    Start/Stop spectrometer, Clear logs, Change resolution, Change bits, Overload,
     *    Calibrate Batt, Low battery, Clear history, Need upgrade
     *
     * Время вычисляется как `unixNow - (messTm - tm) * 1000` — конвертация относительного
     * timestamp прибора в абсолютный.
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
                eventStr = when (logData[idx].act.toInt() ) {
                    1 -> "Turn on"
                    2 -> "<font color=#FFBF00>Level 1"
                    3 -> "<font color=#C80000>Level 2"
                    4 -> "<font color=#B02EE8>Level 3"
                    5 -> "<font color=#1AFF00>Normal"
                    6 -> "Dosimeter reset"
                    7 -> "Spectrometer reset"
                    8 -> "Write to flash"
                    9 -> "Start spectrometer"
                    10 -> "Stop spectrometer"
                    11 -> "Clear logs"
                    12 -> "Change resolution"
                    13 -> "Change bits of chan"
                    14 -> "<font color=#FF2EE8>Overload"
                    15 -> "Calibrate Batt"
                    16 -> "<font color=#FF2EE8>Low battery"
                    17 -> "Clear history"
                    18 -> "<font color=#FF2EE8>Need upgrade."
                    else -> "Unknown: ${logData[idx].act.toInt()}"
                }
                //Log.i("BluZ-BT", "LT: " + GO.messTm.toString())
                var logTime = unixTime - (GO.messTm.toInt() - logData[idx].tm.toLong()) * 1000
                s = s + sdf.format(logTime) + " " + eventStr + "</font><br>"
            }
        }
        logsText.text = Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT)
    }
}