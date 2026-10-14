package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevelsTest {
    private val seeds = 0 until 200

    @Test
    fun `five levels with the web steps and samples`() {
        assertEquals(listOf(60, 30, 15, 5, 1), Levels.all.map { it.step })
        assertEquals(listOf(180, 150, 555, 260, 463), Levels.all.map { it.sample })
        assertEquals(listOf(0, 1, 2, 3, 4), Levels.all.map { it.index })
    }

    @Test
    fun `random time stays within the period and the level step`() {
        for (level in Levels.all) {
            for (h24 in listOf(false, true)) {
                for (seed in seeds) {
                    val t = Levels.randomTime(level, h24, Random(seed))
                    assertTrue("$t", t in 0 until ClockTime.period(h24))
                    assertEquals("taso ${level.index} aika $t", 0, (t % 60) % level.step)
                }
            }
        }
    }

    @Test
    fun `minutes level mostly avoids multiples of five`() {
        val random = Random(42)
        val n = 2000
        val odd = (0 until n).count { Levels.all[4].minutes(random) % 5 != 0 }
        assertTrue("$odd", odd in (n * 0.68).toInt()..(n * 0.82).toInt())
    }

    @Test
    fun `make tasks gives ten unique valid times alternating question types`() {
        for (level in Levels.all) {
            for (h24 in listOf(false, true)) {
                for (seed in seeds) {
                    val tasks = Levels.makeTasks(level, h24, Random(seed))
                    assertEquals(10, tasks.size)
                    assertEquals(10, tasks.map { it.time }.toSet().size)
                    tasks.forEachIndexed { i, task ->
                        assertEquals(if (i % 2 == 0) TaskType.READ else TaskType.SET, task.type)
                        assertTrue(task.time in 0 until ClockTime.period(h24))
                        assertEquals(0, (task.time % 60) % level.step)
                    }
                }
            }
        }
    }

    @Test
    fun `options are three distinct step valid times including the answer`() {
        for (level in Levels.all) {
            for (h24 in listOf(false, true)) {
                for (seed in seeds) {
                    val random = Random(seed)
                    val t = Levels.randomTime(level, h24, random)
                    val options = Levels.optionsFor(t, level, h24, random)
                    assertEquals(3, options.size)
                    assertEquals(3, options.toSet().size)
                    assertTrue(t in options)
                    assertValidOptions(options, h24, level)
                }
            }
        }
    }

    private fun assertValidOptions(
        options: List<Int>,
        h24: Boolean,
        level: Level,
    ) {
        for (option in options) {
            assertTrue(option in 0 until ClockTime.period(h24))
            assertEquals("taso ${level.index}: $option", 0, (option % 60) % level.step)
        }
    }

    @Test
    fun `options include typical confusions`() {
        // 3.15 vartit-tasolla: tunti väärin (2.15 tai 4.15) on aina mukana
        val options = Levels.optionsFor(195, Levels.all[2], h24 = false, random = Random(1))
        assertTrue(options.any { it == 135 || it == 255 })
    }

    @Test
    fun `24 h options often show the same hands in the other day half`() {
        val level = Levels.all[2]
        val n = 1000
        val random = Random(7)
        var mirrored = 0
        repeat(n) {
            val t = Levels.randomTime(level, true, random)
            if (Levels.optionsFor(t, level, true, random).contains((t + 720) % 1440)) mirrored++
        }
        assertTrue("$mirrored", mirrored in 600..800)
    }

    @Test
    fun `12 h options never leave the half day`() {
        for (seed in seeds) {
            val options = Levels.optionsFor(690, Levels.all[1], h24 = false, random = Random(seed))
            assertTrue(options.all { it < 720 })
        }
    }

    @Test
    fun `stars from mistakes`() {
        assertEquals(3, Levels.stars(0))
        assertEquals(3, Levels.stars(1))
        assertEquals(2, Levels.stars(2))
        assertEquals(2, Levels.stars(3))
        assertEquals(1, Levels.stars(4))
        assertEquals(1, Levels.stars(20))
    }

    @Test
    fun `set task starts in the target day half away from the answer`() {
        assertEquals(0, Levels.setTaskStart(195))
        assertEquals(360, Levels.setTaskStart(0))
        assertEquals(360, Levels.setTaskStart(720 - 720))
        assertEquals(720, Levels.setTaskStart(930))
        assertEquals(1080, Levels.setTaskStart(720))
    }

    @Test
    fun `random clock time is a different five minute time`() {
        for (seed in seeds) {
            val t = Levels.randomClockTime(period = 720, current = 180, random = Random(seed))
            assertTrue(t != 180 && t % 5 == 0 && t in 0 until 720)
        }
    }

    @Test
    fun `challenge question uses the first four levels and changes the time`() {
        val random = Random(3)
        var previous: Int? = null
        repeat(300) {
            val q = Levels.challengeQuestion(h24 = false, previous = previous, random = random)
            assertTrue(q.level.index in 0..3)
            assertTrue(q.time != previous)
            previous = q.time
        }
    }
}
