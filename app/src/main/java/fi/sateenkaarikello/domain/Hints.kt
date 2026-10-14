package fi.sateenkaarikello.domain

/** Missä tuntiviisari on (web: hourPos). */
sealed interface HourPosition {
    data class Exact(
        val number: Int,
    ) : HourPosition

    data class JustPassed(
        val number: Int,
    ) : HourPosition

    data class Between(
        val number: Int,
        val next: Int,
    ) : HourPosition

    data class Almost(
        val next: Int,
    ) : HourPosition
}

/** Mihin minuuttiviisarin pitää osoittaa (web: minutePos). */
sealed interface MinutePosition {
    data object Top : MinutePosition

    data object Bottom : MinutePosition

    data object QuarterPast : MinutePosition

    data object QuarterTo : MinutePosition

    data class Number(
        val number: Int,
        val minutes: Int,
    ) : MinutePosition

    data class Ticks(
        val minutes: Int,
    ) : MinutePosition
}

/** Lempeä vinkki ensimmäisen virheen jälkeen. Tekstit ovat strings.xml:ssä (ui/HintText.kt). */
sealed interface Hint {
    data class HandsRight(
        val dayPart: DayPart,
    ) : Hint

    data class ReadHour(
        val position: HourPosition,
        val nearHalf: Boolean,
    ) : Hint

    data class ReadMinute(
        val position: MinutePosition,
        val alsoHour: Boolean,
    ) : Hint

    data class SetMinute(
        val position: MinutePosition,
    ) : Hint

    data class SetHour(
        val position: HourPosition,
        val turnTip: Boolean,
    ) : Hint
}

object Hints {
    fun hourPosition(t: Int): HourPosition {
        val v = ClockTime.mod(t, ClockTime.HALF_DAY)
        val h = v / ClockTime.MINUTES_PER_HOUR
        val m = v % ClockTime.MINUTES_PER_HOUR
        val hn = if (h == 0) ClockTime.HOURS_PER_HALF_DAY else h
        val next = hn % ClockTime.HOURS_PER_HALF_DAY + 1
        return when {
            m == 0 -> HourPosition.Exact(hn)
            m <= JUST_PASSED_THROUGH -> HourPosition.JustPassed(hn)
            m < ALMOST_FROM -> HourPosition.Between(hn, next)
            else -> HourPosition.Almost(next)
        }
    }

    fun minutePosition(m: Int): MinutePosition =
        when {
            m == 0 -> MinutePosition.Top
            m == ClockTime.MINUTES_PER_HALF_HOUR -> MinutePosition.Bottom
            m == QUARTER_PAST -> MinutePosition.QuarterPast
            m == QUARTER_TO -> MinutePosition.QuarterTo
            m % ClockTime.MINUTES_PER_NUMBER == 0 -> MinutePosition.Number(m / ClockTime.MINUTES_PER_NUMBER, m)
            else -> MinutePosition.Ticks(m)
        }

    /** Mitä kello on? -tehtävän vinkki, kun valittiin [guess] eikä [target]. */
    fun forRead(
        target: Int,
        guess: Int,
    ): Hint {
        if (ClockTime.sameHands(target, guess)) return Hint.HandsRight(FinnishTime.dayPart(target))
        val tm = target % ClockTime.MINUTES_PER_HOUR
        val gm = guess % ClockTime.MINUTES_PER_HOUR
        return if (tm == gm) {
            Hint.ReadHour(hourPosition(target), nearHalf = tm in 21..39)
        } else {
            val hourDiffers =
                ClockTime.mod(target, ClockTime.HALF_DAY) / ClockTime.MINUTES_PER_HOUR !=
                    ClockTime.mod(guess, ClockTime.HALF_DAY) / ClockTime.MINUTES_PER_HOUR
            Hint.ReadMinute(minutePosition(tm), alsoHour = hourDiffers)
        }
    }

    /** Laita kello -tehtävän vinkki, kun kello näyttää [current] eikä [target]. */
    fun forSet(
        target: Int,
        current: Int,
    ): Hint {
        val tm = target % ClockTime.MINUTES_PER_HOUR
        val cm = current % ClockTime.MINUTES_PER_HOUR
        return if (tm != cm) {
            Hint.SetMinute(minutePosition(tm))
        } else {
            Hint.SetHour(hourPosition(target), turnTip = tm != 0)
        }
    }
}

private const val JUST_PASSED_THROUGH = 20
private const val ALMOST_FROM = 40
private const val QUARTER_PAST = 15
private const val QUARTER_TO = 45
