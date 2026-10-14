package fi.sateenkaarikello.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class Hand { HOUR, MINUTE }

/**
 * Kellon ympyrälaskenta (webissä makeClock-funktion sisällä). Aika on minuutteja, ja jakso
 * on 720 (12 h) tai 1440 (24 h). Kulmat ovat asteita ylhäältä myötäpäivään.
 * Säteet ovat kellotaulun 400×400-koordinaatistossa (keskipiste 200, 200).
 *
 * Keep the shared clock geometry API together, including its Int/Float overloads.
 */
@Suppress("TooManyFunctions")
object ClockTime {
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_HALF_HOUR = 30
    const val MINUTES_PER_NUMBER = 5
    const val HOURS_PER_HALF_DAY = 12
    const val HOURS_PER_DAY = 24
    const val DEGREES_PER_MINUTE = 6f
    const val HALF_TURN = 180f
    const val FULL_TURN = 360f
    const val HALF_DAY = 720
    const val DAY = 1440

    /** Tuntiviisarin valinnan säde ja kulmaraja sekä keskialue, josta ei aloiteta raahausta. */
    private const val HOUR_PICK_RADIUS = 112f
    private const val PICK_ANGLE = 40f
    private const val DEAD_CENTER = 16f

    fun mod(
        a: Int,
        n: Int,
    ): Int = ((a % n) + n) % n

    fun mod(
        a: Float,
        n: Float,
    ): Float = ((a % n) + n) % n

    fun period(h24: Boolean): Int = if (h24) DAY else HALF_DAY

    /** Pyöristää tason tarkkuuteen (JavaScriptin Math.round) ja kiertää jakson ympäri. */
    fun snap(
        raw: Float,
        step: Int,
        period: Int,
    ): Int = mod(floor(raw / step + HALF).toInt() * step, period)

    /** Lyhin etumerkillinen matka ajasta [from] aikaan [to]. Puolikas kierros kulkee eteenpäin. */
    fun circularDelta(
        from: Float,
        to: Float,
        period: Int,
    ): Float {
        val d = mod(to - from, period.toFloat())
        return if (d > period / 2f) d - period else d
    }

    fun hourAngle(raw: Float): Float = mod(raw, HALF_DAY.toFloat()) / 2f

    fun minuteAngle(raw: Float): Float = mod(raw, MINUTES_PER_HOUR.toFloat()) * DEGREES_PER_MINUTE

    /** Pisteen kulma keskipisteestä; y kasvaa alaspäin kuten näytöllä. */
    fun angleOf(
        x: Float,
        y: Float,
    ): Float = mod((atan2(x, -y) * HALF_TURN / PI.toFloat()), FULL_TURN)

    private fun angleDiff(
        a: Float,
        b: Float,
    ): Float {
        val d = mod(a - b, FULL_TURN)
        return if (d > HALF_TURN) FULL_TURN - d else d
    }

    /** Minuuttiviisarin raahaus vie tuntiviisaria mukanaan kuten oikeassa kellossa. */
    fun minuteDrag(
        raw: Float,
        angleDeg: Float,
        period: Int,
    ): Float {
        var d = angleDeg / DEGREES_PER_MINUTE - mod(raw, MINUTES_PER_HOUR.toFloat())
        if (d > MINUTES_PER_HALF_HOUR.toFloat()) d -= MINUTES_PER_HOUR.toFloat()
        if (d < -MINUTES_PER_HALF_HOUR.toFloat()) d += MINUTES_PER_HOUR.toFloat()
        return mod(raw + d, period.toFloat())
    }

    /** Tuntiviisarin raahaus asettaa ajan kulmasta. 24 h -tilassa valitaan lähempi vuorokauden puolisko. */
    fun hourDrag(
        raw: Float,
        angleDeg: Float,
        period: Int,
    ): Float {
        val c = angleDeg * 2f
        if (period != DAY) return c
        val other = c + HALF_DAY
        return if (abs(circularDelta(raw, other, period)) < abs(circularDelta(raw, c, period))) other else c
    }

    /** Kumpaa viisaria kosketus tarttuu. Null, jos kosketus on aivan keskellä. */
    fun pickHand(
        radius: Float,
        angleDeg: Float,
        raw: Float,
    ): Hand? {
        if (radius < DEAD_CENTER) return null
        val dh = angleDiff(angleDeg, hourAngle(raw))
        val dm = angleDiff(angleDeg, minuteAngle(raw))
        return when {
            radius < HOUR_PICK_RADIUS && dh < PICK_ANGLE -> Hand.HOUR
            dm < PICK_ANGLE -> Hand.MINUTE
            dh < dm -> Hand.HOUR
            else -> Hand.MINUTE
        }
    }

    /** Sama viisariasento: 3.30 ja 15.30 näyttävät kellotaululla samalta. */
    fun sameHands(
        a: Int,
        b: Int,
    ): Boolean = mod(a, HALF_DAY) == mod(b, HALF_DAY)

    /** Webin animaation pehmennys (ease-in-out, neliöllinen). */
    fun easeInOut(k: Float): Float = if (k < HALF) 2f * k * k else 1f - (-EASING_MULTIPLIER * k + 2f).pow(2) / 2f

    /** Animaation kesto: tarkka [ms] opetuksen kierroksille, muuten matkan mukaan 200 ms – [ms]. */
    fun animationMillis(
        delta: Float,
        ms: Int,
        exact: Boolean,
    ): Int = if (exact) ms else max(MIN_ANIMATION_MS, min(ms, (abs(delta) * MS_PER_MINUTE).toInt()))
}

private const val HALF = 0.5f
private const val EASING_MULTIPLIER = 2f
private const val MIN_ANIMATION_MS = 200
private const val MS_PER_MINUTE = 3f
