package ru.starline.bluz.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.starline.bluz.data.entity.DetectorType
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

    @Query(value = "SELECT cps2urh FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getCPS2URH(trackId: Long): Float?

    @Query(value = "SELECT MIN(id) FROM tracks")
    suspend fun getFirstTrack(): Long

    @Query("SELECT * FROM tracks WHERE is_hidden = 0")
    fun getActiveTracksFlow(): Flow<Track>

    /* Получение точек по track_id */
    @Query("SELECT * FROM track_details WHERE track_id = :trackId ORDER BY timestamp")
    suspend fun getPointsForTrack(trackId: Long): List<TrackDetail>

    /* Получим, минимальное и максимальное значение в треке */
    @Query("SELECT ifnull(max(cps),0) as max, ifnull(min(cps), 0) as min FROM track_details WHERE cps > 0 and track_id = :trackId")
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

    /* Добавление детектора */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetector(detector: DetectorType): Long

    /* Переименование детектора */
    @Query(value = "UPDATE detectors SET name = :detectorName WHERE id = :detectorId")
    suspend fun editDetector(detectorId: Long, detectorName: String)

    /* Изменение вектора детектора */
    @Query(value = "UPDATE detectors SET chiVector = :detectorVector WHERE id = :detectorId")
    suspend fun editDetectorCHI(detectorId: Long, detectorVector: DoubleArray)

    /* Удаление детектора */
    @Query(value = "DELETE FROM detectors WHERE id = :detectorId")
    suspend fun deleteDetector(detectorId: Long)

    /* Выборка конкретного детектора */
    @Query("SELECT * FROM detectors WHERE id = :id")
    suspend fun getByIdDetector(id: Long): DetectorType?

    /* Список детекторов без вектора */
    @Query("SELECT id, name, changeAt, curActive FROM detectors ORDER BY name")
    suspend fun getAllDetector(): List<DetectorSummary>

    /* Переключение активности детектора */
    @Query("UPDATE detectors SET curActive = CASE WHEN id = :detectorId THEN 1 ELSE 0 END")
    suspend fun activateDetector(detectorId: Long)
}

data class MaxMinDetail (
    val max: Long,
    val min: Long
)

// DTO для списка в UI без загрузки BLOB)
data class DetectorSummary(
    val id: Long,
    val name: String,
    val changeAt: Long,
    val curActive: Boolean
)

