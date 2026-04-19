package com.prog7313.buglogger.data.model

data class Bug(
    val id: Int = 0,
    val title: String? = "",
    val description: String? = "",
    val severity: String? = "",
    val reportedBy: String? = "",
    val createdAt: String = "",
    val isResolved: Boolean = false
)