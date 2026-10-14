package fi.sateenkaarikello.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkType

/** Yläpalkki: takaisin-siru vasemmalla, otsikko keskellä ja valinnainen siru oikealla (web: .topbar). */
@Composable
fun TopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        ChipButton(backLabel, onBack)
        WordSafeText(title, SkType.Title, Modifier.weight(1f).semantics { heading() })
        if (trailing != null) trailing() else Box(Modifier.padding(start = 1.dp))
    }
}

/**
 * Näkymän runko: reunasta reunaan, turva-alueet huomioiden, 16 dp:n sivumarginaali ja
 * vieritys pienillä näytöillä. [clockSize] on kellon koko: min(leveys − 32 dp, korkeus × 0,42).
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(clockSize: Dp) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().safeDrawingPadding()) {
        val clockSize = min(maxWidth - SkDimens.Gutter * 2, maxHeight * CLOCK_HEIGHT_SHARE)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SkDimens.Gutter, vertical = SkDimens.GapS),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
        ) {
            content(clockSize)
        }
    }
}

private const val CLOCK_HEIGHT_SHARE = 0.42f

/**
 * Keskitetty teksti, joka ei katkea kesken sanan: fonttia pienennetään niin, että pisin sana
 * mahtuu riville (esim. "Tasatunnit" tai "Sateenkaarihiukset" kapealla näytöllä).
 */
@Composable
fun WordSafeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minScale: Float = MIN_SCALE,
) {
    BasicText(
        text = text,
        style = style.merge(TextStyle(textAlign = TextAlign.Center)),
        modifier = modifier,
        autoSize = WordSafeAutoSize(style.fontSize.value, style.fontSize.value * minScale),
    )
}

/** Suurin fonttikoko, jolla mikään rivinvaihto ei osu sanan keskelle. Ei käytä alikoostamista. */
private data class WordSafeAutoSize(
    val maxSp: Float,
    val minSp: Float,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var size = maxSp
        while (size > minSp) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.didOverflowWidth && !layout.breaksInsideWord(text.text)) return size.sp
            size -= STEP_SP
        }
        return minSp.sp
    }

    private fun TextLayoutResult.breaksInsideWord(s: String): Boolean =
        (0 until lineCount - 1).any { line ->
            val end = getLineEnd(line)
            end in 1 until s.length && !s[end - 1].isWhitespace() && s[end - 1] != '-' && !s[end].isWhitespace()
        }

    private companion object {
        const val STEP_SP = 0.5f
    }
}

private const val MIN_SCALE = 0.68f
