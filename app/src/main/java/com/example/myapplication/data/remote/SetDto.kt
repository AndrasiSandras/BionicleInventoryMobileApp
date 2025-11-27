package com.example.myapplication.data.remote

import com.squareup.moshi.Json
import com.example.myapplication.data.local.SetEntity

data class SetDto(
    @Json(name = "set_num") val setNum: String,
    @Json(name = "name") val name: String,
    @Json(name = "set_img_url") val imageUrl: String?,
    @Json(name = "num_parts") val numParts: Int?
)

fun SetDto.toEntity(initialQuantity: Int = 0) =
    SetEntity(
        setNum = setNum,
        name = name,
        imageUrl = imageUrl,
        quantity = initialQuantity
    )
