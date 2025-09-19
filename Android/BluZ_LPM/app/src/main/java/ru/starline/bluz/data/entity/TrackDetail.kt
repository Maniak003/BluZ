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
        ),
        androidx.room.ForeignKey(
            entity = PointType::class,
            parentColumns = ["id"],
            childColumns = ["point_type_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("track_id"),
        Index("point_type_id"),
        Index("timestamp"),
        Index("latitude", "longitude")
    ]
)
data class TrackDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: Long,
    @ColumnInfo(name = "point_type_id") val pointTypeId: Int,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "timestamp", defaultValue = "(strftime('%s', 'now'))") val timestamp: Long = 0
)
