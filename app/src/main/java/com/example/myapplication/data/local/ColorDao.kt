package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ColorDao {

    @Query("SELECT * FROM colors")
    suspend fun getAllColors(): List<ColorEntity>

    @Query("SELECT * FROM colors WHERE id = :id")
    suspend fun getColorById(id: Int): ColorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColors(colors: List<ColorEntity>)

    @Query("DELETE FROM colors")
    suspend fun deleteAll()
}
