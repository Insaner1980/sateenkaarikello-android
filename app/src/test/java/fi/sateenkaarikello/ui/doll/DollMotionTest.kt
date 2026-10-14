package fi.sateenkaarikello.ui.doll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DollMotionTest {
    private val eps = 0.01f

    @Test
    fun `eyes are open most of the time and blink fully now and then`() {
        val samples = (0L until 60_000L step 10L).map { DollMotion.blink(it) }
        assertTrue(samples.all { it in 0f..1f })
        val closedShare = samples.count { it > 0f }.toFloat() / samples.size
        assertTrue("silmät kiinni $closedShare", closedShare < 0.1f)
        assertTrue(samples.any { it > 0.95f })
        assertEquals(0f, DollMotion.blink(0L), eps)
    }

    @Test
    fun `blinks happen at least every five seconds`() {
        var lastBlink = 0L
        for (t in 0L until 60_000L step 10L) {
            if (DollMotion.blink(t) > 0.5f) lastBlink = t
            assertTrue("ei räpytystä $t", t - lastBlink < 5_000L)
        }
    }

    @Test
    fun `sway is gentle and starts upright`() {
        assertEquals(0f, DollMotion.sway(0L), eps)
        assertTrue((0L until 20_000L step 50L).all { abs(DollMotion.sway(it)) <= DollMotion.SWAY_DEG + eps })
        assertTrue((0L until 5_000L step 50L).any { abs(DollMotion.sway(it)) > DollMotion.SWAY_DEG / 2 })
    }

    @Test
    fun `no celebration is done at once`() {
        assertEquals(0L, DollMotion.duration(spin = false, jump = false))
        assertTrue(DollMotion.celebration(0L, spin = false, jump = false).done)
    }

    @Test
    fun `spin turns all the way around and shows the new outfit after the first quarter`() {
        val spin = DollMotion.SPIN_MS
        assertEquals(1f, DollMotion.celebration(0L, spin = true, jump = false).pose.turn, eps)
        assertFalse(DollMotion.celebration(0L, spin = true, jump = false).showNewOutfit)
        assertEquals(-1f, DollMotion.celebration(spin / 2, spin = true, jump = false).pose.turn, eps)
        assertTrue(DollMotion.celebration(spin / 4 + 1, spin = true, jump = false).showNewOutfit)
        val end = DollMotion.celebration(spin, spin = true, jump = false)
        assertTrue(end.done)
        assertTrue(end.showNewOutfit)
        assertEquals(1f, end.pose.turn, eps)
    }

    @Test
    fun `jump hops twice while waving and lands at rest`() {
        val d = DollMotion.JUMP_MS
        val frames = (0L..d step 10L).map { DollMotion.celebration(it, spin = false, jump = true).pose }
        assertEquals(0f, frames.first().lift, eps)
        assertTrue(frames.any { it.lift > DollMotion.HOP_HEIGHT * 0.9f })
        assertTrue(frames.any { it.armDeg > 60f })
        // Kaksi hyppyä: nosto palaa lähelle nollaa puolivälissä.
        assertEquals(0f, DollMotion.celebration(d / 2, spin = false, jump = true).pose.lift, 0.5f)
        val end = DollMotion.celebration(d, spin = false, jump = true)
        assertTrue(end.done)
        assertEquals(0f, end.pose.lift, eps)
        assertEquals(0f, end.pose.armDeg, eps)
    }

    @Test
    fun `spin comes before the jump`() {
        val f = DollMotion.celebration(DollMotion.SPIN_MS / 2, spin = true, jump = true)
        assertEquals(0f, f.pose.lift, eps)
        assertFalse(f.done)
        assertEquals(DollMotion.SPIN_MS + DollMotion.JUMP_MS, DollMotion.duration(spin = true, jump = true))
        val hop = DollMotion.celebration(DollMotion.SPIN_MS + DollMotion.JUMP_MS / 4, spin = true, jump = true)
        assertTrue(hop.pose.lift > 0f)
        assertEquals(1f, hop.pose.turn, eps)
    }
}
