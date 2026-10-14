package fi.sateenkaarikello.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.sateenkaarikello.R
import fi.sateenkaarikello.Screen
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.FinnishTime
import fi.sateenkaarikello.domain.TaskType
import fi.sateenkaarikello.ui.AppViewModel
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.ButtonKind
import fi.sateenkaarikello.ui.components.ClockBox
import fi.sateenkaarikello.ui.components.ConfettiState
import fi.sateenkaarikello.ui.components.DigitalPill
import fi.sateenkaarikello.ui.components.FeedbackBox
import fi.sateenkaarikello.ui.components.FeedbackKind
import fi.sateenkaarikello.ui.components.HelpChip
import fi.sateenkaarikello.ui.components.OptionButton
import fi.sateenkaarikello.ui.components.OptionState
import fi.sateenkaarikello.ui.components.Panel
import fi.sateenkaarikello.ui.components.ProgressDots
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.components.TopBar
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.components.rememberCelebrationAnchors
import fi.sateenkaarikello.ui.digitalLabel
import fi.sateenkaarikello.ui.levelName
import fi.sateenkaarikello.ui.praise
import fi.sateenkaarikello.ui.resources
import fi.sateenkaarikello.ui.text
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType
import fi.sateenkaarikello.ui.timeLabel

@Composable
fun GameRoute(
    key: Screen.Game,
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    onFinish: (Screen.Result) -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: GameViewModel =
        viewModel(
            factory = viewModelFactory { initializer { GameViewModel(key.level, h24, app.services) } },
        ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(state.finished) { state.finished?.let(currentOnFinish) }
    GameScreen(
        state = state,
        help = help,
        onToggleHelp = app::toggleHelp,
        onBack = onBack,
        onPick = vm::pick,
        onClockChange = vm::onClockChange,
        onDone = vm::done,
        onNext = vm::next,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    help: Boolean,
    onToggleHelp: () -> Unit,
    onBack: () -> Unit,
    onPick: (Int) -> Unit,
    onClockChange: (Int) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Edistymispallojen keskipisteet: onnistumisen jälkeen kultainen tähti lentää nykyiseen palloon.
    val dots = remember { mutableStateMapOf<Int, Offset>() }
    val anchors =
        rememberCelebrationAnchors(state.celebrate, SUCCESS_CONFETTI, flyTo = { dots[state.taskIndex] }) {
            if (state.task.type == TaskType.READ) state.task.time else DONE_ANCHOR
        }
    ScreenScaffold(modifier) { clockSize ->
        TopBar(
            title = stringResource(levelName(state.level)),
            backLabel = stringResource(R.string.back_levels),
            onBack = onBack,
            trailing = { HelpChip(help, onToggleHelp) },
        )
        ProgressDots(
            state.taskCount,
            state.taskIndex,
            currentDone = state.feedback is GameFeedback.Praise,
            fillDelayMs = ConfettiState.FLIGHT_MS,
            onDotCenter = { i, c -> dots[i] = c },
        )
        ClockBox(clockSize) {
            ClockFace(
                command = state.clock,
                period = ClockTime.period(state.h24),
                step = state.step,
                interactive = state.interactive,
                help = help,
                onChange = onClockChange,
            )
        }
        Panel {
            if (state.task.type == TaskType.READ) {
                GameOptions(state, anchors, onPick)
            } else {
                SetTarget(state.task.time, state.h24)
            }
            if (state.showDone) {
                SkButton(
                    stringResource(R.string.done),
                    onDone,
                    kind = ButtonKind.PRIMARY,
                    modifier = Modifier.fillMaxWidth().confettiAnchor { anchors[DONE_ANCHOR] = it },
                )
            }
            GameFeedbackMessage(state.feedback, state.h24)
            if (state.showNext) {
                SkButton(
                    stringResource(R.string.next),
                    onNext,
                    kind = ButtonKind.GREEN,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Paljastuksessa aika pienellä alkukirjaimella keskellä lausetta, kuten webissä. */
private fun timeText(
    t: Int,
    h24: Boolean,
) = FinnishTime.fullWords(t, h24)

private const val DONE_ANCHOR = -1
private const val SUCCESS_CONFETTI = 45

@Composable
private fun GameFeedbackMessage(
    feedback: GameFeedback?,
    h24: Boolean,
) {
    val res = resources()
    feedback?.let { fb ->
        val (text, kind) =
            when (fb) {
                is GameFeedback.Praise -> {
                    val p = praise(res, fb.praiseIndex)
                    (
                        if (fb.time ==
                            null
                        ) {
                            p
                        } else {
                            res.getString(R.string.praise_with_time, p, timeLabel(fb.time, h24))
                        }
                    ) to
                        FeedbackKind.OK
                }

                is GameFeedback.HintText -> {
                    fb.hint.text(res) to FeedbackKind.HINT
                }

                is GameFeedback.RevealRead -> {
                    res.getString(R.string.reveal_read, timeText(fb.time, h24)) to FeedbackKind.HINT
                }

                is GameFeedback.RevealSet -> {
                    res.getString(R.string.reveal_set, timeText(fb.time, h24)) to FeedbackKind.HINT
                }
            }
        FeedbackBox(text, kind)
    }
}

@Composable
private fun ColumnScope.GameOptions(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
) {
    Text(stringResource(R.string.question_read), style = SkType.Question)
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
        for (t in state.options) {
            OptionButton(
                words = timeLabel(t, state.h24),
                digital = FinnishTime.digital(t, state.h24),
                state = state.optionStates[t] ?: OptionState.NORMAL,
                enabled = !state.busy && state.optionStates[t] != OptionState.WRONG,
                onClick = { onPick(t) },
                modifier = Modifier.confettiAnchor { anchors[t] = it },
            )
        }
    }
}

/** Laita kello -tehtävän tavoite: aika sanoina ja numeroina. */
@Composable
private fun SetTarget(
    time: Int,
    h24: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        Text(stringResource(R.string.question_set), style = SkType.Question)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            Text(timeLabel(time, h24), style = SkType.Words)
            DigitalPill(digitalLabel(resources(), time, h24))
        }
    }
}
