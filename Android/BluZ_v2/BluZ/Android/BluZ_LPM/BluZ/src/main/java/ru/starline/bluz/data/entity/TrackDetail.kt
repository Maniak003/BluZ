package ru.starline.bluz.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/**
 * Сущность Room — одна точка трека: GPS-позиция + CPS + магнитное поле + accuracy.
 *
 * Foreign key на [Track] с `onDelete = CASCADE` — при удалении трека все его точки
 * удаляются. Индексы: `track_id` (для быстрой выборки точек трека), `timestamp` (для
 * сортировки), `(latitude, longitude)` (для пространственных запросов в будущем).
 *
 * @property id Сгенерированный Room ID.
 * @property trackId FK на родительский [Track].
 * @property latitude / longitude / altitude Координаты (WGS84).
 * @property accuracy Точность GPS в метрах. 0 = не определена.
 * @property cps Импульсов в секунду в момент фиксации.
 * @property speed Скорость, м/с.
 * @property magnitude Модуль вектора магнитного поля в мкТл (усреднён по 20 измерениям
 *  магнитометра). Только в [ru.starline.bluz.BleMonitoringService] — в foreground всегда 0.
 * @property timestamp Unix-timestamp фиксации.
 */
@Entity(
    tableName = "track_details",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("track_id"),
        Index("timestamp"),
        Index("latitude", "longitude")
    ]
)
data class TrackDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "accuracy") val accuracy: Float,
    @ColumnInfo(name = "cps") val cps: Float,
    @ColumnInfo(name = "altitude") val altitude: Double,
    @ColumnInfo(name = "speed") val speed: Float,
    @ColumnInfo(name = "magnitude") val magnitude: Double,
    @ColumnInfo(name = "timestamp", defaultValue = "(strftime('%s', 'now'))") val timestamp: Long = 0
)

