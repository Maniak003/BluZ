package ru.starline.bluz.data

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Type-converter Room для сериализации [DoubleArray] ↔ [ByteArray] (little-endian, 8 байт на double).
 *
 * Используется для хранения χ-вектора детектора (`detectors.chiVector`) — Room не умеет
 * хранить `DoubleArray` напрямую, поэтому конвертируем в BLOB.
 *
 * Аннотируется на [ru.starline.bluz.data.AppDatabase] через `@TypeConverters(DatabaseConverters::class)`.
 */
class DatabaseConverters {

    /** Сериализует [DoubleArray] в [ByteArray] little-endian, 8 байт на элемент. null → null. */
    @TypeConverter
    fun fromDoubleArray(value: DoubleArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer.allocate(value.size * java.lang.Double.BYTES)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putDouble(it) }
        return buffer.array()
    }

    /** Десериализует [ByteArray] (little-endian, кратно 8 байт) обратно в [DoubleArray].
     *  null → null. */
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
