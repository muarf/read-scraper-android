package com.readscraper.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readscraper.android.data.model.*
import com.readscraper.android.data.preferences.PreferencesManager
import com.readscraper.android.data.repository.ReadScraperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val currentJobId: String? = null,
    val jobStatus: JobStatus? = null,
    val article: Article? = null,
    val errorMessage: String? = null,
    val searchInput: String = "",
    val isPolling: Boolean = false,
    val apiKey: String? = null,
    val apiUrl: String = "http://104.244.74.191:5000"
)

class MainViewModel(
    private val repository: ReadScraperRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private var pollingJob: Job? = null
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesManager.apiKey.collect { key ->
                _uiState.value = _uiState.value.copy(apiKey = key)
            }
        }
        viewModelScope.launch {
            preferencesManager.apiUrl.collect { url ->
                _uiState.value = _uiState.value.copy(apiUrl = url ?: "http://104.244.74.191:5000")
            }
        }
    }
    
    fun updateSearchInput(input: String) {
        _uiState.value = _uiState.value.copy(searchInput = input, errorMessage = null)
    }
    
    fun getTempApiKey() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getTempApiKey().fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(apiKey = response.api_key)
                    preferencesManager.saveApiKey(response.api_key)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors de la récupération de la clé API"
                    )
                }
            )
        }
    }
    
    fun scrape() {
        val input = _uiState.value.searchInput.trim()
        if (input.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Veuillez entrer une URL ou des termes de recherche")
            return
        }
        
        val apiKey = _uiState.value.apiKey
        if (apiKey == null) {
            // Essayer d'obtenir une clé temporaire d'abord
            getTempApiKey()
            viewModelScope.launch {
                delay(1000) // Attendre que la clé soit obtenue
                scrape() // Relancer
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val request = if (input.startsWith("http://") || input.startsWith("https://")) {
                ScrapeRequest(url = input)
            } else {
                ScrapeRequest(search_terms = input)
            }
            
            repository.scrape(apiKey, request).fold(
                onSuccess = { response ->
                    if (response.cached && response.article_id != null) {
                        // Article en cache, récupérer directement
                        getArticle(response.article_id)
                    } else if (response.job_id != null) {
                        // Nouveau job, commencer le polling
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentJobId = response.job_id,
                            isPolling = true
                        )
                        startPolling(response.job_id)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Réponse inattendue du serveur"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors du scraping"
                    )
                }
            )
        }
    }
    
    private fun startPolling(jobId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(2000) // Poll toutes les 2 secondes
                
                val apiKey = _uiState.value.apiKey ?: break
                repository.getJobStatus(apiKey, jobId).fold(
                    onSuccess = { status ->
                        _uiState.value = _uiState.value.copy(jobStatus = status)
                        
                        when (status.status) {
                            "completed" -> {
                                if (status.article_id != null) {
                                    getArticle(status.article_id)
                                    stopPolling()
                                }
                            }
                            "failed" -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPolling = false,
                                    errorMessage = status.error_message ?: "Le job a échoué"
                                )
                                stopPolling()
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPolling = false,
                            errorMessage = error.message ?: "Erreur lors de la récupération du statut"
                        )
                        stopPolling()
                    }
                )
            }
        }
    }
    
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.value = _uiState.value.copy(isPolling = false)
    }
    
    private fun getArticle(articleId: String) {
        viewModelScope.launch {
            val apiKey = _uiState.value.apiKey ?: return@launch
            repository.getArticle(apiKey, articleId).fold(
                onSuccess = { article ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        article = article,
                        isPolling = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors de la récupération de l'article"
                    )
                }
            )
        }
    }
    
    fun retrySearch() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        scrape()
    }
    
    fun downloadPdf() {
        val articleId = _uiState.value.article?.id ?: return
        viewModelScope.launch {
            val apiKey = _uiState.value.apiKey ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.downloadPdf(apiKey, articleId).fold(
                onSuccess = { pdfBytes ->
                    // Le téléchargement sera géré par l'UI
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors du téléchargement du PDF"
                    )
                }
            )
        }
    }
    
    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(key)
            _uiState.value = _uiState.value.copy(apiKey = key)
        }
    }
    
    fun saveApiUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.saveApiUrl(url)
            _uiState.value = _uiState.value.copy(apiUrl = url)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

