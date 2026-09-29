package com.bahoque.gastonomo.feature.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La tarjeta de revision: lo que se entendio, y Guardar o Dictar de nuevo.
 *
 * **Nada esta creado todavia.** Guardar es la primera escritura, y pasa por el mismo caso de uso
 * que el formulario con su clave de idempotencia.
 *
 * Guardar se apaga cuando falta el importe o la persona. Quien decide que falta es la API, no esta
 * pantalla: asi el movil y la web no pueden discrepar sobre cuando se puede guardar.
 */
@Composable
internal fun ReviewBlock(state: DictateUiState, actions: DictateActions) {
    val draft = state.draft ?: return
    Column(modifier = Modifier.fillMaxWidth()) {
        Quote(state.text)
        Spacer(Modifier.height(CARD_GAP))
        DraftCard(state = state, draft = draft)
        state.blocker?.let { blocker ->
            NoticeLine(
                icon = GastonomoIcons.INFO,
                text = stringResource(messageOf(blocker)),
                tint = GastonomoTheme.colors.red,
            )
        }
        state.problem?.let { problem ->
            NoticeLine(
                icon = GastonomoIcons.INFO,
                text = messageOf(problem),
                tint = GastonomoTheme.colors.red,
            )
        }
        Spacer(Modifier.height(BUTTON_GAP))
        PrimaryButton(
            text = stringResource(R.string.dictation_save),
            onClick = actions.onSave,
            enabled = state.canSave,
        )
        Spacer(Modifier.height(GastonomoTheme.dimens.cardGapTight))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGapTight),
        ) {
            SecondaryButton(
                text = stringResource(R.string.dictation_edit),
                onClick = actions.onEdit,
                modifier = Modifier.weight(1f),
                icon = GastonomoIcons.EDIT,
            )
            SecondaryButton(
                text = stringResource(R.string.dictation_redo),
                onClick = actions.onRedo,
                modifier = Modifier.weight(1f),
                icon = GastonomoIcons.MIC,
            )
        }
    }
}

/** El mensaje de lo que impide guardar, que dice tambien como arreglarlo. */
private fun messageOf(blocker: DictationBlocker): Int = when (blocker) {
    DictationBlocker.MISSING_AMOUNT -> R.string.dictation_missing_amount
    DictationBlocker.MISSING_PERSON -> R.string.dictation_missing_person
    DictationBlocker.NO_RATE -> R.string.dictation_no_rate
}

private val CARD_GAP = 14.dp
private val BUTTON_GAP = 16.dp

/** El padding interior de la tarjeta del borrador. */
internal val CARD_PADDING = 16.dp
