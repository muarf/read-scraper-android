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
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.readscraper.android.data.model.Article
import com.readscraper.android.data.repository.ReadScraperRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article,
    apiKey: String?,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadPdf(article.id, apiKey, context, scope)
        } else {
            Toast.makeText(context, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'article") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (article.site_source != null) {
                Text(
                    text = "Source: ${article.site_source}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Date: ${article.created_at}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        downloadPdf(article.id, apiKey, context, scope)
                    } else {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            downloadPdf(article.id, apiKey, context, scope)
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey != null
            ) {
                Text("Télécharger le PDF")
            }
            
            if (article.url.isNotBlank()) {
                OutlinedButton(
                    onClick = { /* TODO: Ouvrir dans un navigateur */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ouvrir l'article original")
                }
            }
        }
    }
}

private fun downloadPdf(
    articleId: String,
    apiKey: String?,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    if (apiKey == null) return
    
    scope.launch {
        val repository = ReadScraperRepository()
        repository.downloadPdf(apiKey, articleId).fold(
            onSuccess = { pdfBytes ->
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "article_${articleId}.pdf"
                val file = java.io.File(downloadsDir, fileName)
                
                java.io.FileOutputStream(file).use { it.write(pdfBytes) }
                
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

