package fi.sateenkaarikello.ui.challenge

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.sateenkaarikello.AppServices
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Levels
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

enum class ChallengePhase { START, RUN, END }

data class ChallengeUiState(
    val phase: ChallengePhase = ChallengePhase.START,
    val score: Int = 0,
    val best: Int = 0,
    val newRecord: Boolean = false,
    val question: Int = 0,
    val options: List<Int> = emptyList(),
    val optionStates: Map<Int, OptionState> = emptyMap(),
    val busy: Boolean = false,
    val clock: ClockCommand = ClockCommand(START_TIME),
    /** Kasvaa jokaisella aloituksella: UI käynnistää ajastinpalkin alusta. */
    val runId: Int = 0,
) {
    companion object {
        /** Aloitusnäkymän kello: varttia vaille yksi (web: hClock.set(45)). */
        const val START_TIME = 45
    }
}

/**
 * Minuuttihaaste: niin monta Mitä kello on? -kysymystä kuin minuutissa ehtii.
 * Väärästä vastauksesta näytetään oikea, ja peli jatkuu (web: haasteStart, pickH, haasteEnd).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class ChallengeViewModel(
    private val h24: Boolean,
    private val services: AppServices,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeUiState())
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()

    private var timer: Job? = null
    private var pending: Job? = null

    init {
        viewModelScope.launch {
            val best =
                services.repository.progress
                    .first()
                    .challengeBest
            _state.update { it.copy(best = best) }
        }
    }

    fun start() {
        timer?.cancel()
        pending?.cancel()
        _state.update {
            it.copy(phase = ChallengePhase.RUN, score = 0, newRecord = false, runId = it.runId + 1)
        }
        nextQuestion()
        timer =
            viewModelScope.launch {
                delay(DURATION_MS)
                end()
            }
    }

    private fun nextQuestion() {
        val previous = _state.value.question.takeIf { _state.value.options.isNotEmpty() }
        val q = Levels.challengeQuestion(h24, previous, random)
        _state.update {
            it.copy(
                question = q.time,
                options = Levels.optionsFor(q.time, q.level, h24, random),
                optionStates = emptyMap(),
                busy = false,
                clock = ClockCommand(q.time, MOVE_MS),
            )
        }
    }

    fun pick(time: Int) {
        val s = _state.value
        if (s.phase != ChallengePhase.RUN || s.busy) return
        val correct = time == s.question
        services.play(if (correct) Sound.OK else Sound.BAD)
        _state.value =
            if (correct) {
                s.copy(busy = true, score = s.score + 1, optionStates = mapOf(time to OptionState.CORRECT))
            } else {
                s.copy(busy = true, optionStates = mapOf(time to OptionState.WRONG, s.question to OptionState.CORRECT))
            }
        pending =
            viewModelScope.launch {
                delay(if (correct) NEXT_AFTER_CORRECT_MS else NEXT_AFTER_WRONG_MS)
                if (_state.value.phase == ChallengePhase.RUN) nextQuestion()
            }
    }

    private suspend fun end() {
        pending?.cancel()
        val s = _state.value
        val record = s.score > s.best
        if (record) services.repository.updateProgress { it.withChallengeScore(s.score) }
        services.play(Sound.WIN)
        _state.update {
            it.copy(
                phase = ChallengePhase.END,
                best = if (record) s.score else s.best,
                newRecord = record && s.score > 0,
                busy = true,
            )
        }
    }

    companion object {
        const val DURATION_MS = 60_000L
        private const val MOVE_MS = 350
        private const val NEXT_AFTER_CORRECT_MS = 380L
        private const val NEXT_AFTER_WRONG_MS = 1300L
    }
}
