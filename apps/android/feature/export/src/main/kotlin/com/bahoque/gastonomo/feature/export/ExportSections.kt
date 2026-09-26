package com.bahoque.gastonomo.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.FilterPill
import com.bahoque.gastonomo.core.designsystem.component.FilterPillRow
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCheckbox
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.ListRow
import com.bahoque.gastonomo.core.designsystem.component.RadioCard
import com.bahoque.gastonomo.core.designsystem.component.RadioCardLayout
import com.bahoque.gastonomo.core.designsystem.component.tintOfCategoryColor
import com.bahoque.gastonomo.core.designsystem.format.periodLabel
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** Las dos tarjetas-radio de formato: Excel y CSV. */
@Composable
internal fun FormatCards(
    selected: ExportFormat,
    onSelect: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        FormatCard(ExportFormat.XLSX, selected, onSelect, Modifier.weight(1f))
        FormatCard(ExportFormat.CSV, selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun FormatCard(
    format: ExportFormat,
    selected: ExportFormat,
    onSelect: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = format.color()
    RadioCard(
        title = stringResource(format.titleRes()),
        subtitle = stringResource(format.hintRes()),
        selected = format == selected,
        onClick = { onSelect(format) },
        modifier = modifier,
        layout = RadioCardLayout.Stacked,
        leading = {
            IconTile(
                background = tintOfCategoryColor(color),
                size = GastonomoTheme.dimens.iconTileMedium,
                radius = GastonomoTheme.radii.iconChoice,
            ) {
                GastonomoIcon(name = format.icon(), tint = color)
            }
        },
    )
}

/** Los tres conjuntos con su conteo de filas y su checkbox. */
@Composable
internal fun SetRows(
    state: ExportUiState,
    onToggle: (ExportSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    GastonomoCard(modifier = modifier) {
        ExportSet.entries.forEach { set ->
            ListRow(
                title = stringResource(set.titleRes()),
                subtitle = pluralStringResource(R.plurals.export_rows, state.rowsOf(set), state.rowsOf(set)),
                showDivider = set != ExportSet.entries.last(),
                minHeight = GastonomoTheme.dimens.listRowTall,
                onClick = { onToggle(set) },
                leading = {
                    IconTile(
                        background = GastonomoTheme.colors.iconTile,
                        size = GastonomoTheme.dimens.iconTileTiny,
                        radius = GastonomoTheme.radii.iconTileTiny,
                    ) {
                        GastonomoIcon(name = set.icon(), size = ICON_SIZE)
                    }
                },
                trailing = {
                    GastonomoCheckbox(checked = set in state.sets, onCheckedChange = { onToggle(set) })
                },
            )
        }
    }
}

/** Las dos pills de periodo. "Periodo actual" lleva el mes que se esta mirando. */
@Composable
internal fun RangePills(
    state: ExportUiState,
    onSelect: (ExportRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    // El mismo label que ensena el selector de Inicio, rango incluido.
    val period = GastonomoTheme.formats.periodLabel(state.period)
    FilterPillRow(modifier = modifier) {
        FilterPill(
            label = stringResource(R.string.export_period_all),
            selected = state.range == ExportRange.ALL,
            onClick = { onSelect(ExportRange.ALL) },
            compact = true,
        )
        FilterPill(
            label = stringResource(R.string.export_period_current, period),
            selected = state.range == ExportRange.PERIOD,
            onClick = { onSelect(ExportRange.PERIOD) },
            compact = true,
        )
    }
}

/** La caja de resumen: el nombre del archivo que va a salir y una nota de que lleva. */
@Composable
internal fun SummaryBox(state: ExportUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GastonomoTheme.colors.chipBg, RoundedCornerShape(GastonomoTheme.radii.control))
            .padding(horizontal = BOX_SIDE_PADDING, vertical = BOX_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BOX_GAP),
    ) {
        GastonomoIcon(
            name = GastonomoIcons.DESCRIPTION,
            size = BOX_ICON,
            tint = GastonomoTheme.colors.noteText,
        )
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = fileNameOf(state),
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            val note = noteOf(state)
            if (note.isNotEmpty()) {
                GastonomoText(
                    text = note,
                    style = GastonomoTheme.typography.secondarySmall,
                    color = GastonomoTheme.colors.noteText,
                )
            }
        }
    }
}

private val CARD_GAP = 10.dp
private val ICON_SIZE = 18.dp
private val BOX_PADDING = 14.dp
private val BOX_SIDE_PADDING = 16.dp
private val BOX_GAP = 12.dp
private val BOX_ICON = 22.dp
