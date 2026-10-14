package fi.sateenkaarikello.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import fi.sateenkaarikello.R
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.ButtonKind
import fi.sateenkaarikello.ui.components.ClockBox
import fi.sateenkaarikello.ui.components.DigitalPill
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.HelpTopBar
import fi.sateenkaarikello.ui.components.LocalConfetti
import fi.sateenkaarikello.ui.components.Panel
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.digitalLabel
import fi.sateenkaarikello.ui.resources
import fi.sateenkaarikello.ui.spokenTime
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType
import fi.sateenkaarikello.ui.timeLabel
import kotlin.random.Random

private enum class AskPhase { SET, ASK, REVEALED }

/** Kysy siskolta: isosisko asettaa kellon, pikkusisko vastaa ääneen, ja vastaus paljastetaan. */
@Composable
fun AskScreen(
    h24: Boolean,
    help: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val confetti = LocalConfetti.current
    val period = ClockTime.period(h24)
    var phase by rememberSaveable { mutableStateOf(AskPhase.SET) }
    var time by rememberSaveable { mutableIntStateOf(0) }
    var command by remember { mutableStateOf(ClockCommand(time)) }
    val wordsAnchor = remember { mutableStateOf(Offset.Zero) }
    ScreenScaffold(modifier) { clockSize ->
        HelpTopBar(
            title = stringResource(R.string.ask_title),
            help = help,
            onBack = onBack,
            onToggleHelp = app::toggleHelp,
        )
        ClockBox(clockSize) {
            ClockFace(
                command = command,
                period = period,
                step = STEP,
                interactive = phase == AskPhase.SET,
                help = help,
                onChange = { time = it },
            )
        }
        Panel {
            if (phase == AskPhase.SET) {
                AskSetup(
                    onRandom = {
                        time = Levels.randomClockTime(period, time, Random.Default)
                        command = ClockCommand(time, RANDOM_MOVE_MS)
                        app.play(Sound.POP)
                    },
                    onAsk = {
                        app.play(Sound.POP)
                        phase = AskPhase.ASK
                    },
                )
            } else {
                Eyebrow(stringResource(R.string.ask_small_eyebrow))
                Text(stringResource(R.string.question_read), style = SkType.Question)
                Text(stringResource(R.string.ask_small_text), style = SkType.Body)
                if (phase == AskPhase.ASK) {
                    SkButton(stringResource(R.string.ask_reveal), {
                        phase = AskPhase.REVEALED
                        app.play(Sound.OK)
                        confetti?.burst(wordsAnchor.value, REVEAL_CONFETTI)
                    }, kind = ButtonKind.PRIMARY, modifier = Modifier.confettiAnchor { wordsAnchor.value = it })
                } else {
                    AskAnswer(time, h24, speechAvailable)
                }
                SkButton(stringResource(R.string.ask_new), {
                    app.play(Sound.POP)
                    phase = AskPhase.SET
                }, kind = ButtonKind.GREEN)
            }
        }
    }
}

private const val STEP = 5
private const val RANDOM_MOVE_MS = 900
private const val REVEAL_CONFETTI = 40

@Composable
private fun ColumnScope.AskSetup(
    onRandom: () -> Unit,
    onAsk: () -> Unit,
) {
    Eyebrow(stringResource(R.string.ask_big_eyebrow))
    Text(stringResource(R.string.ask_big_question), style = SkType.Question)
    Text(stringResource(R.string.ask_big_text), style = SkType.Body)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        SkButton(stringResource(R.string.random_time), onRandom)
        SkButton(stringResource(R.string.ask_go), onAsk, kind = ButtonKind.PRIMARY)
    }
}

@Composable
private fun AskAnswer(
    time: Int,
    h24: Boolean,
    speechAvailable: Boolean,
) {
    val app = LocalAppViewModel.current
    val res = resources()
    Text(timeLabel(time, h24), style = SkType.Words)
    DigitalPill(digitalLabel(res, time, h24))
    if (speechAvailable) {
        SkButton(
            stringResource(R.string.listen),
            { app.speak(spokenTime(res, time, h24)) },
        )
    }
}
