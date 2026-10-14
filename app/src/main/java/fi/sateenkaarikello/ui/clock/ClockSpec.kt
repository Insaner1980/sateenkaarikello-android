package fi.sateenkaarikello.ui.clock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import fi.sateenkaarikello.domain.MarkColor
import fi.sateenkaarikello.ui.theme.SkColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kellotaulun mitat web-version SVG:n 400×400-koordinaatistossa (keskipiste 200, 200).
 * Kaikki säteet, viivanleveydet ja fonttikoot ovat tämän koordinaatiston yksiköitä.
 */
internal object ClockSpec {
    const val CENTER = 200f

    /** Iso kello: viewBox -34 -34 468 468 (tilaa minuuttiluvuille). Pieni kello: 12 12 376 376. */
    const val VIEW_ORIGIN = -34f
    const val VIEW_SIZE = 468f
    const val MINI_VIEW_ORIGIN = 12f
    const val MINI_VIEW_SIZE = 376f

    const val RING_RADIUS = 178f
    const val RING_WIDTH = 17f
    const val FACE_RADIUS = 170f
    const val HALF_RADIUS = 168f
    const val MINUTE_LABEL_RADIUS = 207f
    const val INNER_24_RADIUS = 84f
    const val TICK_OUTER = 162f
    const val TICK_BIG_INNER = 145f
    const val TICK_SMALL_INNER = 153f
    const val TICK_BIG_WIDTH = 4f
    const val TICK_SMALL_WIDTH = 2f
    const val NUMBER_RADIUS = 121f

    const val HOUR_TAIL = 16f
    const val HOUR_LENGTH = 92f
    const val HOUR_WIDTH = 16f
    const val HOUR_WIDTH_PULSE = 25f
    const val HOUR_KNOB = 12f
    const val MINUTE_TAIL = 22f
    const val MINUTE_LENGTH = 144f
    const val MINUTE_WIDTH = 10f
    const val MINUTE_WIDTH_PULSE = 17f
    const val MINUTE_KNOB = 11f
    const val KNOB_STROKE = 4f
    const val SHADOW_DY = 3f
    const val SHADOW_ALPHA = 0.22f
    const val DIMMED_ALPHA = 0.22f
    const val PULSE_HALF_MS = 550

    const val PIN_RADIUS = 14f
    const val PIN_DOT = 5f

    const val SUN_CY = 232f
    const val SUN_RADIUS = 9f
    const val SUN_RAY_FROM = 13f
    const val SUN_RAY_TO = 18f
    const val SUN_RAY_WIDTH = 3f
    const val DAY_TEXT_Y = 266f

    const val ZONE_YLI_X = 244f
    const val ZONE_VAILLE_X = 157f
    const val ZONE_Y = 207f

    // Opetuksen merkinnät
    const val GLOW_NUMBER_RADIUS = 27f
    const val GLOW_TICK_AT = 157f
    const val GLOW_TICK_RADIUS = 7f
    const val BUBBLE_RADIUS = 18f
    const val BUBBLE_STROKE = 3f

    // Fonttikoot
    const val NUMBER_FONT = 42f
    const val MINUTE_FONT = 17f
    const val ZONE_FONT = 19f
    const val INNER_24_FONT = 16f
    const val DAY_FONT = 17f
    const val BUBBLE_FONT = 16f

    /** Dragging: kosketuksen täytyy olla vähintään tämän verran keskeltä liikkeen aikana. */
    const val DRAG_MIN_RADIUS = 8f

    /** Piste kellotaululla: säde ja kulma (0 = ylös, myötäpäivään). */
    fun point(
        radius: Float,
        degrees: Float,
    ): Offset {
        val a = Math.toRadians(degrees.toDouble())
        return Offset(CENTER + radius * sin(a).toFloat(), CENTER - radius * cos(a).toFloat())
    }

    fun markColor(color: MarkColor): Color =
        when (color) {
            MarkColor.YLI -> SkColors.Yli
            MarkColor.VAILLE -> SkColors.Vaille
            MarkColor.HALF -> SkColors.MarkHalf
            MarkColor.RED -> SkColors.MarkRed
            MarkColor.GREEN -> SkColors.MarkGreen
            MarkColor.BLUE -> SkColors.MarkBlue
        }
}
