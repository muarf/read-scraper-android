package com.readscraper.android.data.model

data class JobStatus(
    val id: String,
    val url: String?,
    val status: String,
    val created_at: String?,
    val started_at: String?,
    val completed_at: String?,
    val error_message: String?,
    val current_step: String?,
    val step_description: String?,
    val search_terms: String?,
    val extracted_title: String?,
    val search_results_count: Int?,
    val best_match_title: String?,
    val best_match_percentage: Int?,
    val best_match_source: String?,
    val article_id: String?
)

