package fi.sateenkaarikello.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.sateenkaarikello.R
import fi.sateenkaarikello.Screen
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.ui.AppViewModel
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.ButtonKind
import fi.sateenkaarikello.ui.components.ClockBox
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.FeedbackBox
import fi.sateenkaarikello.ui.components.FeedbackKind
import fi.sateenkaarikello.ui.components.OptionButton
import fi.sateenkaarikello.ui.components.OptionState
import fi.sateenkaarikello.ui.components.Panel
import fi.sateenkaarikello.ui.components.Pill
import fi.sateenkaarikello.ui.components.PillKind
import fi.sateenkaarikello.ui.components.ProgressDots
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.components.TopBar
import fi.sateenkaarikello.ui.components.cardShadow
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.components.rememberCelebrationAnchors
import fi.sateenkaarikello.ui.praise
import fi.sateenkaarikello.ui.resolve
import fi.sateenkaarikello.ui.resources
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType

/** Kellokoulu: oppituntien lista. 24 h -oppitunti näkyy vain 24 h -tilassa. */
@Composable
fun LessonListScreen(
    h24: Boolean,
    done: Set<String>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.koulu_title), stringResource(R.string.back), onBack)
        Text(stringResource(R.string.koulu_intro), style = SkType.Intro)
        LessonCatalog.all.filter { !it.h24Only || h24 }.forEachIndexed { i, lesson ->
            LessonCard(i, lesson.title.resolve(), lesson.description.resolve(), lesson.id in done) { onOpen(lesson.id) }
        }
    }
}

@Composable
private fun LessonCard(
    index: Int,
    title: String,
    description: String,
    learned: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .cardShadow(depth = 5.dp, color = SkColors.Line, soft = false)
            .background(SkColors.Surface, SkShapes.Card)
            .border(SkDimens.Border, SkColors.Line, SkShapes.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(SkDimens.LessonNumber)
                .background(SkColors.Legend, CircleShape)
                .border(4.dp, SkColors.Rainbow7[index % SkColors.Rainbow7.size], CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text((index + 1).toString(), style = SkType.LessonNumber) }
        Column(Modifier.weight(1f)) {
            Text(title, style = SkType.LessonTitle)
            Text(description, style = SkType.Small.copy(color = SkColors.InkSoft))
        }
        if (learned) Pill(stringResource(R.string.lesson_done), kind = PillKind.OK)
    }
}

@Composable
fun LessonRoute(
    key: Screen.Lesson,
    speechAvailable: Boolean,
    onLeave: () -> Unit,
    onStartGame: (Int) -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: LessonViewModel =
        viewModel(
            factory = viewModelFactory { initializer { LessonViewModel(key.queue, key.thenLevel, app.services) } },
        ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val leave = {
        if (key.thenLevel != null) onStartGame(key.thenLevel) else onLeave()
    }
    LaunchedEffect(state.finished) { if (state.finished) leave() }
    LessonScreen(
        state = state,
        skipToGame = key.thenLevel != null,
        speechAvailable = speechAvailable,
        onBack = {
            app.play(Sound.POP)
            leave()
        },
        onNext = vm::next,
        onPrevious = vm::previous,
        onReplay = vm::replay,
        onPick = vm::pick,
        onSpeak = app::speak,
    )
}

@Composable
fun LessonScreen(
    state: LessonUiState,
    skipToGame: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onPick: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = resources()
    val anchors =
        rememberCelebrationAnchors(state.celebrate, QUIZ_CONFETTI) {
            state.choiceStates.entries
                .firstOrNull { it.value == OptionState.CORRECT }
                ?.key
        }
    val page = state.lesson.pages.getOrNull(state.page)
    val paragraphs = page?.text?.map { it.resolve(res) }.orEmpty()
    val caption = state.caption?.resolve(res).orEmpty()
    ScreenScaffold(modifier) { clockSize ->
        TopBar(
            title = state.lesson.title.resolve(),
            backLabel = stringResource(if (skipToGame) R.string.lesson_skip else R.string.back),
            onBack = onBack,
        )
        ProgressDots(state.pageCount, state.page)
        ClockBox(clockSize) {
            ClockFace(
                command = state.clock,
                period = state.period,
                help = state.help,
                highlight = state.highlight,
                under = state.under,
                over = state.over,
            )
        }
        Panel {
            if (page != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    paragraphs.forEach { Text(it, style = SkType.LessonText) }
                }
                if (caption.isNotEmpty()) {
                    LessonCaption(caption)
                }
            } else {
                LessonQuiz(state, anchors, onPick)
            }
            if (page != null) {
                LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SkButton(stringResource(R.string.lesson_prev), onPrevious, enabled = state.page > 0)
                if (!state.isQuiz || state.quizDone) {
                    SkButton(stringResource(nextLabel(state.nextAction)), onNext, kind = ButtonKind.PRIMARY)
                }
            }
        }
    }
}

private fun nextLabel(action: NextAction): Int =
    when (action) {
        NextAction.NEXT_PAGE -> R.string.next
        NextAction.TRY_IT -> R.string.lesson_try
        NextAction.NEXT_LESSON -> R.string.lesson_next_lesson
        NextAction.START_GAME -> R.string.lesson_start_game
        NextAction.FINISH -> R.string.done
    }

@Composable
private fun LessonCaption(caption: String) {
    Text(
        caption,
        style = SkType.Caption,
        modifier =
            Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(SkColors.Caption, SkShapes.Caption)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private const val QUIZ_CONFETTI = 60

@Composable
private fun ColumnScope.LessonQuiz(
    state: LessonUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
) {
    val res = resources()
    Eyebrow(stringResource(R.string.lesson_try))
    Text(
        state.lesson.quiz.question
            .resolve(),
        style = SkType.Question,
    )
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
        state.choices.forEachIndexed { i, choice ->
            OptionButton(
                words = choice.text.resolve(),
                state = state.choiceStates[i] ?: OptionState.NORMAL,
                enabled = !state.quizDone && state.choiceStates[i] != OptionState.WRONG,
                onClick = { onPick(i) },
                modifier = Modifier.confettiAnchor { anchors[i] = it },
            )
        }
    }
    if (state.quizDone) {
        FeedbackBox(
            stringResource(R.string.lesson_learned, praise(res, state.praiseIndex), state.lesson.title.resolve()),
            FeedbackKind.OK,
        )
    } else if (state.showHint) {
        FeedbackBox(
            state.lesson.quiz.hint
                .resolve(),
            FeedbackKind.HINT,
        )
    }
}

@Composable
private fun LessonPlayback(
    speechAvailable: Boolean,
    paragraphs: List<String>,
    caption: String,
    onSpeak: (String) -> Unit,
    onReplay: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        if (speechAvailable) {
            SkButton(
                stringResource(R.string.listen),
                { onSpeak((paragraphs + caption).filter { it.isNotEmpty() }.joinToString(" ")) },
            )
        }
        SkButton(stringResource(R.string.lesson_replay), onReplay)
    }
}
