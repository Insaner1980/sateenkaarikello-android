package fi.sateenkaarikello.domain

/** Aloitusnäkymän tervehdys; tekstit ovat strings.xml:ssä. */
enum class Greeting { MORNING, DAY, EVENING, NIGHT }

/** Vuorokaudenaika: nimi ("iltapäivä"), adverbi ("iltapäivällä") ja näytetäänkö aurinko. */
data class DayPart(
    val name: String,
    val adverb: String,
    val isSun: Boolean,
)

/**
 * Kellonajat suomeksi puhekielen tapaan. Suora käännös web-version funktioista
 * timeWords, digital, dayPart ja spoken24. Aika on minuutteja keskiyöstä tai kello 12:sta.
 */
object FinnishTime {
    private val NUM =
        listOf(
            "",
            "yksi",
            "kaksi",
            "kolme",
            "neljä",
            "viisi",
            "kuusi",
            "seitsemän",
            "kahdeksan",
            "yhdeksän",
            "kymmenen",
            "yksitoista",
            "kaksitoista",
            "kolmetoista",
            "neljätoista",
            "viisitoista",
            "kuusitoista",
            "seitsemäntoista",
            "kahdeksantoista",
            "yhdeksäntoista",
            "kaksikymmentä",
        )
    private val PART = mapOf(5 to "viittä", 10 to "kymmentä", 15 to "varttia", 20 to "kahtakymmentä")
    private val TENS = listOf("", "", "kaksikymmentä", "kolmekymmentä", "neljäkymmentä", "viisikymmentä")

    private fun minutesPhrase(n: Int): String = PART[n] ?: if (n == 1) "yksi minuutti" else "${NUM[n]} minuuttia"

    /** Kellonaika sanoina 12 tunnin kellossa, esim. "viittä vaille puoli neljä". */
    fun timeWords(total: Int): String {
        val t = ClockTime.mod(total, ClockTime.HALF_DAY)
        val h = t / ClockTime.MINUTES_PER_HOUR
        val m = t % ClockTime.MINUTES_PER_HOUR
        val hn = if (h == 0) ClockTime.HOURS_PER_HALF_DAY else h
        val hour = NUM[hn]
        val next = NUM[hn % ClockTime.HOURS_PER_HALF_DAY + 1]
        return when {
            m == 0 -> "tasan $hour"

            m == ClockTime.MINUTES_PER_HALF_HOUR -> "puoli $next"

            m <= HOUR_RELATIVE_LIMIT -> "${minutesPhrase(m)} yli $hour"

            m < ClockTime.MINUTES_PER_HALF_HOUR -> "${minutesPhrase(
                ClockTime.MINUTES_PER_HALF_HOUR - m,
            )} vaille puoli $next"

            m < NEXT_HOUR_FROM -> "${minutesPhrase(m - ClockTime.MINUTES_PER_HALF_HOUR)} yli puoli $next"

            else -> "${minutesPhrase(ClockTime.MINUTES_PER_HOUR - m)} vaille $next"
        }
    }

    /** Numeroaika: "3.30" tai 24 h -tilassa "15.30". */
    fun digital(
        total: Int,
        h24: Boolean = false,
    ): String {
        val t = ClockTime.mod(total, ClockTime.DAY)
        val m = t % ClockTime.MINUTES_PER_HOUR
        val h =
            if (h24) {
                t / ClockTime.MINUTES_PER_HOUR
            } else {
                (
                    t / ClockTime.MINUTES_PER_HOUR %
                        ClockTime.HOURS_PER_HALF_DAY
                ).let { if (it == 0) ClockTime.HOURS_PER_HALF_DAY else it }
            }
        return "$h.${m.toString().padStart(2, '0')}"
    }

    fun dayPart(total: Int): DayPart {
        val t = ClockTime.mod(total, ClockTime.DAY)
        val h = t / ClockTime.MINUTES_PER_HOUR
        val sun = h in SUNRISE_HOUR..LAST_SUN_HOUR
        return when {
            t == 0 -> DayPart("keskiyö", "keskiyöllä", sun)
            t == ClockTime.HALF_DAY -> DayPart("keskipäivä", "keskipäivällä", sun)
            h < MORNING_FROM || h >= NIGHT_FROM -> DayPart("yö", "yöllä", sun)
            h < LATE_MORNING_FROM -> DayPart("aamu", "aamulla", sun)
            h < ClockTime.HOURS_PER_HALF_DAY -> DayPart("aamupäivä", "aamupäivällä", sun)
            h < EVENING_FROM -> DayPart("iltapäivä", "iltapäivällä", sun)
            else -> DayPart("ilta", "illalla", sun)
        }
    }

    private fun numWord(n: Int): String =
        when {
            n == 0 -> "nolla"
            n <= LAST_SIMPLE_NUMBER -> NUM[n]
            else -> TENS[n / DECIMAL_BASE] + (if (n % DECIMAL_BASE != 0) NUM[n % DECIMAL_BASE] else "")
        }

    /** Numeroaika ääneen, esim. 15.30 = "viisitoista kolmekymmentä". */
    fun spoken24(total: Int): String {
        val t = ClockTime.mod(total, ClockTime.DAY)
        val h = t / ClockTime.MINUTES_PER_HOUR
        val m = t % ClockTime.MINUTES_PER_HOUR
        val minutes =
            when {
                m == 0 -> "nolla nolla"
                m < DECIMAL_BASE -> "nolla ${numWord(m)}"
                else -> numWord(m)
            }
        return "${numWord(h)} $minutes"
    }

    /** Tervehdys vuorokaudenajan mukaan (samat rajat kuin [dayPart]). */
    fun greeting(total: Int): Greeting {
        val h = ClockTime.mod(total, ClockTime.DAY) / ClockTime.MINUTES_PER_HOUR
        return when {
            h < MORNING_FROM || h >= NIGHT_FROM -> Greeting.NIGHT
            h < LATE_MORNING_FROM -> Greeting.MORNING
            h < EVENING_FROM -> Greeting.DAY
            else -> Greeting.EVENING
        }
    }

    /** Sanat ja 24 h -tilassa vuorokaudenaika, esim. "puoli neljä iltapäivällä". */
    fun fullWords(
        total: Int,
        h24: Boolean,
    ): String = if (h24) "${timeWords(total)} ${dayPart(total).adverb}" else timeWords(total)
}

fun String.capitalizeFirst(): String = replaceFirstChar { it.uppercaseChar() }

private const val HOUR_RELATIVE_LIMIT = 20
private const val NEXT_HOUR_FROM = 40
private const val SUNRISE_HOUR = 6
private const val LAST_SUN_HOUR = 17
private const val MORNING_FROM = 5
private const val NIGHT_FROM = 22
private const val LATE_MORNING_FROM = 10
private const val EVENING_FROM = 18
private const val LAST_SIMPLE_NUMBER = 20
private const val DECIMAL_BASE = 10
