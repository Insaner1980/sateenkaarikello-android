package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonsTest {
    @Test
    fun `level lessons match the web mapping`() {
        assertEquals(
            // CPD-OFF: Keep expected lesson mapping independent of production data.
            listOf(
                listOf("viisarit", "kierros", "tasan"),
                listOf("puoli"),
                listOf("vartti", "ylivaille"),
                listOf("viisi"),
                listOf("minuutit"),
            ),
            // CPD-ON
            Lessons.levelLessons,
        )
    }

    @Test
    fun `first play of a level shows its unlearned lessons`() {
        val plan = Lessons.introPlan(level = 0, h24 = false, progress = Progress())
        assertEquals(listOf("viisarit", "kierros", "tasan"), plan.queue)
        assertEquals(setOf("0"), plan.introShown)
    }

    @Test
    fun `learned lessons are skipped`() {
        val progress = Progress(lessonsDone = setOf("kierros"))
        assertEquals(listOf("viisarit", "tasan"), Lessons.introPlan(0, false, progress).queue)
    }

    @Test
    fun `lessons are shown automatically only once per level`() {
        val progress = Progress(introShown = setOf("0"))
        assertTrue(Lessons.introPlan(0, false, progress).queue.isEmpty())
    }

    @Test
    fun `24 h mode adds the day lesson once`() {
        val first = Lessons.introPlan(1, h24 = true, progress = Progress())
        assertEquals(listOf("puoli", "vrk"), first.queue)
        assertEquals(setOf("1", "h24"), first.introShown)
        val again = Lessons.introPlan(2, h24 = true, progress = Progress(introShown = first.introShown))
        assertEquals(listOf("vartti", "ylivaille"), again.queue)
    }

    @Test
    fun `24 h lesson is skipped when already learned`() {
        val plan =
            Lessons.introPlan(
                0,
                h24 = true,
                progress = Progress(lessonsDone = setOf("vrk"), introShown = setOf("0")),
            )
        assertTrue(plan.queue.isEmpty())
        assertEquals(setOf("0", "h24"), plan.introShown)
    }

    @Test
    fun `demo builder records steps in order`() {
        val steps =
            demo {
                pause(700)
                highlight(Hand.HOUR)
                move(240, 6000, exact = true)
                under(Mark.NumberGlow(3), Mark.TickGlow(15))
                over(Mark.Bubble(5, "5"))
                say(Text.Plain("Tasan kolme"))
                say(null)
            }
        assertEquals(
            listOf(
                DemoStep.Wait(700),
                DemoStep.Highlight(Hand.HOUR),
                DemoStep.Move(240, 6000, exact = true),
                DemoStep.Under(listOf(Mark.NumberGlow(3), Mark.TickGlow(15))),
                DemoStep.Over(listOf(Mark.Bubble(5, "5"))),
                DemoStep.Say(Text.Plain("Tasan kolme")),
                DemoStep.Say(null),
            ),
            steps,
        )
    }
}
