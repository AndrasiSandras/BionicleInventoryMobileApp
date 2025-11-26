package com.example.myapplication.data.remote

import com.squareup.moshi.Json

data class PartColorDetailDto(
    @Json(name = "part_img_url") val partImgUrl: String?
)
