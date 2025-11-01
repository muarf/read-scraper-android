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
    val apiUrl: String = "http://104.244.74.191"
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
                _uiState.value = _uiState.value.copy(apiUrl = url ?: "http://104.244.74.191")
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
                    _uiState.value = _uiState.value.copy(
                        apiKey = response.api_key,
                        isLoading = false
                    )
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
        // Si un scraping est en cours, annuler plutôt que de relancer
        if (_uiState.value.isPolling && _uiState.value.currentJobId != null) {
            cancelScraping()
            return
        }
        
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
                scrape() // Relancer une seule fois
            }
            return
        }
        
        // Éviter le double lancement
        if (_uiState.value.isLoading || _uiState.value.isPolling) {
            android.util.Log.d("MainViewModel", "Scraping déjà en cours, ignoré")
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Démarrage du scraping: $input")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val request = if (input.startsWith("http://") || input.startsWith("https://")) {
                ScrapeRequest(url = input)
            } else {
                ScrapeRequest(search_terms = input)
            }
            
            repository.scrape(apiKey, request).fold(
                onSuccess = { response ->
                    android.util.Log.d("MainViewModel", "Réponse scraping: cached=${response.cached}, article_id=${response.article_id}, job_id=${response.job_id}")
                    if (response.cached && response.article_id != null) {
                        // Article en cache, récupérer directement
                        // Conserver le job_id si présent pour permettre le rejet
                        val jobId = response.job_id
                        _uiState.value = _uiState.value.copy(
                            currentJobId = jobId
                        )
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
                    android.util.Log.e("MainViewModel", "Erreur scraping", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors du scraping"
                    )
                }
            )
        }
    }
    
    fun cancelScraping() {
        val jobId = _uiState.value.currentJobId
        val apiKey = _uiState.value.apiKey
        if (jobId == null || apiKey == null) {
            android.util.Log.w("MainViewModel", "Impossible d'annuler: jobId ou apiKey null")
            stopPolling()
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Annulation du scraping pour job: $jobId")
            stopPolling()
            repository.cancelJob(apiKey, jobId).fold(
                onSuccess = { response ->
                    android.util.Log.d("MainViewModel", "Job annulé: ${response.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPolling = false,
                        currentJobId = null,
                        jobStatus = null
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("MainViewModel", "Erreur lors de l'annulation", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPolling = false,
                        errorMessage = error.message ?: "Erreur lors de l'annulation"
                    )
                }
            )
        }
    }
    
    private fun startPolling(jobId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Démarrage du polling pour job: $jobId")
            while (true) {
                delay(2000) // Poll toutes les 2 secondes
                
                val apiKey = _uiState.value.apiKey ?: break
                android.util.Log.d("MainViewModel", "Polling - récupération du statut pour job: $jobId")
                repository.getJobStatus(apiKey, jobId).fold(
                    onSuccess = { status ->
                        android.util.Log.d("MainViewModel", "Statut reçu: status=${status.status}, article_id=${status.article_id}, current_step=${status.current_step}")
                        _uiState.value = _uiState.value.copy(jobStatus = status)
                        
                        when (status.status) {
                            "completed" -> {
                                android.util.Log.d("MainViewModel", "Job completed: article_id=${status.article_id}")
                                // Arrêter le polling même si article_id est null (PDF peut être généré sans article_id dans certains cas)
                                stopPolling()
                                if (status.article_id != null) {
                                    android.util.Log.d("MainViewModel", "Récupération de l'article avec id: ${status.article_id}")
                                    getArticle(status.article_id)
                                } else {
                                    android.util.Log.w("MainViewModel", "Job completed mais article_id est null")
                                    // Job terminé mais pas d'article_id - peut-être que le PDF est disponible directement
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        isPolling = false,
                                        errorMessage = "Job terminé mais aucun article trouvé"
                                    )
                                }
                            }
                            "failed" -> {
                                android.util.Log.e("MainViewModel", "Job failed: ${status.error_message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPolling = false,
                                    errorMessage = status.error_message ?: "Le job a échoué"
                                )
                                stopPolling()
                            }
                            "cancelled" -> {
                                android.util.Log.d("MainViewModel", "Job cancelled")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPolling = false,
                                    errorMessage = "Job annulé"
                                )
                                stopPolling()
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("MainViewModel", "Erreur lors de la récupération du statut", error)
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
            val currentJobId = _uiState.value.currentJobId // Conserver le job_id
            android.util.Log.d("MainViewModel", "Récupération de l'article: $articleId")
            repository.getArticle(apiKey, articleId).fold(
                onSuccess = { article ->
                    android.util.Log.d("MainViewModel", "Article récupéré avec succès: id=${article.id}, title=${article.title}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        article = article,
                        isPolling = false,
                        currentJobId = currentJobId // Conserver le job_id pour permettre le rejet
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("MainViewModel", "Erreur lors de la récupération de l'article: $articleId", error)
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
    
    fun rejectArticle(jobId: String) {
        viewModelScope.launch {
            val apiKey = _uiState.value.apiKey ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.rejectArticle(apiKey, jobId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        article = null,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors du rejet de l'article"
                    )
                }
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

