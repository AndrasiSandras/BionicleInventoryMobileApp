package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class PartColorDto(
    @Json(name = "color_id") val colorId: Int
)
