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
                val status = response.body()!!
                android.util.Log.d("ReadScraperRepository", "JobStatus reçu: id=${status.id}, status=${status.status}, article_id=${status.article_id}")
                Result.success(status)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("ReadScraperRepository", "Erreur getJobStatus: code=${response.code()}, message=${response.message()}, errorBody=$errorBody")
                Result.failure(Exception("Erreur: ${response.code()} ${response.message() ?: errorBody ?: "Erreur inconnue"}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ReadScraperRepository", "Exception lors de la récupération du statut", e)
            Result.failure(e)
        }
    }
    
    suspend fun getArticle(apiKey: String, articleId: String): Result<Article> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ReadScraperRepository", "Récupération de l'article: articleId=$articleId, apiKey présent=${apiKey.isNotBlank()}")
            val response = api.getArticle(apiKey, articleId)
            android.util.Log.d("ReadScraperRepository", "Réponse getArticle: code=${response.code()}, isSuccessful=${response.isSuccessful}, body null=${response.body() == null}")
            if (response.isSuccessful && response.body() != null) {
                val article = response.body()!!
                android.util.Log.d("ReadScraperRepository", "Article récupéré: id=${article.id}, title=${article.title}, html_content length=${article.html_content?.length ?: 0}")
                Result.success(article)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("ReadScraperRepository", "Erreur getArticle: code=${response.code()}, message=${response.message()}, errorBody=$errorBody")
                Result.failure(Exception("Erreur: ${response.code()} ${response.message() ?: errorBody ?: "Erreur inconnue"}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ReadScraperRepository", "Exception lors de la récupération de l'article", e)
            Result.failure(e)
        }
    }
    
    suspend fun downloadPdf(apiKey: String, articleId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadPdf(apiKey, articleId)
            when {
                response.isSuccessful && response.body() != null -> {
                    val bytes = response.body()!!.bytes()
                    if (bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(Exception("Erreur: Le PDF est vide"))
                    }
                }
                response.code() == 404 -> {
                    Result.failure(Exception("Erreur: 404 - PDF non trouvé. L'article n'a peut-être pas encore de PDF généré."))
                }
                else -> {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Erreur: ${response.code()} - $errorBody"))
                }
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
    
    suspend fun rejectArticle(apiKey: String, jobId: String): Result<RejectResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.rejectArticle(apiKey, jobId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Erreur: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelJob(apiKey: String, jobId: String): Result<CancelResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ReadScraperRepository", "Annulation du job: $jobId")
            val response = api.cancelJob(apiKey, jobId)
            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("ReadScraperRepository", "Job annulé avec succès: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                android.util.Log.e("ReadScraperRepository", "Erreur lors de l'annulation: ${response.code()} - $errorBody")
                Result.failure(Exception("Erreur: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ReadScraperRepository", "Exception lors de l'annulation", e)
            Result.failure(e)
        }
    }
}

