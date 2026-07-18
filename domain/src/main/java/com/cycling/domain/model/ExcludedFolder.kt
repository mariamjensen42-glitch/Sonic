package com.cycling.domain.model

data class ExcludedFolder(
    val path: String,
    val addedAt: Long = System.currentTimeMillis()
)
