package ru.starline.bluz

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Просмотр логов: application log (накопленный в [globalObj.appLogBuffer] через
 * [drawLogs.appendAppLogs]) и hardware log с прибора ([drawLogs.updateLogs]).
 *
 * **Layout:** `log_layout.xml`. Два ScrollView: верхний для app-лога с HTML-форматированием,
 * нижний — для hardware-лога (50 последних событий прибора).
 *
 * **Кнопки:**
 *  - Save App-log → экспорт в `/Documents/BluZ/applog_*.html`
 *  - Clear App-log → `GO.appLogBuffer = ""`
 *  - Clear HW-log → `sendCommand(4u)` — стереть лог в приборе
 *
 * Открывается из [SettingsFragment] через отдельную [LogActivity] (не в bottom-nav).
 */
class LogFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.log_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSaveAppLogs: Button = view.findViewById(R.id.buttonSaveAppLog)
        btnSaveAppLogs.setOnClickListener {
            val simpleDateFormat = SimpleDateFormat("yyyyMMdd'_'HHmmss", Locale.getDefault())
            val fileName = "AppLog" + simpleDateFormat.format(Date().time)
            var errFlag = false
            try {
                val SDPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
                val direct = File("$SDPath/BluZ")
                if (!direct.exists()) {
                    if (direct.mkdir()) {
                        Log.d("BluZ-BT", "SD Path: $SDPath/BluZ")
                        GO.drawLOG.appendAppLogs("Create ok dir: ${direct.toString()}", 3)
                    } else {
                        Log.d("BluZ-BT", "Create dir error.")
                        Toast.makeText(context, "Directory create error.", Toast.LENGTH_LONG).show()
                        GO.drawLOG.appendAppLogs("Create error dir: ${direct.toString()}", 0)
                        errFlag = true
                    }
                }
                if (!errFlag) {
                    val myFile = File("$SDPath/BluZ/$fileName.html")
                    if (myFile.createNewFile()) {
                        Log.d("BluZ-BT", "File create Ok.")
                        GO.drawLOG.appendAppLogs("Create ok file: ${myFile.toString()}", 3)
                        val outputStream = FileOutputStream(myFile)
                        outputStream.write(GO.appLogBuffer.toByteArray())
                        outputStream.close()
                        Toast.makeText(context, "Log ${myFile} save complete.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "File create error.", Toast.LENGTH_LONG).show()
                        Log.d("BluZ-BT", "Create file error.")
                    }
                }
            } catch (e: Exception) {
                Log.e("BluZ-BT", "Error: ${e.message}")
                GO.drawLOG.appendAppLogs("Save: ${e.message}", 0)
            }
        }

        val buttonClearAppLog: Button = view.findViewById(R.id.buttonClearAppLog)
        buttonClearAppLog.setOnClickListener {
            GO.appLogBuffer = ""
            GO.drawLOG.updateAppLogs()
        }

        val btnClearLog: Button = view.findViewById(R.id.buttonClearLog)
        btnClearLog.setOnClickListener {
            GO.BTT.sendCommand(4u)
            Toast.makeText(GO.mainContext, R.string.resetLogs, Toast.LENGTH_LONG).show()
        }

        GO.drawLOG.logView = view.findViewById(R.id.logScrolView)
        GO.drawLOG.logsText = view.findViewById(R.id.logsText)
        GO.drawLOG.appLogView = view.findViewById(R.id.appScrolView)
        GO.drawLOG.appLogText = view.findViewById(R.id.appLogText)

        if (!GO.drawLOG.logsDrawIsInit) {
            GO.drawLOG.updateLogs()
            GO.drawLOG.updateAppLogs()
            GO.drawLOG.logsDrawIsInit = true
        }

        GO.drawLOG.appLogView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawLOG.appLogView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawLOG.updateAppLogs()
                    GO.drawLOG.updateLogs()
                }
            }
        )
    }
}
