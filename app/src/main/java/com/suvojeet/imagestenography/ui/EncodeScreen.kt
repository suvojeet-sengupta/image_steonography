package com.suvojeet.imagestenography.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.imagestenography.utils.ImageUtils
import com.suvojeet.imagestenography.utils.SteganographyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var encodedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedUri = uri
        encodedBitmap = null // Reset previous result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encode Message", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Image Picker Section
            Text(
                text = "Select an Image", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                         pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (selectedUri != null) {
                         coil.compose.AsyncImage(
                             model = selectedUri,
                             contentDescription = "Selected Image",
                             contentScale = ContentScale.Crop,
                             modifier = Modifier.fillMaxSize()
                         )
                         // Overlay to indicate clickable to change
                         Box(
                             modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                         )
                         Icon(
                             imageVector = Icons.Default.AddPhotoAlternate,
                             contentDescription = "Change Image",
                             tint = Color.White.copy(alpha = 0.8f),
                             modifier = Modifier.size(48.dp)
                         )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                 imageVector = Icons.Default.AddPhotoAlternate,
                                 contentDescription = null,
                                 modifier = Modifier.size(64.dp),
                                 tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tap to pick an image", 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Message Input Section
            Text(
                text = "Enter Secret Message", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Your Secret") },
                placeholder = { Text("Type something to hide...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))


            // Action Buttons
            if (encodedBitmap == null) {
                Button(
                    onClick = {
                        if (selectedUri == null) {
                            Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (message.isBlank()) {
                            Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        scope.launch {
                            try {
                                val bitmap = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(selectedUri!!)?.use { stream ->
                                        val original = BitmapFactory.decodeStream(stream)
                                        original.copy(Bitmap.Config.ARGB_8888, true)
                                    }
                                }

                                if (bitmap != null) {
                                    val result = withContext(Dispatchers.Default) {
                                        SteganographyUtils.encodeMessage(bitmap, message)
                                    }
                                    
                                    if (result != null) {
                                        encodedBitmap = result
                                        Toast.makeText(context, "Encryption Complete! Download the image now.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Message too long for this image", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && selectedUri != null && message.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), 
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Encrypting...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypt Image", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.spacedBy(16.dp) 
                ) {
                     Button(
                        onClick = {
                            encodedBitmap = null
                            message = ""
                            selectedUri = null // Reset completely or just reset state? User logic seems to imply finishing flow.
                            // Let's just reset result to start over or pick new image
                        },
                         modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                     ) {
                         Text("Reset")
                     }
                     
                     Button(
                        onClick = {
                            if (encodedBitmap != null) {
                                scope.launch {
                                    val savedUri = ImageUtils.saveBitmapToGallery(context, encodedBitmap!!, "Hidden_${System.currentTimeMillis()}")
                                    if (savedUri != null) {
                                        Toast.makeText(context, "Image saved to Gallery!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                             containerColor = MaterialTheme.colorScheme.secondary,
                             contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) // Save icon (using add photo as existing available icon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pro Tip: Use PNG images for best results. Sharing via WhatsApp may remove the hidden message due to compression.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
