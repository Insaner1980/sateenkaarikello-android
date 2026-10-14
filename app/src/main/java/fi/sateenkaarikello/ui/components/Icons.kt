package fi.sateenkaarikello.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import fi.sateenkaarikello.ui.theme.SkColors

/** Kuvakkeen kerros: SVG-polku, täyttö ja valinnainen ääriviiva. Polkua ei muuteta luomisen jälkeen. */
@Immutable
class IconLayer(
    d: String,
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
    val cap: StrokeCap = StrokeCap.Butt,
) {
    val path: Path = PathParser().parsePathString(d).toPath()
}

/** Web-version SVG-kuvakkeet sellaisinaan. */
@Immutable
class SvgIcon(
    val viewBox: Float,
    val layers: List<IconLayer>,
)

@Composable
fun SkIcon(
    icon: SvgIcon,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / icon.viewBox
        scale(s, s, pivot = Offset.Zero) {
            for (layer in icon.layers) {
                layer.fill?.let { drawPath(layer.path, it) }
                layer.stroke?.let {
                    drawPath(
                        layer.path,
                        it,
                        style = Stroke(layer.strokeWidth, cap = layer.cap, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

object SkIcons {
    private const val STAR_D = "M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5L2.5 9.4l6.6-.9z"
    private const val INK_STROKE = 3f

    val StarOn = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOn, SkColors.StarOnStroke, 1.4f)))
    val StarOff = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOff, SkColors.StarOffStroke, 1.4f)))

    val Lock =
        SvgIcon(
            24f,
            listOf(
                IconLayer("M8 11h8a3 3 0 0 1 3 3v4a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-4a3 3 0 0 1 3-3z", SkColors.Lock),
                IconLayer("M8 11V8a4 4 0 0 1 8 0v3", stroke = SkColors.Lock, strokeWidth = 2.6f),
            ),
        )

    val Book =
        SvgIcon(
            40f,
            listOf(
                IconLayer(
                    "M20 10C15 6 9 6 4 8v24c5-2 11-2 16 2 5-4 11-4 16-2V8c-5-2-11-2-16 2z",
                    SkColors.Surface,
                    SkColors.Ink,
                    INK_STROKE,
                ),
                IconLayer("M20 10v24", stroke = SkColors.Ink, strokeWidth = INK_STROKE),
            ),
        )

    val Clock =
        SvgIcon(
            40f,
            listOf(
                IconLayer("M20 3a17 17 0 1 1 0 34a17 17 0 1 1 0-34z", SkColors.Surface, SkColors.Ink, INK_STROKE),
                IconLayer("M20 20V9M20 20l7 4", stroke = SkColors.Ink, strokeWidth = 3.5f, cap = StrokeCap.Round),
            ),
        )

    val Star = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOn, SkColors.StarOnStroke, 1.4f)))

    val Speech =
        SvgIcon(
            40f,
            listOf(
                IconLayer(
                    "M6 8h28a3 3 0 0 1 3 3v15a3 3 0 0 1-3 3H17l-8 6v-6H6a3 3 0 0 1-3-3V11a3 3 0 0 1 3-3z",
                    SkColors.Surface,
                    SkColors.Ink,
                    INK_STROKE,
                ),
                IconLayer(
                    "M16 15a4 4 0 1 1 5 4c-1 .4-1 1-1 2",
                    stroke = SkColors.Ink,
                    strokeWidth = INK_STROKE,
                    cap = StrokeCap.Round,
                ),
                IconLayer("M20 23.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z", SkColors.Ink),
            ),
        )

    val Dress =
        SvgIcon(
            40f,
            listOf(
                IconLayer("M14 5h12l-2 7 9 22H7l9-22z", SkColors.Surface, SkColors.Ink, INK_STROKE),
                IconLayer("M15 12h10", stroke = SkColors.MenuDressBelt, strokeWidth = INK_STROKE),
            ),
        )
}
