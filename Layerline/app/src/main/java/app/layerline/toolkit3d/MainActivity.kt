package app.layerline.toolkit3d

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.layerline.toolkit3d.ui.LayerlineRoot
import app.layerline.toolkit3d.ui.theme.LayerlineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash hands over straight away to the animated Compose splash.
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val app = application as LayerlineApp
        app.ads.gatherConsent(this)

        setContent {
            LayerlineTheme {
                LayerlineRoot(app)
            }
        }
    }
}
