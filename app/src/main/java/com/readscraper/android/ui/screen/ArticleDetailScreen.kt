package com.readscraper.android.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import android.webkit.WebView
import android.webkit.WebViewClient
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
    
    var isDownloading by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isDownloading = true
            downloadPdf(article.id, apiKey, article.pdf_path, context, scope) { isDownloading = false }
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
                        isDownloading = true
                        downloadPdf(article.id, apiKey, article.pdf_path, context, scope) { isDownloading = false }
                    } else {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            isDownloading = true
                            downloadPdf(article.id, apiKey, article.pdf_path, context, scope) { isDownloading = false }
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey != null && !isDownloading && article.pdf_path != null
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (article.pdf_path == null) "PDF non disponible" else "Télécharger le PDF")
            }
            
            if (article.pdf_path == null) {
                Text(
                    text = "Aucun PDF disponible pour cet article",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Afficher le contenu HTML de l'article
            if (article.html_content != null && article.html_content.isNotBlank()) {
                Text(
                    text = "Contenu de l'article",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            
                            // Charger le contenu HTML avec un style de base
                            val htmlContent = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                            padding: 16px;
                                            line-height: 1.6;
                                            color: #333;
                                            max-width: 100%;
                                            word-wrap: break-word;
                                            background-color: #ffffff;
                                        }
                                        img {
                                            max-width: 100%;
                                            height: auto;
                                            display: block;
                                            margin: 10px 0;
                                        }
                                        a {
                                            color: #0066cc;
                                            text-decoration: none;
                                        }
                                        p {
                                            margin: 10px 0;
                                        }
                                        h1, h2, h3, h4, h5, h6 {
                                            margin-top: 20px;
                                            margin-bottom: 10px;
                                        }
                                    </style>
                                </head>
                                <body>
                                    ${article.html_content}
                                </body>
                                </html>
                            """.trimIndent()
                            
                            loadDataWithBaseURL("http://104.244.74.191:5000/", htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                )
            } else {
                // Si pas de contenu HTML, proposer d'ouvrir dans le navigateur
                OutlinedButton(
                    onClick = {
                        val articleUrl = "http://104.244.74.191:5000/read/article/${article.id}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ouvrir l'article dans le navigateur")
                }
            }
        }
    }
}

private fun downloadPdf(
    articleId: String,
    apiKey: String?,
    pdfPath: String?,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        try {
            val pdfBytes: ByteArray? = if (pdfPath != null) {
                // Essayer d'abord avec l'URL statique
                try {
                    val staticUrl = if (pdfPath.startsWith("http")) {
                        pdfPath
                    } else {
                        // L'URL statique utilise le port 5000 : http://104.244.74.191:5000/static/...
                        "http://104.244.74.191:5000${if (pdfPath.startsWith("/")) pdfPath else "/$pdfPath"}"
                    }
                    
                    val url = java.net.URL(staticUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    
                    if (connection.responseCode == 200) {
                        connection.inputStream.readBytes()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            // Si l'URL statique n'a pas fonctionné, essayer l'endpoint API
            val finalPdfBytes = pdfBytes ?: if (apiKey != null) {
                val repository = ReadScraperRepository()
                repository.downloadPdf(apiKey, articleId).fold(
                    onSuccess = { it },
                    onFailure = { null }
                )
            } else {
                null
            }
            
            if (finalPdfBytes == null || finalPdfBytes.isEmpty()) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "PDF non trouvé. L'article n'a peut-être pas encore de PDF généré.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                }
                return@launch
            }
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "article_${articleId}.pdf"
            val file = java.io.File(downloadsDir, fileName)
            
            java.io.FileOutputStream(file).use { it.write(finalPdfBytes) }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                // Ouvrir automatiquement le PDF après téléchargement
                try {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        Toast.makeText(
                            context,
                            "PDF téléchargé et ouvert",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "PDF téléchargé dans: $fileName\nAucune application de lecture PDF trouvée",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "PDF téléchargé: $fileName\nErreur lors de l'ouverture: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onComplete()
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete()
            }
        }
    }
}

