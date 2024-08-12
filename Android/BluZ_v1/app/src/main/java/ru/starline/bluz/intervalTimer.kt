package ru.starline.bluz

import java.util.Timer
import java.util.TimerTask


internal class intervalTimer {
    private var timer: Timer = Timer()
    private var mTimerTask: TimerTask = MyTimerTask()

    fun startTimer() {
        timer.schedule(mTimerTask, 2000, 10000)
    }
}

internal class MyTimerTask : TimerTask() {
    override fun run() {
        if (GO.BTT.connected) {
            // BT connected
        } else {
            GO.BTT.destroyDevice()
            GO.BTT.initLeDevice()
        }
    }
}

