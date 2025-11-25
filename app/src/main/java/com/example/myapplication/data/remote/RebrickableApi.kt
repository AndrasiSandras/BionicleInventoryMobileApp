package com.example.myapplication.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface RebrickableApi {

    @GET("api/v3/lego/colors/")
    suspend fun getColors(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): ColorsResponseDto
}
