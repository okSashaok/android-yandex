package ru.netology.nmedia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.netology.nmedia.data.dao.MarkerDao
import ru.netology.nmedia.data.entity.MarkerEntity

@Database(entities = [MarkerEntity::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDataBase? = null //???

        fun getInstance(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDataBase::class.java,
                    "markers_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}