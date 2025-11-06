package ru.starline.bluz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.starline.bluz.data.dao.DosimeterDao
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

@Database(
    entities = [Track::class, TrackDetail::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dosimeterDao(): DosimeterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder( context.applicationContext,AppDatabase::class.java,"dosimeter.db")
                    .addMigrations(MIGRATION_1_2)
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