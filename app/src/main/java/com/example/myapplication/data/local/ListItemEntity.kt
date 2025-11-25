package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["partId"],
            childColumns = ["partId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("listId"),
        Index("partId")
    ]
)
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val partId: String,
    val colorId: Int,
    val quantity: Int
)
