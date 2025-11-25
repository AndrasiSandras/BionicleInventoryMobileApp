package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ListDao {

    @Query("SELECT * FROM lists")
    suspend fun getAllLists(): List<ListEntity>

    @Insert
    suspend fun insertList(list: ListEntity): Long

    @Query("DELETE FROM lists")
    suspend fun deleteAll()
}