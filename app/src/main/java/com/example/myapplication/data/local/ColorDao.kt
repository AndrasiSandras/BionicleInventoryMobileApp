package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ColorDao {

    @Query("SELECT * FROM colors")
    suspend fun getAllColors(): List<ColorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colors: List<ColorEntity>)

    @Query("DELETE FROM colors")
    suspend fun deleteAll()
}
