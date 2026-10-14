package fi.sateenkaarikello.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Sound { OK, BAD, WIN, POP, TICK }

/** Yksi sävel: taajuus, alkuhetki ja kesto sekunteina, aaltomuoto ja voimakkuus (web: tone()). */
internal data class Tone(
    val freq: Double,
    val start: Double,
    val duration: Double,
    val triangle: Boolean = true,
    val volume: Double = 0.14,
)

/**
 * Äänitehosteet syntetisoidaan samoista sävelistä kuin web-versiossa (WebAudio).
 * Puhdas Kotlin, joten sävelet voi testata JVM:llä.
 */
internal object ToneSynth {
    const val SAMPLE_RATE = 44_100
    private const val FLOOR = 0.0001
    private const val ATTACK = 0.02
    private const val TAIL = 0.05

    // Musical score from the web version: frequencies in Hz and timings in seconds.
    @Suppress("MagicNumber")
    fun tones(sound: Sound): List<Tone> =
        when (sound) {
            Sound.OK -> listOf(Tone(659.0, 0.0, 0.12), Tone(784.0, 0.1, 0.12), Tone(1047.0, 0.2, 0.28))

            Sound.BAD -> listOf(Tone(330.0, 0.0, 0.16, false, 0.12), Tone(262.0, 0.15, 0.26, false, 0.12))

            Sound.WIN -> listOf(523.0, 659.0, 784.0, 1047.0, 1319.0).mapIndexed { i, f -> Tone(f, i * 0.11, 0.32) }

            Sound.POP -> listOf(Tone(880.0, 0.0, 0.08, false, 0.08))

            // Aloitusnäkymä: lyhyt tik ja matalampi tak, kun viisarit pysähtyvät.
            Sound.TICK -> listOf(Tone(1760.0, 0.0, 0.05, false, 0.1), Tone(1320.0, 0.14, 0.06, false, 0.1))
        }

    /** Eksponentiaalinen nousu ja lasku kuten exponentialRampToValueAtTime. */
    private fun envelope(
        t: Double,
        tone: Tone,
    ): Double =
        when {
            t < 0 || t > tone.duration + TAIL -> 0.0
            t < ATTACK -> FLOOR * exp(ln(tone.volume / FLOOR) * t / ATTACK)
            t < tone.duration -> tone.volume * exp(ln(FLOOR / tone.volume) * (t - ATTACK) / (tone.duration - ATTACK))
            else -> FLOOR
        }

    private fun wave(
        phase: Double,
        triangle: Boolean,
    ): Double = if (triangle) 2.0 / PI * asin(sin(phase)) else sin(phase)

    /** 16-bittinen mono-PCM. */
    fun render(sound: Sound): ShortArray {
        val tones = tones(sound)
        val length = tones.maxOf { it.start + it.duration + TAIL }
        val n = ceil(length * SAMPLE_RATE).toInt()
        val mix = DoubleArray(n)
        for (tone in tones) {
            val from = (tone.start * SAMPLE_RATE).toInt()
            val to = minOf(n, ((tone.start + tone.duration + TAIL) * SAMPLE_RATE).toInt())
            for (i in from until to) {
                val t = i.toDouble() / SAMPLE_RATE - tone.start
                mix[i] += envelope(t, tone) * wave(2 * PI * tone.freq * t, tone.triangle)
            }
        }
        val peak = mix.maxOf { abs(it) }.coerceAtLeast(1.0)
        return ShortArray(n) { (mix[it] / peak * Short.MAX_VALUE).roundToInt().toShort() }
    }
}
