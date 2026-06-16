package com.asylus.context.data.model

import java.io.File

data class Recording(
    val file: File,
    val displayName: String,
    val durationFormatted: String,
    val timestamp: Long,
    val transcript: String? = null
)
