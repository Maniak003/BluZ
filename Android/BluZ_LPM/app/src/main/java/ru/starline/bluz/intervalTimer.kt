package ru.starline.bluz

import java.util.Timer
import java.util.TimerTask


class intervalTimer {
    private var timer: Timer? = null
    private var mTimerTask: TimerTask? = null
    private var isRunning: Boolean = false

    fun startTimer() {
        if (! isRunning) {
            timer?.cancel() // на всякий случай
            timer = Timer()
            mTimerTask = MyTimerTask()
            isRunning = true
            timer?.schedule(mTimerTask, 2000, 10000)
        }
    }

    fun stopTimer() {
        if(isRunning) {
            isRunning = false
            timer?.purge()
            timer?.cancel()
        }
    }
}

internal class MyTimerTask : TimerTask() {
    override fun run() {
        if (GO.allPermissionAccept) {
            if (GO.needTerminate) {     // Завершение приложения
                System.exit(0)
            }
            if (GO.BTT.connected) {
                // BT connected
            } else {
                GO.BTT.destroyDevice()
                GO.BTT.initLeDevice()
            }
        } else {
            GO.BTT.connected = false
        }
    }
}

