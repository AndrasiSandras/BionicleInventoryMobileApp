package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SetDao {

    @Query("SELECT * FROM sets ORDER BY name")
    suspend fun getAllSets(): List<SetEntity>

    @Query("SELECT * FROM sets WHERE setNum = :setNum LIMIT 1")
    suspend fun getSetByNum(setNum: String): SetEntity?

    // Ha már létezik ez a setNum, felülírjuk (pl. új quantity-vel)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(set: SetEntity)

    @Query("UPDATE sets SET quantity = :quantity WHERE setNum = :setNum")
    suspend fun updateQuantity(setNum: String, quantity: Int)

    @Delete
    suspend fun delete(set: SetEntity)
}
