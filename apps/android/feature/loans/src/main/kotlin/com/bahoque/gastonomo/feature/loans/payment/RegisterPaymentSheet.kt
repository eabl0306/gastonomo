package com.bahoque.gastonomo.feature.loans.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoDateField
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SheetHeader
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.FailureText
import com.bahoque.gastonomo.feature.loans.MoneyEntryBlock
import com.bahoque.gastonomo.feature.loans.R

/**
 * Pantalla 11. El sheet de Registrar abono, que se abre encima de Prestamos o del detalle
 * sin navegar.
 *
 * El importe puede pasarse del saldo a proposito: es lo que produce los intereses ganados,
 * asi que aqui no se limita.
 */
@Composable
fun RegisterPaymentSheet(
    state: PaymentUiState,
    actions: PaymentActions,
    modifier: Modifier = Modifier,
) {
    val loan = state.loan ?: return
    GastonomoBottomSheet(onDismiss = actions.onDismiss, modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.payment_title),
            subtitle = stringResource(
                R.string.payment_subtitle,
                loan.person,
                GastonomoTheme.formats.amount(state.outstanding),
            ),
        )
        MoneyEntryBlock(
            entry = state.entry,
            context = state.context,
            color = GastonomoTheme.colors.ink,
            actions = actions.entry,
            amountStyle = GastonomoTheme.typography.amountInputSmall,
            symbolSize = SHEET_SYMBOL_SIZE,
            topPadding = AMOUNT_TOP_PADDING,
        )
        PortionChips(onFill = actions.onFill)
        GastonomoDateField(
            label = stringResource(R.string.loan_field_date),
            value = state.date,
            onValueChange = actions.onDate,
            compact = true,
        )
        state.failure?.let { FailureText(it, modifier = Modifier.padding(top = FAILURE_GAP)) }
        PrimaryButton(
            text = stringResource(R.string.payment_submit),
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.padding(top = BUTTON_GAP),
        )
    }
}

/** Los dos chips que rellenan el importe con el saldo entero o con la mitad. */
@Composable
private fun PortionChips(onFill: (PaymentPortion) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = CHIPS_GAP),
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PortionChip(stringResource(R.string.payment_fill_all)) { onFill(PaymentPortion.ALL) }
        PortionChip(stringResource(R.string.payment_fill_half)) { onFill(PaymentPortion.HALF) }
    }
}

/**
 * Un chip que ejecuta una accion, no un filtro: por eso no es un `FilterPill`, que ademas
 * se anuncia al lector de pantalla como una opcion elegible.
 */
@Composable
private fun PortionChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CHIP_RADIUS)
    Box(
        modifier = Modifier
            .heightIn(min = GastonomoTheme.dimens.minTouchTarget)
            .pressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(CHIP_HEIGHT)
                .background(GastonomoTheme.colors.surface, shape)
                .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.inputBorder, shape)
                .padding(horizontal = CHIP_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            GastonomoText(
                text = label,
                style = GastonomoTheme.typography.secondary.copy(fontWeight = FontWeight.SemiBold),
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
        }
    }
}

/** El simbolo del monto del abono es mas pequeno que el de un formulario: 38 frente a 40. */
private const val SHEET_SYMBOL_SIZE = 38

private val AMOUNT_TOP_PADDING = 24.dp
private val CHIPS_GAP = 18.dp
private val CHIP_GAP = 6.dp
private val CHIP_HEIGHT = 34.dp
private val CHIP_RADIUS = 17.dp
private val CHIP_PADDING = 14.dp
private val FAILURE_GAP = 10.dp
private val BUTTON_GAP = 20.dp
