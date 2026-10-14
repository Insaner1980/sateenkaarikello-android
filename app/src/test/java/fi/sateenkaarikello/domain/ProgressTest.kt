package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    @Test
    fun `fresh progress`() {
        val p = Progress()
        assertEquals(listOf(0, 0, 0, 0, 0), p.stars)
        assertEquals(0, p.totalStars)
        assertEquals(Outfit.Default, p.outfit)
        assertEquals("", p.dollName)
    }

    @Test
    fun `level result keeps the best stars`() {
        val p = Progress().withLevelResult(0, 2).withLevelResult(0, 1)
        assertEquals(2, p.stars[0])
        assertEquals(3, p.withLevelResult(0, 3).stars[0])
        assertEquals(2, p.totalStars)
    }

    @Test
    fun `challenge keeps the best score`() {
        val p = Progress().withChallengeScore(7)
        assertEquals(7, p.challengeBest)
        assertEquals(7, p.withChallengeScore(5).challengeBest)
        assertEquals(9, p.withChallengeScore(9).challengeBest)
    }

    @Test
    fun `lesson completion is remembered once`() {
        val p = Progress().withLessonDone("puoli").withLessonDone("puoli")
        assertEquals(setOf("puoli"), p.lessonsDone)
    }

    @Test
    fun `settings default to 12 h with sound and help on`() {
        val s = Settings()
        assertTrue(s.sound)
        assertTrue(s.help)
        assertFalse(s.h24)
    }
}
