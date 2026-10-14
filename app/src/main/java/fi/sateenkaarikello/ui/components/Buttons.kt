package fi.sateenkaarikello.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType

/**
 * "3D-nappi": täytetty pinta, jonka alla on tummempi kiinteä varjo. Painettaessa pinta
 * painuu alas varjon päälle (web: box-shadow 0 5px 0 ja :active translateY).
 */
@Composable
fun DepthSurface(
    onClick: () -> Unit,
    fill: Color,
    shadow: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    depth: Dp = SkDimens.ButtonDepth,
    pressedDepth: Dp = SkDimens.ButtonDepthPressed,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    dimWhenDisabled: Boolean = true,
    role: Role = Role.Button,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift = if (pressed && enabled) depth - pressedDepth else 0.dp
    Box(
        modifier
            .alpha(if (enabled || !dimWhenDisabled) 1f else DISABLED_ALPHA)
            .offset { IntOffset(0, shift.roundToPx()) }
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                translate(0f, (depth - shift).toPx()) { drawOutline(outline, shadow) }
            }.background(fill, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = role, onClick = onClick),
        propagateMinConstraints = true,
    ) { content() }
}

private const val DISABLED_ALPHA = 0.5f

enum class ButtonKind { NORMAL, PRIMARY, GREEN }

/** Tavallinen nappi (web: .btn, .btn.primary, .btn.green, .btn.small). */
@Composable
fun SkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.NORMAL,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val (fill, shadow) =
        when (kind) {
            ButtonKind.NORMAL -> SkColors.BtnFill to SkColors.BtnShadow
            ButtonKind.PRIMARY -> SkColors.BtnPrimaryFill to SkColors.BtnPrimaryShadow
            ButtonKind.GREEN -> SkColors.BtnGreenFill to SkColors.BtnGreenShadow
        }
    DepthSurface(onClick, fill, shadow, SkShapes.Button, modifier, enabled = enabled) {
        Box(
            Modifier.padding(if (small) PaddingValues(20.dp, 13.dp) else PaddingValues(22.dp, 15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = if (small) SkType.ButtonSmall else SkType.Button, textAlign = TextAlign.Center)
        }
    }
}

/** Pieni pyöreä sirunappi. [selected] != null tekee siitä päälle/pois-valinnan (web: aria-pressed). */
@Composable
fun ChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
) {
    val on = selected == true
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .semantics { if (selected != null) this.selected = on }
            .background(if (on) SkColors.Pressed else SkColors.Surface, SkShapes.Pill)
            .border(SkDimens.BorderThin, if (on) SkColors.Gold else SkColors.Line, SkShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SkType.Chip, textAlign = TextAlign.Center)
    }
}

/** Kaksiosainen valinta, esim. "Kello 12 h / 24 h" (web: .seg). */
@Composable
fun SegmentedControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(SkColors.Surface, SkShapes.Pill)
            .border(SkDimens.BorderThin, SkColors.Line, SkShapes.Pill)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            style = SkType.Chip.copy(color = SkColors.InkSoft),
            modifier = Modifier.padding(start = 10.dp, end = 4.dp),
        )
        options.forEachIndexed { i, option ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { selected = on }
                    .then(
                        if (on) {
                            Modifier
                                .background(
                                    SkColors.Pressed,
                                    SkShapes.Pill,
                                ).border(2.dp, SkColors.Gold, SkShapes.Pill)
                        } else {
                            Modifier
                        },
                    ).clickable(role = Role.RadioButton) { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(option, style = SkType.Segment) }
        }
    }
}

/** Etusivun iso valikkonappi: kuvake, otsikko ja selite (web: .big-btn). */
@Composable
fun BigMenuButton(
    title: String,
    subtitle: String,
    icon: SvgIcon,
    fill: Color,
    shadow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DepthSurface(
        onClick,
        fill,
        shadow,
        SkShapes.Card,
        modifier,
        depth = SkDimens.BigButtonDepth,
        pressedDepth = 2.dp,
    ) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkIcon(icon, SkDimens.MenuIcon, Modifier.padding(bottom = 4.dp))
            Text(title, style = SkType.BigButton)
            Text(subtitle, style = SkType.BigButtonSmall)
        }
    }
}

/** Haamuvarjo kortille: ohut kiinteä varjo alareunassa (web: 0 4px 0 rgba(58,36,83,.06)). */
fun Modifier.cardShadow(
    shape: Shape = SkShapes.Card,
    depth: Dp = 4.dp,
    color: Color = SkColors.Shadow.copy(alpha = 0.06f),
    soft: Boolean = true,
): Modifier =
    drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(0f, depth.toPx()) { drawOutline(outline, color) }
        if (soft) translate(0f, depth.toPx() * 2) { drawOutline(outline, color.copy(alpha = color.alpha / 2)) }
    }

/** Valkoinen kortti (web: .card). */
@Composable
fun SkCard(
    modifier: Modifier = Modifier,
    padding: Dp = SkDimens.CardPadding,
    color: Color = SkColors.Surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .cardShadow()
            .background(color, SkShapes.Card)
            .padding(padding),
    ) { content() }
}
