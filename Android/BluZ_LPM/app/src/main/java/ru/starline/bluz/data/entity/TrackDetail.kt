package ru.starline.bluz.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

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
