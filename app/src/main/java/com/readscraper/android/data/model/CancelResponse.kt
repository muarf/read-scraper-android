package com.readscraper.android.data.model

data class CancelResponse(
    val message: String,
    val previous_status: String,
    val new_status: String
)

