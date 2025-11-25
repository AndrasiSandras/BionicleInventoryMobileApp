package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PartDao {

    @Query("SELECT * FROM parts WHERE partId = :partId LIMIT 1")
    suspend fun getPartById(partId: String): PartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: PartEntity)
}
