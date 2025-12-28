package com.suvojeet.imagestenography.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.imagestenography.utils.CryptoUtils
import com.suvojeet.imagestenography.utils.SteganographyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DecodeResultItem(
    val uri: Uri,
    val message: String?,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDecodeScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var decodeResults by remember { mutableStateOf<List<DecodeResultItem>>(emptyList()) }
    var password by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Multi-image picker
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            decodeResults = emptyList() // Reset results on new selection
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Decode", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Select Images
            OutlinedButton(
                onClick = { 
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedUris.isEmpty()) "Select Images to Decode" else "Selected ${selectedUris.size} Images")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Optional Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Decryption Password (If needed)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Action
            Button(
                onClick = {
                    if (selectedUris.isEmpty()) {
                        Toast.makeText(context, "Select images first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isProcessing = true
                    decodeResults = emptyList()
                    
                    scope.launch {
                        val results = mutableListOf<DecodeResultItem>()
                        
                        // Process sequentially to keep it simple, or parallel with async
                        withContext(Dispatchers.Default) {
                            selectedUris.forEach { uri ->
                                try {
                                    // Load bitmap on IO
                                    val bitmap = withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(uri)?.use { stream ->
                                            val original = BitmapFactory.decodeStream(stream)
                                            original?.copy(Bitmap.Config.ARGB_8888, true)
                                        }
                                    }
                                    
                                    if (bitmap != null) {
                                        val rawResult = SteganographyUtils.decodeMessage(bitmap)
                                        var finalMsg = rawResult.lsbMessage // Prioritize LSB for now
                                        
                                        // Decrypt if password provided
                                        if (password.isNotEmpty() && finalMsg != null) {
                                            val decrypted = CryptoUtils.decrypt(finalMsg, password)
                                            if (decrypted != null) {
                                                finalMsg = decrypted
                                            }
                                        }
                                        
                                        results.add(DecodeResultItem(uri, finalMsg))
                                    } else {
                                        results.add(DecodeResultItem(uri, null, isError = true))
                                    }
                                } catch (e: Exception) {
                                    results.add(DecodeResultItem(uri, null, isError = true))
                                }
                            }
                        }
                        
                        decodeResults = results
                        isProcessing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing && selectedUris.isNotEmpty()
            ) {
                 if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Decoding...")
                } else {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decode All")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 4. Results List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                decodeResults.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                             Text(
                                text = "File: ${item.uri.lastPathSegment}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (item.isError) {
                                Text("Error reading image", color = MaterialTheme.colorScheme.error)
                            } else if (item.message.isNullOrEmpty()) {
                                Text("No hidden message found", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = item.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
