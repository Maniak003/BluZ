package ru.starline.bluz.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.starline.bluz.data.entity.DetectorType
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

/**
 * Data Access Object для всех операций с БД [ru.starline.bluz.data.AppDatabase].
 *
 * Все методы — `suspend`, вызываются из корутин (обычно `viewLifecycleOwner.lifecycleScope.launch`).
 *
 * **Группы методов:**
 *  - **Tracks:** [getActiveTracks], [getAllTracks], [getCurrentTrack], [getSelectTrack],
 *    [getCPS2URH], [getFirstTrack], [insertTrack], [editTrack], [activateTrack],
 *    [deactivateTrack], [deactivateAllTracks], [deleteTrack]
 *  - **Track details:** [getPointsForTrack], [getMaxMinForTrack], [insertPoint]
 *  - **Detectors:** [insertDetector], [editDetector], [editDetectorCHI], [deleteDetector],
 *    [getByIdDetector], [getAllDetector], [activateDetector]
 */
@Dao
interface DosimeterDao {

    /** Не скрытые треки в порядке убывания даты создания. */
    @Query("SELECT * FROM tracks WHERE is_hidden = 0 ORDER BY created_at DESC")
    suspend fun getActiveTracks(): List<Track>

    /** Все треки включая скрытые. */
    @Query("SELECT * FROM tracks ORDER BY created_at DESC")
    suspend fun getAllTracks(): List<Track>

    /** ID активного трека (туда пишем точки). null если нет активного. */
    @Query(value = "SELECT id FROM tracks WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentTrack(): Long?

    /** Имя трека по ID. */
    @Query(value = "SELECT name FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getSelectTrack(trackId: Long): String

    /** Коэф `cps2urh`, который был активен в момент создания трека.
     *  Используется для корректной отрисовки точек, если в настройках сейчас другой коэф. */
    @Query(value = "SELECT cps2urh FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getCPS2URH(trackId: Long): Float?

    /** ID самого первого (старого) трека. Используется как fallback при отсутствии активного. */
    @Query(value = "SELECT MIN(id) FROM tracks")
    suspend fun getFirstTrack(): Long

    /** Flow для подписки на изменения списка треков. Не используется в текущей версии. */
    @Query("SELECT * FROM tracks WHERE is_hidden = 0")
    fun getActiveTracksFlow(): Flow<Track>

    /** Все точки трека в хронологическом порядке. */
    @Query("SELECT * FROM track_details WHERE track_id = :trackId ORDER BY timestamp")
    suspend fun getPointsForTrack(trackId: Long): List<TrackDetail>

    /** Min/max CPS в треке — для нормализации цветовой палитры при отрисовке.
     *  Точки с `cps == 0` игнорируются (нет данных). */
    @Query("SELECT ifnull(max(cps),0) as max, ifnull(min(cps), 0) as min FROM track_details WHERE cps > 0 and track_id = :trackId")
    suspend fun getMaxMinForTrack(trackId: Long): MaxMinDetail?

    /** Снимает флаг активности с указанного трека. */
    @Query("UPDATE tracks SET is_active = 0 WHERE id = :trackId")
    suspend fun deactivateTrack(trackId: Long)

    /** Снимает флаг активности со всех треков (используется перед `activateTrack` нового). */
    @Query("UPDATE tracks SET is_active = 0")
    suspend fun deactivateAllTracks()

    /** Переименовать трек. */
    @Query(value = "UPDATE tracks SET name = :trcName WHERE id = :trackId")
    suspend fun editTrack(trackId: Long, trcName: String)

    /** Активировать трек (запись будет писаться сюда). */
    @Query("UPDATE tracks SET is_active = 1 WHERE id = :trackId")
    suspend fun activateTrack(trackId: Long)

    /** Полное удаление трека (CASCADE удаляет связанные `track_details`). */
    @Query("DELETE FROM tracks where id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    /** Вставка одной точки в трек. Вызывается из [ru.starline.bluz.MainActivity.recordTrackPoint]
     *  при каждом BLE-фрейме (если активна запись) и из [ru.starline.bluz.BleMonitoringService.saveToTrack]
     *  при каждом BLE-advertising. */
    @Insert
    suspend fun insertPoint(detail: TrackDetail)

    /** Создание нового трека. Возвращает сгенерированный ID. */
    @Insert
    suspend fun insertTrack(track: Track): Long

    /** Добавить или обновить детектор по conflict-стратегии REPLACE. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetector(detector: DetectorType): Long

    /** Переименовать детектор. */
    @Query(value = "UPDATE detectors SET name = :detectorName WHERE id = :detectorId")
    suspend fun editDetector(detectorId: Long, detectorName: String)

    /** Обновить χ-вектор детектора (передаётся как ByteArray из 1024 × 8 = 8192 байт). */
    @Query(value = "UPDATE detectors SET chiVector = :detectorVector WHERE id = :detectorId")
    suspend fun editDetectorCHI(detectorId: Long, detectorVector: ByteArray): Int

    /** Удалить детектор. */
    @Query(value = "DELETE FROM detectors WHERE id = :detectorId")
    suspend fun deleteDetector(detectorId: Long)

    /** Получить полный детектор (включая χ-вектор) по ID. null если не найден. */
    @Query("SELECT * FROM detectors WHERE id = :id")
    suspend fun getByIdDetector(id: Long): DetectorType?

    /** Лёгкая выборка для UI-списка — без BLOB χ-вектора. */
    @Query("SELECT id, name, changeAt, curActive FROM detectors ORDER BY name")
    suspend fun getAllDetector(): List<DetectorSummary>

    /** Активировать один детектор (с CASE-выражением — все остальные деактивируются за один запрос).
     *  После активации [ru.starline.bluz.DoseCalculator.chiVectorOrg] должен быть перезагружен
     *  отдельным `getByIdDetector` + копированием. */
    @Query("UPDATE detectors SET curActive = CASE WHEN id = :detectorId THEN 1 ELSE 0 END")
    suspend fun activateDetector(detectorId: Long)
}

/** DTO для [DosimeterDao.getMaxMinForTrack] — min и max CPS трека. */
data class MaxMinDetail (
    val max: Long,
    val min: Long
)

/** DTO для [DosimeterDao.getAllDetector] — список детекторов без BLOB-вектора (для UI). */
data class DetectorSummary(
    val id: Long,
    val name: String,
    val changeAt: Long,
    val curActive: Boolean
)
