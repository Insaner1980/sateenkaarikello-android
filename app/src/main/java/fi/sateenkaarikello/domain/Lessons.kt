package fi.sateenkaarikello.domain

/**
 * Käyttäjälle näkyvä teksti ilman Android-riippuvuutta. [Res] ja [Plural] viittaavat
 * strings.xml-resurssiin (id on R-luokan kokonaisluku), [Time] on kellonaika sanoina isolla
 * alkukirjaimella ja [Plain] valmis merkkijono (esim. luku).
 */
sealed interface Text {
    data class Res(
        val id: Int,
        val args: List<Any> = emptyList(),
    ) : Text

    data class Plural(
        val id: Int,
        val count: Int,
    ) : Text

    data class Time(
        val minutes: Int,
    ) : Text

    data class Plain(
        val value: String,
    ) : Text
}

/** Opetuksen merkintöjen värit; UI muuttaa ne teeman väreiksi. */
enum class MarkColor { YLI, VAILLE, HALF, RED, GREEN, BLUE }

/** Merkinnät kellon päällä (web: ovSector, ovNum, ovTick, ovBubble). */
sealed interface Mark {
    /** Sektori minuutista [fromMin] minuuttiin [toMin] (0–60). */
    data class Sector(
        val fromMin: Int,
        val toMin: Int,
        val color: MarkColor,
    ) : Mark

    data class NumberGlow(
        val n: Int,
    ) : Mark

    data class TickGlow(
        val minute: Int,
    ) : Mark

    data class Bubble(
        val minute: Int,
        val text: String,
    ) : Mark
}

/**
 * Oppitunnin animaation askeleet. [Move] käynnistää viisarien liikkeen eikä jää odottamaan;
 * [Wait] odottaa. [Under] ja [Over] korvaavat koko kerroksen merkinnät. [Say] null tyhjentää kuvatekstin.
 */
sealed interface DemoStep {
    data class Wait(
        val ms: Long,
    ) : DemoStep

    data class Move(
        val to: Int,
        val ms: Int,
        val exact: Boolean = false,
    ) : DemoStep

    data class Highlight(
        val hand: Hand?,
    ) : DemoStep

    data class Under(
        val marks: List<Mark>,
    ) : DemoStep

    data class Over(
        val marks: List<Mark>,
    ) : DemoStep

    data class Say(
        val text: Text?,
    ) : DemoStep
}

class DemoBuilder {
    private val steps = mutableListOf<DemoStep>()

    fun pause(ms: Long) {
        steps += DemoStep.Wait(ms)
    }

    fun move(
        to: Int,
        ms: Int,
        exact: Boolean = false,
    ) {
        steps += DemoStep.Move(to, ms, exact)
    }

    fun highlight(hand: Hand?) {
        steps += DemoStep.Highlight(hand)
    }

    fun under(vararg marks: Mark) {
        steps += DemoStep.Under(marks.toList())
    }

    fun under(marks: List<Mark>) {
        steps += DemoStep.Under(marks.toList())
    }

    fun over(vararg marks: Mark) {
        steps += DemoStep.Over(marks.toList())
    }

    fun over(marks: List<Mark>) {
        steps += DemoStep.Over(marks.toList())
    }

    fun say(text: Text?) {
        steps += DemoStep.Say(text)
    }

    internal fun build(): List<DemoStep> = steps.toList()
}

/** Pieni DSL oppituntien animaatioille, esim. `demo { pause(700); highlight(Hand.HOUR) }`. */
fun demo(block: DemoBuilder.() -> Unit): List<DemoStep> = DemoBuilder().apply(block).build()

/** Oppitunnin sivu: alkuaika, jakso, apu-tila, tekstikappaleet, alkumerkinnät ja animaatio. */
data class LessonPage(
    val time: Int,
    val text: List<Text>,
    val demo: List<DemoStep>,
    val period: Int = ClockTime.HALF_DAY,
    val help: Boolean = false,
    val under: List<Mark> = emptyList(),
)

data class Choice(
    val text: Text,
    val correct: Boolean,
)

data class Quiz(
    val time: Int,
    val question: Text,
    val choices: List<Choice>,
    val hint: Text,
    val period: Int = ClockTime.HALF_DAY,
)

data class Lesson(
    val id: String,
    val title: Text,
    val description: Text,
    val pages: List<LessonPage>,
    val quiz: Quiz,
    val h24Only: Boolean = false,
)

/** Mitkä oppitunnit näytetään ennen tasoa, ja mitkä tasot merkitään näytetyiksi. */
data class IntroPlan(
    val queue: List<String>,
    val introShown: Set<String>,
)

object Lessons {
    const val DAY_LESSON = "vrk"
    private const val H24_INTRO = "h24"

    /** Tasojen oppitunnit (web: LEVEL_LESSONS). */
    val levelLessons: List<List<String>> =
        listOf(
            listOf("viisarit", "kierros", "tasan"),
            listOf("puoli"),
            listOf("vartti", "ylivaille"),
            listOf("viisi"),
            listOf("minuutit"),
        )

    /**
     * Tason ensimmäisellä pelikerralla näytetään ensin sen osaamattomat oppitunnit, ja
     * 24 h -tilassa kerran myös vuorokausi-oppitunti (web: playLevel).
     */
    fun introPlan(
        level: Int,
        h24: Boolean,
        progress: Progress,
    ): IntroPlan {
        val key = level.toString()
        val shown = progress.introShown
        val queue = mutableListOf<String>()
        if (key !in shown) queue += levelLessons[level].filter { it !in progress.lessonsDone }
        if (h24 && H24_INTRO !in shown && DAY_LESSON !in progress.lessonsDone) queue += DAY_LESSON
        val newShown = shown + key + (if (h24) setOf(H24_INTRO) else emptySet())
        return IntroPlan(queue, newShown)
    }
}
