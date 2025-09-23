package ru.starline.bluz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.starline.bluz.data.dao.DosimeterDao
import ru.starline.bluz.data.entity.Track
import ru.starline.bluz.data.entity.TrackDetail

@Database(
    entities = [Track::class, TrackDetail::class],
    version = 1,
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
                    .fallbackToDestructiveMigration(false) // При миграции пересоздать все таблици.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
