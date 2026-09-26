package com.bahoque.gastonomo.feature.loans.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.Avatar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.ProgressBar
import com.bahoque.gastonomo.core.designsystem.component.StatusBadge
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.LoanVisuals
import com.bahoque.gastonomo.feature.loans.R
import com.bahoque.gastonomo.feature.loans.highlightAmountOf
import com.bahoque.gastonomo.feature.loans.loanVisualsOf
import com.bahoque.gastonomo.feature.loans.progressOf

/**
 * La tarjeta grande del detalle: el avatar, el nombre, la fecha de creacion, el badge de
 * direccion, la nota, el saldo en 34/700, la barra y las tres columnas.
 *
 * La fecha de creacion lleva detras el monto original cuando el prestamo se registro en otra
 * moneda, igual que cada abono.
 */
@Composable
fun LoanSummaryCard(row: LoanRow, modifier: Modifier = Modifier) {
    val visuals = loanVisualsOf(row.loan.direction, row.summary)
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        radius = GastonomoTheme.radii.balanceCard,
        contentPadding = PaddingValues(CARD_PADDING),
    ) {
        Header(row = row, visuals = visuals)
        if (row.loan.note.isNotBlank()) {
            Note(note = row.loan.note)
        }
        GastonomoText(
            text = visuals.highlightLabel,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.muted,
            modifier = Modifier.padding(top = HIGHLIGHT_GAP),
        )
        GastonomoAmountText(
            text = highlightAmountOf(row.summary),
            style = GastonomoTheme.typography.amountDetail,
            color = visuals.highlightColor,
        )
        val progress = stringResource(R.string.loan_progress_description, row.summary.percent)
        ProgressBar(
            fraction = progressOf(row.summary),
            fill = visuals.barColor,
            height = GastonomoTheme.dimens.progressBarTall,
            modifier = Modifier
                .padding(top = BAR_GAP)
                .semantics { contentDescription = progress },
        )
        Columns(row = row, visuals = visuals)
    }
}

@Composable
private fun Header(row: LoanRow, visuals: LoanVisuals) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
    ) {
        Avatar(
            initials = row.loan.person.take(1).uppercase(),
            size = AVATAR_SIZE,
            background = visuals.avatarBackground,
            contentColor = visuals.avatarContent,
        )
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = row.loan.person,
                style = GastonomoTheme.typography.amountCard,
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            GastonomoText(
                text = createdLabel(row),
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
                maxLines = 1,
            )
        }
        StatusBadge(
            label = visuals.directionLabel,
            background = visuals.avatarBackground,
            contentColor = visuals.avatarContent,
            large = true,
        )
    }
}

@Composable
private fun Note(note: String) {
    GastonomoText(
        text = note,
        style = GastonomoTheme.typography.bodySmall,
        color = GastonomoTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = NOTE_GAP)
            .background(GastonomoTheme.colors.loanNote, RoundedCornerShape(NOTE_RADIUS))
            .padding(horizontal = NOTE_PADDING_X, vertical = NOTE_PADDING_Y),
    )
}

/** Original, Abonado y Estado, la ultima alineada a la derecha. */
@Composable
private fun Columns(row: LoanRow, visuals: LoanVisuals) {
    val formats = GastonomoTheme.formats
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = COLUMNS_GAP),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
    ) {
        DetailColumn(
            label = stringResource(R.string.loan_detail_column_original),
            value = formats.amount(row.summary.total),
        )
        DetailColumn(
            label = stringResource(R.string.loan_detail_column_paid),
            value = formats.amount(row.summary.paid),
        )
        DetailColumn(
            label = stringResource(R.string.loan_detail_column_status),
            value = stringResource(
                R.string.loan_detail_status_percent,
                visuals.statusLabel,
                row.summary.percent,
            ),
            valueColor = visuals.badge.content,
            alignment = TextAlign.End,
            // El estado no es un importe: con cifras de ancho fijo "Pagado - 100%" no cabe
            // en el tercio que le toca.
            tabular = false,
            weight = STATUS_COLUMN_WEIGHT,
        )
    }
}

@Composable
private fun RowScope.DetailColumn(
    label: String,
    value: String,
    valueColor: Color = GastonomoTheme.colors.ink,
    alignment: TextAlign = TextAlign.Start,
    tabular: Boolean = true,
    weight: Float = 1f,
) {
    val style = GastonomoTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Bold,
        textAlign = alignment,
    )
    Column(modifier = Modifier.weight(weight)) {
        GastonomoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = GastonomoTheme.typography.caption.copy(textAlign = alignment),
            color = GastonomoTheme.colors.muted,
            maxLines = 1,
        )
        val valueModifier = Modifier.fillMaxWidth().padding(top = COLUMN_VALUE_GAP)
        if (tabular) {
            GastonomoAmountText(text = value, modifier = valueModifier, style = style, color = valueColor)
        } else {
            GastonomoText(text = value, modifier = valueModifier, style = style, color = valueColor, maxLines = 1)
        }
    }
}

/** El texto del estado es mas largo que un importe, asi que se lleva algo mas de ancho. */
private const val STATUS_COLUMN_WEIGHT = 1.3f

/** "Creado el {fecha}", con el monto original detras si se registro en otra moneda. */
@Composable
private fun createdLabel(row: LoanRow): String {
    val formats = GastonomoTheme.formats
    val created = stringResource(R.string.loan_detail_created_on, formats.dates.long(row.loan.openedOn))
    if (!row.loan.money.isForeignTo(row.summary.total.currency)) return created
    return created + stringResource(R.string.loan_separator) + formats.amount(row.loan.money.original)
}

private val CARD_PADDING = 20.dp
private val AVATAR_SIZE = 48.dp
private val HEADER_GAP = 12.dp
private val NOTE_GAP = 12.dp
private val NOTE_RADIUS = 12.dp
private val NOTE_PADDING_X = 12.dp
private val NOTE_PADDING_Y = 10.dp
private val HIGHLIGHT_GAP = 22.dp
private val BAR_GAP = 14.dp
private val COLUMNS_GAP = 16.dp
private val COLUMN_GAP = 8.dp
private val COLUMN_VALUE_GAP = 2.dp
