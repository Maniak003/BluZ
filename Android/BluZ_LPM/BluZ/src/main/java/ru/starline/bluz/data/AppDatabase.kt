package ru.starline.bluz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.starline.bluz.data.dao.DosimeterDao
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail
import ru.starline.bluz.data.entity.*

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder( context.applicationContext,AppDatabase::class.java,"dosimeter.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false) // При миграции не пересоздавать все таблицы.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tracks ADD COLUMN cps2urh REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_2_3 = object : Migration(1, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаём таблицу detectors с BLOB-полем для вектора
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS detectors (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                change_at INTEGER NOT NULL,
                chi_vector BLOB NOT NULL
            )
        """.trimIndent())

        // Опционально: создаём индекс для быстрого поиска по имени
        database.execSQL("CREATE INDEX IF NOT EXISTS index_detectors_name ON detectors(name)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE detectors ADD COLUMN curActive INTEGER NOT NULL DEFAULT 0")
    }
}

