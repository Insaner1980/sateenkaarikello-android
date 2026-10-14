package fi.sateenkaarikello.ui.doll

import androidx.compose.runtime.Immutable
import fi.sateenkaarikello.domain.Outfit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Nuken asento yhdellä hetkellä. [blink] 0 = silmät auki, 1 = kiinni. [swayDeg] keinunta jalkojen
 * ympäri, [lift] hypyn korkeus nuken koordinaatistossa, [armDeg] vasemman käden nosto vilkutukseen
 * ja [turn] vaakasuuntainen skaalaus pyörähdykseen (1 = edestä, −1 = takaa).
 */
@Immutable
data class DollPose(
    val blink: Float = 0f,
    val swayDeg: Float = 0f,
    val lift: Float = 0f,
    val armDeg: Float = 0f,
    val turn: Float = 1f,
)

/** Juhlinnan yksi ruutu: asento, näytetäänkö jo uusi asu ja onko juhlinta ohi. */
data class CelebrationFrame(
    val pose: DollPose,
    val showNewOutfit: Boolean,
    val done: Boolean,
)

/**
 * Nuken liikkeet puhtaina ajan funktioina, jotta ne voi testata JVM:llä ja piirtää ilman
 * uudelleenkoostamista. Lepoliike: räpytys ja kevyt keinunta. Juhlinta: pyörähdys uuteen asuun
 * ja kaksi hyppyä vilkuttaen.
 */
object DollMotion {
    const val SWAY_DEG = 2f
    const val SPIN_MS = 800L
    const val JUMP_MS = 1400L
    const val HOP_HEIGHT = 14f

    private const val BLINK_MS = 180L
    private const val SWAY_PERIOD_MS = 3200f

    /** Räpytysten alkuhetket toistuvassa jaksossa: epätasaiset välit näyttävät luonnollisilta. */
    private val BLINK_STARTS = longArrayOf(3_200L, 7_300L, 9_900L, 14_420L)
    private const val BLINK_CYCLE_MS = 14_600L

    private const val ARM_RAISE_DEG = 95f
    private const val ARM_WIGGLE_DEG = 25f
    private const val ARM_WAVES = 3
    private const val ARM_EASE = 0.15f

    fun blink(nowMs: Long): Float {
        val t = nowMs % BLINK_CYCLE_MS
        val start = BLINK_STARTS.firstOrNull { t >= it && t < it + BLINK_MS } ?: return 0f
        val k = (t - start).toFloat() / BLINK_MS
        return 1f - abs(2f * k - 1f)
    }

    fun sway(nowMs: Long): Float = SWAY_DEG * sin(2f * PI.toFloat() * nowMs / SWAY_PERIOD_MS)

    fun idle(nowMs: Long): DollPose = DollPose(blink = blink(nowMs), swayDeg = sway(nowMs))

    fun duration(
        spin: Boolean,
        jump: Boolean,
    ): Long = (if (spin) SPIN_MS else 0L) + (if (jump) JUMP_MS else 0L)

    fun celebration(
        elapsedMs: Long,
        spin: Boolean,
        jump: Boolean,
    ): CelebrationFrame {
        val spinMs = if (spin) SPIN_MS else 0L
        return when {
            elapsedMs >= duration(spin, jump) -> {
                CelebrationFrame(DollPose(), showNewOutfit = spin, done = true)
            }

            elapsedMs < spinMs -> {
                val k = elapsedMs.toFloat() / SPIN_MS
                CelebrationFrame(
                    DollPose(turn = cos(2f * PI.toFloat() * k)),
                    showNewOutfit = elapsedMs > SPIN_MS / 4,
                    done = false,
                )
            }

            else -> {
                CelebrationFrame(jumpPose((elapsedMs - spinMs).toFloat() / JUMP_MS), showNewOutfit = spin, done = false)
            }
        }
    }

    /** Kaksi hyppyä ja vilkutus; käsi nousee ja laskee pehmeästi alussa ja lopussa. */
    private fun jumpPose(k: Float): DollPose {
        val twoPiK = 2f * PI.toFloat() * k
        val ease = min(1f, min(k / ARM_EASE, (1f - k) / ARM_EASE))
        return DollPose(
            lift = HOP_HEIGHT * abs(sin(twoPiK)),
            armDeg = ease * (ARM_RAISE_DEG + ARM_WIGGLE_DEG * sin(ARM_WAVES * twoPiK)),
        )
    }
}

/**
 * Nuken juhlinta: [newOutfit] (uudet vaatteet päällä) tulee esiin pyörähdyksellä, ja [jump]
 * hyppää kahdesti vilkuttaen. Alkaa [delayMs] kuluttua nuken ilmestymisestä.
 */
@Immutable
data class DollCelebration(
    val newOutfit: Outfit?,
    val jump: Boolean,
    val delayMs: Long = 0L,
) {
    val spin: Boolean get() = newOutfit != null
}
