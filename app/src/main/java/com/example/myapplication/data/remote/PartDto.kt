package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class PartDto(
    @Json(name = "part_num") val partNum: String,
    @Json(name = "name") val name: String,
    @Json(name = "part_img_url") val imageUrl: String?
)

data class PartsResponseDto(
    @Json(name = "results") val results: List<PartDto>
)
