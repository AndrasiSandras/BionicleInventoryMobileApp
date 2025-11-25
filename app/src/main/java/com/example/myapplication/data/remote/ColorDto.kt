package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class ColorDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "rgb") val rgb: String
)

data class ColorsResponseDto(
    @Json(name = "results") val results: List<ColorDto>
)
