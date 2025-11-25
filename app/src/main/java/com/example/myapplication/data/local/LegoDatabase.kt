package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ListEntity::class, ColorEntity::class, PartEntity::class],
    version = 3,
    exportSchema = false
)
abstract class LegoDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao
    abstract fun colorDao(): ColorDao
    abstract fun partDao(): PartDao

    companion object {
        @Volatile
        private var INSTANCE: LegoDatabase? = null

        fun getInstance(context: Context): LegoDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LegoDatabase::class.java,
                    "lego_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { db ->
                        INSTANCE = db
                    }
            }
        }
    }
}
