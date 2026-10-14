package fi.sateenkaarikello.ui.doll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Item
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.domain.Slot
import fi.sateenkaarikello.domain.Wardrobe
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.components.Eyebrow
import fi.sateenkaarikello.ui.components.Pill
import fi.sateenkaarikello.ui.components.PillKind
import fi.sateenkaarikello.ui.components.ScreenScaffold
import fi.sateenkaarikello.ui.components.SkIcon
import fi.sateenkaarikello.ui.components.SkIcons
import fi.sateenkaarikello.ui.components.TopBar
import fi.sateenkaarikello.ui.components.WordSafeText
import fi.sateenkaarikello.ui.components.cardShadow
import fi.sateenkaarikello.ui.itemName
import fi.sateenkaarikello.ui.slotName
import fi.sateenkaarikello.ui.theme.SkColors
import fi.sateenkaarikello.ui.theme.SkDimens
import fi.sateenkaarikello.ui.theme.SkShapes
import fi.sateenkaarikello.ui.theme.SkType

/** Nuken vaatekaappi: iso nukke, nimi ja vaatteet paikoittain (web: renderNukke). */
@Composable
fun DollScreen(
    progress: Progress,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val total = progress.totalStars
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.doll_title), stringResource(R.string.back), onBack)
        DollStage(progress.outfit, progress.dollName, onNameChange = { app.setDollName(it.trim()) })
        Text(
            if (Wardrobe.lockedCount(total) >
                0
            ) {
                pluralStringResource(R.plurals.doll_info, total, total)
            } else {
                stringResource(R.string.doll_all_open)
            },
            style = SkType.Body.copy(fontSize = SkType.FeedbackHint.fontSize),
        )
        for (slot in Slot.entries) {
            Text(
                stringResource(slotName(slot)),
                style = SkType.SectionTitle,
                modifier = Modifier.semantics { heading() },
            )
            if (slot.optional) {
                Text(
                    stringResource(R.string.doll_optional_hint),
                    style = SkType.Small.copy(color = SkColors.InkSoft),
                )
            }
            ItemGrid(Wardrobe.itemsFor(slot)) { item, cell ->
                if (item.star <= total) {
                    ItemTile(item, progress.outfit, cell, on = progress.outfit.isOn(item), onClick = {
                        app.toggleItem(item)
                        app.play(Sound.POP)
                    })
                } else {
                    LockedTile(item, cell)
                }
            }
        }
    }
}

@Composable
private fun DollStage(
    outfit: Outfit,
    name: String,
    onNameChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardShadow()
            .background(Brush.verticalGradient(listOf(SkColors.DollStageTop, SkColors.DollStageBottom)), SkShapes.Card)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LivelyDoll(outfit, Modifier.width(SkDimens.DollBigMax).aspectRatio(DollParts.WIDTH / DollParts.HEIGHT))
        Eyebrow(stringResource(R.string.doll_name_label))
        NameField(name, onNameChange)
    }
}

/** Nimikenttä: tallentuu kirjoitettaessa, enintään 16 merkkiä (web: maxlength 16). */
@Composable
private fun NameField(
    saved: String,
    onChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(saved) }
    val label = stringResource(R.string.doll_name_label)
    BasicTextField(
        value = text,
        onValueChange = {
            text = it.take(MAX_NAME)
            onChange(text)
        },
        singleLine = true,
        textStyle = SkType.NameField.copy(textAlign = TextAlign.Center),
        cursorBrush = SolidColor(SkColors.Hour),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(SkColors.Surface, SkShapes.Field)
                    .border(SkDimens.Border, SkColors.Line, SkShapes.Field)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) {
                    Text(
                        stringResource(R.string.doll_name_placeholder),
                        style = SkType.NameField.copy(color = SkColors.InkSoft, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            }
        },
    )
}

/** Kolmen sarakkeen ruudukko; viimeisen rivin tyhjät paikat jäävät tyhjiksi (web: .items). */
@Composable
private fun ItemGrid(
    items: List<Item>,
    tile: @Composable (Item, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        for (row in items.chunked(GRID_COLUMNS)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                modifier = Modifier.height(IntrinsicSize.Max),
            ) {
                row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Vaate pikkukuvana: nukke puettuna tällä vaatteella ja rajattuna paikan kohdalle. */
@Composable
fun ItemTile(
    item: Item,
    outfit: Outfit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val name = stringResource(itemName(item.id))
    Column(
        modifier
            .semantics { if (onClick != null) selected = on }
            .background(if (on) SkColors.ItemOnBg else SkColors.Surface, SkShapes.Item)
            .border(SkDimens.Border, if (on) SkColors.ItemOnBorder else SkColors.Line, SkShapes.Item)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Doll(outfit.with(item.slot, item.id), Modifier.size(SkDimens.ItemArt), crop = DollParts.crop(item.slot))
        WordSafeText(name, SkType.ItemName)
        if (on) Pill(stringResource(R.string.doll_item_on))
    }
}

@Composable
private fun LockedTile(
    item: Item,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.doll_item_locked, item.star)
    Column(
        modifier
            .background(SkColors.ItemLockedBg, SkShapes.Item)
            .drawBehind {
                val w = SkDimens.Border.toPx()
                drawRoundRect(
                    SkColors.Line,
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(w / 2, w / 2),
                    size =
                        androidx.compose.ui.geometry
                            .Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))),
                )
            }.semantics(mergeDescendants = true) { contentDescription = description }
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(SkDimens.ItemArt), contentAlignment = Alignment.Center) { SkIcon(SkIcons.Lock, 34.dp) }
        WordSafeText(stringResource(itemName(item.id)), SkType.ItemName.copy(color = SkColors.InkSoft))
        Pill(item.star.toString(), kind = PillKind.MUTED, icon = SkIcons.StarOn)
    }
}

private const val MAX_NAME = 16
private const val GRID_COLUMNS = 3
