package fi.sateenkaarikello.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.domain.FinnishTime
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.ButtonKind
import fi.sateenkaarikello.ui.components.ChipButton
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.Pill
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.components.StarRow
import fi.sateenkaarikello.ui.components.TopBar
import fi.sateenkaarikello.ui.components.cardShadow
import fi.sateenkaarikello.ui.levelName
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType

/** Tasojen valinta: viisi tasokorttia ja lopuksi Minuuttihaaste (web: renderLevels). */
@Composable
fun LevelsScreen(
    progress: Progress,
    onPlay: (Int) -> Unit,
    onLesson: (Int) -> Unit,
    onChallenge: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.levels_title), stringResource(R.string.back), onBack)
        for (level in Levels.all) {
            val i = level.index
            LevelCard(
                color = SkColors.LevelCards[i],
                sample = level.sample,
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkButton(
                            stringResource(R.string.play),
                            { onPlay(i) },
                            kind = ButtonKind.PRIMARY,
                            small = true,
                        )
                        ChipButton(stringResource(R.string.watch_lesson), { onLesson(i) })
                    }
                },
            ) {
                Eyebrow(stringResource(R.string.level_eyebrow, i + 1))
                Text(
                    stringResource(levelName(i)),
                    style = SkType.LevelTitle,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.level_example, FinnishTime.timeWords(level.sample)),
                    style = SkType.Small.copy(color = SkColors.InkSoft),
                )
                StarRow(
                    progress.stars[i],
                    pluralStringResource(R.plurals.stars_count, progress.stars[i], progress.stars[i]),
                )
            }
        }
        ChallengeCard(progress, onChallenge)
    }
}

/** Tasokortti: pieni kello, tiedot ja napit. Kaikki tasot ovat pelattavissa missä järjestyksessä tahansa. */
@Composable
private fun LevelCard(
    color: Color,
    sample: Int,
    actions: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val command = remember(sample) { ClockCommand(sample) }
    Column(
        Modifier
            .fillMaxWidth()
            .cardShadow(
                depth = SkDimens.LevelDepth,
                color = SkColors.Shadow.copy(alpha = LEVEL_SHADOW_ALPHA),
                soft = false,
            ).background(color, SkShapes.Card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            ClockFace(
                command = command,
                mini = true,
                modifier = Modifier.size(SkDimens.MiniClockLevel).padding(bottom = 4.dp),
            )
            content()
        }
        Column(Modifier.padding(top = 6.dp)) { actions() }
    }
}

private const val CHALLENGE_SAMPLE = 45
private const val LEVEL_SHADOW_ALPHA = 0.10f

@Composable
private fun ChallengeCard(
    progress: Progress,
    onChallenge: () -> Unit,
) {
    LevelCard(
        color = SkColors.ChallengeCard,
        sample = CHALLENGE_SAMPLE,
        actions = {
            SkButton(
                stringResource(R.string.play),
                onChallenge,
                kind = ButtonKind.PRIMARY,
                small = true,
            )
        },
    ) {
        Eyebrow(stringResource(R.string.challenge_eyebrow))
        Text(
            stringResource(R.string.challenge_title),
            style = SkType.LevelTitle,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.challenge_desc), style = SkType.Small.copy(color = SkColors.InkSoft))
        if (progress.challengeBest > 0) Pill(stringResource(R.string.record, progress.challengeBest))
    }
}
