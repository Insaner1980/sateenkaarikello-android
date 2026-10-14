package fi.sateenkaarikello.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.domain.Settings
import fi.sateenkaarikello.domain.Wardrobe
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.BigMenuButton
import fi.sateenkaarikello.ui.components.ChipButton
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SegmentedControl
import fi.sateenkaarikello.ui.components.SkCard
import fi.sateenkaarikello.ui.components.SkIcons
import fi.sateenkaarikello.ui.components.StarIcon
import fi.sateenkaarikello.ui.doll.DollParts
import fi.sateenkaarikello.ui.doll.LivelyDoll
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType
import fi.sateenkaarikello.ui.timeLabel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    progress: Progress,
    settings: Settings,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    ScreenScaffold(modifier) {
        Hero()
        NowCard(settings.h24)
        DollCard(progress) { onOpen(HomeDestination.NUKKE) }
        Menu(onOpen)
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            SegmentedControl(
                label = stringResource(R.string.mode_label),
                options = listOf(stringResource(R.string.mode_12), stringResource(R.string.mode_24)),
                selectedIndex = if (settings.h24) 1 else 0,
                onSelect = {
                    app.setH24(it == 1)
                    app.play(Sound.POP)
                },
            )
            ChipButton(
                stringResource(if (settings.sound) R.string.sound_on else R.string.sound_off),
                app::toggleSound,
                selected = settings.sound,
            )
            ResetChip(onReset = app::resetProgress)
        }
    }
}

/** Sateenkaarikaaret ja monivärinen otsikko (web: .hero ja .logo). */
@Composable
private fun Hero() {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(SkDimens.HeroLogo).aspectRatio(HERO_W / HERO_H).clearAndSetSemantics { }) {
            val s = size.width / HERO_W
            SkColors.HeroArcs.forEachIndexed { i, color ->
                val r = (RAINBOW_RADIUS - i * RAINBOW_GAP) * s
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(RAINBOW_CENTER_X * s - r, RAINBOW_CENTER_Y * s - r),
                    size = Size(2 * r, 2 * r),
                    style = Stroke(RAINBOW_STROKE * s, cap = StrokeCap.Round),
                )
            }
        }
        val title = stringResource(R.string.app_name)
        Text(
            buildAnnotatedString {
                title.forEachIndexed {
                    i,
                    c,
                    ->
                    withStyle(SpanStyle(color = SkColors.logoColor(i))) { append(c) }
                }
            },
            style = SkType.Logo,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.semantics {
                    heading()
                    contentDescription = title
                },
        )
        Text(stringResource(R.string.tagline), style = SkType.Tagline, textAlign = TextAlign.Center)
    }
}

/** Oikean ajan kello, päivittyy 10 sekunnin välein (web: tickNow). */
@Composable
private fun NowCard(h24: Boolean) {
    var now by remember { mutableIntStateOf(ClockTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(NOW_TICK_MS)
            now = ClockTime.now()
        }
    }
    val t = if (h24) now else now % ClockTime.HALF_DAY
    SkCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapL),
        ) {
            ClockFace(command = ClockCommand(t), mini = true, modifier = Modifier.size(SkDimens.MiniClockHome))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(R.string.now_eyebrow))
                Text(timeLabel(t, h24), style = SkType.NowWords)
            }
        }
    }
}

@Composable
private fun DollCard(
    progress: Progress,
    onClick: () -> Unit,
) {
    SkCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapL),
        ) {
            LivelyDoll(
                progress.outfit,
                Modifier.width(SkDimens.DollMini).aspectRatio(DollParts.WIDTH / DollParts.HEIGHT),
            )
            Column(Modifier.weight(1f)) {
                Eyebrow(progress.dollName.ifEmpty { stringResource(R.string.doll_default_name) })
                Row(
                    Modifier.padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                ) {
                    StarIcon(true, size = SkDimens.StarCountIcon)
                    Text(
                        stringResource(R.string.home_star_total, progress.totalStars, Wardrobe.MAX_STARS),
                        style = SkType.StarCount,
                    )
                }
                Text(stringResource(R.string.home_doll_hint), style = SkType.BodySoft)
            }
        }
    }
}

@Composable
private fun Menu(onOpen: (HomeDestination) -> Unit) {
    val gap = 14.dp
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
        BigMenuButton(
            stringResource(R.string.menu_koulu),
            stringResource(R.string.menu_koulu_sub),
            SkIcons.Book,
            SkColors.KouluFill,
            SkColors.KouluShadow,
            { onOpen(HomeDestination.KOULU) },
            Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            BigMenuButton(
                stringResource(R.string.menu_vapaa),
                stringResource(R.string.menu_vapaa_sub),
                SkIcons.Clock,
                SkColors.VapaaFill,
                SkColors.VapaaShadow,
                { onOpen(HomeDestination.VAPAA) },
                Modifier.weight(1f),
            )
            BigMenuButton(
                stringResource(R.string.menu_pelaa),
                stringResource(R.string.menu_pelaa_sub),
                SkIcons.Star,
                SkColors.PelaaFill,
                SkColors.PelaaShadow,
                { onOpen(HomeDestination.PELAA) },
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            BigMenuButton(
                stringResource(R.string.menu_kysy),
                stringResource(R.string.menu_kysy_sub),
                SkIcons.Speech,
                SkColors.KysyFill,
                SkColors.KysyShadow,
                { onOpen(HomeDestination.KYSY) },
                Modifier.weight(1f),
            )
            BigMenuButton(
                stringResource(R.string.menu_nukke),
                stringResource(R.string.menu_nukke_sub),
                SkIcons.Dress,
                SkColors.NukkeFill,
                SkColors.NukkeShadow,
                { onOpen(HomeDestination.NUKKE) },
                Modifier.weight(1f),
            )
        }
    }
}

/** Kaksivaiheinen nollaus: ensimmäinen painallus kysyy varmistusta 5 sekunnin ajan (web: resetBtn). */
@Composable
private fun ResetChip(onReset: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(RESET_TIMEOUT_MS)
            armed = false
        }
    }
    ChipButton(stringResource(if (armed) R.string.reset_confirm else R.string.reset), {
        if (armed) {
            armed = false
            onReset()
        } else {
            armed = true
        }
    })
}

private const val HERO_W = 120f
private const val HERO_H = 66f
private const val NOW_TICK_MS = 10_000L
private const val RESET_TIMEOUT_MS = 5_000L

private const val RAINBOW_RADIUS = 54
private const val RAINBOW_GAP = 8
private const val RAINBOW_CENTER_X = 60
private const val RAINBOW_CENTER_Y = 62
private const val RAINBOW_STROKE = 7
