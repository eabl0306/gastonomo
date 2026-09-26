package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.Avatar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.ProgressBar
import com.bahoque.gastonomo.core.designsystem.component.StatusBadge
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.LoanVisuals
import com.bahoque.gastonomo.feature.loans.R
import com.bahoque.gastonomo.feature.loans.highlightAmountOf
import com.bahoque.gastonomo.feature.loans.loanVisualsOf
import com.bahoque.gastonomo.feature.loans.progressOf

/**
 * Una tarjeta de prestamo: el avatar con la inicial, el nombre, el subtitulo
 * "Me debe - {nota} - {fecha}", el badge de estado, el bloque destacado con el saldo y la
 * barra de progreso.
 *
 * El boton "Registrar abono" solo sale si queda saldo y abre el sheet sin navegar, asi que
 * consume el toque y no dispara [onOpen].
 */
@Composable
fun LoanCard(
    row: LoanRow,
    onOpen: () -> Unit,
    onRegisterPayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visuals = loanVisualsOf(row.loan.direction, row.summary)
    GastonomoCard(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onOpen, role = Role.Button),
        radius = GastonomoTheme.radii.loanCard,
        contentPadding = PaddingValues(CARD_PADDING),
    ) {
        LoanHeader(row = row, visuals = visuals)
        Highlight(row = row, visuals = visuals)
        val progress = stringResource(R.string.loan_progress_description, row.summary.percent)
        ProgressBar(
            fraction = progressOf(row.summary),
            fill = visuals.barColor,
            modifier = Modifier
                .padding(top = BAR_GAP)
                .semantics { contentDescription = progress },
        )
        if (row.summary.status.isOpen) {
            RegisterPaymentButton(onClick = onRegisterPayment)
        }
    }
}

@Composable
private fun LoanHeader(row: LoanRow, visuals: LoanVisuals) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
    ) {
        Avatar(
            initials = row.loan.person.take(1).uppercase(),
            size = GastonomoTheme.dimens.iconTile,
            background = visuals.avatarBackground,
            contentColor = visuals.avatarContent,
        )
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = row.loan.person,
                style = GastonomoTheme.typography.body,
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            GastonomoText(
                text = subtitleOf(row, visuals),
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.muted,
                maxLines = 1,
                modifier = Modifier.padding(top = SUBTITLE_GAP),
            )
        }
        StatusBadge(
            label = visuals.statusLabel,
            background = visuals.badge.background,
            contentColor = visuals.badge.content,
        )
    }
}

/** El bloque de abajo: el saldo a la izquierda y "de {original}" con el porcentaje a la derecha. */
@Composable
private fun Highlight(row: LoanRow, visuals: LoanVisuals) {
    val formats = GastonomoTheme.formats
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = HIGHLIGHT_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = visuals.highlightLabel,
                style = GastonomoTheme.typography.caption,
                color = GastonomoTheme.colors.muted,
                maxLines = 1,
            )
            GastonomoAmountText(
                text = highlightAmountOf(row.summary),
                style = GastonomoTheme.typography.amountCard,
                color = visuals.highlightColor,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            GastonomoText(
                text = stringResource(R.string.loan_of_original, formats.amount(row.summary.total)),
                style = GastonomoTheme.typography.caption,
                color = GastonomoTheme.colors.muted,
                maxLines = 1,
            )
            GastonomoText(
                text = stringResource(R.string.loan_percent, row.summary.percent),
                style = GastonomoTheme.typography.secondarySmall.copy(fontWeight = FontWeight.Bold),
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
                modifier = Modifier.padding(top = SUBTITLE_GAP),
            )
        }
    }
}

/** El boton con borde ink de la tarjeta. Es mas bajo que un boton secundario del diseno. */
@Composable
private fun RegisterPaymentButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(BUTTON_RADIUS)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BUTTON_GAP)
            .height(BUTTON_HEIGHT)
            .background(GastonomoTheme.colors.surface, shape)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.ink, shape)
            .pressable(onClick = onClick, role = Role.Button),
        horizontalArrangement = Arrangement.spacedBy(BUTTON_ICON_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoIcon(GastonomoIcons.ADD_CARD, size = BUTTON_ICON_SIZE, tint = GastonomoTheme.colors.ink)
        GastonomoText(
            text = stringResource(R.string.loan_register_payment),
            style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
    }
}

/** "Me debe - {nota} - {fecha}", con la nota solo si la hay. */
@Composable
private fun subtitleOf(row: LoanRow, visuals: LoanVisuals): String {
    val separator = stringResource(R.string.loan_separator)
    val date = GastonomoTheme.formats.dates.short(row.loan.openedOn)
    val note = row.loan.note.takeIf { it.isNotBlank() }
    return listOfNotNull(visuals.directionLabel, note, date).joinToString(separator)
}

private val CARD_PADDING = 16.dp
private val HEADER_GAP = 10.dp
private val SUBTITLE_GAP = 1.dp
private val HIGHLIGHT_GAP = 14.dp
private val BAR_GAP = 10.dp
private val BUTTON_GAP = 14.dp
private val BUTTON_HEIGHT = 42.dp
private val BUTTON_RADIUS = 13.dp
private val BUTTON_ICON_SIZE = 18.dp
private val BUTTON_ICON_GAP = 6.dp
