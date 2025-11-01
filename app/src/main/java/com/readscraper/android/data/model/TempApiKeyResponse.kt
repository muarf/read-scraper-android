package com.readscraper.android.data.model

data class TempApiKeyResponse(
    val api_key: String,
    val expires_in: Int,
    val message: String
)

