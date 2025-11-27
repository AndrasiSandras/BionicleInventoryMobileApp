package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sets")
data class SetEntity(
    @PrimaryKey
    val setNum: String,      // pl. "8534-1"
    val name: String,        // szett neve
    val imageUrl: String?,   // szett képe (Rebrickable set_img_url)
    val quantity: Int        // ennyit birtokolsz ebből a szettből
)
