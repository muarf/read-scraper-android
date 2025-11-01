package com.readscraper.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readscraper.android.data.model.Article
import com.readscraper.android.data.preferences.PreferencesManager
import com.readscraper.android.data.repository.ReadScraperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val total: Int = 0
)

class ArticlesViewModel(
    private val repository: ReadScraperRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ArticlesUiState())
    val uiState: StateFlow<ArticlesUiState> = _uiState.asStateFlow()
    
    private var currentApiKey: String? = null
    
    init {
        viewModelScope.launch {
            preferencesManager.apiKey.collect { key ->
                currentApiKey = key
                loadArticles()
            }
        }
    }
    
    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getArticles(currentApiKey, limit = 50, offset = 0).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        articles = response.articles,
                        total = response.total
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erreur lors du chargement des articles"
                    )
                }
            )
        }
    }
}

