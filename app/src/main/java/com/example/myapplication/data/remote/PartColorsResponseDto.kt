package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class PartColorsResponseDto(
    @Json(name = "results") val results: List<PartColorDto>
)
