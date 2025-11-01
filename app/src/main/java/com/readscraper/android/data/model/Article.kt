package com.readscraper.android.data.model

data class Article(
    val id: String,
    val url: String,
    val title: String,
    val html_content: String?,
    val pdf_path: String?,
    val site_source: String?,
    val created_at: String,
    val scraped_at: String?
)

