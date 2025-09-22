package ru.starline.bluz.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.starline.bluz.data.entity.PointType
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

@Dao
interface DosimeterDao {

    @Query("SELECT * FROM tracks WHERE is_hidden = 0 ORDER BY created_at DESC")
    suspend fun getActiveTracks(): List<Track>

    @Query("SELECT * FROM tracks WHERE is_hidden = 0 ORDER BY created_at DESC")
    suspend fun getAllTracks(): List<Track>

    @Query(value = "SELECT id FROM tracks WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentTrack(): Long?

    @Query(value = "SELECT MIN(id) FROM tracks")
    suspend fun getFirstTrack(): Long

    @Query("SELECT * FROM tracks WHERE is_hidden = 0")
    fun getActiveTracksFlow(): Flow<Track>

    /* Получение точек по track_id */
    @Query("SELECT * FROM track_details WHERE track_id = :trackId ORDER BY timestamp")
    suspend fun getPointsForTrack(trackId: Long): List<TrackDetail>

    @Query("SELECT * FROM track_details WHERE track_id = :trackId AND point_type_id = :typeId")
    suspend fun getPointsByType(trackId: Long, typeId: Int): List<TrackDetail>

    @Query("UPDATE tracks SET is_active = 0 WHERE id = :trackId")
    suspend fun deactivateTrack(trackId: Long)

    @Query("UPDATE tracks SET is_active = 1 WHERE id = :trackId")
    suspend fun activateTrack(trackId: Long)

    @Query(value = "SELECT * FROM point_types")
    suspend fun getAllPointTypes():List<PointType>

    @Insert
    suspend fun insertPoint(detail: TrackDetail)

    @Insert
    suspend fun insertTrack(track: Track): Long

    @Insert
    suspend fun insertPointType(pointType: PointType): Long
}
