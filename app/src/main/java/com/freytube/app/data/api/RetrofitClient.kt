package com.freytube.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val DEFAULT_BASE_URL = "https://pipedapi.kavin.rocks/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var retrofit: Retrofit? = null
    private var pipedApi: PipedApi? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "FreyTube/1.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getApi(baseUrl: String = DEFAULT_BASE_URL): PipedApi {
        if (pipedApi == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            pipedApi = retrofit!!.create(PipedApi::class.java)
        }
        return pipedApi!!
    }

    fun getOkHttpClient(): OkHttpClient = okHttpClient

    // List of known Piped API instances for fallback
    val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks/",
        "https://pipedapi.adminforge.de/",
        "https://api.piped.projectsegfau.lt/",
        "https://pipedapi.in.projectsegfau.lt/",
        "https://pipedapi.leptons.xyz/"
    )
}
