package com.bahoque.gastonomo.feature.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.AmountInputField
import com.bahoque.gastonomo.core.designsystem.component.AppBarNavigation
import com.bahoque.gastonomo.core.designsystem.component.ConversionBox
import com.bahoque.gastonomo.core.designsystem.component.CurrencyChip
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheet
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheetOrigin
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoDateField
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.Segment
import com.bahoque.gastonomo.core.designsystem.component.SegmentedControl
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.feature.movements.ui.CategoryGrid

/**
 * Nuevo movimiento, la pantalla 7: tipo, monto con su moneda, categoria, fecha y nota.
 *
 * El boton de guardar esta fijo abajo y se habilita con monto mayor que 0, equivalencia mayor
 * que 0 y categoria elegida, que es lo que pide el diseno.
 */
@Composable
fun NewMovementScreen(
    state: NewMovementUiState,
    actions: NewMovementActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(GastonomoTheme.colors.bgApp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GastonomoAppBar(
                title = stringResource(R.string.new_movement_title),
                onNavigate = actions.onClose,
                navigation = AppBarNavigation.Close,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SIDE_PADDING),
            ) {
                KindSegments(state = state, onKind = actions.onKind)
                AmountBlock(state = state, actions = actions)
                GastonomoText(
                    text = stringResource(R.string.new_movement_category),
                    modifier = Modifier.padding(bottom = LABEL_GAP),
                    style = GastonomoTheme.typography.sliceAmount,
                    color = GastonomoTheme.colors.ink,
                )
                CategoryGrid(
                    categories = state.offeredCategories,
                    selected = state.selectedCategoryId,
                    onSelect = actions.onCategory,
                )
                Fields(state = state, actions = actions)
                Spacer(Modifier.height(BOTTOM_GAP))
            }
            SaveBar(state = state, onSave = actions.onSave)
        }
        GastonomoSheetHost(visible = state.currencySheetOpen) {
            CurrencySheet(
                origin = CurrencySheetOrigin.Record,
                currencies = state.currencies,
                selected = state.currency,
                query = state.currencyQuery,
                onQueryChange = actions.onCurrencyQuery,
                onSelect = actions.onCurrency,
                onDismiss = { actions.onCurrencySheet(false) },
            )
        }
    }
}

@Composable
private fun KindSegments(state: NewMovementUiState, onKind: (MovementKind) -> Unit) {
    SegmentedControl(
        segments = listOf(
            Segment(
                label = stringResource(GastonomoStrings.kindExpense),
                icon = GastonomoIcons.NORTH_EAST,
                activeColor = GastonomoTheme.colors.red,
            ),
            Segment(
                label = stringResource(GastonomoStrings.kindIncome),
                icon = GastonomoIcons.SOUTH_WEST,
                activeColor = GastonomoTheme.colors.green,
            ),
        ),
        selectedIndex = if (state.kind.isIncome) 1 else 0,
        onSelect = { index -> onKind(if (index == 1) MovementKind.IN else MovementKind.OUT) },
        modifier = Modifier.padding(top = TOP_GAP),
    )
}

@Composable
private fun AmountBlock(state: NewMovementUiState, actions: NewMovementActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AMOUNT_TOP, bottom = AMOUNT_BOTTOM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountInputField(
            raw = state.amountRaw,
            onRawChange = actions.onAmount,
            currency = state.currency,
            color = if (state.kind.isIncome) {
                GastonomoTheme.colors.green
            } else {
                GastonomoTheme.colors.red
            },
        )
        CurrencyChip(
            currency = state.currency,
            onClick = { actions.onCurrencySheet(true) },
            modifier = Modifier.padding(top = CHIP_GAP),
            fallbackName = state.currencies.firstOrNull { it.code == state.currency }?.name
                ?: state.currency.value,
            enabled = state.canConvert,
            disabledHint = stringResource(GastonomoStrings.offlineCurrency),
        )
        // Sin poder convertir no hay nada que ensenar: el registro va en la moneda principal y
        // su equivalencia es el propio importe.
        if (state.suggestion.isForeign && state.canConvert) {
            ConversionBox(
                suggestion = state.suggestion,
                baseRaw = state.baseRaw,
                onBaseChange = actions.onBase,
                onUseSuggested = { actions.onBase(null) },
                modifier = Modifier.padding(top = CONVERSION_GAP),
            )
        }
    }
}

@Composable
private fun Fields(state: NewMovementUiState, actions: NewMovementActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = FIELDS_TOP),
        verticalArrangement = Arrangement.spacedBy(FIELD_GAP),
    ) {
        GastonomoDateField(
            label = stringResource(R.string.new_movement_date),
            value = state.date,
            onValueChange = actions.onDate,
            maxDate = state.today,
        )
        GastonomoTextField(
            value = state.note,
            onValueChange = actions.onNote,
            label = stringResource(R.string.new_movement_note),
            optional = true,
            placeholder = stringResource(R.string.new_movement_note_placeholder),
            imeAction = ImeAction.Done,
            error = state.messageFor(FIELD_NOTE),
        )
    }
}

@Composable
private fun SaveBar(state: NewMovementUiState, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = BAR_TOP, bottom = BAR_BOTTOM),
    ) {
        problemOf(state)?.let { message ->
            GastonomoText(
                text = message,
                modifier = Modifier.padding(bottom = LABEL_GAP),
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.red,
            )
        }
        PrimaryButton(
            text = stringResource(R.string.new_movement_save),
            onClick = onSave,
            enabled = state.canSave,
        )
    }
}

/**
 * Lo que hay que decirle al usuario si el guardado fallo. Los mensajes de campo llegan de la
 * API **ya en castellano**, asi que se pintan tal cual.
 */
@Composable
private fun problemOf(state: NewMovementUiState): String? {
    val fields = state.fieldErrors.filter { it.field != FIELD_NOTE }
    if (fields.isNotEmpty()) return fields.joinToString(separator = "\n") { it.message }
    return state.error?.let { stringResource(errorTextOf(it)) }
}

/** El nombre del campo de la nota en los errores de la API. */
private const val FIELD_NOTE = "note"

private val SIDE_PADDING = 20.dp
private val TOP_GAP = 4.dp
private val LABEL_GAP = 10.dp
private val AMOUNT_TOP = 28.dp
private val AMOUNT_BOTTOM = 22.dp
private val CHIP_GAP = 6.dp
private val CONVERSION_GAP = 14.dp
private val FIELDS_TOP = 20.dp
private val FIELD_GAP = 14.dp
private val BOTTOM_GAP = 20.dp
private val BAR_TOP = 12.dp
private val BAR_BOTTOM = 24.dp
