package com.suvojeet.imagestenography.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.imagestenography.utils.CryptoUtils
import com.suvojeet.imagestenography.utils.ImageUtils
import com.suvojeet.imagestenography.utils.SteganographyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEncodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableStateOf(0) }
    var processingStatus by remember { mutableStateOf("") } // "Processing 2/5..."

    // Multi-image picker
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Encode", fontWeight = FontWeight.SemiBold) },
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
                Text(if (selectedUris.isEmpty()) "Select Images" else "Selected ${selectedUris.size} Images")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Input Message
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Secret Message") },
                placeholder = { Text("Message to hide in all images") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Optional Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // 4. Progress or Action
            if (isProcessing) {
                LinearProgressIndicator(
                    progress = if (selectedUris.isNotEmpty()) processingProgress.toFloat() / selectedUris.size else 0f,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(processingStatus, style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = {
                        if (selectedUris.isEmpty()) {
                            Toast.makeText(context, "Select at least one image", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (message.isBlank()) {
                            Toast.makeText(context, "Enter a message", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isProcessing = true
                        processingProgress = 0
                        
                        scope.launch {
                            val total = selectedUris.size
                            var successCount = 0
                            
                            // Prepare message
                            val finalMessage = if (password.isNotEmpty()) {
                                CryptoUtils.encrypt(message, password) ?: message
                            } else {
                                message
                            }
                            
                            selectedUris.forEachIndexed { index, uri ->
                                processingStatus = "Processing image ${index + 1} of $total..."
                                try {
                                    withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(uri)?.use { stream ->
                                            val original = BitmapFactory.decodeStream(stream)
                                            val mutable = original?.copy(Bitmap.Config.ARGB_8888, true)
                                            
                                            // Encode (LSB for speed in batch)
                                            if (mutable != null) {
                                                val encoded = SteganographyUtils.encodeMessage(mutable, finalMessage)
                                                
                                                // Save
                                                if (encoded != null) {
                                                    ImageUtils.saveBitmapToGallery(context, encoded, "BatchEncoded_${System.currentTimeMillis()}_$index")
                                                }
                                            }
                                        }
                                    }
                                    successCount++
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                processingProgress = index + 1
                            }
                            
                            isProcessing = false
                            processingStatus = "Done! Saved $successCount of $total images."
                            Toast.makeText(context, "Batch Complete: $successCount/$total saved", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedUris.isNotEmpty()
                ) {
                    Text("Encode All Images")
                }
            }
            
            // 5. List of selected files (Simple preview)
            if (selectedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Selected Files:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(selectedUris) { uri ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uri.lastPathSegment ?: "Image",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
