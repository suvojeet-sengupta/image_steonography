package com.suvojeet.imagestenography.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suvojeet.imagestenography.utils.ImageUtils
import com.suvojeet.imagestenography.utils.SteganographyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncodeScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isWatermarkEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var encodedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val authorName = remember { 
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("author_name", "Anonymous") ?: "Anonymous"
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedUri = uri
        encodedBitmap = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encode Secret", fontWeight = FontWeight.SemiBold) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- Success View (After Encoding) ---
            if (encodedBitmap != null) {
                SuccessView(
                    bitmap = encodedBitmap!!,
                    onReset = {
                        encodedBitmap = null
                        message = ""
                        selectedUri = null
                    },
                    onSave = {
                        scope.launch {
                            val savedUri = ImageUtils.saveBitmapToGallery(context, encodedBitmap!!, "Hidden_${System.currentTimeMillis()}")
                            if (savedUri != null) {
                                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                return@Scaffold
            }

            // --- Step 1: Image Selection ---
            StepCard(step = 1, title = "Choose Carrier Image") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { 
                             pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Selected",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Edit overlay
                        Box(
                             modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha=0.6f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to select image", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Step 2: Secret Message ---
            StepCard(step = 2, title = "Compose Message") {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Enter the text you want to hide...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                // Capacity Logic (Simplified for visual)
                if (selectedUri != null && message.isNotEmpty()) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Capacity check runs on encoding...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Step 3: Security ---
            StepCard(step = 3, title = "Security Layers") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Encryption Password (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                    )

                    // Watermark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isWatermarkEnabled = !isWatermarkEnabled }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = isWatermarkEnabled, onCheckedChange = { isWatermarkEnabled = it })
                        Column {
                            Text("Invisible Watermark", fontWeight = FontWeight.Medium)
                            Text("Embed author signature ('$authorName')", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Action Button ---
            Button(
                onClick = {
                    if (selectedUri == null) {
                         Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                         return@Button
                    }
                    if (message.isEmpty()) {
                         Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                         return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            var bitmap = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(selectedUri!!)?.use { stream ->
                                    val original = BitmapFactory.decodeStream(stream)
                                    original.copy(Bitmap.Config.ARGB_8888, true)
                                }
                            }

                            if (bitmap != null) {
                                // Watermark
                                if (isWatermarkEnabled) {
                                   bitmap = withContext(Dispatchers.Default) {
                                        com.suvojeet.imagestenography.utils.WatermarkUtils.applyInvisibleWatermark(bitmap!!, authorName)
                                   }
                                }
                                
                                // Encrypt
                                val finalMessage = if (password.isNotEmpty()) {
                                    withContext(Dispatchers.Default) {
                                        com.suvojeet.imagestenography.utils.CryptoUtils.encrypt(message, password)
                                    }
                                } else {
                                    message
                                }
                                
                                if (finalMessage == null) {
                                    Toast.makeText(context, "Encryption failed", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                    return@launch
                                }

                                val result = withContext(Dispatchers.Default) {
                                    SteganographyUtils.encodeMessage(bitmap!!, finalMessage)
                                }
                                
                                if (result != null) {
                                    encodedBitmap = result
                                } else {
                                    Toast.makeText(context, "Message too long for this image", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                 if (isLoading) {
                     CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                     Spacer(modifier = Modifier.width(12.dp))
                     Text("Encrypting...")
                 } else {
                     Icon(Icons.Default.Lock, contentDescription = null)
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Encrypt & Hide")
                 }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepCard(step: Int, title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(step.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SuccessView(bitmap: Bitmap, onReset: () -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check, 
            contentDescription = null, 
            modifier = Modifier.size(80.dp).background(Color(0xFF4CAF50), CircleShape).padding(16.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Encryption Successful!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your secret is now hidden inside the image.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Preview
        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            AsyncImage(model = bitmap, contentDescription = "Result", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to Gallery")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Encrypt Another")
        }
    }
}