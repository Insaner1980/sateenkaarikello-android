package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ClockTimeTest {
    private val eps = 0.001f

    @Test
    fun `mod is always non-negative`() {
        assertEquals(719, ClockTime.mod(-1, 720))
        assertEquals(0, ClockTime.mod(1440, 720))
        assertEquals(59.5f, ClockTime.mod(-0.5f, 60f), eps)
    }

    @Test
    fun `period depends on mode`() {
        assertEquals(720, ClockTime.period(h24 = false))
        assertEquals(1440, ClockTime.period(h24 = true))
    }

    @Test
    fun `snap rounds to the level step and wraps`() {
        assertEquals(185, ClockTime.snap(187.4f, 5, 720))
        assertEquals(190, ClockTime.snap(187.6f, 5, 720))
        assertEquals(715, ClockTime.snap(717f, 5, 720))
        assertEquals(0, ClockTime.snap(718f, 5, 720))
        assertEquals(180, ClockTime.snap(205f, 60, 720))
        assertEquals(240, ClockTime.snap(215f, 60, 720))
        assertEquals(195, ClockTime.snap(200f, 15, 720))
        assertEquals(210, ClockTime.snap(203f, 15, 720))
        assertEquals(187, ClockTime.snap(187.4f, 1, 720))
        assertEquals(0, ClockTime.snap(1438f, 5, 1440))
    }

    @Test
    fun `circular delta takes the shortest way`() {
        assertEquals(10f, ClockTime.circularDelta(710f, 0f, 720), eps)
        assertEquals(-10f, ClockTime.circularDelta(0f, 710f, 720), eps)
        assertEquals(60f, ClockTime.circularDelta(180f, 240f, 720), eps)
        // Täsmälleen puolikas kierros kulkee eteenpäin, kuten webissä (vrk-oppitunti).
        assertEquals(720f, ClockTime.circularDelta(0f, 720f, 1440), eps)
        assertEquals(720f, ClockTime.circularDelta(720f, 0f, 1440), eps)
    }

    @Test
    fun `hand angles`() {
        assertEquals(105f, ClockTime.hourAngle(210f), eps)
        assertEquals(180f, ClockTime.minuteAngle(210f), eps)
        assertEquals(105f, ClockTime.hourAngle(930f), eps)
    }

    @Test
    fun `minute drag moves forward within the hour`() {
        // 3.00, minuuttiviisari numeroon 3 (90°) -> 3.15
        assertEquals(195f, ClockTime.minuteDrag(180f, 90f, 720), eps)
    }

    @Test
    fun `minute drag past twelve advances the hour`() {
        // 11.55 -> minuuttiviisari 1 minuutin kohdalle -> 12.01 eli 0.01
        assertEquals(1f, ClockTime.minuteDrag(715f, 6f, 720), eps)
        // 3.58 -> 4.02
        assertEquals(242f, ClockTime.minuteDrag(238f, 12f, 720), eps)
    }

    @Test
    fun `minute drag backwards past twelve goes back an hour`() {
        assertEquals(175f, ClockTime.minuteDrag(182f, 330f, 720), eps)
        assertEquals(715f, ClockTime.minuteDrag(2f, 330f, 720), eps)
    }

    @Test
    fun `minute drag switches day half in 24 h mode`() {
        assertEquals(721f, ClockTime.minuteDrag(715f, 6f, 1440), eps)
        assertEquals(1f, ClockTime.minuteDrag(1435f, 6f, 1440), eps)
    }

    @Test
    fun `hour drag sets time from the hour angle`() {
        // Tuntiviisari numeroon 4 (120°) -> 4.00
        assertEquals(240f, ClockTime.hourDrag(180f, 120f, 720), eps)
        // Puolivälissä 3 ja 4 (105°) -> 3.30
        assertEquals(210f, ClockTime.hourDrag(180f, 105f, 720), eps)
    }

    @Test
    fun `hour drag keeps the closest day half in 24 h mode`() {
        assertEquals(960f, ClockTime.hourDrag(900f, 120f, 1440), eps)
        assertEquals(240f, ClockTime.hourDrag(180f, 120f, 1440), eps)
        // 23.30 -> numeroon 1 (30°): lähempänä on 1.00 seuraavana yönä
        assertEquals(60f, ClockTime.hourDrag(1410f, 30f, 1440), eps)
    }

    @Test
    fun `pick hand follows the web rule`() {
        // Klo 3.00: tuntiviisari 90°, minuuttiviisari 0°
        assertNull(ClockTime.pickHand(radius = 10f, angleDeg = 90f, raw = 180f))
        // Klo 3.15: tuntiviisari 97,5°, minuuttiviisari 90°. Sisäkehällä tunti, ulkokehällä minuutti.
        assertEquals(Hand.HOUR, ClockTime.pickHand(radius = 80f, angleDeg = 92f, raw = 195f))
        assertEquals(Hand.MINUTE, ClockTime.pickHand(radius = 150f, angleDeg = 92f, raw = 195f))
        assertEquals(Hand.MINUTE, ClockTime.pickHand(radius = 80f, angleDeg = 10f, raw = 180f))
        // Kaukana molemmista: lähempi kulma voittaa
        assertEquals(Hand.HOUR, ClockTime.pickHand(radius = 150f, angleDeg = 140f, raw = 180f))
        assertEquals(Hand.MINUTE, ClockTime.pickHand(radius = 150f, angleDeg = 300f, raw = 180f))
    }

    @Test
    fun `angle of a point measured clockwise from twelve`() {
        assertEquals(0f, ClockTime.angleOf(0f, -10f), eps)
        assertEquals(90f, ClockTime.angleOf(10f, 0f), eps)
        assertEquals(180f, ClockTime.angleOf(0f, 10f), eps)
        assertEquals(270f, ClockTime.angleOf(-10f, 0f), eps)
    }

    @Test
    fun `same hands ignores day half`() {
        assertTrue(ClockTime.sameHands(930, 210))
        assertFalse(ClockTime.sameHands(930, 215))
    }

    @Test
    fun `ease in out quad`() {
        assertEquals(0f, ClockTime.easeInOut(0f), eps)
        assertEquals(0.5f, ClockTime.easeInOut(0.5f), eps)
        assertEquals(1f, ClockTime.easeInOut(1f), eps)
        assertEquals(0.125f, ClockTime.easeInOut(0.25f), eps)
    }

    @Test
    fun `animation duration follows the web rule`() {
        assertEquals(6000, ClockTime.animationMillis(delta = 60f, ms = 6000, exact = true))
        assertEquals(200, ClockTime.animationMillis(delta = 10f, ms = 700, exact = false))
        assertEquals(700, ClockTime.animationMillis(delta = 300f, ms = 700, exact = false))
        assertEquals(450, ClockTime.animationMillis(delta = -150f, ms = 700, exact = false))
    }

    @Test
    fun `minutes of a local time count from midnight`() {
        assertEquals(0, ClockTime.minutesOf(LocalTime.MIDNIGHT))
        assertEquals(15 * 60 + 30, ClockTime.minutesOf(LocalTime.of(15, 30, 59)))
        assertEquals(ClockTime.DAY - 1, ClockTime.minutesOf(LocalTime.MAX))
    }
}
