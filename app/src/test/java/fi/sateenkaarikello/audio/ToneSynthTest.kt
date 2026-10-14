package fi.sateenkaarikello.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthTest {
    @Test
    fun `tones match the web version`() {
        assertEquals(listOf(659.0, 784.0, 1047.0), ToneSynth.tones(Sound.OK).map { it.freq })
        assertEquals(listOf(330.0, 262.0), ToneSynth.tones(Sound.BAD).map { it.freq })
        assertEquals(listOf(523.0, 659.0, 784.0, 1047.0, 1319.0), ToneSynth.tones(Sound.WIN).map { it.freq })
        assertEquals(listOf(880.0), ToneSynth.tones(Sound.POP).map { it.freq })
        assertEquals(listOf(1760.0, 1320.0), ToneSynth.tones(Sound.TICK).map { it.freq })
    }

    @Test
    fun `rendered sounds are short and audible`() {
        for (sound in Sound.entries) {
            val pcm = ToneSynth.render(sound)
            val seconds = pcm.size.toDouble() / ToneSynth.SAMPLE_RATE
            assertTrue("$sound $seconds s", seconds in 0.1..1.0)
            assertTrue("$sound hiljainen", pcm.maxOf { abs(it.toInt()) } > 2_000)
        }
    }

    @Test
    fun `sounds start and end near silence`() {
        for (sound in Sound.entries) {
            val pcm = ToneSynth.render(sound)
            assertTrue(abs(pcm.first().toInt()) < 200)
            assertTrue(abs(pcm.last().toInt()) < 200)
        }
    }
}
