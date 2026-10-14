package fi.sateenkaarikello.ui.components

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val STAR_POINTS = 10
private const val STAR_INNER_RATIO = 0.45f

/** Viisisakarainen tähti keskipisteellä ([x], [y]) ja säteellä [r] (web: starPts). */
fun starPath(
    x: Float,
    y: Float,
    r: Float,
): Path =
    Path().apply {
        for (i in 0 until STAR_POINTS) {
            val a = PI / (STAR_POINTS / 2) * i - PI / 2
            val rr = if (i % 2 == 1) r * STAR_INNER_RATIO else r
            val px = x + rr * cos(a).toFloat()
            val py = y + rr * sin(a).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

/** Sydän yksikköruudussa (−0,5…0,5), konfettia varten. */
@Suppress("MagicNumber")
val HeartPath: Path =
    Path().apply {
        moveTo(0f, 0.45f)
        cubicTo(-0.55f, 0.1f, -0.45f, -0.45f, 0f, -0.18f)
        cubicTo(0.45f, -0.45f, 0.55f, 0.1f, 0f, 0.45f)
        close()
    }
