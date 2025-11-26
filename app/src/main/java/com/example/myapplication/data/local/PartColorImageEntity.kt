package com.example.myapplication.data.local

import androidx.room.Entity

@Entity(
    tableName = "part_color_images",
    primaryKeys = ["partId", "colorId"]
)
data class PartColorImageEntity(
    val partId: String,
    val colorId: Int,
    val imageUrl: String
)
