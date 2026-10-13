package factor.hamrah.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import factor.hamrah.app.ui.navigation.AppNavigation
import factor.hamrah.app.ui.screens.SplashScreen
import factor.hamrah.app.ui.theme.MyApplicationTheme
import factor.hamrah.app.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }

            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val isDark = when (settings.appTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 350),
                        label = "SplashCrossfade"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            AppNavigation(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
