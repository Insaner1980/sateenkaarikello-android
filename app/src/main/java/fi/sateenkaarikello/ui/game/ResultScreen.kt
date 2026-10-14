package fi.sateenkaarikello.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.Screen
import fi.sateenkaarikello.domain.Item
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.domain.Wardrobe
import fi.sateenkaarikello.ui.components.ButtonKind
import fi.sateenkaarikello.ui.components.LocalConfetti
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkButton
import fi.sateenkaarikello.ui.components.SkCard
import fi.sateenkaarikello.ui.components.StarIcon
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.doll.DollCelebration
import fi.sateenkaarikello.ui.doll.DollParts
import fi.sateenkaarikello.ui.doll.ItemTile
import fi.sateenkaarikello.ui.doll.LivelyDoll
import fi.sateenkaarikello.ui.levelName
import fi.sateenkaarikello.ui.rememberReduceMotion
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType
import kotlinx.coroutines.delay

/**
 * Tasotulos: tähdet, uudet vaatteet ja jatkonapit (web: finish). Kaikki lasketaan avaimesta,
 * joten näkymä on sama, vaikka siihen palattaisiin myöhemmin.
 */
@Composable
fun ResultScreen(
    result: Screen.Result,
    outfit: Outfit,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onDress: () -> Unit,
    onLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stars = result.stars
    val after = result.starsBefore + maxOf(0, stars - result.previousBest)
    val newItems = Wardrobe.newlyUnlocked(result.starsBefore, after)
    val text = resultMessage(result, newItems.size)

    val confetti = LocalConfetti.current
    var celebrated by rememberSaveable { mutableStateOf(false) }
    // Nukke juhlii vain ensimmäisellä kerralla, ei palattaessa samaan tulokseen.
    val firstVisit = remember { !celebrated }
    val dollCelebration = remember(firstVisit) { if (firstVisit) dollCelebration(outfit, newItems, stars) else null }
    val anchor = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        if (celebrated) return@LaunchedEffect
        celebrated = true
        delay(CONFETTI_DELAY_MS)
        confetti?.burst(anchor.value, RESULT_CONFETTI)
    }

    ScreenScaffold(modifier) {
        SkCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SkDimens.Gap),
            ) {
                ResultStars(stars) { anchor.value = it }
                LivelyDoll(
                    outfit,
                    Modifier.width(SkDimens.DollResult).aspectRatio(DollParts.WIDTH / DollParts.HEIGHT),
                    dollCelebration,
                )
                ResultTitle(stars)
                Text(text, style = SkType.ResultText, textAlign = TextAlign.Center)
                if (newItems.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                        newItems.forEach { ItemTile(it, outfit, Modifier.width(SkDimens.ItemMinWidth)) }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                ) {
                    SkButton(stringResource(R.string.play_again), onPlayAgain)
                    if (result.level < Levels.all.lastIndex) {
                        SkButton(stringResource(R.string.next_level), onNextLevel, kind = ButtonKind.PRIMARY)
                    }
                    if (newItems.isNotEmpty()) {
                        SkButton(
                            stringResource(R.string.dress_doll),
                            onDress,
                            kind = ButtonKind.GREEN,
                        )
                    }
                    SkButton(stringResource(R.string.levels_short), onLevels)
                }
            }
        }
    }
}

/** Tähti ponnahtaa esiin pyörähtäen (web: @keyframes pop). */
@Composable
private fun PoppingStar(
    on: Boolean,
    delayMs: Int,
) {
    val reduceMotion = rememberReduceMotion()
    val k = remember { Animatable(if (on && !reduceMotion) 0f else 1f) }
    LaunchedEffect(on) {
        if (on && !reduceMotion) {
            delay(delayMs.toLong())
            k.animateTo(
                1f,
                keyframes {
                    durationMillis = POP_MS
                    0f at 0
                    POP_OVERSHOOT at (POP_MS * POP_PEAK_FRACTION).toInt()
                    1f at POP_MS
                },
            )
        }
    }
    StarIcon(
        on,
        Modifier.graphicsLayer {
            scaleX = k.value
            scaleY = k.value
            rotationZ = if (k.value < 1f && on) -POP_ROTATION * (1f - k.value) else 0f
        },
        size = SkDimens.StarResult,
    )
}

private const val MAX_STARS = 3
private const val POP_MS = 500
private const val CONFETTI_DELAY_MS = 250L
private const val RESULT_CONFETTI = 120

/** Nukke alkaa juhlia, kun kolmas tähti on ponnahtanut esiin. */
private const val DOLL_CELEBRATE_DELAY_MS = 1_100L

private const val POP_OVERSHOOT = 1.2f
private const val POP_PEAK_FRACTION = 0.7f
private const val POP_ROTATION = 40f

@Composable
private fun ResultTitle(stars: Int) {
    Text(
        stringResource(
            when (stars) {
                MAX_STARS -> R.string.result_title_3
                2 -> R.string.result_title_2
                else -> R.string.result_title_1
            },
        ),
        style = SkType.ResultTitle,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun resultMessage(
    result: Screen.Result,
    newItemCount: Int,
): String {
    val stars = result.stars
    val name = stringResource(levelName(result.level))
    return buildList {
        add(pluralStringResource(R.plurals.result_text, stars, name, stars))
        if (result.previousBest > stars) {
            add(pluralStringResource(R.plurals.result_previous_best, result.previousBest, result.previousBest))
        }
        when {
            newItemCount == 1 -> add(stringResource(R.string.result_new_item))
            newItemCount > 1 -> add(stringResource(R.string.result_new_items))
            stars < MAX_STARS -> add(stringResource(R.string.result_play_more))
        }
    }.joinToString(" ")
}

/** Kolme tähteä, jotka ponnahtavat esiin vuorotellen. [onCenter] antaa konfetin lähtöpisteen. */
@Composable
private fun ResultStars(
    stars: Int,
    onCenter: (Offset) -> Unit,
) {
    val description = pluralStringResource(R.plurals.result_stars_description, stars, stars)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description }.confettiAnchor(onCenter),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(MAX_STARS) { k -> PoppingStar(on = k < stars, delayMs = STAR_DELAY_MS + k * STAR_STAGGER_MS) }
    }
}

/** Nuken juhlinta: uudet vaatteet pyörähdyksellä ja kolmesta tähdestä hyppy vilkuttaen. */
private fun dollCelebration(
    outfit: Outfit,
    newItems: List<Item>,
    stars: Int,
): DollCelebration =
    DollCelebration(
        newOutfit = newItems.takeIf { it.isNotEmpty() }?.fold(outfit) { o, item -> o.with(item.slot, item.id) },
        jump = stars == MAX_STARS,
        delayMs = DOLL_CELEBRATE_DELAY_MS,
    )

private const val STAR_DELAY_MS = 150
private const val STAR_STAGGER_MS = 250
