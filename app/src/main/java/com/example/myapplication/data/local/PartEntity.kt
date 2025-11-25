package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey val partId: String,   // Rebrickable part_num
    val name: String,
    val imageUrl: String?
)
