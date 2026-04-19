package com.prog7313.buglogger.data.repository

import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.remote.RetrofitProvider

class BugRepository {
    private val api = RetrofitProvider.api

    suspend fun getBugs(): Result<List<Bug>> = runCatching {
        api.getBugs()
    }

    suspend fun getBugById(id: Int): Result<Bug> = runCatching {
        api.getBugById(id)
    }

    suspend fun createBug(request: Bug): Result<Bug> = runCatching {
        api.createBug(request)
    }
}