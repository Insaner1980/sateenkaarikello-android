package fi.sateenkaarikello.ui.intro

import fi.sateenkaarikello.domain.ClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import fi.sateenkaarikello.ui.intro.IntroTimeline as T

class IntroTimelineTest {
    private val eps = 0.001f

    @Test
    fun `hands start on the hour an hour before and make one sweep forward`() {
        assertEquals(8 * 60, T.spinFrom(9 * 60 + 37))
        assertEquals(14 * 60, T.spinFrom(15 * 60))
        // Puolenyön yli: 0.15 alkaa edellisen vuorokauden puolelta.
        assertEquals(23 * 60, T.spinFrom(15))
        for (target in 0 until ClockTime.DAY) {
            val delta = ClockTime.circularDelta(T.spinFrom(target).toFloat(), target.toFloat(), ClockTime.HALF_DAY)
            assertTrue("$target: $delta", delta >= 60f && delta < 120f)
        }
    }

    @Test
    fun `clock pops in with a small overshoot and settles`() {
        assertEquals(T.CLOCK_START_SCALE, T.clockScale(0L), eps)
        assertTrue((0L..T.CLOCK_IN_MS step 10L).any { T.clockScale(it) > 1f })
        assertEquals(1f, T.clockScale(T.CLOCK_IN_MS), eps)
        assertEquals(1f, T.clockScale(T.END_MS), eps)
        assertEquals(0f, T.clockAlpha(0L), eps)
        assertEquals(1f, T.clockAlpha(T.CLOCK_FADE_MS), eps)
    }

    @Test
    fun `the tick comes when the hands stop`() {
        assertEquals(T.SPIN_START_MS + T.SPIN_MS, T.TICK_AT_MS)
        assertTrue(T.TICK_AT_MS < T.FADE_START_MS)
    }

    @Test
    fun `doll rises into place before it starts jumping`() {
        assertEquals(T.DOLL_RISE, T.dollDrop(0L), eps)
        assertEquals(0f, T.dollAlpha(T.DOLL_IN_START_MS), eps)
        assertEquals(0f, T.dollDrop(T.DOLL_JUMP_AT_MS), eps)
        assertEquals(1f, T.dollAlpha(T.DOLL_JUMP_AT_MS), eps)
    }

    @Test
    fun `title letters appear one after another and are all shown before the fade`() {
        val title = "Sateenkaarikello"
        val firstShown = { i: Int -> (0L..T.END_MS step 5L).first { T.letterAlpha(it, i) > 0f } }
        val starts = title.indices.map(firstShown)
        assertEquals(starts.sorted(), starts)
        assertTrue(starts.zipWithNext().all { (a, b) -> b > a })
        title.indices.forEach {
            assertEquals(1f, T.letterScale(T.FADE_START_MS, it), eps)
            assertEquals(1f, T.letterAlpha(T.FADE_START_MS, it), eps)
        }
        assertEquals(1f, T.greetingAlpha(T.FADE_START_MS), eps)
    }

    @Test
    fun `content fades before the background so the screens never overlap`() {
        assertEquals(1f, T.contentAlpha(T.FADE_START_MS), eps)
        assertEquals(1f, T.overlayAlpha(0L), eps)
        val backgroundStart = T.FADE_START_MS + T.CONTENT_FADE_MS
        assertEquals(0f, T.contentAlpha(backgroundStart), eps)
        assertEquals(1f, T.overlayAlpha(backgroundStart), eps)
        assertEquals(0f, T.overlayAlpha(T.END_MS), eps)
        val fade = (T.FADE_START_MS..T.END_MS step 10L).map(T::overlayAlpha)
        assertEquals(fade.sortedDescending(), fade)
        assertTrue((0L..T.END_MS step 10L).all { T.contentAlpha(it) < 1f || T.overlayAlpha(it) == 1f })
    }

    @Test
    fun `ease out back starts at zero and ends at one`() {
        assertEquals(0f, easeOutBack(0f), eps)
        assertEquals(1f, easeOutBack(1f), eps)
        assertEquals(0f, progress(-5L, 0L, 100L), eps)
        assertEquals(0.5f, progress(150L, 100L, 100L), eps)
        assertEquals(1f, progress(500L, 100L, 100L), eps)
    }
}
