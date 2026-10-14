package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Vinkkien valinta, kuten webin hintRead ja hintSet. Tekstit ovat strings.xml:ssä. */
class HintsTest {
    @Test
    fun `hour hand positions`() {
        assertEquals(HourPosition.Exact(3), Hints.hourPosition(180))
        assertEquals(HourPosition.Exact(12), Hints.hourPosition(0))
        assertEquals(HourPosition.JustPassed(3), Hints.hourPosition(200))
        assertEquals(HourPosition.Between(3, 4), Hints.hourPosition(210))
        assertEquals(HourPosition.Between(12, 1), Hints.hourPosition(30))
        assertEquals(HourPosition.Almost(4), Hints.hourPosition(220))
        assertEquals(HourPosition.Almost(1), Hints.hourPosition(55))
        assertEquals(HourPosition.Between(3, 4), Hints.hourPosition(930))
    }

    @Test
    fun `minute hand positions`() {
        assertEquals(MinutePosition.Top, Hints.minutePosition(0))
        assertEquals(MinutePosition.Bottom, Hints.minutePosition(30))
        assertEquals(MinutePosition.QuarterPast, Hints.minutePosition(15))
        assertEquals(MinutePosition.QuarterTo, Hints.minutePosition(45))
        assertEquals(MinutePosition.Number(4, 20), Hints.minutePosition(20))
        assertEquals(MinutePosition.Ticks(23), Hints.minutePosition(23))
    }

    @Test
    fun `read hint when hands are right but day half is wrong`() {
        assertEquals(Hint.HandsRight(FinnishTime.dayPart(930)), Hints.forRead(target = 930, guess = 210))
    }

    @Test
    fun `read hint points to the hour hand when minutes match`() {
        assertEquals(Hint.ReadHour(HourPosition.Exact(3), nearHalf = false), Hints.forRead(180, 240))
        assertEquals(Hint.ReadHour(HourPosition.Between(3, 4), nearHalf = true), Hints.forRead(210, 270))
        assertEquals(Hint.ReadHour(HourPosition.Almost(4), nearHalf = false), Hints.forRead(225, 285))
        assertEquals(Hint.ReadHour(HourPosition.Between(3, 4), nearHalf = true), Hints.forRead(201, 261))
    }

    @Test
    fun `read hint points to the minute hand when minutes differ`() {
        assertEquals(Hint.ReadMinute(MinutePosition.QuarterPast, alsoHour = false), Hints.forRead(195, 225))
        assertEquals(Hint.ReadMinute(MinutePosition.QuarterPast, alsoHour = true), Hints.forRead(195, 285))
    }

    @Test
    fun `set hint asks for minutes first`() {
        assertEquals(Hint.SetMinute(MinutePosition.QuarterPast), Hints.forSet(target = 195, current = 180))
    }

    @Test
    fun `set hint asks for hour when minutes are right`() {
        assertEquals(Hint.SetHour(HourPosition.JustPassed(3), turnTip = true), Hints.forSet(195, 255))
        assertEquals(Hint.SetHour(HourPosition.Exact(3), turnTip = false), Hints.forSet(180, 240))
    }
}
