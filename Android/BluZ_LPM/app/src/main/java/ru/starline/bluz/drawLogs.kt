package ru.starline.bluz

/**
 * Created by ed on 27,февраль,2025
 */
class drawLogs {
    data class LG(
        var tm: UInt,
        var act: UByte
        )

    val LOG_BUFFER_SIZE = 50
    public val logData = Array(LOG_BUFFER_SIZE) { LG(0u, 0u) }
}