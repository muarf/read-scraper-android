package com.readscraper.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.readscraper.android.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKeyInput by remember { mutableStateOf(uiState.apiKey ?: "") }
    var apiUrlInput by remember { mutableStateOf(uiState.apiUrl) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineMedium
        )
        
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("Clé API") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = apiUrlInput,
            onValueChange = { apiUrlInput = it },
            label = { Text("URL de l'API") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        
        Button(
            onClick = {
                viewModel.saveApiKey(apiKeyInput)
                viewModel.saveApiUrl(apiUrlInput)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }
        
        Divider()
        
        Text(
            text = "Obtenir une clé API temporaire (valide 24h)",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Button(
            onClick = { viewModel.getTempApiKey() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Obtenir une clé temporaire")
        }
    }
}

