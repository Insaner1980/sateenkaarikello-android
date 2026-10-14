package fi.sateenkaarikello.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import fi.sateenkaarikello.ui.rememberReduceMotion
import fi.sateenkaarikello.ui.theme.SkColors
import kotlin.random.Random

// A mutable simulation record; grouping its independent coordinates adds no domain meaning.
@Suppress("LongParameterList")
internal class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var rotation: Float,
    val spin: Float,
    val shape: ConfettiShape = ConfettiShape.RECT,
    var life: Int = 0,
)

/** Edistymispalloon lentävä tähti. [k] etenee 0:sta 1:een. */
internal class Flight(
    val from: Offset,
    val to: Offset,
    var k: Float = 0f,
)

/**
 * Konfetti, sama fysiikka kuin webissä (painovoima 0,38, ilmanvastus 0,99, 110 ruutua).
 * Web-arvot ovat CSS-pikseleitä, joten ne kerrotaan näytön tiheydellä.
 * Ohitetaan, kun järjestelmän animaatiot on poistettu.
 */
@Stable
class ConfettiState internal constructor(
    private val density: Float,
    private val enabled: Boolean,
) {
    internal val particles = mutableListOf<Particle>()
    internal val flights = mutableListOf<Flight>()
    internal var frame by mutableLongStateOf(0L)
    internal var running by mutableLongStateOf(0L)
    private val random = Random.Default

    fun burst(
        origin: Offset,
        count: Int = 60,
    ) {
        if (!enabled) return
        repeat(count) {
            val shape = ConfettiMath.pickShape(random.nextFloat())
            particles +=
                Particle(
                    x = origin.x,
                    y = origin.y,
                    vx = (random.nextFloat() - 0.5f) * 13f * density,
                    vy = (-random.nextFloat() * 12f - 3f) * density,
                    size = (5f + random.nextFloat() * 6f) * density * shapeScale(shape),
                    color = if (shape == ConfettiShape.STAR) SkColors.StarOn else SkColors.Rainbow7.random(random),
                    rotation = random.nextFloat() * 6f,
                    spin = (random.nextFloat() - 0.5f) * 0.3f,
                    shape = shape,
                )
        }
        running++
    }

    /** Pieni kultainen tähtisuihku, esim. tähden laskeutuessa tai nuken vaihtaessa asua. */
    fun sparkle(
        origin: Offset,
        count: Int = SPARKLE_COUNT,
    ) {
        if (!enabled) return
        repeat(count) {
            particles +=
                Particle(
                    x = origin.x,
                    y = origin.y,
                    vx = (random.nextFloat() - 0.5f) * SPARKLE_SPEED * density,
                    vy = (-random.nextFloat() * SPARKLE_SPEED - 1f) * density,
                    size = (6f + random.nextFloat() * 5f) * density,
                    color = if (random.nextBoolean()) SkColors.StarOn else SkColors.Pressed,
                    rotation = random.nextFloat() * 6f,
                    spin = (random.nextFloat() - 0.5f) * 0.4f,
                    shape = ConfettiShape.STAR,
                )
        }
        running++
    }

    /** Kultainen tähti lentää kaarena kohdasta [from] kohtaan [to] ja säihkähtää perillä. */
    fun flyStar(
        from: Offset,
        to: Offset,
    ) {
        if (!enabled) return
        flights += Flight(from, to)
        running++
    }

    internal val isActive: Boolean get() = particles.isNotEmpty() || flights.isNotEmpty()

    private fun shapeScale(shape: ConfettiShape): Float = if (shape == ConfettiShape.RECT) 1f else SHAPE_SCALE

    internal fun step(height: Float) {
        val g = GRAVITY * density
        particles.removeAll { it.life >= LIFE || it.y >= height + OFFSCREEN_MARGIN * density }
        val landed = flights.filter { it.k >= 1f }
        flights.removeAll(landed)
        landed.forEach { sparkle(it.to, LANDING_SPARKLES) }
        for (f in flights) f.k = (f.k + 1f / FLIGHT_FRAMES).coerceAtMost(1f)
        for (p in particles) {
            p.vy += g
            p.vx *= DRAG
            p.x += p.vx
            p.y += p.vy
            p.rotation += p.spin
            p.life++
        }
        frame++
    }

    companion object {
        /** Tähden lennon kesto; edistymispallo täyttyy samaan aikaan. */
        const val FLIGHT_MS = 650L
        private const val FLIGHT_FRAMES = FLIGHT_MS / 16.667f
        private const val GRAVITY = 0.38f
        private const val DRAG = 0.99f
        private const val LIFE = 110
        private const val SHAPE_SCALE = 1.5f
        private const val SPARKLE_COUNT = 24
        private const val SPARKLE_SPEED = 7f
        private const val LANDING_SPARKLES = 10
    }
}

val LocalConfetti = staticCompositionLocalOf<ConfettiState?> { null }

/** Koko näytön konfettikerros sisällön päällä. */
@Composable
fun ConfettiHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current.density
    val reduceMotion = rememberReduceMotion()
    val state = remember(density, reduceMotion) { ConfettiState(density, !reduceMotion) }
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalConfetti provides state) {
            content()
            val height = remember { floatArrayOf(0f) }
            LaunchedEffect(state.running) {
                // Web laskee yhden askeleen ruutua kohti 60 Hz:llä; tässä askeleet otetaan ajan mukaan.
                var last = withFrameNanos { it }
                var pending = 0L
                while (state.isActive) {
                    withFrameNanos { now ->
                        pending += now - last
                        last = now
                        while (pending >= FRAME_NANOS) {
                            state.step(height[0])
                            pending -= FRAME_NANOS
                        }
                        state.frame++
                    }
                }
                state.frame++
            }
            Canvas(Modifier.fillMaxSize()) {
                height[0] = size.height
                state.frame // luetaan, jotta piirretään joka ruudulla
                for (p in state.particles) {
                    translate(p.x, p.y) {
                        rotateRad(p.rotation, pivot = Offset.Zero) {
                            scale(p.size, p.size, pivot = Offset.Zero) { drawConfettiShape(p.shape, p.color) }
                        }
                    }
                }
                val starSize = FLYING_STAR_DP * density
                for (f in state.flights) {
                    val at = ConfettiMath.flightPoint(f.from, f.to, f.k)
                    translate(at.x, at.y) {
                        rotateRad(f.k * FLIGHT_TURN, pivot = Offset.Zero) {
                            val s = starSize * ConfettiMath.flightScale(f.k)
                            scale(s, s, pivot = Offset.Zero) {
                                drawPath(UnitStar, SkColors.StarOn)
                                drawPath(UnitStar, SkColors.StarOnStroke, style = Stroke(STAR_OUTLINE))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Onnistumisen konfetti: elementit tallentavat keskipisteensä palautettuun karttaan avaimella
 * (`Modifier.confettiAnchor { anchors[key] = it }`). Kun [celebrate] kasvaa, konfetti ammutaan
 * [anchorKey]-avaimen kohdalta. Jos [flyTo] antaa kohteen, sinne lentää lisäksi kultainen tähti.
 */
@Composable
fun rememberCelebrationAnchors(
    celebrate: Int,
    count: Int,
    flyTo: () -> Offset? = { null },
    anchorKey: () -> Int?,
): SnapshotStateMap<Int, Offset> {
    val confetti = LocalConfetti.current
    val anchors = remember { mutableStateMapOf<Int, Offset>() }
    val currentAnchorKey by rememberUpdatedState(anchorKey)
    val currentFlyTo by rememberUpdatedState(flyTo)
    LaunchedEffect(celebrate) {
        if (celebrate == 0) return@LaunchedEffect
        val from = currentAnchorKey()?.let { anchors[it] } ?: return@LaunchedEffect
        confetti?.burst(from, count)
        currentFlyTo()?.let { confetti?.flyStar(from, it) }
    }
    return anchors
}

/** Ankkuri konfetille: [onCenter] saa elementin keskipisteen juurikoordinaateissa. */
fun Modifier.confettiAnchor(onCenter: (Offset) -> Unit): Modifier =
    onGloballyPositioned { onCenter(it.boundsInRoot().center) }

private const val FRAME_NANOS = 16_666_667L

private const val OFFSCREEN_MARGIN = 30f
private const val RECTANGLE_Y_DIVISOR = 3
private const val RECTANGLE_HEIGHT_RATIO = 0.66f
private const val UNIT_HALF = 0.5f
private const val FLYING_STAR_DP = 30f
private const val FLIGHT_TURN = 6.283f
private const val STAR_OUTLINE = 0.06f
private const val ARC_RADIUS = 0.45f
private const val ARC_WIDTH = 0.22f

private val UnitStar = starPath(0f, 0f, UNIT_HALF)

/** Yksi konfetin pala yksikkökoossa; kutsuja skaalaa sen oikeaan kokoon. */
private fun DrawScope.drawConfettiShape(
    shape: ConfettiShape,
    color: Color,
) {
    when (shape) {
        ConfettiShape.RECT -> {
            drawRect(color, Offset(-UNIT_HALF, -1f / RECTANGLE_Y_DIVISOR), Size(1f, RECTANGLE_HEIGHT_RATIO))
        }

        ConfettiShape.STAR -> {
            drawPath(UnitStar, color)
        }

        ConfettiShape.HEART -> {
            drawPath(HeartPath, color)
        }

        ConfettiShape.ARC -> {
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-ARC_RADIUS, -ARC_RADIUS),
                size = Size(2 * ARC_RADIUS, 2 * ARC_RADIUS),
                style = Stroke(ARC_WIDTH, cap = StrokeCap.Round),
            )
        }
    }
}
