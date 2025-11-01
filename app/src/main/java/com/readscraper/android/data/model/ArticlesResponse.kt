package com.readscraper.android.data.model

data class ArticlesResponse(
    val articles: List<Article>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

