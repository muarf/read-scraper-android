package com.readscraper.android.data.model

data class ScrapeResponse(
    val job_id: String?,
    val status: String,
    val url: String?,
    val search_terms: String?,
    val message: String?,
    val article_id: String? = null,
    val cached: Boolean = false
)

