package ru.starline.bluz.data

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DatabaseConverters {

    @TypeConverter
    fun fromDoubleArray(value: DoubleArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer.allocate(value.size * java.lang.Double.BYTES)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putDouble(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toDoubleArray(value: ByteArray?): DoubleArray? {
        if (value == null) return null
        val buffer = ByteBuffer.wrap(value)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val result = DoubleArray(value.size / java.lang.Double.BYTES)
        for (i in result.indices) {
            result[i] = buffer.getDouble()
        }
        return result
    }
}
