package com.bahoque.gastonomo.feature.settings.pending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.EmptyState
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.component.SheetHeader
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.settings.R

/**
 * Los registros que el usuario hizo sin conexion y que todavia no han llegado a la nube.
 *
 * Existe por una razon concreta: **un registro que la API rechaza no puede desaparecer en
 * silencio**. Si desapareciera, el usuario creeria que gasto ese dinero y el gasto no estaria en
 * ninguna parte.
 *
 * Lo unico que se corrige aqui es el importe, que es lo que la API rechaza cuando rechaza algo.
 * Cambiar lo demas seria escribir otro registro, no corregir este.
 */
@Composable
fun PendingWritesScreen(
    state: PendingWritesUiState,
    actions: PendingWritesActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GastonomoAppBar(title = stringResource(R.string.pending_title), onNavigate = actions.onBack)
        GastonomoScreen {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GAP),
            ) {
                if (state.isEmpty) {
                    EmptyState(
                        icon = GastonomoIcons.CLOUD_DONE,
                        message = stringResource(R.string.pending_empty),
                    )
                } else {
                    state.writes.forEach { write ->
                        WriteCard(write = write, state = state, actions = actions)
                    }
                    SecondaryButton(
                        text = stringResource(R.string.pending_retry),
                        onClick = actions.onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (state.confirming != null) {
        DiscardConfirmation(actions = actions)
    }
}

@Composable
private fun WriteCard(
    write: PendingWrite,
    state: PendingWritesUiState,
    actions: PendingWritesActions,
) {
    GastonomoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(TEXT_GAP),
        ) {
            GastonomoText(
                text = stringResource(labelOf(write)),
                style = GastonomoTheme.typography.rowTitle,
                color = GastonomoTheme.colors.ink,
            )
            GastonomoText(
                text = GastonomoTheme.formats.amount(moneyOf(write).original),
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
            )
            GastonomoText(
                text = write.failure ?: stringResource(R.string.pending_waiting),
                style = GastonomoTheme.typography.secondarySmall,
                color = if (write.needsFixing) {
                    GastonomoTheme.colors.red
                } else {
                    GastonomoTheme.colors.muted
                },
            )
            if (state.editing == write.id) {
                AmountEditor(state = state, actions = actions)
            } else if (write.needsFixing) {
                BrokenActions(write = write, actions = actions)
            }
        }
    }
}

@Composable
private fun AmountEditor(state: PendingWritesUiState, actions: PendingWritesActions) {
    Column(verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
        GastonomoTextField(
            value = state.amount,
            onValueChange = actions.onAmount,
            label = stringResource(R.string.pending_amount),
            keyboardType = KeyboardType.Decimal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
            PrimaryButton(
                text = stringResource(R.string.pending_fix),
                onClick = actions.onSave,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(GastonomoStrings.actionCancel),
                onClick = actions.onCancelEdit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BrokenActions(write: PendingWrite, actions: PendingWritesActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
        PrimaryButton(
            text = stringResource(R.string.pending_fix),
            onClick = { actions.onEdit(write) },
            modifier = Modifier.weight(1f),
        )
        SecondaryButton(
            text = stringResource(R.string.pending_discard),
            onClick = { actions.onAskDiscard(write) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DiscardConfirmation(actions: PendingWritesActions) {
    GastonomoSheetHost(visible = true) {
        GastonomoBottomSheet(
            onDismiss = actions.onCancelDiscard,
        ) {
            SheetHeader(
                title = stringResource(R.string.pending_discard_title),
                subtitle = stringResource(R.string.pending_discard_body),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
                PrimaryButton(
                    text = stringResource(R.string.pending_discard),
                    onClick = actions.onConfirmDiscard,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(GastonomoStrings.actionCancel),
                    onClick = actions.onCancelDiscard,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val GAP = 12.dp
private val CARD_PADDING = 16.dp
private val TEXT_GAP = 8.dp
