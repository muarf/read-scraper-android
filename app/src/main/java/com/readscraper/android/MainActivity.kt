package com.readscraper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.readscraper.android.data.preferences.PreferencesManager
import com.readscraper.android.data.repository.ReadScraperRepository
import com.readscraper.android.ui.navigation.BottomNavigationBar
import com.readscraper.android.ui.screen.ArticleDetailScreen
import com.readscraper.android.ui.screen.ArticlesScreen
import com.readscraper.android.ui.screen.ScraperScreen
import com.readscraper.android.ui.screen.SettingsScreen
import com.readscraper.android.ui.theme.ReadScraperTheme
import com.readscraper.android.ui.viewmodel.ArticlesViewModel
import com.readscraper.android.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val preferencesManager = PreferencesManager(applicationContext)
        val repository = ReadScraperRepository()
        
        setContent {
            ReadScraperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(navController = navController)
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "scraper",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("scraper") {
                                ScraperScreen(
                                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                                @Suppress("UNCHECKED_CAST")
                                                return MainViewModel(repository, preferencesManager) as T
                                            }
                                        }
                                    )
                                )
                            }
                            
                            composable("articles") {
                                ArticlesScreen(
                                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                                @Suppress("UNCHECKED_CAST")
                                                return ArticlesViewModel(repository, preferencesManager) as T
                                            }
                                        }
                                    ),
                                    navController = navController
                                )
                            }
                            
                            composable("article_detail/{articleId}") { backStackEntry ->
                                val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                                val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            @Suppress("UNCHECKED_CAST")
                                            return MainViewModel(repository, preferencesManager) as T
                                        }
                                    }
                                )
                                
                                val uiState by mainViewModel.uiState.collectAsState()
                                var article by remember { mutableStateOf<com.readscraper.android.data.model.Article?>(null) }
                                var isLoadingArticle by remember { mutableStateOf(true) }
                                
                                // Trouver l'article dans la liste des articles
                                val articlesViewModel: ArticlesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            @Suppress("UNCHECKED_CAST")
                                            return ArticlesViewModel(repository, preferencesManager) as T
                                        }
                                    }
                                )
                                
                                val articlesState by articlesViewModel.uiState.collectAsState()
                                val foundArticle = articlesState.articles.find { it.id == articleId }
                                
                                LaunchedEffect(articleId, foundArticle) {
                                    if (foundArticle != null) {
                                        article = foundArticle
                                        isLoadingArticle = false
                                    } else {
                                        // Charger depuis l'API
                                        val apiKey = uiState.apiKey
                                        if (apiKey != null && articleId.isNotBlank()) {
                                            repository.getArticle(apiKey, articleId).fold(
                                                onSuccess = { loadedArticle ->
                                                    article = loadedArticle
                                                    isLoadingArticle = false
                                                },
                                                onFailure = {
                                                    isLoadingArticle = false
                                                }
                                            )
                                        } else {
                                            isLoadingArticle = false
                                        }
                                    }
                                }
                                
                                if (isLoadingArticle) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (article != null) {
                                    ArticleDetailScreen(
                                        article = article!!,
                                        apiKey = uiState.apiKey,
                                        navController = navController
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Article non trouvé")
                                    }
                                }
                            }
                            
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                                @Suppress("UNCHECKED_CAST")
                                                return MainViewModel(repository, preferencesManager) as T
                                            }
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

