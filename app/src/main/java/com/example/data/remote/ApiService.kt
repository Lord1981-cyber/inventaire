package com.example.data.remote

import com.example.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("actions/mobile_login.php")
    suspend fun login(
        @Body credentials: Map<String, String>
    ): Response<LoginResponse>

    @GET("actions/mobile_equipements.php")
    suspend fun getEquipements(
        @Query("action") action: String = "list",
        @Query("salle_id") salleId: Int? = null
    ): Response<EquipementsResponse>

    @GET("actions/mobile_equipements.php")
    suspend fun getEquipementByCode(
        @Query("action") action: String = "get_by_code",
        @Query("code") code: String
    ): Response<EquipementDetailResponse>

    @POST("actions/mobile_controle.php")
    suspend fun submitControle(
        @Body controleData: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("actions/mobile_anomalie.php")
    suspend fun submitAnomalie(
        @Body anomalieData: Map<String, String>
    ): Response<Map<String, Any>>
}
