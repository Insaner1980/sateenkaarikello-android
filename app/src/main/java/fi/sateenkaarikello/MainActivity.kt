package fi.sateenkaarikello

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Vaalea ulkoasu aina: tummat kuvakkeet tila- ja navigointipalkissa myös tummassa tilassa.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(NAV_SCRIM_LIGHT, NAV_SCRIM_DARK),
        )
        super.onCreate(savedInstanceState)
        setContent { SateenkaariApp() }
    }

    override fun onPause() {
        super.onPause()
        (application as SateenkaariApplication).container.pause()
    }

    private companion object {
        val NAV_SCRIM_LIGHT = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val NAV_SCRIM_DARK = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
