package fi.sateenkaarikello.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.ui.rememberReduceMotion
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType
import kotlinx.coroutines.delay
import java.util.Locale

enum class PillKind { GOLD, MUTED, OK }

/** Pieni pilleri (web: .pill, .pill.muted, .pill.ok). */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.GOLD,
    icon: SvgIcon? = null,
) {
    val (bg, fg) =
        when (kind) {
            PillKind.GOLD -> SkColors.Pressed to SkColors.Ink
            PillKind.MUTED -> SkColors.PillMuted to SkColors.InkSoft
            PillKind.OK -> SkColors.GoodBg to SkColors.Good
        }
    Row(
        modifier.background(bg, SkShapes.Pill).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let { SkIcon(it, 14.dp) }
        Text(text, style = SkType.Pill.copy(color = fg))
    }
}

/** Pieni harmaa yläotsikko isoilla kirjaimilla (web: .eyebrow). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text.uppercase(Locale.forLanguageTag("fi-FI")), style = SkType.Eyebrow, modifier = modifier)
}

/** Numeroaika harmaalla taustalla (web: .digital). */
@Composable
fun DigitalPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = SkType.Digital,
        modifier =
            modifier
                .background(
                    SkColors.DigitalBg,
                    SkShapes.Digital,
                ).padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
fun StarIcon(
    on: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = SkDimens.Star,
) {
    SkIcon(if (on) SkIcons.StarOn else SkIcons.StarOff, size, modifier)
}

/** Kolme tähteä, joista [count] on täynnä. [description] luetaan ruudunlukijalla. */
@Composable
fun StarRow(
    count: Int,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = SkDimens.Star,
) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(STAR_COUNT) { StarIcon(it < count, size = size) }
    }
}

/**
 * Edistymispallot: tehdyt sateenkaaren väreillä, nykyinen renkaalla (web: .progress).
 * [currentDone] täyttää nykyisen pallon pompahtaen [fillDelayMs] kuluttua (kun lentävä tähti saapuu),
 * ja [onDotCenter] kertoo pallojen keskipisteet tähden lentoa varten.
 */
@Composable
fun ProgressDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
    currentDone: Boolean = false,
    fillDelayMs: Long = 0L,
    onDotCenter: ((Int, Offset) -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS, Alignment.CenterHorizontally),
    ) {
        repeat(count) { k ->
            val anchor = if (onDotCenter != null) Modifier.confettiAnchor { onDotCenter(k, it) } else Modifier
            val dot = Modifier.size(SkDimens.ProgressDot).then(anchor)
            val color = SkColors.Rainbow7[k % SkColors.Rainbow7.size]
            when {
                k < current -> {
                    Box(dot.background(color, CircleShape))
                }

                k == current && currentDone -> {
                    PoppingDot(color, fillDelayMs, dot)
                }

                k == current -> {
                    Box(
                        dot.background(SkColors.Surface, CircleShape).border(3.dp, SkColors.Hour, CircleShape),
                    )
                }

                else -> {
                    Box(dot.background(SkColors.DotEmpty, CircleShape))
                }
            }
        }
    }
}

enum class FeedbackKind { OK, HINT }

/** Palautelaatikko: onnistuminen vihreällä, vinkki oranssilla (web: .feedback). */
@Composable
fun FeedbackBox(
    text: String,
    kind: FeedbackKind,
    modifier: Modifier = Modifier,
) {
    val (bg, style) =
        when (kind) {
            FeedbackKind.OK -> SkColors.GoodBg to SkType.FeedbackOk
            FeedbackKind.HINT -> SkColors.TryBg to SkType.FeedbackHint
        }
    Text(
        text,
        style = style,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(bg, SkShapes.Feedback)
                .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

enum class OptionState { NORMAL, CORRECT, WRONG }

/**
 * Vastausvaihtoehto: sanat vasemmalla ja numeroaika oikealla (web: .opt).
 * Väärä vastaus tärähtää ja himmenee.
 */
@Composable
fun OptionButton(
    words: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    digital: String? = null,
    state: OptionState = OptionState.NORMAL,
    enabled: Boolean = true,
    wordsStyle: TextStyle = SkType.OptWords,
) {
    val (bg, border, shadow) =
        when (state) {
            OptionState.NORMAL -> Triple(SkColors.Surface, SkColors.Line, SkColors.Line)
            OptionState.CORRECT -> Triple(SkColors.GoodBg, SkColors.OptCorrectBorder, SkColors.OptCorrectShadow)
            OptionState.WRONG -> Triple(SkColors.TryBg, SkColors.OptWrongBorder, SkColors.OptWrongShadow)
        }
    val shake = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(state) {
        if (state == OptionState.WRONG && !reduceMotion) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = SHAKE_MS
                    0f at 0
                    -SHAKE_LEFT_START at (SHAKE_MS * SHAKE_FIRST_FRACTION).toInt()
                    SHAKE_RIGHT at (SHAKE_MS * SHAKE_SECOND_FRACTION).toInt()
                    -SHAKE_LEFT_END at (SHAKE_MS * SHAKE_THIRD_FRACTION).toInt()
                    0f at SHAKE_MS
                },
            )
        }
    }
    DepthSurface(
        onClick = onClick,
        fill = bg,
        shadow = shadow,
        shape = SkShapes.Option,
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = shake.value * density
                    alpha = if (state == OptionState.WRONG) WRONG_ALPHA else 1f
                },
        depth = SkDimens.OptionDepth,
        pressedDepth = 1.dp,
        border = BorderStroke(SkDimens.Border, border),
        enabled = enabled,
        dimWhenDisabled = false,
    ) {
        OptionLabel(words, digital, wordsStyle)
    }
}

private const val SHAKE_MS = 350
private const val WRONG_ALPHA = 0.75f

private const val STAR_COUNT = 3
private const val SHAKE_LEFT_START = 7f
private const val SHAKE_RIGHT = 6f
private const val SHAKE_LEFT_END = 4f
private const val SHAKE_FIRST_FRACTION = 0.2f
private const val SHAKE_SECOND_FRACTION = 0.45f
private const val SHAKE_THIRD_FRACTION = 0.7f

@Composable
private fun OptionLabel(
    words: String,
    digital: String?,
    wordsStyle: TextStyle,
) {
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(words, style = wordsStyle, modifier = Modifier.weight(1f))
        if (digital != null) {
            Spacer(Modifier.width(14.dp))
            Text(
                digital,
                style = SkType.OptDigital,
                modifier =
                    Modifier
                        .background(
                            SkColors.OptDigitalBg,
                            SkShapes.OptDigital,
                        ).padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

/** Nykyinen pallo täyttyy värillä ja pompahtaa, kun tähti laskeutuu siihen. */
@Composable
private fun PoppingDot(
    color: Color,
    delayMs: Long,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(delayMs)
        scale.animateTo(
            1f,
            keyframes {
                durationMillis = DOT_POP_MS
                0f at 0
                DOT_POP_PEAK at DOT_POP_MS / 2
                1f at DOT_POP_MS
            },
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .border(3.dp, SkColors.Hour, CircleShape),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }.background(color, CircleShape),
        )
    }
}

private const val DOT_POP_MS = 320
private const val DOT_POP_PEAK = 1.5f
