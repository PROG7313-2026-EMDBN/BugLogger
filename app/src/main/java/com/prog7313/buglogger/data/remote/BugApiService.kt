package com.prog7313.buglogger.data.remote

import com.prog7313.buglogger.data.model.Bug
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BugApiService {
    @GET("bugs")
    suspend fun getBugs(): List<Bug>

    @GET("bugs/{id}")
    suspend fun getBugById(@Path("id") id: Int): Bug

    @POST("bugs")
    suspend fun createBug(@Body request: Bug): Bug
}