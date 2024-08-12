package ru.starline.bluz

import java.util.Timer
import java.util.TimerTask


internal class intervalTimer {
    var timer: Timer = Timer()
    var mTimerTask: TimerTask = MyTimerTask()

    fun startTimer() {
        timer.schedule(mTimerTask, 1000, 30000)
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

