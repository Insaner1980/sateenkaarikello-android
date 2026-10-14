package fi.sateenkaarikello.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ConfettiMathTest {
    private val eps = 0.01f

    @Test
    fun `confetti mixes rectangles with stars hearts and rainbow arcs`() {
        val random = Random(4)
        val shapes = List(4000) { ConfettiMath.pickShape(random.nextFloat()) }
        val share = ConfettiShape.entries.associateWith { s -> shapes.count { it == s } / 4000f }
        assertEquals(0.45f, share.getValue(ConfettiShape.RECT), 0.04f)
        assertEquals(0.25f, share.getValue(ConfettiShape.STAR), 0.04f)
        assertEquals(0.15f, share.getValue(ConfettiShape.HEART), 0.04f)
        assertEquals(0.15f, share.getValue(ConfettiShape.ARC), 0.04f)
    }

    @Test
    fun `flight starts at the answer and ends at the progress dot`() {
        val from = Offset(200f, 1200f)
        val to = Offset(500f, 150f)
        assertEquals(from.x, ConfettiMath.flightPoint(from, to, 0f).x, eps)
        assertEquals(from.y, ConfettiMath.flightPoint(from, to, 0f).y, eps)
        assertEquals(to.x, ConfettiMath.flightPoint(from, to, 1f).x, eps)
        assertEquals(to.y, ConfettiMath.flightPoint(from, to, 1f).y, eps)
    }

    @Test
    fun `flight arcs upwards`() {
        val from = Offset(100f, 800f)
        val to = Offset(400f, 800f)
        val mid = ConfettiMath.flightPoint(from, to, 0.5f)
        assertTrue("kaari ylöspäin: ${mid.y}", mid.y < 800f - 50f)
    }

    @Test
    fun `flying star grows then shrinks into the dot`() {
        assertTrue(ConfettiMath.flightScale(0.3f) > ConfettiMath.flightScale(0f))
        assertTrue(ConfettiMath.flightScale(1f) < ConfettiMath.flightScale(0.3f))
    }
}
