// Literal SVG coordinates preserve the original artwork and its drawing order.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.sateenkaarikello.ui.doll

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.ui.components.starPath
import fi.sateenkaarikello.ui.theme.SkColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import fi.sateenkaarikello.ui.theme.SkDollColors as C

/**
 * Pukeutumisnukke kerroksittain kuten web-version dollSVG: siivet, takahiukset, jalat, kengät,
 * kaula, mekko, kädet ja hihat, pää ja kasvot, otsatukka, päähine ja laukku.
 * [crop] rajaa näkyvän alueen (vaatekaapin pikkukuvat), muuten koko nukke 200×340.
 * [pose] luetaan vasta piirrettäessä, joten liike piirtää kuvan uudelleen ilman uudelleenkoostamista.
 */
@Composable
fun Doll(
    outfit: Outfit,
    modifier: Modifier = Modifier,
    crop: Rect = DollParts.Full,
    pose: () -> DollPose = RestPose,
) {
    Canvas(modifier.clearAndSetSemantics { }) {
        // SVG:n oletus: xMidYMid meet (koko rajaus näkyy, keskitettynä).
        val s = min(size.width / crop.width, size.height / crop.height)
        val dx = (size.width - crop.width * s) / 2f
        val dy = (size.height - crop.height * s) / 2f
        translate(dx, dy) {
            scale(s, s, pivot = Offset.Zero) {
                translate(-crop.left, -crop.top) {
                    val p = pose()
                    posed(p) { clipRect(crop) { drawDoll(outfit, p) } }
                }
            }
        }
    }
}

private val RestPose: () -> DollPose = { DollPose() }

/** Hyppy ylöspäin, keinunta jalkojen ympäri ja pyörähdys pystyakselin ympäri. */
private inline fun DrawScope.posed(
    p: DollPose,
    crossinline block: DrawScope.() -> Unit,
) {
    translate(0f, -p.lift) {
        rotate(p.swayDeg, Offset(CENTER_X, FEET_Y)) {
            scale(p.turn, 1f, pivot = Offset(CENTER_X, MIDDLE_Y)) { block() }
        }
    }
}

/** Rajaa vaatekaapin osakuviin; koko nukkea ei rajata, jotta vilkuttava käsi ei katkea reunaan. */
private inline fun DrawScope.clipRect(
    r: Rect,
    block: DrawScope.() -> Unit,
) {
    if (r == DollParts.Full) block() else clipPath(Path().apply { addRect(r) }, block = block)
}

private sealed interface Fill {
    data class Solid(
        val color: Color,
    ) : Fill

    data object Dots : Fill

    data object Stars : Fill

    data object RainbowGradient : Fill
}

private fun DrawScope.fillPath(
    path: Path,
    fill: Fill,
) {
    when (fill) {
        is Fill.Solid -> {
            drawPath(path, fill.color)
        }

        Fill.RainbowGradient -> {
            val b = path.getBounds()
            drawPath(path, Brush.verticalGradient(SkColors.Rainbow7, startY = b.top, endY = b.bottom))
        }

        Fill.Dots, Fill.Stars -> {
            clipPath(path) { drawPattern(fill, path.getBounds()) }
        }
    }
}

private fun circlePath(
    cx: Float,
    cy: Float,
    r: Float,
) = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

/** Toistuva kuvio käyttäjän koordinaatistossa (web: patternUnits="userSpaceOnUse"). */
private fun DrawScope.drawPattern(
    fill: Fill,
    bounds: Rect,
) {
    val tile = if (fill == Fill.Dots) 16f else 22f
    drawRect(if (fill == Fill.Dots) C.DotsBg else C.StarsBg, bounds.topLeft, bounds.size)
    var y = kotlin.math.floor(bounds.top / tile) * tile
    while (y < bounds.bottom) {
        var x = kotlin.math.floor(bounds.left / tile) * tile
        while (x < bounds.right) {
            if (fill == Fill.Dots) {
                drawCircle(C.White, 2.6f, Offset(x + 4f, y + 4f))
                drawCircle(C.White, 2.6f, Offset(x + 12f, y + 12f))
            } else {
                drawPath(starPath(x + 6f, y + 6f, 4.5f), C.StarYellow)
                drawPath(starPath(x + 17f, y + 16f, 3.2f), C.White)
            }
            x += tile
        }
        y += tile
    }
}

private fun DrawScope.ellipse(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Color,
    stroke: Color? = null,
    width: Float = 0f,
) {
    val topLeft = Offset(cx - rx, cy - ry)
    val size = Size(2 * rx, 2 * ry)
    drawOval(color, topLeft, size)
    if (stroke != null) drawOval(stroke, topLeft, size, style = Stroke(width))
}

// SVG rectangle attributes map directly to the drawing call.
private fun DrawScope.roundRect(
    topLeft: Offset,
    w: Float,
    h: Float,
    r: Float,
    color: Color,
    stroke: Color? = null,
    width: Float = 0f,
) {
    drawRoundRect(color, topLeft, Size(w, h), CornerRadius(r))
    if (stroke != null) drawRoundRect(stroke, topLeft, Size(w, h), CornerRadius(r), style = Stroke(width))
}

// SVG line endpoints and stroke attributes map directly to the drawing call.
@Suppress("LongParameterList")
private fun DrawScope.line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    width: Float,
    cap: StrokeCap = StrokeCap.Round,
) {
    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, cap)
}

private fun DrawScope.strokePath(
    path: Path,
    color: Color,
    width: Float,
    cap: StrokeCap = StrokeCap.Butt,
) {
    drawPath(path, color, style = Stroke(width, cap = cap, join = StrokeJoin.Round))
}

private fun hairFill(id: String): Fill =
    when (id) {
        "hair-pitkat" -> Fill.Solid(C.HairGold)
        CURLY_HAIR -> Fill.Solid(C.HairPink)
        "hair-sateenkaari" -> Fill.RainbowGradient
        else -> Fill.Solid(C.HairBrown)
    }

private val CurlCircles =
    listOf(
        Triple(60f, 60f, 24f),
        Triple(52f, 98f, 22f),
        Triple(56f, 134f, 20f),
        Triple(68f, 164f, 16f),
        Triple(140f, 60f, 24f),
        Triple(148f, 98f, 22f),
        Triple(144f, 134f, 20f),
        Triple(132f, 164f, 16f),
        Triple(100f, 36f, 32f),
    )

// Each wing is an SVG ellipse: center x/y, radii x/y, and rotation.
@Suppress("LongMethod", "CyclomaticComplexMethod", "DestructuringDeclarationWithTooManyEntries")
private fun DrawScope.drawDoll(
    o: Outfit,
    pose: DollPose,
) {
    // Siivet selän taakse
    if (o.extra == "extra-siivet") {
        for ((cx, cy, rx, ry, deg) in listOf(
            listOf(56f, 150f, 32f, 46f, -30f),
            listOf(64f, 202f, 20f, 28f, 28f),
            listOf(144f, 150f, 32f, 46f, 30f),
            listOf(136f, 202f, 20f, 28f, -28f),
        )) {
            rotate(deg, Offset(cx, cy)) { ellipse(cx, cy, rx, ry, C.Wing, C.Violet, 3f) }
        }
        drawCircle(C.WingSpot, 7f, Offset(50f, 140f))
        drawCircle(C.WingSpot, 7f, Offset(150f, 140f))
    }

    // Hiukset taakse
    val hair = hairFill(o.hair)
    when (o.hair) {
        "hair-letit" -> {
            ellipse(50f, 98f, 16f, 25f, C.HairBrown)
            ellipse(150f, 98f, 16f, 25f, C.HairBrown)
            drawCircle(C.Bow, 7f, Offset(58f, 76f))
            drawCircle(C.Bow, 7f, Offset(142f, 76f))
        }

        "hair-pitkat", "hair-sateenkaari" -> {
            fillPath(DollParts.LongHair, hair)
        }

        CURLY_HAIR -> {
            CurlCircles.forEach { (x, y, r) -> drawCircle(C.HairPink, r, Offset(x, y)) }
        }
    }

    // Jalat ja kengät
    line(86f, 226f, 86f, 298f, C.Skin, 16f)
    line(114f, 226f, 114f, 298f, C.Skin, 16f)
    when (o.shoes) {
        "shoes-punaiset" -> {
            ellipse(83f, 305f, 14f, 8f, C.Red, C.RedDeep, 2.5f)
            ellipse(117f, 305f, 14f, 8f, C.Red, C.RedDeep, 2.5f)
            line(79f, 297f, 93f, 297f, C.RedDeep, 3f)
            line(107f, 297f, 121f, 297f, C.RedDeep, 3f)
        }

        "shoes-saappaat" -> {
            roundRect(Offset(77f, 268f), 18f, 38f, 6f, C.Violet, C.VioletDeep, 2.5f)
            roundRect(Offset(105f, 268f), 18f, 38f, 6f, C.Violet, C.VioletDeep, 2.5f)
            ellipse(83f, 306f, 14f, 8f, C.Violet, C.VioletDeep, 2.5f)
            ellipse(117f, 306f, 14f, 8f, C.Violet, C.VioletDeep, 2.5f)
            roundRect(Offset(75f, 266f), 22f, 8f, 4f, C.PrincessLight)
            roundRect(Offset(103f, 266f), 22f, 8f, 4f, C.PrincessLight)
        }

        "shoes-kimalle" -> {
            ellipse(83f, 305f, 14f, 8f, C.Gold, C.GoldDeep, 2.5f)
            ellipse(117f, 305f, 14f, 8f, C.Gold, C.GoldDeep, 2.5f)
            drawPath(starPath(80f, 303f, 4f), C.White)
            drawPath(starPath(114f, 303f, 4f), C.White)
            drawPath(starPath(91f, 300f, 2.5f), C.White)
        }

        else -> {
            ellipse(83f, 305f, 14f, 8f, C.White, C.ShoeOutline, 2.5f)
            ellipse(117f, 305f, 14f, 8f, C.White, C.ShoeOutline, 2.5f)
        }
    }

    // Kaula ja mekko
    roundRect(Offset(92f, 110f), 16f, 24f, 6f, C.Skin)
    var sleeve: Fill = Fill.Solid(C.DressYellow)
    var sleeveR = 12f
    when (o.dress) {
        "dress-pilkku" -> {
            sleeve = Fill.Dots
            fillPath(DollParts.Dress, Fill.Dots)
            strokePath(DollParts.Collar, C.White, 6f, StrokeCap.Round)
        }

        "dress-tahti" -> {
            sleeve = Fill.Stars
            fillPath(DollParts.Dress, Fill.Stars)
            strokePath(DollParts.StarBelt, C.StarYellow, 7f)
        }

        "dress-prinsessa" -> {
            sleeve = Fill.Solid(C.PrincessLight)
            sleeveR = 16f
            drawPath(DollParts.Dress, C.Princess)
            drawPath(DollParts.PrincessSkirt, C.PrincessLight)
            roundRect(Offset(63f, 157f), 74f, 10f, 5f, C.Gold)
            drawPath(starPath(78f, 205f, 5f), C.White)
            drawPath(starPath(118f, 196f, 4f), C.White)
            drawPath(starPath(100f, 222f, 4.5f), C.White)
        }

        "dress-sateenkaari" -> {
            sleeve = Fill.Solid(C.RainbowSleeve)
            clipPath(DollParts.Dress) {
                SkColors.Rainbow7.forEachIndexed {
                    i,
                    c,
                    ->
                    drawRect(c, Offset(30f, 118f + i * 19.6f), Size(140f, 20.6f))
                }
            }
        }

        else -> {
            drawPath(DollParts.Dress, C.DressYellow)
            strokePath(DollParts.Collar, C.White, 6f, StrokeCap.Round)
            strokePath(DollParts.Hem, C.DressHem, 4f)
        }
    }

    // Kädet ja hihat. Vasen käsi nousee vilkuttamaan olkapään ympäri (laukku on oikeassa kädessä).
    rotate(pose.armDeg, Offset(74f, 136f)) {
        line(72f, 140f, 54f, 206f, C.Skin, 14f)
        drawCircle(C.Skin, 9f, Offset(53f, 210f))
    }
    line(128f, 140f, 146f, 206f, C.Skin, 14f)
    drawCircle(C.Skin, 9f, Offset(147f, 210f))
    fillPath(circlePath(74f, 136f, sleeveR), sleeve)
    fillPath(circlePath(126f, 136f, sleeveR), sleeve)

    // Pää ja kasvot
    drawCircle(C.Skin, 44f, Offset(100f, 78f))
    // Räpytys: silmät litistyvät viivoiksi, ja kiilto katoaa niiden ollessa lähes kiinni.
    val eyeRy = 7.5f * (1f - EYE_CLOSE * pose.blink)
    ellipse(85f, 86f, 5.5f, eyeRy, C.Eye)
    ellipse(115f, 86f, 5.5f, eyeRy, C.Eye)
    if (pose.blink < HIGHLIGHT_HIDE) {
        drawCircle(C.White, 2.2f, Offset(87f, 83f))
        drawCircle(C.White, 2.2f, Offset(117f, 83f))
    }
    drawCircle(C.Cheek, 6.5f, Offset(74f, 100f), alpha = CHEEK_ALPHA)
    drawCircle(C.Cheek, 6.5f, Offset(126f, 100f), alpha = CHEEK_ALPHA)
    strokePath(DollParts.Mouth, C.Mouth, 3.2f, StrokeCap.Round)

    // Otsatukka
    fillPath(DollParts.Bangs, hair)
    if (o.hair == CURLY_HAIR) {
        drawCircle(C.HairPink, 9f, Offset(58f, 78f))
        drawCircle(C.HairPink, 9f, Offset(142f, 78f))
    }

    // Päähän
    when (o.head) {
        "head-rusetti" -> {
            drawPath(DollParts.BowLeft, C.BowRed)
            drawPath(DollParts.BowRight, C.BowRed)
            drawCircle(C.RedDeep, 6f, Offset(128f, 40f))
        }

        "head-kukat" -> {
            val colors = listOf(C.FlowerPink, C.White, C.FlowerViolet, C.White, C.FlowerPink)
            listOf(-70f, -35f, 0f, 35f, 70f).forEachIndexed { i, deg ->
                val a = deg * PI.toFloat() / 180f
                val x = 100f + 46f * sin(a)
                val y = 78f - 46f * cos(a)
                for (k in 0 until 5) {
                    val b = k * PI.toFloat() * 2f / 5f
                    val c = Offset(x + 6f * cos(b), y + 6f * sin(b))
                    drawCircle(colors[i], 5.2f, c)
                    drawCircle(C.Bow, 5.2f, c, style = Stroke(1f))
                }
                drawCircle(C.FlowerCenter, 3.6f, Offset(x, y))
            }
        }

        "head-kruunu" -> {
            drawPath(DollParts.Crown, C.Gold)
            drawPath(DollParts.Crown, C.GoldDeep, style = Stroke(2.5f, join = StrokeJoin.Round))
            drawCircle(C.BowRed, 4.5f, Offset(100f, 32f))
            drawCircle(C.Jewel, 3.2f, Offset(86f, 35f))
            drawCircle(C.Jewel, 3.2f, Offset(114f, 35f))
        }
    }

    // Laukku käteen
    if (o.extra == "extra-laukku") {
        strokePath(DollParts.BagStrap, C.BagDeep, 3f)
        roundRect(Offset(138f, 198f), 30f, 24f, 7f, C.Bag, C.BagDeep, 2.5f)
        strokePath(DollParts.BagFlap, C.BagDeep, 2.5f)
        drawCircle(C.StarYellow, 3f, Offset(153f, 210f))
    }
}

private const val CHEEK_ALPHA = 0.7f
private const val EYE_CLOSE = 0.88f
private const val HIGHLIGHT_HIDE = 0.5f
private const val CENTER_X = 100f
private const val MIDDLE_Y = 170f
private const val FEET_Y = 312f
private const val CURLY_HAIR = "hair-kiharat"
