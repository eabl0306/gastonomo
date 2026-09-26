package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El filtro de fechas que comparten Inicio y Movimientos: la caja blanca del mes con sus
 * dos flechas y, al lado, el boton que abre el sheet de rango.
 *
 * Con un rango activo el diseno cambia tres cosas de golpe: la flecha izquierda se oculta,
 * la derecha pasa a `close` y vuelve al mes actual, y el boton de rango se invierte a ink
 * con el icono en lime. El label lo pinta quien llama, con
 * [com.bahoque.gastonomo.core.designsystem.format.periodLabel].
 *
 * ```
 * PeriodPickerRow(
 *     label = GastonomoTheme.formats.periodLabel(state.selection.period),
 *     isRange = state.selection.isRange,
 *     canGoForward = state.selection.canGoForward(state.today),
 *     onPrevious = viewModel::onPreviousMonth,
 *     onNextOrClear = viewModel::onNextMonthOrClearRange,
 *     onOpenRange = viewModel::onOpenRangeSheet,
 * )
 * ```
 *
 * @param canGoForward false en el mes actual: el selector no deja pasar de hoy y la flecha
 *   derecha se queda al 25 por ciento.
 */
@Composable
fun PeriodPickerRow(
    label: String,
    isRange: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNextOrClear: () -> Unit,
    onOpenRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthBox(
            label = label,
            isRange = isRange,
            canGoForward = canGoForward,
            onPrevious = onPrevious,
            onNextOrClear = onNextOrClear,
            modifier = Modifier.weight(1f),
        )
        RangeButton(active = isRange, onClick = onOpenRange)
    }
}

@Composable
private fun MonthBox(
    label: String,
    isRange: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNextOrClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.monthPicker)
    Row(
        modifier = modifier
            .height(GastonomoTheme.dimens.monthPicker)
            .clip(shape)
            .background(GastonomoTheme.colors.surface)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.line, shape)
            .padding(horizontal = BOX_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(
            icon = GastonomoIcons.CHEVRON_LEFT,
            description = stringResource(R.string.gastonomo_period_previous_month),
            // Con un rango activo la flecha izquierda no existe: el diseno la esconde.
            opacity = if (isRange) 0f else 1f,
            enabled = !isRange,
            onClick = onPrevious,
        )
        GastonomoText(
            text = label,
            modifier = Modifier.weight(1f),
            style = GastonomoTheme.typography.body.copy(textAlign = TextAlign.Center),
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
        ArrowButton(
            icon = if (isRange) GastonomoIcons.CLOSE else GastonomoIcons.CHEVRON_RIGHT,
            description = stringResource(
                if (isRange) R.string.gastonomo_period_clear_range else R.string.gastonomo_period_next_month,
            ),
            opacity = if (isRange || canGoForward) 1f else DISABLED_ARROW_ALPHA,
            enabled = isRange || canGoForward,
            onClick = onNextOrClear,
        )
    }
}

@Composable
private fun ArrowButton(
    icon: String,
    description: String,
    opacity: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(GastonomoTheme.dimens.minTouchTarget)
            .fillMaxHeight()
            .alpha(opacity)
            .pressable(onClick = onClick, enabled = enabled, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = icon,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.ink,
            contentDescription = description.takeIf { enabled },
        )
    }
}

@Composable
private fun RangeButton(active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.monthPicker)
    Box(
        modifier = Modifier
            .size(GastonomoTheme.dimens.monthPicker)
            .clip(shape)
            .background(if (active) GastonomoTheme.colors.ink else GastonomoTheme.colors.surface)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.line, shape)
            .pressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = GastonomoIcons.DATE_RANGE,
            size = ICON_SIZE,
            tint = if (active) GastonomoTheme.colors.lime else GastonomoTheme.colors.ink,
            contentDescription = stringResource(R.string.gastonomo_period_open_range),
        )
    }
}

/** Lo que el diseno deja ver de la flecha que no se puede pulsar. */
private const val DISABLED_ARROW_ALPHA = 0.25f

private val GAP = 8.dp
private val BOX_PADDING = 4.dp
private val ICON_SIZE = 22.dp
