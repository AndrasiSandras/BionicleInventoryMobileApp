package com.example.myapplication.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object RebrickableClient {

    // IDE ÍRD BE A SAJÁT API KEY-T
    private const val API_KEY = "6e70678dba9c826cb2f4e7d14fdb2bd1"

    private const val BASE_URL = "https://rebrickable.com/"

    // Interceptor: minden kéréshez hozzáteszi a ?key=API_KEY paramétert
    private val authInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        val originalUrl: HttpUrl = originalRequest.url()

        val urlWithKey = originalUrl.newBuilder()
            .addQueryParameter("key", API_KEY)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(urlWithKey)
            .build()

        chain.proceed(newRequest)
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: RebrickableApi = retrofit.create(RebrickableApi::class.java)
}
