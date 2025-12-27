package com.suvojeet.imagestenography.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.suvojeet.imagestenography.utils.SteganographyMethod
import com.suvojeet.imagestenography.utils.SteganographyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var decodedMessage by remember { mutableStateOf<String?>(null) }
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
                title = { Text("Decode Message") },
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
                        Text("Select Encoded Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Text("Decoding Method:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var selectedMethod by remember { mutableStateOf(SteganographyMethod.LSB) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMethod == SteganographyMethod.LSB,
                    onClick = { selectedMethod = SteganographyMethod.LSB }
                )
                Text("Standard")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMethod == SteganographyMethod.DCT,
                    onClick = { selectedMethod = SteganographyMethod.DCT }
                )
                Text("Robust")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedUri == null) {
                        Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
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
                                    SteganographyUtils.decodeMessage(bitmap, selectedMethod)
                                }
                                
                                decodedMessage = result ?: "No hidden message found using ${selectedMethod.name} method."
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
                enabled = !isLoading && selectedUri != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decoding...")
                } else {
                    Text("Decode Message")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (decodedMessage != null) {
                Text("Hidden Message:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    SelectionContainer {
                        Text(
                            text = decodedMessage!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
