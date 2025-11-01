package com.readscraper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.readscraper.android.data.preferences.PreferencesManager
import com.readscraper.android.data.repository.ReadScraperRepository
import com.readscraper.android.ui.navigation.BottomNavigationBar
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
                                    )
                                )
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

