package ru.starline.bluz.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

/**
 * Сущность Room — трек (контейнер для группы точек GPS-измерений).
 *
 * @property id Сгенерированный Room ID.
 * @property name Имя трека (задаётся пользователем при создании).
 * @property createdAt Unix-timestamp создания (по умолчанию `strftime('%s', 'now')`).
 * @property isActive Флаг активного трека — в него пишутся новые точки в [ru.starline.bluz.MainActivity.recordTrackPoint]
 *  и [ru.starline.bluz.BleMonitoringService.saveToTrack]. Активным может быть только один.
 * @property isHidden Мягкое удаление — трек скрыт из списка но не удалён физически.
 *  Сейчас при `deleteTrack` запись удаляется CASCADE, флаг резерв на будущее.
 * @property cps2urh Снимок коэффициента CPS→μR/h на момент создания трека. Хранится, чтобы
 *  при отрисовке трека в будущем использовать правильное соответствие дозы (если в настройках
 *  сейчас другой коэф).
 */
@Entity(
    tableName = "tracks",
    indices = [Index("is_active")]
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at", defaultValue = "(strftime('%s', 'now'))") val createdAt: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
    @ColumnInfo(name = "cps2urh") val cps2urh: Float = 0.0f
)