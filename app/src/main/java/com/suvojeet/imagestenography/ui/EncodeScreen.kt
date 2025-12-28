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
    
    // Capacity State
    var dctLimit by remember { mutableIntStateOf(0) }
    var lsbLimit by remember { mutableIntStateOf(0) }

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
    
    // Calculate Capacity
    LaunchedEffect(selectedUri) {
        if (selectedUri != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(selectedUri!!)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    val w = options.outWidth
                    val h = options.outHeight
                    
                    if (w > 0 && h > 0) {
                        dctLimit = com.suvojeet.imagestenography.utils.DCTUtils.getMaxMessageLength(w, h)
                        lsbLimit = SteganographyUtils.getMaxLsbCapacity(w, h)
                    }
                }
            }
        } else {
            dctLimit = 0
            lsbLimit = 0
        }
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
                
                // Capacity Logic
                if (dctLimit > 0) {
                     val currentLength = message.length
                     val isDctSafe = currentLength <= dctLimit
                     val dctProgress = (currentLength.toFloat() / dctLimit.toFloat()).coerceIn(0f, 1f)
                     
                     Spacer(modifier = Modifier.height(12.dp))
                     
                     Text("Storage Capacity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                     Spacer(modifier = Modifier.height(4.dp))
                     
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("Robust:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                         LinearProgressIndicator(
                             progress = { dctProgress },
                             modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                             color = if (isDctSafe) Color(0xFF4CAF50) else Color(0xFFFF9800),
                             trackColor = MaterialTheme.colorScheme.surfaceVariant
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("$currentLength / $dctLimit", style = MaterialTheme.typography.labelSmall)
                     }
                     
                     if (!isDctSafe) {
                          Text(
                             "⚠️ Exceeds robust limit. May not survive compression.",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color(0xFFFF9800),
                             fontSize = 11.sp,
                             modifier = Modifier.padding(top = 4.dp)
                          )
                     }
                     
                     Spacer(modifier = Modifier.height(4.dp))
                     Text("Max (Standard): $lsbLimit chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                } else if (selectedUri != null) {
                    // Loading or error calculating
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Calculating capacity...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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

                    // Strength Meter
                    if (password.isNotEmpty()) {
                        val strength = calculatePasswordStrength(password)
                        val color = when (strength) {
                            0, 1 -> Color(0xFFE57373) // Red (Weak)
                            2 -> Color(0xFFFFB74D) // Orange (Medium)
                            3 -> Color(0xFFFFF176) // Yellow (Good)
                            else -> Color(0xFF81C784) // Green (Strong)
                        }
                        val label = when (strength) {
                            0, 1 -> "Weak"
                            2 -> "Medium"
                            3 -> "Good"
                            else -> "Strong"
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { (strength + 1) / 5f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Strength: $label", style = MaterialTheme.typography.bodySmall, color = color)
                                Text(
                                    text = "Generate Strong", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        password = generateStrongPassword()
                                    }
                                )
                            }
                        }
                    } else {
                         Text(
                            text = "Generate Strong Password", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End).clickable {
                                password = generateStrongPassword()
                            }
                        )
                    }

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

fun calculatePasswordStrength(password: String): Int {
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++ // Symbol
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    
    // Normalize to 0-4 range roughly
    // Just simple logic: 
    // <8 = 0
    // 8 chars = 1
    // 8 + mixed = 2
    // 12 + mixed = 3
    // 12 + mixed + symbols = 4
    
    // Let's rely on the accumulative score but clamp it
    return score.coerceIn(0, 4)
}

fun generateStrongPassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
    return (1..16)
        .map { chars.random() }
        .joinToString("")
}