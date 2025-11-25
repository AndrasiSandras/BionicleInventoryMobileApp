package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ListItemDao {

    @Query("SELECT * FROM list_items WHERE listId = :listId")
    suspend fun getItemsForList(listId: Long): List<ListItemEntity>

    @Insert
    suspend fun insertItem(item: ListItemEntity)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: Long)
}
