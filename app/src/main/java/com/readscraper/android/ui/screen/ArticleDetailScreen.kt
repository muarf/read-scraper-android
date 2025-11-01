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
            // Section header scrollable
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
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
                
                // Afficher l'article directement depuis l'URL
                Text(
                    text = "Contenu de l'article",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // WebView dans sa propre zone avec taille définie
            val articleUrl = "http://104.244.74.191/read/article/${article.id}"
            Log.d("ArticleDetail", "Chargement article depuis URL: $articleUrl")
            
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        Log.d("ArticleDetail", "Création WebView")
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("ArticleDetail", "Début chargement page: $url")
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("ArticleDetail", "Page WebView chargée: $url")
                                
                                // Vérifier le contenu de la page après chargement
                                view?.postDelayed({
                                    try {
                                        val html = view.title
                                        val scrollY = view.scrollY
                                        val contentHeight = view.contentHeight
                                        val height = view.height
                                        val width = view.width
                                        
                                        Log.d("ArticleDetail", "WebView après chargement - Title: $html, ScrollY: $scrollY, ContentHeight: $contentHeight, ViewSize: ${width}x${height}")
                                        
                                        // Essayer d'injecter du JavaScript pour voir le contenu
                                        view.evaluateJavascript("document.body.innerHTML.length") { result ->
                                            Log.d("ArticleDetail", "Longueur HTML body: $result")
                                        }
                                        
                                        view.evaluateJavascript("document.body.scrollHeight") { result ->
                                            Log.d("ArticleDetail", "Hauteur scroll body: $result")
                                        }
                                        
                                        view.evaluateJavascript("window.getComputedStyle(document.body).display") { result ->
                                            Log.d("ArticleDetail", "Display body: $result")
                                        }
                                        
                                        view.evaluateJavascript("document.body.style.opacity") { result ->
                                            Log.d("ArticleDetail", "Opacity body: $result")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ArticleDetail", "Erreur lors de l'inspection de la page", e)
                                    }
                                }, 500)
                            }
                            
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                val errorCode = error?.errorCode ?: -1
                                val description = error?.description?.toString() ?: "Unknown"
                                val failingUrl = request?.url?.toString() ?: "Unknown"
                                val isMainFrame = request?.isForMainFrame ?: false
                                Log.e("ArticleDetail", "Erreur WebView: $errorCode - $description - $failingUrl (mainFrame: $isMainFrame)")
                            }
                            
                            override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                val statusCode = errorResponse?.statusCode ?: -1
                                val url = request?.url?.toString() ?: "Unknown"
                                val isMainFrame = request?.isForMainFrame ?: false
                                Log.e("ArticleDetail", "Erreur HTTP WebView: $statusCode pour $url (mainFrame: $isMainFrame)")
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
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        
                        // Forcer un background blanc
                        setBackgroundColor(0xFFFFFFFF.toInt())
                        
                        // Désactiver le cache pour éviter les problèmes
                        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                        
                        // Ajouter WebChromeClient pour capturer les erreurs JS et console
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val level = consoleMessage?.messageLevel()
                                val message = consoleMessage?.message()
                                val sourceId = consoleMessage?.sourceId()
                                val lineNumber = consoleMessage?.lineNumber()
                                
                                when (level) {
                                    android.webkit.ConsoleMessage.MessageLevel.ERROR -> {
                                        Log.e("ArticleDetail", "JS Console ERROR: $message (at $sourceId:$lineNumber)")
                                    }
                                    android.webkit.ConsoleMessage.MessageLevel.WARNING -> {
                                        Log.w("ArticleDetail", "JS Console WARNING: $message (at $sourceId:$lineNumber)")
                                    }
                                    android.webkit.ConsoleMessage.MessageLevel.LOG -> {
                                        Log.d("ArticleDetail", "JS Console: $message")
                                    }
                                    android.webkit.ConsoleMessage.MessageLevel.DEBUG -> {
                                        Log.d("ArticleDetail", "JS Console DEBUG: $message")
                                    }
                                    else -> {
                                        Log.d("ArticleDetail", "JS Console ($level): $message")
                                    }
                                }
                                return true
                            }
                            
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                Log.d("ArticleDetail", "Progress WebView: $newProgress%")
                            }
                        }
                        
                        Log.d("ArticleDetail", "Chargement URL dans WebView: $articleUrl")
                        Log.d("ArticleDetail", "WebView settings - JS: ${settings.javaScriptEnabled}, DOMStorage: ${settings.domStorageEnabled}, UserAgent: ${settings.userAgentString}")
                        
                        loadUrl(articleUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .height(0.dp) // Nécessaire pour que weight fonctionne
            )
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
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "article_${articleId}.pdf"
            val file = java.io.File(downloadsDir, fileName)
            
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
                    
                    // Créer un intent plus générique pour ouvrir le PDF
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    
                    Log.d("ArticleDetail", "Intent créé: action=${intent.action}, data=${intent.data}, type=${intent.type}")
                    
                    // Essayer d'abord avec le type spécifique
                    var resolved: android.content.ComponentName? = intent.resolveActivity(context.packageManager)
                    
                    // Si pas trouvé, essayer avec un type plus générique
                    if (resolved == null) {
                        Log.d("ArticleDetail", "Aucune app pour application/pdf, essai avec type générique")
                        intent.type = "*/*"
                        resolved = intent.resolveActivity(context.packageManager)
                    }
                    
                    if (resolved != null) {
                        val packageName = resolved.packageName
                        val activityName = resolved.className
                        Log.d("ArticleDetail", "Application trouvée: $packageName/$activityName")
                        try {
                            context.startActivity(intent)
                            Toast.makeText(
                                context,
                                "PDF téléchargé et ouvert",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("ArticleDetail", "Erreur lors du démarrage de l'activité", e)
                            Toast.makeText(
                                context,
                                "PDF téléchargé dans: $fileName\nErreur lors de l'ouverture: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
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

