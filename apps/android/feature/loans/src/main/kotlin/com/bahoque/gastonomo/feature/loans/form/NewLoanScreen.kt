package com.bahoque.gastonomo.feature.loans.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.AppBarNavigation
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoDateField
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.Segment
import com.bahoque.gastonomo.core.designsystem.component.SegmentedControl
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.FailureText
import com.bahoque.gastonomo.feature.loans.InfoBox
import com.bahoque.gastonomo.feature.loans.MoneyEntryBlock
import com.bahoque.gastonomo.feature.loans.R

/**
 * Pantalla 10. Nuevo prestamo.
 *
 * Un solo POST: la API crea el prestamo y su movimiento a la vez, y la caja de info se lo
 * cuenta al usuario con los dos textos exactos del diseno.
 */
@Composable
fun NewLoanScreen(
    state: NewLoanUiState,
    actions: NewLoanActions,
    modifier: Modifier = Modifier,
) {
    val lent = state.direction == LoanDirection.LENT
    Column(modifier = modifier.fillMaxWidth()) {
        GastonomoAppBar(
            title = stringResource(R.string.new_loan_title),
            onNavigate = actions.onClose,
            navigation = AppBarNavigation.Close,
        )
        GastonomoScreen(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = TOP_PADDING, bottom = BOTTOM_PADDING),
            ) {
                DirectionControl(direction = state.direction, onDirection = actions.onDirection)
                MoneyEntryBlock(
                    entry = state.entry,
                    context = state.context,
                    color = if (lent) GastonomoTheme.colors.blue else GastonomoTheme.colors.amber,
                    actions = actions.entry,
                    caption = stringResource(R.string.new_loan_total),
                )
                Fields(state = state, actions = actions, lent = lent)
                InfoBox(
                    text = stringResource(
                        if (lent) R.string.new_loan_hint_lent else R.string.new_loan_hint_borrowed,
                    ),
                    modifier = Modifier.padding(top = INFO_GAP),
                )
                state.failure?.let { FailureText(it, modifier = Modifier.padding(top = FAILURE_GAP)) }
            }
            PrimaryButton(
                text = stringResource(R.string.new_loan_submit),
                onClick = actions.onSave,
                enabled = state.canSave,
                modifier = Modifier.padding(top = BUTTON_TOP, bottom = BUTTON_BOTTOM),
            )
        }
    }
}

@Composable
private fun DirectionControl(direction: LoanDirection, onDirection: (LoanDirection) -> Unit) {
    SegmentedControl(
        segments = listOf(
            Segment(
                label = stringResource(R.string.new_loan_lent),
                activeColor = GastonomoTheme.colors.ink,
            ),
            Segment(
                label = stringResource(R.string.new_loan_borrowed),
                activeColor = GastonomoTheme.colors.ink,
            ),
        ),
        selectedIndex = if (direction == LoanDirection.LENT) 0 else 1,
        onSelect = { index ->
            onDirection(if (index == 0) LoanDirection.LENT else LoanDirection.BORROWED)
        },
    )
}

@Composable
private fun Fields(state: NewLoanUiState, actions: NewLoanActions, lent: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGapLoose),
    ) {
        GastonomoTextField(
            value = state.person,
            onValueChange = actions.onPerson,
            label = stringResource(
                if (lent) R.string.new_loan_person_lent else R.string.new_loan_person_borrowed,
            ),
            placeholder = stringResource(R.string.new_loan_person_placeholder),
            error = state.failure?.person,
        )
        GastonomoDateField(
            label = stringResource(R.string.loan_field_date),
            value = state.date,
            onValueChange = actions.onDate,
        )
        GastonomoTextField(
            value = state.note,
            onValueChange = actions.onNote,
            label = stringResource(R.string.loan_field_note),
            optional = true,
            placeholder = stringResource(R.string.new_loan_note_placeholder),
            imeAction = ImeAction.Done,
        )
    }
}

private val TOP_PADDING = 4.dp
private val BOTTOM_PADDING = 20.dp
private val INFO_GAP = 18.dp
private val FAILURE_GAP = 10.dp
private val BUTTON_TOP = 12.dp
private val BUTTON_BOTTOM = 24.dp
