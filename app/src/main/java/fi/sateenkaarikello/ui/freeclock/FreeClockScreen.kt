package fi.sateenkaarikello.ui.freeclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.ClockBox
import fi.sateenkaarikello.ui.components.DigitalPill
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.HelpTopBar
import fi.sateenkaarikello.ui.components.Panel
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.digitalLabel
import fi.sateenkaarikello.ui.resources
import fi.sateenkaarikello.ui.spokenTime
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType
import fi.sateenkaarikello.ui.timeLabel
import kotlin.random.Random

/** Vapaa kello: raahattava kello, aika sanoina ja numeroina, Kuuntele ja Arvo aika (web: Opettele). */
@Composable
fun FreeClockScreen(
    h24: Boolean,
    help: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val res = resources()
    val period = ClockTime.period(h24)
    var time by rememberSaveable { mutableIntStateOf(START_TIME) }
    var command by remember { mutableStateOf(ClockCommand(time)) }
    // 24 h -tilassa aloitetaan iltapäivästä (15.00) eikä yöstä (3.00), kuten webissä.
    LaunchedEffect(h24) {
        val adjusted = if (h24 && time < ClockTime.HALF_DAY) time + ClockTime.HALF_DAY else ClockTime.mod(time, period)
        if (adjusted != time) {
            time = adjusted
            command = ClockCommand(adjusted)
        }
    }
    ScreenScaffold(modifier) { clockSize ->
        HelpTopBar(
            title = stringResource(R.string.free_title),
            help = help,
            onBack = onBack,
            onToggleHelp = app::toggleHelp,
        )
        ClockBox(clockSize) {
            ClockFace(
                command = command,
                period = period,
                step = STEP,
                interactive = true,
                help = help,
                onChange = { time = it },
            )
        }
        Panel {
            Eyebrow(stringResource(R.string.clock_is))
            Text(timeLabel(time, h24), style = SkType.Words)
            DigitalPill(digitalLabel(res, time, h24))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                if (speechAvailable) {
                    SkButton(
                        stringResource(R.string.listen),
                        { app.speak(spokenTime(res, time, h24)) },
                    )
                }
                SkButton(stringResource(R.string.random_time), {
                    val t = Levels.randomClockTime(period, time, Random.Default)
                    time = t
                    command = ClockCommand(t, RANDOM_MOVE_MS)
                    app.play(Sound.POP)
                })
            }
            Legend()
        }
    }
}

/** Selite viisareista (web: .legend). */
@Composable
private fun Legend() {
    Column(
        Modifier.background(SkColors.Legend, SkShapes.Feedback).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            Box(
                Modifier.width(54.dp),
            ) { Box(Modifier.width(34.dp).height(12.dp).background(SkColors.Hour, SkShapes.Pill)) }
            Text(stringResource(R.string.legend_hour), style = SkType.Small)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            Box(Modifier.width(54.dp).height(8.dp).background(SkColors.Minute, SkShapes.Pill))
            Text(stringResource(R.string.legend_minute), style = SkType.Small)
        }
        Text(stringResource(R.string.legend_drag), style = SkType.Small)
    }
}

private const val START_TIME = 180
private const val STEP = 5
private const val RANDOM_MOVE_MS = 900
