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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
        
        // Statut du job avec écran de chargement amélioré
        if (uiState.jobStatus != null || uiState.isPolling || uiState.isLoading) {
            val jobStatus = uiState.jobStatus
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icône newspaper avec animation
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Chargement",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "En attente de démarrage du scraping...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
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
                    
                    Divider()
                    
                    // Afficher le contenu HTML de l'article
                    val htmlContent = article.html_content
                    if (htmlContent != null && htmlContent.isNotBlank()) {
                        Text(
                            text = "Contenu de l'article",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // WebView dans sa propre zone scrollable (hors de la Column scrollable)
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    webViewClient = object : android.webkit.WebViewClient() {}
                                    
                                    settings.javaScriptEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = true
                                    settings.textZoom = 150 // Augmenter la taille du texte de 50%
                                    // Activer le scrolling vertical dans la WebView
                                    isVerticalScrollBarEnabled = true
                                    isHorizontalScrollBarEnabled = false
                                    // Important: permettre le scroll dans la WebView
                                    overScrollMode = android.view.View.OVER_SCROLL_ALWAYS
                                    scrollBarStyle = android.view.View.SCROLLBARS_INSIDE_OVERLAY
                                    setBackgroundColor(0xFFFFFFFF.toInt())
                                    
                                    val fullHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                                            <style>
                                                * {
                                                    -webkit-text-size-adjust: 100%;
                                                }
                                                body {
                                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                                    font-size: 18px;
                                                    line-height: 1.8;
                                                    max-width: 100%;
                                                    margin: 0;
                                                    padding: 20px;
                                                    color: #333;
                                                }
                                                h1, h2, h3, h4, h5, h6 {
                                                    font-size: 1.3em;
                                                    margin-top: 1.2em;
                                                    margin-bottom: 0.8em;
                                                }
                                                p {
                                                    margin-bottom: 1em;
                                                }
                                                img {
                                                    max-width: 100%;
                                                    height: auto;
                                                    display: block;
                                                    margin: 1em auto;
                                                }
                                                a {
                                                    color: #1976d2;
                                                }
                                            </style>
                                        </head>
                                        <body>
                                            $htmlContent
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    
                                    loadDataWithBaseURL("http://104.244.74.191", fullHtml, "text/html", "UTF-8", null)
                                    
                                    // Appliquer un zoom initial pour améliorer la lisibilité
                                    post {
                                        zoomIn()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(800.dp) // Hauteur fixe suffisante pour bonne lisibilité
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            modifier = Modifier.weight(1f),
                            enabled = article.pdf_path != null
                        ) {
                            Text(if (article.pdf_path == null) "PDF N/A" else "PDF")
                        }
                        
                        val currentJobId = uiState.currentJobId
                        if (currentJobId != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.rejectArticle(currentJobId)
                                    Toast.makeText(context, "Article rejeté", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Rejeter")
                            }
                        }
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
}

private fun downloadPdf(
    viewModel: MainViewModel,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val articleId = viewModel.uiState.value.article?.id ?: return
    val apiKey = viewModel.uiState.value.apiKey ?: return
    
    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            android.util.Log.d("ScraperScreen", "Téléchargement PDF - articleId: $articleId")
            val repository = ReadScraperRepository()
            repository.downloadPdf(apiKey, articleId).fold(
                onSuccess = { pdfBytes ->
                    if (pdfBytes.isEmpty()) {
                        android.util.Log.e("ScraperScreen", "PDF vide")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Le PDF est vide ou n'existe pas",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@fold
                    }
                    
                    // Utiliser le cache interne (pas besoin de permission)
                    val fileName = "article_${articleId}.pdf"
                    val file = File(context.cacheDir, fileName)
                    
                    android.util.Log.d("ScraperScreen", "Écriture PDF dans: ${file.absolutePath}")
                    FileOutputStream(file).use { it.write(pdfBytes) }
                    android.util.Log.d("ScraperScreen", "PDF écrit avec succès, taille: ${file.length()} bytes")
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        // Ouvrir automatiquement le PDF
                        try {
                            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                android.net.Uri.fromFile(file)
                            }
                            
                            // Chercher une app PDF (pas Photos/Gallery)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            
                            val activities = context.packageManager.queryIntentActivities(intent, 0)
                            val pdfActivity = activities.firstOrNull { activity ->
                                val packageName = activity.activityInfo.packageName.lowercase()
                                !packageName.contains("photos") && 
                                !packageName.contains("gallery") && 
                                !packageName.contains("image")
                            }
                            
                            val pdfIntent = if (pdfActivity != null) {
                                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    setClassName(pdfActivity.activityInfo.packageName, pdfActivity.activityInfo.name)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            } else {
                                intent // Fallback
                            }
                            
                            if (pdfIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(pdfIntent)
                                Toast.makeText(
                                    context,
                                    "PDF téléchargé et ouvert",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "PDF téléchargé dans: $fileName\nAucune application de lecture PDF trouvée\n\nInstallez MuPDF (open source, sans pub) depuis F-Droid ou Play Store",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScraperScreen", "Erreur ouverture PDF", e)
                            Toast.makeText(
                                context,
                                "PDF téléchargé: $fileName\nErreur lors de l'ouverture: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("ScraperScreen", "Erreur téléchargement PDF", error)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val errorMessage = when {
                            error.message?.contains("404") == true -> "PDF non trouvé (404). L'article n'a peut-être pas encore de PDF généré."
                            error.message?.contains("Erreur: 404") == true -> "PDF non trouvé. L'article n'a peut-être pas encore de PDF généré."
                            else -> "Erreur: ${error.message ?: "Erreur inconnue"}"
                        }
                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("ScraperScreen", "Exception téléchargement PDF", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

