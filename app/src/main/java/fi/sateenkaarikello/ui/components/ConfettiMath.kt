package fi.sateenkaarikello.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Konfetin muodot: suorakulmio kuten webissä sekä tähti, sydän ja sateenkaaren kaari. */
enum class ConfettiShape { RECT, STAR, HEART, ARC }

/** Konfetin ja lentävän tähden laskenta, puhdas Kotlin testejä varten. */
internal object ConfettiMath {
    private const val RECT_SHARE = 0.45f
    private const val STAR_SHARE = 0.25f
    private const val HEART_SHARE = 0.15f

    /** Kaaren korkeus suhteessa lentomatkaan. */
    private const val FLIGHT_ARC = 0.4f
    private const val FLIGHT_GROW = 0.8f
    private const val FLIGHT_SHRINK = 0.5f

    /** Muoto tasajakautuneesta luvusta 0–1: 45 % suorakulmioita, 25 % tähtiä, 15 % sydämiä, 15 % kaaria. */
    fun pickShape(r: Float): ConfettiShape =
        when {
            r < RECT_SHARE -> ConfettiShape.RECT
            r < RECT_SHARE + STAR_SHARE -> ConfettiShape.STAR
            r < RECT_SHARE + STAR_SHARE + HEART_SHARE -> ConfettiShape.HEART
            else -> ConfettiShape.ARC
        }

    /** Lentävän tähden paikka hetkellä [k] (0–1): toisen asteen Bézier-kaari, joka kaartuu ylöspäin. */
    fun flightPoint(
        from: Offset,
        to: Offset,
        k: Float,
    ): Offset {
        val distance = hypot(to.x - from.x, to.y - from.y)
        val control = Offset((from.x + to.x) / 2f, min(from.y, to.y) - distance * FLIGHT_ARC)
        val u = 1f - k
        return from * (u * u) + control * (2f * u * k) + to * (k * k)
    }

    /** Tähti kasvaa lennon alussa ja pienenee laskeutuessaan edistymispalloon. */
    fun flightScale(k: Float): Float = (1f + FLIGHT_GROW * sin(PI.toFloat() * k)) * (1f - FLIGHT_SHRINK * k)
}
