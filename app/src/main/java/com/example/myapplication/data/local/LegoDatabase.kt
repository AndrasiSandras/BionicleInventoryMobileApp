package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ListEntity::class,
        ListItemEntity::class,
        PartEntity::class,
        ColorEntity::class,
        PartColorImageEntity::class
    ],
    version = 3,
    exportSchema = false
)

abstract class LegoDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao
    abstract fun colorDao(): ColorDao
    abstract fun partDao(): PartDao
    abstract fun listItemDao(): ListItemDao

    abstract fun partColorImageDao(): PartColorImageDao // <- ÚJ

    companion object {
        @Volatile
        private var INSTANCE: LegoDatabase? = null

        fun getInstance(context: Context): LegoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LegoDatabase::class.java,
                    "lego_db"
                )
                    .fallbackToDestructiveMigration()   // ÚJ: verzióváltásnál törli/újraépíti a DB-t
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
