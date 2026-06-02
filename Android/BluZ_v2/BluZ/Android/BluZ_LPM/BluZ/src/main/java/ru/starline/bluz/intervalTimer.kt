package ru.starline.bluz

import java.util.Timer
import java.util.TimerTask


/**
 * Таймер автоматического переподключения к прибору по BLE.
 *
 * Запускается через [startTimer] (из [globalObj.startBluetoothTimer] или после Save в Settings).
 * Каждые 10 секунд (с начальной задержкой 2 сек) вызывает [MyTimerTask.run]:
 *  - Если `GO.needTerminate` — `System.exit(0)` (выход через Back)
 *  - Иначе если `GO.BTT.connected == false` → `destroyDevice() + initLeDevice()` —
 *    попытка переподключения
 *
 * Останавливается через [stopTimer]. Используется в обработчиках смены MAC, выхода
 * и переключения сценариев.
 */
class intervalTimer {
    private var timer: Timer? = null
    private var mTimerTask: TimerTask? = null
    private var isRunning: Boolean = false

    /** Старт таймера. Защита от дубля через [isRunning]. Период 10 сек, начальная задержка 2 сек. */
    fun startTimer() {
        if (! isRunning) {
            timer?.cancel() // на всякий случай
            timer = Timer()
            mTimerTask = MyTimerTask()
            isRunning = true
            timer?.schedule(mTimerTask, 2000, 10000)
        }
    }

    /** Останавливает таймер, отменяет pending TimerTask. Безопасно вызывать многократно. */
    fun stopTimer() {
        if(isRunning) {
            isRunning = false
            timer?.purge()
            timer?.cancel()
        }
    }
}

/**
 * `TimerTask`, который запускается раз в 10 секунд для проверки состояния BLE и корректной
 * реакции на потерю связи.
 *
 *  - Если `GO.needTerminate == true` → `System.exit(0)` (это сигнал из performExit для
 *    тех приборов, где нужно закрыть приложение по таймеру)
 *  - Если `GO.BTT.connected == false` → принудительный `destroyDevice + initLeDevice`
 */
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

