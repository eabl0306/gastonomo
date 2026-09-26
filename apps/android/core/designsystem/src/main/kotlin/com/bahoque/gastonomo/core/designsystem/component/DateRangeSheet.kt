package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import java.time.LocalDate

/**
 * El sheet de rango del filtro de fechas, el mismo en Inicio y en Movimientos.
 *
 * Los presets solo rellenan Desde y Hasta; hasta que no se pulsa "Aplicar rango" el periodo
 * no cambia. Editar una de las dos fechas deselecciona el preset, y eso lo decide quien
 * tiene el estado: aqui solo se avisa de lo que toco el usuario.
 *
 * ```
 * if (state.rangeSheet != null) {
 *     DateRangeSheet(
 *         from = state.rangeSheet.from,
 *         to = state.rangeSheet.to,
 *         selectedPreset = state.rangeSheet.preset,
 *         onPreset = viewModel::onPreset,
 *         onFrom = viewModel::onRangeFrom,
 *         onTo = viewModel::onRangeTo,
 *         onApply = viewModel::onApplyRange,
 *         onDismiss = viewModel::onCloseSheet,
 *     )
 * }
 * ```
 */
@Composable
fun DateRangeSheet(
    from: LocalDate,
    to: LocalDate,
    selectedPreset: PeriodPreset?,
    onPreset: (PeriodPreset) -> Unit,
    onFrom: (LocalDate) -> Unit,
    onTo: (LocalDate) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GastonomoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.gastonomo_period_range_title),
            subtitle = stringResource(R.string.gastonomo_period_range_subtitle),
        )
        PresetRow(selectedPreset = selectedPreset, onPreset = onPreset)
        Spacer(Modifier.height(PRESETS_GAP))
        BoundsRow(from = from, to = to, onFrom = onFrom, onTo = onTo)
        Spacer(Modifier.height(BUTTON_GAP))
        PrimaryButton(
            text = stringResource(R.string.gastonomo_period_range_apply),
            onClick = onApply,
            enabled = !from.isAfter(to),
        )
    }
}

@Composable
private fun PresetRow(selectedPreset: PeriodPreset?, onPreset: (PeriodPreset) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PRESET_GAP),
        verticalArrangement = Arrangement.spacedBy(PRESET_GAP),
    ) {
        PeriodPreset.entries.forEach { preset ->
            FilterPill(
                label = stringResource(preset.labelRes()),
                selected = preset == selectedPreset,
                onClick = { onPreset(preset) },
                compact = true,
            )
        }
    }
}

@Composable
private fun BoundsRow(
    from: LocalDate,
    to: LocalDate,
    onFrom: (LocalDate) -> Unit,
    onTo: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FIELD_GAP),
    ) {
        GastonomoDateField(
            label = stringResource(R.string.gastonomo_period_range_from),
            value = from,
            onValueChange = onFrom,
            modifier = Modifier.weight(1f),
            maxDate = to,
            compact = true,
        )
        GastonomoDateField(
            label = stringResource(R.string.gastonomo_period_range_to),
            value = to,
            onValueChange = onTo,
            modifier = Modifier.weight(1f),
            minDate = from,
            compact = true,
        )
    }
}

/** El texto de cada preset, en el orden en el que el diseno los ensena. */
private fun PeriodPreset.labelRes(): Int = when (this) {
    PeriodPreset.THIS_MONTH -> R.string.gastonomo_period_preset_this_month
    PeriodPreset.LAST_MONTH -> R.string.gastonomo_period_preset_last_month
    PeriodPreset.LAST_30_DAYS -> R.string.gastonomo_period_preset_last_30_days
    PeriodPreset.LAST_3_MONTHS -> R.string.gastonomo_period_preset_last_3_months
    PeriodPreset.THIS_YEAR -> R.string.gastonomo_period_preset_this_year
}

private val PRESET_GAP = 6.dp
private val PRESETS_GAP = 18.dp
private val FIELD_GAP = 10.dp
private val BUTTON_GAP = 20.dp
