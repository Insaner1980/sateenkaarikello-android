package fi.sateenkaarikello.ui.intro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.min
import fi.sateenkaarikello.R
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.FinnishTime
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.components.LocalConfetti
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.doll.DollCelebration
import fi.sateenkaarikello.ui.doll.DollParts
import fi.sateenkaarikello.ui.doll.LivelyDoll
import fi.sateenkaarikello.ui.greetingText
import fi.sateenkaarikello.ui.rememberReduceMotion
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType
import fi.sateenkaarikello.ui.theme.dotBackground
import kotlinx.coroutines.delay
import fi.sateenkaarikello.ui.intro.IntroTimeline as T

/**
 * Aloitusnäkymä kotinäkymän päällä (aikajana: [IntroTimeline]). Kello pyörähtää laitteen
 * kellonaikaan, nukke [outfit]-asussa hyppii vilkuttaen, ja lopuksi näkymä häivytetään.
 * Napautus ohittaa. [onTick] soitetaan viisarien pysähtyessä ja [onDone] lopussa.
 * Kun järjestelmän animaatiot on poistettu, valmis kuva näytetään hetken ilman liikettä.
 */
@Composable
fun IntroOverlay(
    outfit: Outfit,
    onTick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val target = remember { ClockTime.now() }
    val from = remember(target) { T.spinFrom(target) }
    var command by remember { mutableStateOf(ClockCommand(if (reduceMotion) target else from)) }
    val elapsed = remember { mutableLongStateOf(if (reduceMotion) T.FADE_START_MS else 0L) }
    val clockCenter = remember { mutableStateOf(Offset.Zero) }
    val confetti = LocalConfetti.current
    val currentOnDone by rememberUpdatedState(onDone)
    IntroTimelineEffect(
        reduceMotion = reduceMotion,
        onFrame = { elapsed.longValue = it },
        onSpin = { command = ClockCommand(target, T.SPIN_MS, exact = true) },
        onTick = {
            onTick()
            confetti?.sparkle(clockCenter.value)
        },
        onDone = onDone,
    )

    val time = { elapsed.longValue }
    val title = stringResource(R.string.app_name)
    val greeting = stringResource(greetingText(FinnishTime.greeting(target)))
    BoxWithConstraints(
        modifier.introSurface(
            alpha = { T.overlayAlpha(time()) },
            description = stringResource(R.string.intro_description, title, greeting),
            skipLabel = stringResource(R.string.intro_skip),
            onSkip = { currentOnDone() },
        ),
        contentAlignment = Alignment.Center,
    ) {
        val clockSize = min(maxWidth - SkDimens.Gutter * 2, maxHeight * CLOCK_HEIGHT_SHARE)
        val dollHeight = maxHeight * DOLL_HEIGHT_SHARE
        Column(
            Modifier.clearAndSetSemantics { }.graphicsLayer { alpha = T.contentAlpha(time()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            IntroTitle(title, time)
            Text(
                greeting,
                style = SkType.Tagline,
                modifier = Modifier.graphicsLayer { alpha = T.greetingAlpha(time()) },
            )
            IntroClock(
                command = command,
                from = from,
                time = time,
                modifier =
                    Modifier
                        .size(clockSize)
                        .confettiAnchor { clockCenter.value = it },
            )
            IntroDoll(outfit, time, Modifier.height(dollHeight))
        }
    }
}

/** Koko näytön peittävä, läpinäkyvyydeltään [alpha] tausta; napautus ohittaa intron. */
private fun Modifier.introSurface(
    alpha: () -> Float,
    description: String,
    skipLabel: String,
    onSkip: () -> Unit,
): Modifier =
    fillMaxSize()
        .graphicsLayer { this.alpha = alpha() }
        .dotBackground()
        .clickable(interactionSource = null, indication = null, onClickLabel = skipLabel, onClick = onSkip)
        .semantics { contentDescription = description }
        .safeDrawingPadding()

/** Ajaa aikajanan; ruutu-, pyörähdys-, tik- ja lopetuskutsut käyttävät aina viimeisimpiä lambdoja. */
@Composable
private fun IntroTimelineEffect(
    reduceMotion: Boolean,
    onFrame: (Long) -> Unit,
    onSpin: () -> Unit,
    onTick: () -> Unit,
    onDone: () -> Unit,
) {
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnSpin by rememberUpdatedState(onSpin)
    val currentOnTick by rememberUpdatedState(onTick)
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            delay(T.STILL_MS)
        } else {
            runTimeline(onFrame = { currentOnFrame(it) }, onSpin = { currentOnSpin() }, onTick = { currentOnTick() })
        }
        currentOnDone()
    }
}

/** Kello ponnahtaa esiin, ja minuuttiviisari maalaa sateenkaarirenkaan pyörähtäessään. */
@Composable
private fun IntroClock(
    command: ClockCommand,
    from: Int,
    time: () -> Long,
    modifier: Modifier = Modifier,
) {
    ClockFace(
        command = command,
        mini = true,
        paintRingFrom = from,
        modifier =
            modifier.graphicsLayer {
                val s = T.clockScale(time())
                scaleX = s
                scaleY = s
                alpha = T.clockAlpha(time())
            },
    )
}

/** Nukke nousee esiin ja hyppii vilkuttaen; [modifier] antaa korkeuden, leveys tulee nuken mittasuhteesta. */
@Composable
private fun IntroDoll(
    outfit: Outfit,
    time: () -> Long,
    modifier: Modifier = Modifier,
) {
    LivelyDoll(
        outfit = outfit,
        celebration = remember { DollCelebration(newOutfit = null, jump = true, delayMs = T.DOLL_JUMP_AT_MS) },
        modifier =
            modifier
                .aspectRatio(DollParts.WIDTH / DollParts.HEIGHT)
                .graphicsLayer {
                    translationY = T.dollDrop(time()) * size.height
                    alpha = T.dollAlpha(time())
                },
    )
}

/** Otsikko kirjain kerrallaan, jotta jokainen kirjain voi pompata esiin omaan aikaansa. */
@Composable
private fun IntroTitle(
    title: String,
    time: () -> Long,
) {
    Row {
        title.forEachIndexed { i, c ->
            Text(
                c.toString(),
                style = SkType.Logo,
                color = SkColors.logoColor(i),
                modifier =
                    Modifier.graphicsLayer {
                        val s = T.letterScale(time(), i)
                        scaleX = s
                        scaleY = s
                        alpha = T.letterAlpha(time(), i)
                    },
            )
        }
    }
}

/** Ajaa aikajanan ruutu kerrallaan loppuun: viisarit lähtevät [onSpin]-hetkellä ja tik soi [onTick]-hetkellä. */
private suspend fun runTimeline(
    onFrame: (Long) -> Unit,
    onSpin: () -> Unit,
    onTick: () -> Unit,
) {
    val start = withFrameNanos { it }
    var elapsed = 0L
    var spun = false
    var ticked = false
    while (elapsed < T.END_MS) {
        elapsed = withFrameNanos { (it - start) / NANOS_PER_MS }
        onFrame(elapsed)
        if (!spun && elapsed >= T.SPIN_START_MS) {
            spun = true
            onSpin()
        }
        if (!ticked && elapsed >= T.TICK_AT_MS) {
            ticked = true
            onTick()
        }
    }
}

private const val CLOCK_HEIGHT_SHARE = 0.4f
private const val DOLL_HEIGHT_SHARE = 0.24f
private const val NANOS_PER_MS = 1_000_000L
