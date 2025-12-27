package com.suvojeet.imagestenography.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.suvojeet.imagestenography.utils.SteganographyUtils
import com.suvojeet.imagestenography.utils.ImageUtils

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
                title = { Text("Encode Message") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    // Show preview (using Coil not necessary for simple URI if we just want to see it, 
                    // but using coil is better. For now simple Image with coil if available or basic load)
                    // We'll use Coil in real app, but strictly for prompt, I'll attempt to load Bitmap for preview too
                    // or use the AsyncImage if I had the import. 
                    // Since I added coil dependency, I can use AsyncImage.
                    // But I need to import it. simpler to just decode for preview if not too big, 
                    // or just use a placeholder text if I don't want to mess up imports without checking.
                    // I'll assume standard Coil usage.
                     coil.compose.AsyncImage(
                         model = selectedUri,
                         contentDescription = "Selected Image",
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                             imageVector = Icons.Default.AddPhotoAlternate,
                             contentDescription = null,
                             modifier = Modifier.size(48.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Select an Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick Image")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Secret Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Encoding Method:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            var selectedMethod by remember { mutableStateOf(SteganographyMethod.LSB) }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMethod == SteganographyMethod.LSB,
                    onClick = { selectedMethod = SteganographyMethod.LSB }
                )
                Text("Standard (High Capacity, Fragile)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMethod == SteganographyMethod.DCT,
                    onClick = { selectedMethod = SteganographyMethod.DCT }
                )
                Text("Robust (Low Capacity, Survives Compression)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedUri == null) {
                        Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (message.isBlank()) {
                        Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(selectedUri!!)?.use { stream ->
                                    val original = BitmapFactory.decodeStream(stream)
                                    // Make sure we have a mutable ARGB copy (though Utils also copies, safer here)
                                    original.copy(Bitmap.Config.ARGB_8888, true)
                                }
                            }

                            if (bitmap != null) {
                                val result = withContext(Dispatchers.Default) {
                                    SteganographyUtils.encodeMessage(bitmap, message, selectedMethod)
                                }
                                
                                if (result != null) {
                                    encodedBitmap = result
                                    // Save immediately or let user save? User asked "usko save bhi kr payega"
                                    // I'll auto save or button to save. "Encode & Save" is better workflow.
                                    val savedUri = ImageUtils.saveBitmapToGallery(context, result, "Hidden_${System.currentTimeMillis()}")
                                    if (savedUri != null) {
                                        Toast.makeText(context, "Saved to Pictures/Steganography", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Message too long for this image/method", Toast.LENGTH_LONG).show()
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && selectedUri != null && message.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encoding...")
                } else {
                    Text("Encode & Save Image")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedMethod == SteganographyMethod.LSB) 
                            "Note: High capacity but fragile. Crop/Resize/JPEG will destroy message."
                        else 
                            "Note: Robust against JPEG & minor edits. Very low capacity (short texts). Image quality slightly reduced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
