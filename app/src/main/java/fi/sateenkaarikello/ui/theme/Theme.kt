package fi.sateenkaarikello.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

private val SkColorScheme =
    lightColorScheme(
        primary = SkColors.Hour,
        onPrimary = SkColors.Surface,
        secondary = SkColors.Minute,
        background = SkColors.Bg,
        onBackground = SkColors.Ink,
        surface = SkColors.Surface,
        onSurface = SkColors.Ink,
        onSurfaceVariant = SkColors.InkSoft,
        outline = SkColors.Line,
    )

private val SkTypography =
    Typography(
        bodyLarge = SkType.Body,
        bodyMedium = SkType.Body,
        titleLarge = SkType.Title,
        labelLarge = SkType.Button,
    )

/** Sovelluksen ainoa teema: vaalea, kuten web-versiossa. */
@Composable
fun SateenkaariTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkColorScheme, typography = SkTypography, content = content)
}

/** Taustan vaaleanpunainen pistekuvio (web: radial-gradient 26 px:n välein). */
fun Modifier.dotBackground(): Modifier =
    drawBehind {
        drawRect(SkColors.Bg)
        val step = SkDimens.DotPatternStep.toPx()
        val r = SkDimens.DotRadius.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(SkColors.Dots, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
