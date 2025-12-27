package com.suvojeet.imagestenography.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.suvojeet.imagestenography.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPage(page)
        }

        // Indicators and Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Indicators
            Row {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) 
                         MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(10.dp)
                    )
                }
            }

            // Next / Finish Button
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (pagerState.currentPage == 2) "Get Started" else "Next")
                if (pagerState.currentPage < 2) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.NavigateNext, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    val title = when (page) {
        0 -> "Hide Secrets in Plain Sight"
        1 -> "WhatsApp Safe Encryption"
        else -> "Smart Analysis Tool"
    }
    
    val description = when (page) {
        0 -> "Use advanced steganography to hide invisible messages inside your photos. Secure, private, and cool."
        1 -> "Our 'Robust Mode' (DCT) ensures your hidden messages survive image compression on WhatsApp and Facebook."
        else -> "Use our 'Scanner' to detect if an image contains hidden data or high-frequency noise."
    }
    
    val icon = when (page) {
        0 -> Icons.Default.Lock
        1 -> Icons.Default.Security
        else -> Icons.Default.Search
    }
    
    // Gradient Background for the card
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
