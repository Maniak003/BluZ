package ru.starline.bluz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.starline.bluz.data.dao.DosimeterDao
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail
import ru.starline.bluz.data.entity.*

/**
 * Room-обёртка над SQLite-базой `dosimeter.db`.
 *
 * **Версия:** 3.
 *
 * **Стратегия миграции:** [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration]
 * (true). При несовпадении версии БД сносится и создаётся с нуля. Так решено намеренно
 * 2026-05-23 — миграции в репозитории были битые (см. project_state.md), пользователей мало.
 *
 * **Сущности:**
 *  - [Track] — трек (имя, дата, флаги active/hidden, cps2urh)
 *  - [TrackDetail] — точка трека с FK на Track (CASCADE)
 *  - [DetectorType] — детектор с χ-вектором (1024 значения)
 *
 * **DAO:** [DosimeterDao]. Все запросы suspend, вызываются из корутин.
 *
 * **Доступ:** через [getDatabase] — singleton с double-check locking. В приложении лежит
 * в `GO.dao` (инициализируется в `MainActivity.onCreate`).
 */
@Database(
    entities = [Track::class, TrackDetail::class, DetectorType::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dosimeterDao(): DosimeterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Возвращает singleton [AppDatabase]. Использует double-check locking для
         * потокобезопасной инициализации.
         *
         * Стратегия миграции — [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration].
         * При несовпадении версии БД сносится. Решено 2026-05-23 — миграции были битые,
         * пользователей мало.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dosimeter.db")
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
