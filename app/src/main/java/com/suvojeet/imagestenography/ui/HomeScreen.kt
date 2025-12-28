package com.suvojeet.imagestenography.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToEncode: () -> Unit,
    onNavigateToDecode: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToBatchEncode: () -> Unit,
    onNavigateToBatchDecode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top to accommodate scrolling
    ) {
        Spacer(modifier = Modifier.height(32.dp)) // Add top padding manually since Arrangement is Top

        Text(
            text = "Steganography",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Hide secrets in plain sight",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ActionCard(
            title = "Encode Message",
            description = "Hide a text message inside an image securely.",
            buttonText = "Start Encoding",
            onClick = onNavigateToEncode
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Decode Message",
            description = "Reveal a hidden message from an image.",
            buttonText = "Start Decoding",
            onClick = onNavigateToDecode
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Scan Image",
            description = "Detect if an image has hidden secrets (Steganalysis).",
            buttonText = "Scan & Detect",
            onClick = onNavigateToScan
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Batch Operations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Batch Encode Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                onClick = onNavigateToBatchEncode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Batch Encode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hide one message in multiple images.", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Batch Decode Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                onClick = onNavigateToBatchDecode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Batch Decode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Decode secrets from multiple files.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = buttonText)
            }
        }
    }
}
