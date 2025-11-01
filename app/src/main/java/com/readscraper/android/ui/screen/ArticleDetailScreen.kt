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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import android.util.Log
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
    jobId: String? = null,
    onReject: (() -> Unit)? = null,
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
        ) {
            // Section header fixe (pas scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (article.site_source != null) {
                            Text(
                                text = "Source: ${article.site_source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Date: ${article.created_at}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            enabled = apiKey != null && !isDownloading && article.pdf_path != null
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(if (article.pdf_path == null) "PDF N/A" else "PDF", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        if (jobId != null && onReject != null) {
                            OutlinedButton(
                                onClick = {
                                    onReject()
                                    Toast.makeText(context, "Article rejeté", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Rejeter", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                if (article.pdf_path == null) {
                    Text(
                        text = "Aucun PDF disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Divider()
            }
            
            // WebView pour afficher le contenu HTML de l'article
            val htmlContent = article.html_content
            if (htmlContent != null && htmlContent.isNotBlank()) {
                Log.d("ArticleDetail", "Affichage HTML direct depuis article.html_content (${htmlContent.length} caractères)")
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("ArticleDetail", "HTML chargé dans WebView")
                                }
                            }
                            
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            
                            setBackgroundColor(0xFFFFFFFF.toInt())
                            
                            // Créer un HTML complet avec styles pour un meilleur rendu
                            val fullHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                            line-height: 1.6;
                                            max-width: 100%;
                                            margin: 0;
                                            padding: 16px;
                                            color: #333;
                                        }
                                        img {
                                            max-width: 100%;
                                            height: auto;
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
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(0.dp)
                )
            } else {
                // Fallback: charger depuis l'URL si pas de HTML
                val articleUrl = "http://104.244.74.191/read/article/${article.id}"
                Log.d("ArticleDetail", "Pas de html_content, chargement depuis URL: $articleUrl")
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("ArticleDetail", "Page chargée depuis URL: $url")
                                }
                            }
                            
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setBackgroundColor(0xFFFFFFFF.toInt())
                            
                            loadUrl(articleUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(0.dp)
                )
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
    Log.d("ArticleDetail", "downloadPdf appelé - articleId: $articleId, pdfPath: $pdfPath, apiKey présent: ${apiKey != null}")
    
    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            var pdfBytes: ByteArray? = null
            
            // Utiliser uniquement l'endpoint API (pas d'URL statique)
            if (apiKey != null) {
                Log.d("ArticleDetail", "Tentative téléchargement PDF via endpoint API")
                try {
                    val repository = ReadScraperRepository()
                    val result = repository.downloadPdf(apiKey, articleId)
                    result.fold(
                        onSuccess = { bytes ->
                            pdfBytes = bytes
                            Log.d("ArticleDetail", "PDF téléchargé via API, taille: ${bytes.size} bytes")
                        },
                        onFailure = { error ->
                            Log.e("ArticleDetail", "Erreur lors du téléchargement via API", error)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("ArticleDetail", "Exception lors du téléchargement via API", e)
                }
            } else {
                Log.e("ArticleDetail", "Impossible de télécharger via API: apiKey est null")
            }
            
            val finalPdfBytes = pdfBytes
            if (finalPdfBytes == null || finalPdfBytes.isEmpty()) {
                Log.e("ArticleDetail", "PDF non disponible ou vide")
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
            
            // Utiliser le cache interne (pas besoin de permission)
            val fileName = "article_${articleId}.pdf"
            val file = java.io.File(context.cacheDir, fileName)
            
            Log.d("ArticleDetail", "Écriture du PDF dans: ${file.absolutePath}")
            
            try {
                java.io.FileOutputStream(file).use { it.write(finalPdfBytes) }
                Log.d("ArticleDetail", "PDF écrit avec succès, taille fichier: ${file.length()} bytes")
            } catch (e: Exception) {
                Log.e("ArticleDetail", "Erreur lors de l'écriture du PDF", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Erreur lors de l'écriture du fichier: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                }
                return@launch
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                // Ouvrir automatiquement le PDF après téléchargement
                try {
                    Log.d("ArticleDetail", "Tentative d'ouverture du PDF")
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val providerUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        Log.d("ArticleDetail", "URI FileProvider: $providerUri")
                        providerUri
                    } else {
                        val fileUri = Uri.fromFile(file)
                        Log.d("ArticleDetail", "URI File: $fileUri")
                        fileUri
                    }
                    
                    // Chercher une app PDF (pas Photos/Gallery)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    Log.d("ArticleDetail", "Intent créé: action=${intent.action}, data=${intent.data}, type=${intent.type}")
                    
                    val activities = context.packageManager.queryIntentActivities(intent, 0)
                    val pdfActivity = activities.firstOrNull { activity ->
                        val packageName = activity.activityInfo.packageName.lowercase()
                        !packageName.contains("photos") && 
                        !packageName.contains("gallery") && 
                        !packageName.contains("image")
                    }
                    
                    val pdfIntent = if (pdfActivity != null) {
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            setClassName(pdfActivity.activityInfo.packageName, pdfActivity.activityInfo.name)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        intent // Fallback
                    }
                    
                    if (pdfIntent != null) {
                        try {
                            context.startActivity(pdfIntent)
                            Toast.makeText(
                                context,
                                "PDF téléchargé et ouvert",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("ArticleDetail", "Erreur lors du démarrage de l'activité", e)
                            // Fallback : essayer avec type générique
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                                Toast.makeText(context, "PDF ouvert", Toast.LENGTH_SHORT).show()
                            } catch (e2: Exception) {
                                Toast.makeText(
                                    context,
                                    "PDF téléchargé dans le cache de l'app\nErreur lors de l'ouverture: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Log.w("ArticleDetail", "Aucune application trouvée pour ouvrir le PDF")
                        Toast.makeText(
                            context,
                            "PDF téléchargé dans: $fileName\nAucune application de lecture PDF trouvée\n\nInstallez MuPDF (open source, sans pub) depuis F-Droid ou Play Store",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("ArticleDetail", "Erreur lors de l'ouverture du PDF", e)
                    Toast.makeText(
                        context,
                        "PDF téléchargé: $fileName\nErreur lors de l'ouverture: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("ArticleDetail", "Exception générale lors du téléchargement PDF", e)
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

