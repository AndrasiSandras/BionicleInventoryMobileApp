package com.example.myapplication.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.myapplication.data.remote.PartColorDetailDto

interface RebrickableApi {

    @GET("api/v3/lego/colors/")
    suspend fun getColors(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): ColorsResponseDto

    @GET("api/v3/lego/parts/")
    suspend fun searchParts(
        @Query("part_num") partNum: String,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): PartsResponseDto

    @GET("/api/v3/lego/parts/{part_num}/colors/")
    suspend fun getPartColors(
        @Path("part_num") partNum: String,
        @Query("page_size") pageSize: Int = 100
    ): PartColorsResponseDto

    @GET("api/v3/lego/parts/{part_num}/colors/{color_id}/")
    suspend fun getPartColorDetail(
        @Path("part_num") partNum: String,
        @Path("color_id") colorId: Int
    ): PartColorDetailDto


}
