package com.suvojeet.imagestenography

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.suvojeet.imagestenography.ui.DecodeScreen
import com.suvojeet.imagestenography.ui.EncodeScreen
import com.suvojeet.imagestenography.ui.HomeScreen
import com.suvojeet.imagestenography.ui.OnboardingScreen
import com.suvojeet.imagestenography.ui.SteganalysisScreen
import com.suvojeet.imagestenography.ui.theme.ImageStenographyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        
        setContent {
            ImageStenographyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start at Onboarding if first run, else Home
                    var currentScreen by remember { mutableStateOf(if (isFirstRun) Screen.Onboarding else Screen.Home) }

                    when (currentScreen) {
                        Screen.Onboarding -> OnboardingScreen(
                            onFinish = {
                                prefs.edit().putBoolean("is_first_run", false).apply()
                                currentScreen = Screen.Home
                            }
                        )
                        Screen.Home -> HomeScreen(
                            onNavigateToEncode = { currentScreen = Screen.Encode },
                            onNavigateToDecode = { currentScreen = Screen.Decode },
                            onNavigateToScan = { currentScreen = Screen.Scan }
                        )
                        Screen.Encode -> EncodeScreen(
                            onBack = { currentScreen = Screen.Home }
                        )
                        Screen.Decode -> DecodeScreen(
                            onBack = { currentScreen = Screen.Home }
                        )
                        Screen.Scan -> SteganalysisScreen(
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    Onboarding, Home, Encode, Decode, Scan
}