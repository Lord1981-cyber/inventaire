package com.example.data.remote

import com.example.data.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var currentApiUrl: String? = null
    private var cachedApiService: ApiService? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getApiService(sessionManager: SessionManager): ApiService {
        val baseUrl = sessionManager.getApiBaseUrl()
        
        // Recreate service if base URL has changed
        if (cachedApiService != null && currentApiUrl == baseUrl) {
            return cachedApiService!!
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                
                // Add Session Cookie if available
                sessionManager.getSessionId()?.let { sessionId ->
                    requestBuilder.addHeader("Cookie", "PHPSESSID=$sessionId")
                }
                
                chain.proceed(requestBuilder.build())
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        currentApiUrl = baseUrl
        cachedApiService = retrofit.create(ApiService::class.java)
        return cachedApiService!!
    }
}
