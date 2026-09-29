package com.bahoque.gastonomo.feature.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.RowDivider
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.Money

/**
 * El borrador pintado: el tipo arriba, el importe en grande y las filas de detalle.
 *
 * El importe se ensena **en la moneda en la que se dicto**, y debajo su equivalencia si no es la
 * principal. Es lo mismo que hacen los tres formularios, y lo que se guarda son los cuatro campos.
 */
@Composable
internal fun DraftCard(state: DictateUiState, draft: DictationDraft) {
    val colors = GastonomoTheme.colors
    val formats = GastonomoTheme.formats
    GastonomoCard(contentPadding = PaddingValues(CARD_PADDING)) {
        Header(draft = draft)
        Spacer(Modifier.height(AMOUNT_GAP))
        GastonomoText(
            text = draft.amount
                ?.let { formats.amount(Money(it, draft.currency)) }
                ?: stringResource(R.string.dictation_no_amount),
            style = GastonomoTheme.typography.amountDetail,
            color = if (draft.amount == null) colors.faint else colors.ink,
        )
        if (state.isForeign) {
            state.money?.let { money ->
                GastonomoText(
                    text = formats.amount(money.base, money.baseCurrency),
                    style = GastonomoTheme.typography.secondary,
                    color = colors.muted,
                )
            }
        }
        Spacer(Modifier.height(ROWS_GAP))
        DetailRows(draft = draft)
    }
}

/** El tile del tipo, su nombre y la nota que el interprete saco del dictado. */
@Composable
private fun Header(draft: DictationDraft) {
    val colors = GastonomoTheme.colors
    val visuals = visualsOf(draft.entry)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGap),
    ) {
        IconTile(
            background = visuals.background(),
            size = GastonomoTheme.dimens.iconTileLarge,
            radius = GastonomoTheme.radii.control,
        ) {
            GastonomoIcon(name = visuals.icon, size = HEADER_ICON, tint = visuals.tint())
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            GastonomoText(
                text = stringResource(visuals.label),
                style = GastonomoTheme.typography.body,
                color = colors.ink,
            )
            val note = noteOf(draft.entry)
            if (note.isNotBlank()) {
                GastonomoText(
                    text = note,
                    style = GastonomoTheme.typography.secondary,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** La fecha y, segun el tipo, la categoria o la persona. */
@Composable
private fun DetailRows(draft: DictationDraft) {
    val formats = GastonomoTheme.formats
    val rows = buildList {
        add(stringResource(R.string.dictation_row_date) to formats.dates.long(draft.occurredOn))
        when (val entry = draft.entry) {
            is DictatedEntry.Movement ->
                add(stringResource(R.string.dictation_row_category) to entry.category.name)
            is DictatedEntry.Loan -> add(
                stringResource(R.string.dictation_row_person) to
                    entry.person.ifBlank { EMPTY_VALUE },
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) RowDivider()
            DetailRow(label = label, value = value)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ROW_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoText(
            text = label,
            style = GastonomoTheme.typography.bodySmall,
            color = GastonomoTheme.colors.muted,
        )
        GastonomoText(
            text = value,
            modifier = Modifier.padding(start = VALUE_GAP),
            style = GastonomoTheme.typography.rowTitle.copy(textAlign = TextAlign.End),
            color = GastonomoTheme.colors.ink,
        )
    }
}

/** Lo que cambia entre un gasto, un ingreso y los dos sentidos de un prestamo. */
private class DraftVisuals(
    val icon: String,
    val label: Int,
    val background: @Composable () -> Color,
    val tint: @Composable () -> Color,
)

private fun visualsOf(entry: DictatedEntry): DraftVisuals = when (entry) {
    is DictatedEntry.Movement -> if (entry.kind.isIncome) {
        DraftVisuals(
            icon = GastonomoIcons.ARROW_DOWNWARD,
            label = R.string.dictation_income,
            background = { GastonomoTheme.colors.greenBg },
            tint = { GastonomoTheme.colors.green },
        )
    } else {
        DraftVisuals(
            icon = GastonomoIcons.ARROW_UPWARD,
            label = R.string.dictation_expense,
            background = { GastonomoTheme.colors.redBg },
            tint = { GastonomoTheme.colors.red },
        )
    }
    is DictatedEntry.Loan -> DraftVisuals(
        icon = GastonomoIcons.HANDSHAKE,
        label = if (entry.direction == LoanDirection.LENT) {
            R.string.dictation_loan_lent
        } else {
            R.string.dictation_loan_borrowed
        },
        background = { GastonomoTheme.colors.blueBg },
        tint = { GastonomoTheme.colors.blue },
    )
}

private fun noteOf(entry: DictatedEntry): String = when (entry) {
    is DictatedEntry.Movement -> entry.note
    is DictatedEntry.Loan -> entry.note
}

/** Lo que se pinta en una fila sin valor. */
private const val EMPTY_VALUE = "—"

private val HEADER_ICON = 22.dp
private val AMOUNT_GAP = 14.dp
private val ROWS_GAP = 12.dp
private val ROW_PADDING = 11.dp
private val VALUE_GAP = 12.dp
