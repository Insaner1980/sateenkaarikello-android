package fi.sateenkaarikello.ui.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingRevealTest {
    private val eps = 0.001f

    @Test
    fun `ring is painted as the minute hand travels one hour`() {
        assertEquals(0f, ClockSpec.ringReveal(480f, 480, 720), eps)
        assertEquals(180f, ClockSpec.ringReveal(510f, 480, 720), eps)
        assertEquals(360f, ClockSpec.ringReveal(540f, 480, 720), eps)
        assertEquals(360f, ClockSpec.ringReveal(577f, 480, 720), eps)
    }

    @Test
    fun `reveal counts around the period`() {
        // 11.00 → 0.15: animaatio kulkee 735:een asti, ja lopuksi arvo kiertyy 15:een.
        assertEquals(90f, ClockSpec.ringReveal(675f, 660, 720), eps)
        assertEquals(360f, ClockSpec.ringReveal(735f, 660, 720), eps)
        assertEquals(360f, ClockSpec.ringReveal(15f, 660, 720), eps)
        // 24 h -lähtöaika (23.00) 12 h -kellossa.
        assertEquals(360f, ClockSpec.ringReveal(15f, 1380, 720), eps)
    }

    @Test
    fun `pieces cover exactly the painted part`() {
        for (reveal in listOf(0f, 7f, 15f, 100f, 344f, 350f, 360f)) {
            val pieces = (0 until 12).flatMap { ClockSpec.ringPieces(it, reveal) }
            assertEquals("$reveal", reveal, pieces.sumOf { it.second.toDouble() }.toFloat(), eps)
            assertTrue(pieces.all { (from, sweep) -> from >= 0f && from + sweep <= reveal + eps })
        }
    }

    @Test
    fun `the colour at twelve is split around the top`() {
        assertEquals(listOf(0f to 10f), ClockSpec.ringPieces(0, 10f))
        assertEquals(listOf(0f to 15f, 345f to 15f), ClockSpec.ringPieces(0, 360f))
        assertEquals(listOf(15f to 30f), ClockSpec.ringPieces(1, 360f))
        assertEquals(emptyList<Pair<Float, Float>>(), ClockSpec.ringPieces(2, 40f))
    }
}
