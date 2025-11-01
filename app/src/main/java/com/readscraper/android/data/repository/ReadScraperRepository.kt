package com.readscraper.android.data.repository

import com.readscraper.android.data.api.ApiClient
import com.readscraper.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadScraperRepository {
    private val api = ApiClient.api
    
    suspend fun getTempApiKey(): Result<TempApiKeyResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTempApiKey()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun scrape(apiKey: String, request: ScrapeRequest): Result<ScrapeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.scrape(apiKey, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Erreur inconnue"
                Result.failure(Exception("Erreur: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobStatus(apiKey: String, jobId: String): Result<JobStatus> = withContext(Dispatchers.IO) {
        try {
            val response = api.getJobStatus(apiKey, jobId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getArticle(apiKey: String, articleId: String): Result<Article> = withContext(Dispatchers.IO) {
        try {
            val response = api.getArticle(apiKey, articleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadPdf(apiKey: String, articleId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadPdf(apiKey, articleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.bytes())
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getArticles(
        apiKey: String?,
        limit: Int = 50,
        offset: Int = 0,
        search: String? = null,
        siteSource: String? = null
    ): Result<ArticlesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getArticles(apiKey, limit, offset, search, siteSource)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDebugScreenshots(apiKey: String): Result<DebugScreenshotsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDebugScreenshots(apiKey)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

