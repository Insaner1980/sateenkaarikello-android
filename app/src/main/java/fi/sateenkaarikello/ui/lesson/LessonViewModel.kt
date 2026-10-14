package fi.sateenkaarikello.ui.lesson

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.sateenkaarikello.AppServices
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Choice
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.DemoStep
import fi.sateenkaarikello.domain.Hand
import fi.sateenkaarikello.domain.Lesson
import fi.sateenkaarikello.domain.Mark
import fi.sateenkaarikello.domain.Text
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.components.OptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Mitä Seuraava-nappi tekee ja mikä sen teksti on. */
enum class NextAction { NEXT_PAGE, TRY_IT, NEXT_LESSON, START_GAME, FINISH }

data class LessonUiState(
    val lesson: Lesson,
    val queueIndex: Int = 0,
    val page: Int = 0,
    val clock: ClockCommand = ClockCommand(0),
    val period: Int = ClockTime.HALF_DAY,
    val help: Boolean = false,
    val highlight: Hand? = null,
    val under: List<Mark> = emptyList(),
    val over: List<Mark> = emptyList(),
    val caption: Text? = null,
    val choices: List<Choice> = emptyList(),
    val choiceStates: Map<Int, OptionState> = emptyMap(),
    val showHint: Boolean = false,
    val quizDone: Boolean = false,
    val praiseIndex: Int = 0,
    val celebrate: Int = 0,
    val nextAction: NextAction = NextAction.NEXT_PAGE,
    val finished: Boolean = false,
) {
    val isQuiz: Boolean get() = page >= lesson.pages.size
    val pageCount: Int get() = lesson.pages.size + 1
}

/**
 * Oppituntijono: sivut animaatioineen ja lopussa Kokeile itse -kysymys. Animaatio ajetaan
 * korutiinina, joka perutaan sivun vaihtuessa ja näkymästä poistuttaessa (web: runDemo, demoToken).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class LessonViewModel(
    private val queue: List<String>,
    private val thenLevel: Int?,
    private val services: AppServices,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(pageState(0, 0, celebrate = 0))
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var demoJob: Job? = null

    init {
        startDemo()
    }

    private fun pageState(
        queueIndex: Int,
        page: Int,
        celebrate: Int,
    ): LessonUiState {
        val lesson = LessonCatalog.lesson(queue[queueIndex])
        val base = LessonUiState(lesson = lesson, queueIndex = queueIndex, page = page, celebrate = celebrate)
        return if (page < lesson.pages.size) {
            val p = lesson.pages[page]
            base.copy(
                clock = ClockCommand(p.time),
                period = p.period,
                help = p.help,
                under = p.under,
                nextAction = if (page == lesson.pages.size - 1) NextAction.TRY_IT else NextAction.NEXT_PAGE,
            )
        } else {
            val q = lesson.quiz
            base.copy(clock = ClockCommand(q.time), period = q.period, choices = q.choices.shuffled(random))
        }
    }

    private fun show(
        queueIndex: Int,
        page: Int,
    ) {
        demoJob?.cancel()
        _state.value = pageState(queueIndex, page, _state.value.celebrate)
        startDemo()
    }

    private fun startDemo() {
        val s = _state.value
        if (s.isQuiz) return
        val steps = s.lesson.pages[s.page].demo
        demoJob =
            viewModelScope.launch {
                for (step in steps) {
                    when (step) {
                        is DemoStep.Wait -> {
                            delay(step.ms)
                        }

                        is DemoStep.Move -> {
                            _state.update {
                                it.copy(
                                    clock = ClockCommand(step.to, step.ms, step.exact),
                                )
                            }
                        }

                        is DemoStep.Highlight -> {
                            _state.update { it.copy(highlight = step.hand) }
                        }

                        is DemoStep.Under -> {
                            _state.update { it.copy(under = step.marks) }
                        }

                        is DemoStep.Over -> {
                            _state.update { it.copy(over = step.marks) }
                        }

                        is DemoStep.Say -> {
                            _state.update { it.copy(caption = step.text) }
                        }
                    }
                }
            }
    }

    fun next() {
        val s = _state.value
        if (s.finished || (s.isQuiz && !s.quizDone)) return
        if (!s.isQuiz) {
            services.play(Sound.POP)
            show(s.queueIndex, s.page + 1)
            return
        }
        services.play(Sound.POP)
        if (s.queueIndex < queue.size - 1) {
            show(s.queueIndex + 1, 0)
        } else {
            _state.update { it.copy(finished = true) }
        }
    }

    fun previous() {
        val s = _state.value
        if (s.page == 0) return
        services.play(Sound.POP)
        show(s.queueIndex, s.page - 1)
    }

    fun replay() {
        val s = _state.value
        if (!s.isQuiz) show(s.queueIndex, s.page)
    }

    fun pick(index: Int) {
        val s = _state.value
        if (!s.isQuiz || s.quizDone || s.choiceStates[index] == OptionState.WRONG) return
        val choice = s.choices[index]
        if (!choice.correct) {
            services.play(Sound.BAD)
            _state.value = s.copy(choiceStates = s.choiceStates + (index to OptionState.WRONG), showHint = true)
            return
        }
        services.play(Sound.OK)
        val more = s.queueIndex < queue.size - 1
        _state.value =
            s.copy(
                choiceStates = s.choiceStates + (index to OptionState.CORRECT),
                quizDone = true,
                showHint = false,
                praiseIndex = random.nextInt(PRAISE_COUNT),
                celebrate = s.celebrate + 1,
                nextAction =
                    when {
                        more -> NextAction.NEXT_LESSON
                        thenLevel != null -> NextAction.START_GAME
                        else -> NextAction.FINISH
                    },
            )
        viewModelScope.launch { services.repository.updateProgress { it.withLessonDone(s.lesson.id) } }
    }

    private companion object {
        const val PRAISE_COUNT = 7
    }
}
