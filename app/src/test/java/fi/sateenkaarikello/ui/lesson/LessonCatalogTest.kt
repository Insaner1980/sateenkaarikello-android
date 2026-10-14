package fi.sateenkaarikello.ui.lesson

import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.DemoStep
import fi.sateenkaarikello.domain.Lessons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCatalogTest {
    private val all = LessonCatalog.all

    @Test
    fun `nine lessons in the web order`() {
        assertEquals(
            listOf("viisarit", "kierros", "tasan", "puoli", "vartti", "ylivaille", "viisi", "minuutit", "vrk"),
            all.map { it.id },
        )
    }

    @Test
    fun `page counts match the web version`() {
        assertEquals(listOf(3, 3, 3, 4, 3, 3, 3, 2, 3), all.map { it.pages.size })
    }

    @Test
    fun `every level lesson exists and only the day lesson is 24 h only`() {
        Lessons.levelLessons.flatten().forEach { LessonCatalog.lesson(it) }
        assertEquals(listOf(Lessons.DAY_LESSON), all.filter { it.h24Only }.map { it.id })
    }

    @Test
    fun `each quiz has exactly one correct choice`() {
        for (lesson in all) assertEquals(lesson.id, 1, lesson.quiz.choices.count { it.correct })
    }

    @Test
    fun `day lesson uses the 24 h clock with help`() {
        val vrk = LessonCatalog.lesson(Lessons.DAY_LESSON)
        assertTrue(vrk.pages.all { it.period == ClockTime.DAY && it.help })
        assertEquals(ClockTime.DAY, vrk.quiz.period)
        assertEquals(1140, vrk.quiz.time)
    }

    @Test
    fun `demo moves stay on the clock`() {
        for (lesson in all) {
            for (page in lesson.pages) {
                assertTrue(page.time in 0 until page.period)
                page.demo.filterIsInstance<DemoStep.Move>().forEach {
                    assertTrue(
                        "${lesson.id} ${it.to}",
                        it.to in 0 until page.period,
                    )
                }
            }
        }
    }

    @Test
    fun `minute lesson lights all sixty ticks`() {
        val page = LessonCatalog.lesson("kierros").pages[2]
        val last = page.demo.filterIsInstance<DemoStep.Under>().last()
        assertEquals(60, last.marks.size)
    }
}
