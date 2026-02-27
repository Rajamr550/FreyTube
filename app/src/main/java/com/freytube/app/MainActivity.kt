package com.freytube.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.freytube.app.ui.navigation.AppNavigation
import com.freytube.app.ui.theme.FreyTubeTheme

val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    error("No Activity provided")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val videoId = extractVideoId(intent)

        setContent {
            FreyTubeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalActivity provides this) {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            initialVideoId = videoId
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractVideoId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return when {
            data.host == "youtu.be" -> data.pathSegments?.firstOrNull()
            data.queryParameterNames?.contains("v") == true -> data.getQueryParameter("v")
            data.path?.startsWith("/shorts/") == true -> data.pathSegments?.lastOrNull()
            else -> null
        }
    }
}
