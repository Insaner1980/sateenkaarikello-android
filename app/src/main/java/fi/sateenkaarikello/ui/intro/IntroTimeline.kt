package fi.sateenkaarikello.ui.intro

import fi.sateenkaarikello.domain.ClockTime

/**
 * Aloitusnäkymän aikajana puhtaina ajan funktioina (millisekunteja intron alusta), jotta sen voi
 * testata JVM:llä ja lukea vasta piirrettäessä.
 *
 * Kello ponnahtaa esiin, ja viisarit pyörähtävät tunnin verran oikeaan aikaan maalaten
 * sateenkaarirenkaan. Viisarien pysähtyessä kuuluu tik ja keskeltä suihkuaa tähtiä. Samaan aikaan
 * nukke nousee esiin ja hyppii vilkuttaen, otsikon kirjaimet pomppaavat esiin ja tervehdys
 * ilmestyy. Lopuksi intron sisältö ja sitten tausta häivytetään kotinäkymän päältä.
 */
object IntroTimeline {
    const val CLOCK_IN_MS = 350L
    const val CLOCK_FADE_MS = 150L
    const val SPIN_START_MS = 300L
    const val SPIN_MS = 1000
    const val TICK_AT_MS = SPIN_START_MS + SPIN_MS
    const val DOLL_IN_START_MS = 1100L
    const val DOLL_IN_MS = 350L
    const val DOLL_FADE_MS = 120L
    const val DOLL_JUMP_AT_MS = DOLL_IN_START_MS + DOLL_IN_MS
    const val TITLE_START_MS = 1350L
    const val LETTER_STAGGER_MS = 40L
    const val LETTER_MS = 300L
    const val LETTER_FADE_MS = 100L
    const val GREETING_START_MS = 1900L
    const val GREETING_MS = 350L
    const val FADE_START_MS = 2700L
    const val CONTENT_FADE_MS = 200L
    const val BACKGROUND_FADE_MS = 250L
    const val END_MS = FADE_START_MS + CONTENT_FADE_MS + BACKGROUND_FADE_MS

    /** Kun järjestelmän animaatiot on poistettu, valmis kuva näytetään näin kauan. */
    const val STILL_MS = 1200L

    const val CLOCK_START_SCALE = 0.6f

    /** Nuken nousumatka suhteessa nuken korkeuteen. */
    const val DOLL_RISE = 0.35f

    /** Viisarien lähtöaika: tasatunti tuntia ennen kohdetta, joten minuuttiviisari lähtee kahdestatoista. */
    fun spinFrom(target: Int): Int =
        ClockTime.mod(target - ClockTime.MINUTES_PER_HOUR - target % ClockTime.MINUTES_PER_HOUR, ClockTime.DAY)

    fun clockScale(t: Long): Float = lerp(CLOCK_START_SCALE, 1f, easeOutBack(progress(t, 0L, CLOCK_IN_MS)))

    fun clockAlpha(t: Long): Float = progress(t, 0L, CLOCK_FADE_MS)

    /** Nuken pystysiirtymä suhteessa nuken korkeuteen: alussa alempana, lopussa 0. */
    fun dollDrop(t: Long): Float = (1f - easeOutBack(progress(t, DOLL_IN_START_MS, DOLL_IN_MS))) * DOLL_RISE

    fun dollAlpha(t: Long): Float = progress(t, DOLL_IN_START_MS, DOLL_FADE_MS)

    fun letterScale(
        t: Long,
        index: Int,
    ): Float = easeOutBack(progress(t, letterStart(index), LETTER_MS))

    fun letterAlpha(
        t: Long,
        index: Int,
    ): Float = progress(t, letterStart(index), LETTER_FADE_MS)

    fun greetingAlpha(t: Long): Float = progress(t, GREETING_START_MS, GREETING_MS)

    /** Ensin häipyy sisältö taustan päällä, jotta intro ja kotinäkymä eivät näy päällekkäin. */
    fun contentAlpha(t: Long): Float = 1f - progress(t, FADE_START_MS, CONTENT_FADE_MS)

    /** Sitten häipyy tausta ja paljastaa kotinäkymän. */
    fun overlayAlpha(t: Long): Float = 1f - progress(t, FADE_START_MS + CONTENT_FADE_MS, BACKGROUND_FADE_MS)
}

private fun letterStart(index: Int): Long = IntroTimeline.TITLE_START_MS + index * IntroTimeline.LETTER_STAGGER_MS

/** Edistyminen 0..1 välillä [start]..[start] + [duration]. */
internal fun progress(
    t: Long,
    start: Long,
    duration: Long,
): Float = ((t - start).toFloat() / duration).coerceIn(0f, 1f)

/** Pieni yli-jousto lopussa (easeOutBack): 0 → hieman yli 1 → 1. */
internal fun easeOutBack(k: Float): Float {
    val x = k - 1f
    return 1f + BACK_C3 * x * x * x + BACK_C1 * x * x
}

private fun lerp(
    a: Float,
    b: Float,
    k: Float,
): Float = a + (b - a) * k

private const val BACK_C1 = 1.70158f
private const val BACK_C3 = BACK_C1 + 1f
