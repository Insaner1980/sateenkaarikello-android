package fi.sateenkaarikello.domain

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pelin taso. [step] on tarkkuus minuutteina, [sample] tasokortin esimerkkiaika ja
 * [minutes] arpoo minuutit tason painotuksella. Nimi ja väri ovat UI-kerroksessa indeksin mukaan.
 */
class Level(
    val index: Int,
    val step: Int,
    val sample: Int,
    val minutes: (Random) -> Int,
)

enum class TaskType { READ, SET }

data class Task(
    val time: Int,
    val type: TaskType,
)

class ChallengeQuestion(
    val time: Int,
    val level: Level,
)

/** Tasot, tehtävien arvonta ja pisteytys (web: LEVELS, makeTasks, optionsFor, finish). */
object Levels {
    const val TASK_COUNT = 10
    const val OPTION_COUNT = 3

    /** Tähtirajat: enintään 1 virhe = 3 tähteä, enintään 3 virhettä = 2 tähteä. */
    private const val THREE_STAR_MISTAKES = 1
    private const val TWO_STAR_MISTAKES = 3

    /** Minuuttihaaste arpoo kysymykset neljästä ensimmäisestä tasosta. */
    private const val CHALLENGE_LEVELS = 4

    /** 24 h -tilassa sama viisariasento toiseen vuorokaudenaikaan näytetään 70 %:ssa kysymyksistä. */
    private const val MIRROR_CHANCE = 0.7

    val all: List<Level> =
        listOf(
            Level(0, step = 60, sample = 180) { 0 },
            Level(1, step = 30, sample = 150) { r -> if (r.nextDouble() < 0.7) 30 else 0 },
            Level(2, step = 15, sample = 555) { r ->
                if (r.nextDouble() < 0.65) listOf(15, 45).random(r) else listOf(0, 30).random(r)
            },
            Level(3, step = 5, sample = 260) { r ->
                if (r.nextDouble() < 0.75) {
                    listOf(5, 10, 20, 25, 35, 40, 50, 55).random(r)
                } else {
                    listOf(0, 15, 30, 45).random(r)
                }
            },
            Level(4, step = 1, sample = 463) { r ->
                if (r.nextDouble() < 0.75) {
                    generateSequence { r.nextInt(60) }.first { it % 5 != 0 }
                } else {
                    r.nextInt(12) * 5
                }
            },
        )

    fun randomTime(
        level: Level,
        h24: Boolean,
        random: Random,
    ): Int =
        random.nextInt(
            if (h24) ClockTime.HOURS_PER_DAY else ClockTime.HOURS_PER_HALF_DAY,
        ) * ClockTime.MINUTES_PER_HOUR +
            level.minutes(random)

    /** 10 eri aikaa; tehtävät vuorottelevat Mitä kello on? ja Laita kello. */
    fun makeTasks(
        level: Level,
        h24: Boolean,
        random: Random,
    ): List<Task> {
        val out = mutableListOf<Task>()
        val used = HashSet<Int>()
        var guard = 0
        while (out.size < TASK_COUNT && guard++ < TASK_DRAW_LIMIT) {
            val t = randomTime(level, h24, random)
            if (!used.add(t)) continue
            out += Task(t, if (out.size % 2 == 1) TaskType.SET else TaskType.READ)
        }
        return out
    }

    /**
     * Kolme vaihtoehtoa: oikea ja tyypilliset sekaannukset (tunti väärin, yli/vaille, viisarit
     * väärin päin, 24 h -tilassa sama viisariasento toiseen vuorokaudenaikaan).
     */
    fun optionsFor(
        t: Int,
        level: Level,
        h24: Boolean,
        random: Random,
    ): List<Int> {
        val p = ClockTime.period(h24)
        val set = mutableListOf(t)

        fun add(x: Int) {
            val v = ClockTime.mod(x, p)
            if (v !in set && ClockTime.mod(v, ClockTime.MINUTES_PER_HOUR) % level.step == 0) set += v
        }
        val h = t / ClockTime.MINUTES_PER_HOUR
        val m = t % ClockTime.MINUTES_PER_HOUR
        val hn = (h % ClockTime.HOURS_PER_HALF_DAY).let { if (it == 0) ClockTime.HOURS_PER_HALF_DAY else it }
        val half = if (t >= ClockTime.HALF_DAY) ClockTime.HALF_DAY else 0
        if (p == ClockTime.DAY && random.nextDouble() < MIRROR_CHANCE) add(t + ClockTime.HALF_DAY)
        val near = listOf(t + ClockTime.MINUTES_PER_HOUR, t - ClockTime.MINUTES_PER_HOUR).shuffled(random)
        add(near[0])
        val mirrored = h * ClockTime.MINUTES_PER_HOUR + (ClockTime.MINUTES_PER_HOUR - m) % ClockTime.MINUTES_PER_HOUR
        val swapped =
            half +
                ((m / ClockTime.MINUTES_PER_NUMBER.toDouble()).roundToInt() % ClockTime.HOURS_PER_HALF_DAY) *
                ClockTime.MINUTES_PER_HOUR +
                (hn * ClockTime.MINUTES_PER_NUMBER) % ClockTime.MINUTES_PER_HOUR
        for (c in listOf(mirrored, swapped, near[1]).shuffled(random)) {
            if (set.size >= OPTION_COUNT) break
            add(c)
        }
        var guard = 0
        while (set.size < OPTION_COUNT && guard++ < OPTION_DRAW_LIMIT) add(randomTime(level, h24, random))
        return set.shuffled(random)
    }

    fun stars(mistakes: Int): Int =
        when {
            mistakes <= THREE_STAR_MISTAKES -> MAX_LEVEL_STARS
            mistakes <= TWO_STAR_MISTAKES -> 2
            else -> 1
        }

    /**
     * Laita kello -tehtävän alkuaika: samasta vuorokauden puoliskosta kuin tavoite (jotta
     * aurinko tai kuu täsmää), ja tasatunnin tavoitteella puoli kierrosta sivuun.
     */
    fun setTaskStart(target: Int): Int {
        val half = if (target >= ClockTime.HALF_DAY) ClockTime.HALF_DAY else 0
        return half + if (ClockTime.mod(target, ClockTime.HALF_DAY) == 0) ClockTime.HALF_DAY / 2 else 0
    }

    /** Arvo aika -nappi: viiden minuutin aika, joka eroaa nykyisestä. */
    fun randomClockTime(
        period: Int,
        current: Int,
        random: Random,
    ): Int =
        generateSequence {
            random.nextInt(
                period / ClockTime.MINUTES_PER_NUMBER,
            ) * ClockTime.MINUTES_PER_NUMBER
        }.first {
            it !=
                current
        }

    fun challengeQuestion(
        h24: Boolean,
        previous: Int?,
        random: Random,
    ): ChallengeQuestion {
        val level = all[random.nextInt(CHALLENGE_LEVELS)]
        val t = generateSequence { randomTime(level, h24, random) }.first { it != previous }
        return ChallengeQuestion(t, level)
    }
}

private const val TASK_DRAW_LIMIT = 500
private const val OPTION_DRAW_LIMIT = 300
private const val MAX_LEVEL_STARS = 3
