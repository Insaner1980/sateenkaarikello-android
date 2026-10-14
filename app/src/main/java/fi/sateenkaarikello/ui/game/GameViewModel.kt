package fi.sateenkaarikello.ui.game

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.sateenkaarikello.AppServices
import fi.sateenkaarikello.Screen
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Hint
import fi.sateenkaarikello.domain.Hints
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.domain.Task
import fi.sateenkaarikello.domain.TaskType
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.components.OptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Palaute paneelin alla. Tekstit muodostetaan UI:ssa strings.xml:stä. */
@Immutable
sealed interface GameFeedback {
    /** Kehu; Mitä kello on? -tehtävässä mukana oikea aika ([time]). */
    data class Praise(
        val praiseIndex: Int,
        val time: Int?,
    ) : GameFeedback

    data class HintText(
        val hint: Hint,
    ) : GameFeedback

    data class RevealRead(
        val time: Int,
    ) : GameFeedback

    data class RevealSet(
        val time: Int,
    ) : GameFeedback
}

data class GameUiState(
    val level: Int,
    val h24: Boolean,
    val taskIndex: Int,
    val taskCount: Int,
    val task: Task,
    val options: List<Int> = emptyList(),
    val optionStates: Map<Int, OptionState> = emptyMap(),
    val clock: ClockCommand,
    val step: Int,
    val interactive: Boolean = false,
    val showDone: Boolean = false,
    val showNext: Boolean = false,
    val busy: Boolean = false,
    val mistakes: Int = 0,
    val taskMistakes: Int = 0,
    val feedback: GameFeedback? = null,
    /** Kasvaa jokaisella onnistumisella: UI ampuu konfetit. */
    val celebrate: Int = 0,
    val finished: Screen.Result? = null,
)

/**
 * Yksi pelikerta: 10 tehtävää, vinkki ensimmäisen virheen jälkeen ja vastaus toisen jälkeen
 * (web: startLevel, renderTask, pickOption, doneBtn, finish).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class GameViewModel(
    private val level: Int,
    private val h24: Boolean,
    private val services: AppServices,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val lv = Levels.all[level]
    private val tasks = Levels.makeTasks(lv, h24, random)
    private var clockValue = 0
    private var advanceJob: Job? = null

    private val _state = MutableStateFlow(taskState(0, mistakes = 0, celebrate = 0, from = null))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private fun taskState(
        index: Int,
        mistakes: Int,
        celebrate: Int,
        from: GameUiState?,
    ): GameUiState {
        val task = tasks[index]
        val base =
            GameUiState(
                level = level,
                h24 = h24,
                taskIndex = index,
                taskCount = tasks.size,
                task = task,
                clock = ClockCommand(task.time, READ_MOVE_MS),
                step = lv.step,
                mistakes = mistakes,
                celebrate = celebrate,
            )
        return if (task.type == TaskType.READ) {
            base.copy(options = Levels.optionsFor(task.time, lv, h24, random))
        } else {
            val start = Levels.setTaskStart(task.time)
            clockValue = start
            base.copy(
                clock =
                    ClockCommand(
                        start,
                        if (from ==
                            null
                        ) {
                            0
                        } else {
                            SET_MOVE_MS
                        },
                    ),
                interactive = true,
                showDone = true,
            )
        }
    }

    fun onClockChange(value: Int) {
        clockValue = value
    }

    fun pick(time: Int) {
        val s = _state.value
        if (s.busy || s.task.type != TaskType.READ || s.optionStates[time] == OptionState.WRONG) return
        if (time == s.task.time) {
            success(s.copy(optionStates = s.optionStates + (time to OptionState.CORRECT)))
            return
        }
        services.play(Sound.BAD)
        val wrong =
            s.copy(
                mistakes = s.mistakes + 1,
                taskMistakes = s.taskMistakes + 1,
                optionStates = s.optionStates + (time to OptionState.WRONG),
            )
        _state.value =
            if (wrong.taskMistakes >= MAX_TRIES) {
                wrong.copy(
                    busy = true,
                    optionStates = wrong.optionStates + (s.task.time to OptionState.CORRECT),
                    feedback = GameFeedback.RevealRead(s.task.time),
                    showNext = true,
                )
            } else {
                wrong.copy(feedback = GameFeedback.HintText(Hints.forRead(s.task.time, time)))
            }
    }

    fun done() {
        val s = _state.value
        if (s.busy || s.task.type != TaskType.SET) return
        val v = clockValue
        // Viisariasento ratkaisee: 3.30 ja 15.30 näyttävät kellotaululla samalta.
        if (ClockTime.sameHands(v, s.task.time)) {
            success(s)
            return
        }
        services.play(Sound.BAD)
        val wrong = s.copy(mistakes = s.mistakes + 1, taskMistakes = s.taskMistakes + 1)
        _state.value =
            if (wrong.taskMistakes >= MAX_TRIES) {
                wrong.copy(
                    busy = true,
                    interactive = false,
                    clock = ClockCommand(s.task.time, REVEAL_MS),
                    feedback = GameFeedback.RevealSet(s.task.time),
                    showDone = false,
                    showNext = true,
                )
            } else {
                wrong.copy(feedback = GameFeedback.HintText(Hints.forSet(s.task.time, v)))
            }
    }

    private fun success(s: GameUiState) {
        services.play(Sound.OK)
        val isSet = s.task.type == TaskType.SET
        _state.value =
            s.copy(
                busy = true,
                feedback = GameFeedback.Praise(random.nextInt(PRAISE_COUNT), if (isSet) null else s.task.time),
                showDone = false,
                interactive = false,
                clock = if (isSet) ClockCommand(s.task.time, SUCCESS_MOVE_MS) else s.clock,
                celebrate = s.celebrate + 1,
            )
        advanceJob =
            viewModelScope.launch {
                delay(SUCCESS_PAUSE_MS)
                advance()
            }
    }

    fun next() {
        if (!_state.value.showNext) return
        services.play(Sound.POP)
        advance()
    }

    private fun advance() {
        advanceJob?.cancel()
        val s = _state.value
        if (s.finished != null) return
        val nextIndex = s.taskIndex + 1
        if (nextIndex >= tasks.size) {
            finish(s)
        } else {
            _state.value = taskState(nextIndex, s.mistakes, s.celebrate, s)
        }
    }

    private fun finish(s: GameUiState) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val stars = Levels.stars(s.mistakes)
            val before = services.repository.progress.first()
            services.repository.updateProgress { it.withLevelResult(level, stars) }
            services.play(Sound.WIN)
            _state.update {
                it.copy(
                    finished =
                        Screen.Result(
                            level = level,
                            stars = stars,
                            previousBest = before.stars[level],
                            starsBefore = before.totalStars,
                        ),
                )
            }
        }
    }

    private companion object {
        const val MAX_TRIES = 2
        const val PRAISE_COUNT = 7
        const val READ_MOVE_MS = 700
        const val SET_MOVE_MS = 500
        const val SUCCESS_MOVE_MS = 300
        const val REVEAL_MS = 1400
        const val SUCCESS_PAUSE_MS = 1500L
    }
}
