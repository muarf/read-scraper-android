package com.readscraper.android.data.model

data class DebugScreenshot(
    val filename: String,
    val url: String,
    val type: String,
    val job_id: String,
    val timestamp: Long,
    val datetime: String,
    val size: Long
)

data class DebugScreenshotsResponse(
    val screenshots: List<DebugScreenshot>,
    val total: Int
)

