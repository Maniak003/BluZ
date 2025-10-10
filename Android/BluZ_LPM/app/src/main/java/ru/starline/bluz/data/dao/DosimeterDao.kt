package ru.starline.bluz.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

@Dao
interface DosimeterDao {

    @Query("SELECT * FROM tracks WHERE is_hidden = 0 ORDER BY created_at DESC")
    suspend fun getActiveTracks(): List<Track>

    @Query("SELECT * FROM tracks ORDER BY created_at DESC")
    suspend fun getAllTracks(): List<Track>

    @Query(value = "SELECT id FROM tracks WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentTrack(): Long?

    @Query(value = "SELECT name FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getSelectTrack(trackId: Long): String

    @Query(value = "SELECT MIN(id) FROM tracks")
    suspend fun getFirstTrack(): Long

    @Query("SELECT * FROM tracks WHERE is_hidden = 0")
    fun getActiveTracksFlow(): Flow<Track>

    /* Получение точек по track_id */
    @Query("SELECT * FROM track_details WHERE track_id = :trackId ORDER BY timestamp")
    suspend fun getPointsForTrack(trackId: Long): List<TrackDetail>

    /* Получим, минимальное и максимальное значение в треке */
    @Query("SELECT max(cps) as max, min(cps) as min FROM track_details WHERE cps > 0 and track_id = :trackId")
    suspend fun getMaxMinForTrack(trackId: Long): MaxMinDetail?


    @Query("UPDATE tracks SET is_active = 0 WHERE id = :trackId")
    suspend fun deactivateTrack(trackId: Long)

    @Query("UPDATE tracks SET is_active = 0")
    suspend fun deactivateAllTracks()

    @Query(value = "UPDATE tracks SET name = :trcName WHERE id = :trackId")
    suspend fun editTrack(trackId: Long, trcName: String)

    @Query("UPDATE tracks SET is_active = 1 WHERE id = :trackId")
    suspend fun activateTrack(trackId: Long)

    @Query("DELETE FROM tracks where id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    @Insert
    suspend fun insertPoint(detail: TrackDetail)

    @Insert
    suspend fun insertTrack(track: Track): Long
}

data class MaxMinDetail (
    val max: Long,
    val min: Long
)
