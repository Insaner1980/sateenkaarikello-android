package fi.sateenkaarikello.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fi.sateenkaarikello.R
import fi.sateenkaarikello.ui.theme.SkDimens

/** Valkoinen paneeli kellon alla (web: .panel.card). */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SkCard(modifier.fillMaxWidth(), padding = SkDimens.PanelPadding) {
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM), content = content)
    }
}

/** Kello keskitettynä annettuun kokoon. */
@Composable
fun ClockBox(
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size)) { content() }
    }
}

/** Apu päällä / Apu pois -siru yläpalkkiin. */
@Composable
fun HelpChip(
    help: Boolean,
    onToggle: () -> Unit,
) {
    ChipButton(stringResource(if (help) R.string.help_on else R.string.help_off), onToggle, selected = help)
}

@Composable
fun HelpTopBar(
    title: String,
    help: Boolean,
    onBack: () -> Unit,
    onToggleHelp: () -> Unit,
) {
    TopBar(
        title = title,
        backLabel = stringResource(R.string.back),
        onBack = onBack,
        trailing = { HelpChip(help, onToggleHelp) },
    )
}
