package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Portattu web-version testeistä tests/test-aika.js ja tests/test-24.js. */
class FinnishTimeTest {
    @Test
    fun `all 720 times give non-empty unique words`() {
        val seen = mutableMapOf<String, Int>()
        for (t in 0 until 720) {
            val w = FinnishTime.timeWords(t)
            assertTrue("tyhjä $t", w.isNotBlank())
            assertFalse("tuplaväli $t: '$w'", w.contains("  "))
            assertFalse("null $t: '$w'", w.contains("null"))
            val previous = seen.put(w, t)
            assertEquals("tupla: $t ja $previous sanovat '$w'", null, previous)
        }
        assertEquals(720, seen.size)
    }

    @Test
    fun `key twelve hour phrases match the web version`() {
        val expected =
            mapOf(
                0 to "tasan kaksitoista",
                180 to "tasan kolme",
                210 to "puoli neljä",
                195 to "varttia yli kolme",
                225 to "varttia vaille neljä",
                185 to "viittä yli kolme",
                200 to "kahtakymmentä yli kolme",
                205 to "viittä vaille puoli neljä",
                215 to "viittä yli puoli neljä",
                220 to "kahtakymmentä vaille neljä",
                230 to "kymmentä vaille neljä",
                235 to "viittä vaille neljä",
                690 to "puoli kaksitoista",
                30 to "puoli yksi",
                55 to "viittä vaille yksi",
                715 to "viittä vaille kaksitoista",
                183 to "kolme minuuttia yli kolme",
                181 to "yksi minuutti yli kolme",
                208 to "kaksi minuuttia vaille puoli neljä",
                223 to "seitsemäntoista minuuttia vaille neljä",
            )
        for ((t, w) in expected) assertEquals("aika $t", w, FinnishTime.timeWords(t))
    }

    @Test
    fun `time words wrap around the half day`() {
        assertEquals(FinnishTime.timeWords(719), FinnishTime.timeWords(-1))
        assertEquals("puoli neljä", FinnishTime.timeWords(930))
    }

    @Test
    fun `digital forms match the web version`() {
        assertEquals("12.00", FinnishTime.digital(0))
        assertEquals("3.05", FinnishTime.digital(185))
        assertEquals("15.30", FinnishTime.digital(930, h24 = true))
        assertEquals("3.30", FinnishTime.digital(930))
        assertEquals("0.05", FinnishTime.digital(5, h24 = true))
        assertEquals("12.00", FinnishTime.digital(720, h24 = true))
        assertEquals("0.05", FinnishTime.digital(1445, h24 = true))
    }

    @Test
    fun `full words add the day part only in 24 h mode`() {
        assertEquals("puoli neljä iltapäivällä", FinnishTime.fullWords(930, h24 = true))
        assertEquals("puoli neljä yöllä", FinnishTime.fullWords(210, h24 = true))
        assertEquals("puoli neljä", FinnishTime.fullWords(930, h24 = false))
    }

    @Test
    fun `day parts match the web version`() {
        assertEquals("aamulla", FinnishTime.dayPart(480).adverb)
        assertEquals("aamupäivällä", FinnishTime.dayPart(630).adverb)
        assertEquals("illalla", FinnishTime.dayPart(1200).adverb)
        assertEquals("yöllä", FinnishTime.dayPart(1380).adverb)
        assertEquals("keskiyö", FinnishTime.dayPart(0).name)
        assertEquals("keskiyöllä", FinnishTime.dayPart(0).adverb)
        assertEquals("keskipäivä", FinnishTime.dayPart(720).name)
        assertEquals("iltapäivä", FinnishTime.dayPart(930).name)
        assertEquals("ilta", FinnishTime.dayPart(1200).name)
        assertTrue(FinnishTime.dayPart(360).isSun)
        assertFalse(FinnishTime.dayPart(359).isSun)
        assertFalse(FinnishTime.dayPart(1080).isSun)
    }

    @Test
    fun `spoken 24 h numbers match the web version`() {
        assertEquals("viisitoista kolmekymmentä", FinnishTime.spoken24(930))
        assertEquals("viisitoista nolla viisi", FinnishTime.spoken24(905))
        assertEquals("kaksikymmentäkolme neljäkymmentäseitsemän", FinnishTime.spoken24(1380 + 47))
        assertEquals("nolla nolla nolla", FinnishTime.spoken24(0))
        assertEquals("kaksikymmentä kaksikymmentäyksi", FinnishTime.spoken24(1221))
    }

    @Test
    fun `all 1440 times give unique full phrases and opposite sun`() {
        val seen = HashSet<String>()
        for (t in 0 until 1440) {
            val w = FinnishTime.fullWords(t, h24 = true)
            val s = FinnishTime.spoken24(t)
            assertFalse("null $t", (w + s).contains("null"))
            assertNotEquals("aurinko sama $t", FinnishTime.dayPart(t).isSun, FinnishTime.dayPart(t + 720).isSun)
            seen.add(w)
        }
        assertEquals(1440, seen.size)
    }

    @Test
    fun `capitalize first letter only`() {
        assertEquals("Puoli neljä", "puoli neljä".capitalizeFirst())
        assertEquals("Äiti", "äiti".capitalizeFirst())
        assertEquals("", "".capitalizeFirst())
    }

    @Test
    fun `greeting follows the day parts`() {
        assertEquals(Greeting.NIGHT, FinnishTime.greeting(0))
        assertEquals(Greeting.NIGHT, FinnishTime.greeting(4 * 60 + 59))
        assertEquals(Greeting.MORNING, FinnishTime.greeting(5 * 60))
        assertEquals(Greeting.MORNING, FinnishTime.greeting(9 * 60 + 59))
        assertEquals(Greeting.DAY, FinnishTime.greeting(10 * 60))
        assertEquals(Greeting.DAY, FinnishTime.greeting(17 * 60 + 59))
        assertEquals(Greeting.EVENING, FinnishTime.greeting(18 * 60))
        assertEquals(Greeting.EVENING, FinnishTime.greeting(21 * 60 + 59))
        assertEquals(Greeting.NIGHT, FinnishTime.greeting(22 * 60))
    }
}
