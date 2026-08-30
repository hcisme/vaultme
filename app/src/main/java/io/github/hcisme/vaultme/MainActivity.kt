package io.github.hcisme.vaultme

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import io.github.hcisme.vaultme.navigation.NavigationApp
import io.github.hcisme.vaultme.ui.theme.VaultMeTheme
import io.github.hcisme.vaultme.utils.LocalNavController

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            val navController = rememberNavController()

            CompositionLocalProvider(LocalNavController provides navController) {
                VaultMeTheme(
                    darkTheme = false,
                    dynamicColor = false
                ) {
                    NavigationApp()
                }
            }
        }
    }
}
