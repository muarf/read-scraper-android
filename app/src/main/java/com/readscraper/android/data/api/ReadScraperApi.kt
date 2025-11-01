package com.readscraper.android.data.api

import com.readscraper.android.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ReadScraperApi {
    
    @GET("/api/v1/get-temp-key")
    suspend fun getTempApiKey(): Response<TempApiKeyResponse>
    
    @POST("/api/v1/scrape")
    suspend fun scrape(
        @Header("X-API-Key") apiKey: String,
        @Body request: ScrapeRequest
    ): Response<ScrapeResponse>
    
    @GET("/api/v1/job/{job_id}")
    suspend fun getJobStatus(
        @Header("X-API-Key") apiKey: String,
        @Path("job_id") jobId: String
    ): Response<JobStatus>
    
    @GET("/api/v1/article/{article_id}")
    suspend fun getArticle(
        @Header("X-API-Key") apiKey: String,
        @Path("article_id") articleId: String
    ): Response<Article>
    
    @GET("/api/v1/article/{article_id}/pdf")
    suspend fun downloadPdf(
        @Header("X-API-Key") apiKey: String,
        @Path("article_id") articleId: String
    ): Response<ResponseBody>
    
    @GET("/api/v1/articles")
    suspend fun getArticles(
        @Header("X-API-Key") apiKey: String?,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("site_source") siteSource: String? = null
    ): Response<ArticlesResponse>
    
    @GET("/api/v1/debug/screenshots")
    suspend fun getDebugScreenshots(
        @Header("X-API-Key") apiKey: String
    ): Response<DebugScreenshotsResponse>
    
    @POST("/api/v1/job/{job_id}/reject")
    suspend fun rejectArticle(
        @Header("X-API-Key") apiKey: String,
        @Path("job_id") jobId: String
    ): Response<RejectResponse>
    
    @POST("/api/v1/job/{job_id}/cancel")
    suspend fun cancelJob(
        @Header("X-API-Key") apiKey: String,
        @Path("job_id") jobId: String
    ): Response<CancelResponse>
}

