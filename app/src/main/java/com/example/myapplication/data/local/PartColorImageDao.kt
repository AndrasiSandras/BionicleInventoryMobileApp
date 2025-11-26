package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PartColorImageDao {

    @Query(
        "SELECT * FROM part_color_images " +
                "WHERE partId = :partId AND colorId = :colorId LIMIT 1"
    )
    suspend fun getImage(partId: String, colorId: Int): PartColorImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(entity: PartColorImageEntity)
}
