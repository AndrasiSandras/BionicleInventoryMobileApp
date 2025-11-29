package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class SetPartDto(
    @Json(name = "id") val id: Int,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "is_spare") val isSpare: Boolean,
    @Json(name = "part") val part: PartDto,
    @Json(name = "color") val color: ColorDto
)

data class SetPartsResponseDto(
    @Json(name = "results") val results: List<SetPartDto>
)
