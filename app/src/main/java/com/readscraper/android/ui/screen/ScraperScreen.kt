package com.readscraper.android.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.readscraper.android.data.repository.ReadScraperRepository
import com.readscraper.android.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScraperScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadPdf(viewModel, context, scope)
        } else {
            Toast.makeText(context, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Titre
        Text(
            text = "Presse Scraper",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "API Backend - Interface Utilisateur",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Champ de recherche
        OutlinedTextField(
            value = uiState.searchInput,
            onValueChange = viewModel::updateSearchInput,
            label = { Text("Termes de recherche") },
            placeholder = { Text("Entrez l'URL ou les termes de recherche") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isPolling
        )
        
        // Bouton Scraper
        Button(
            onClick = { viewModel.scrape() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !uiState.isPolling && uiState.searchInput.isNotBlank()
        ) {
            if (uiState.isLoading || uiState.isPolling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Scraper")
        }
        
        // Message d'erreur
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aucun résultat trouvé. Essayez avec des termes de recherche différents :",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.retrySearch() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Relancer la recherche")
                    }
                }
            }
        }
        
        // Statut du job
        if (uiState.jobStatus != null || uiState.isPolling) {
            val jobStatus = uiState.jobStatus
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Étape actuelle: ${jobStatus?.current_step ?: "Initialisation"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val stepDescription = jobStatus?.step_description
                    if (stepDescription != null) {
                        Text(
                            text = stepDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "En attente de démarrage du scraping...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (jobStatus?.status == "searching") {
                        val searchResultsCount = jobStatus.search_results_count
                        if (searchResultsCount != null) {
                            Text(
                                text = "Résultats trouvés: $searchResultsCount",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        // Article téléchargé
        val article = uiState.article
        if (article != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (article.site_source != null) {
                        Text(
                            text = "Source: ${article.site_source}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                downloadPdf(viewModel, context, scope)
                            } else {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    downloadPdf(viewModel, context, scope)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Télécharger le PDF")
                    }
                }
            }
        }
        
        // Chargement initial de la clé API
        if (uiState.apiKey == null && !uiState.isLoading) {
            LaunchedEffect(Unit) {
                viewModel.getTempApiKey()
            }
        }
    }
}

private fun downloadPdf(
    viewModel: MainViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val articleId = viewModel.uiState.value.article?.id ?: return
    val apiKey = viewModel.uiState.value.apiKey ?: return
    
    scope.launch {
        val repository = ReadScraperRepository()
        repository.downloadPdf(apiKey, articleId).fold(
            onSuccess = { pdfBytes ->
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "article_${articleId}.pdf"
                val file = File(downloadsDir, fileName)
                
                FileOutputStream(file).use { it.write(pdfBytes) }
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "PDF téléchargé: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onFailure = { error ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Erreur: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

