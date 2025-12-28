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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
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
import coil.compose.AsyncImage
import com.suvojeet.imagestenography.utils.SteganographyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecodeScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var decodedMessage by remember { mutableStateOf<SteganographyUtils.DecodeResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedUri = uri
        decodedMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decode Message", fontWeight = FontWeight.SemiBold) },
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
                text = "Select Encoded Image", 
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
                                "Tap to pick image", 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Change Image")
            }

            
            // Password Input Section
            var encryptionPassword by remember { mutableStateOf("") }
            var isPasswordVisible by remember { mutableStateOf(false) }

            Text(
                text = "Decryption Password (If needed)", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = encryptionPassword,
                onValueChange = { encryptionPassword = it },
                label = { Text("Password") },
                placeholder = { Text("Enter password to unlock") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                trailingIcon = {
                    val image = if (isPasswordVisible)
                        Icons.Default.Visibility
                    else Icons.Default.VisibilityOff

                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password")
                    }
                },
                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = {
                    if (selectedUri == null) {
                        Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
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
                                    val rawResult = SteganographyUtils.decodeMessage(bitmap)
                                    
                                    // Attempt decryption if password provided
                                    if (encryptionPassword.isNotEmpty()) {
                                        val lsbDecrypted = rawResult.lsbMessage?.let { 
                                            com.suvojeet.imagestenography.utils.CryptoUtils.decrypt(it, encryptionPassword) 
                                        }
                                        val dctDecrypted = rawResult.dctMessage?.let { 
                                            com.suvojeet.imagestenography.utils.CryptoUtils.decrypt(it, encryptionPassword) 
                                        }
                                        
                                        // If decryption worked (not null), use it. 
                                        // If returned null (wrong pass/not encrypted), keep raw but maybe warn?
                                        // For simplicity, if decryption fails but password was given, we assume wrong pass and show null or error? 
                                        // Let's return raw if decryption returns null (so user sees garbage and knows password is wrong)
                                        // OR we could try to show visual hint.
                                        
                                        // Better UX: Return what we got. If it was encrypted, it will look like garbage.
                                        // If decryption succeeded, it looks like text.
                                        // Wait, CryptoUtils.decrypt returns NULL on failure.
                                        
                                        SteganographyUtils.DecodeResult(
                                            lsbMessage = lsbDecrypted ?: rawResult.lsbMessage, // Show raw if decrypt fails
                                            dctMessage = dctDecrypted ?: rawResult.dctMessage
                                        )
                                    } else {
                                        rawResult
                                    }
                                }
                                
                                decodedMessage = result
                                
                                // Optional: Feedback if password was used but decryption failed for both (still garbage)
                                // Hard to detect "garbage", but if CryptoUtils returned null, it failed.
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
                enabled = !isLoading && selectedUri != null,
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
                    Text("Decrypting...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.LockOpen, contentDescription = null) // Unlock icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decrypt Message", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Watermark Reveal Section
            var showWatermarkDialog by remember { mutableStateOf(false) }
            val watermarkBitmap by produceState<Bitmap?>(initialValue = null, key1 = showWatermarkDialog) {
                 if (showWatermarkDialog && selectedUri != null) {
                     withContext(Dispatchers.IO) {
                         context.contentResolver.openInputStream(selectedUri!!)?.use { stream ->
                            val original = BitmapFactory.decodeStream(stream)
                            // Convert to Mutable
                            val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
                            value = com.suvojeet.imagestenography.utils.WatermarkUtils.revealWatermark(mutable)
                         }
                     }
                 }
            }

            OutlinedButton(
                onClick = { if (selectedUri != null) showWatermarkDialog = true else Toast.makeText(context, "Select image first", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reveal Invisible Watermark")
            }

            if (showWatermarkDialog) {
                AlertDialog(
                    onDismissRequest = { showWatermarkDialog = false },
                    title = { Text("Watermark Inspection") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Enhanced Blue Channel to reveal hidden patterns.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (watermarkBitmap != null) {
                                AsyncImage(
                                    model = watermarkBitmap,
                                    contentDescription = "Revealed Watermark",
                                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWatermarkDialog = false }) { Text("Close") }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Result Section
            if (decodedMessage != null) {
                val lsb = decodedMessage!!.lsbMessage
                val dct = decodedMessage!!.dctMessage
                
                if (lsb == null && dct == null) {
                     Text(
                        text = "No hidden messages found.", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                     Text(
                        text = "Hidden Messages Found:", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    if (!lsb.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Standard Message (High Quality)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(
                                        text = lsb,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (!dct.isNullOrEmpty() && dct != lsb) { // Don't show twice if identical (unless verified diff needed)
                         Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "WhatsApp Safe Message (Robust)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(
                                        text = dct,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
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
