package com.bahoque.gastonomo.feature.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La hoja del dictado, con sus cuatro fases: escuchando, escrito, interpretando y revisar.
 *
 * Es una hoja y no una pantalla llena porque el diseno la quiere asi, y va dentro de un
 * [GastonomoSheetHost] porque una hoja quitada con un `if` no puede animar su salida.
 *
 * @param opened false en el primer fotograma, para que la hoja se vea subir. `AnimatedVisibility`
 *   no anima una primera composicion que ya llega visible, asi que con true de entrada el panel
 *   aparecia de golpe. En las capturas se pasa true y sale la hoja entera, que es lo que se quiere.
 */
@Composable
fun DictateSheet(
    state: DictateUiState,
    actions: DictateActions,
    modifier: Modifier = Modifier,
    opened: Boolean = true,
) {
    GastonomoSheetHost(visible = opened && !state.closing) {
        GastonomoBottomSheet(onDismiss = actions.onClose, modifier = modifier) {
            DictateHeader(
                title = stringResource(
                    if (state.phase == DictationPhase.REVIEW) {
                        R.string.dictation_review_title
                    } else {
                        R.string.dictation_title
                    },
                ),
                onClose = actions.onClose,
            )
            Phase(state, actions)
        }
    }
}

/** La fase que toca. Cada una es un bloque con su propio fichero. */
@Composable
private fun ColumnScope.Phase(state: DictateUiState, actions: DictateActions) {
    when (state.phase) {
        DictationPhase.LISTENING -> ListeningBlock(state = state, actions = actions)
        DictationPhase.TYPING -> TypingBlock(state = state, actions = actions)
        DictationPhase.PROCESSING -> ProcessingBlock(text = state.text)
        DictationPhase.REVIEW -> ReviewBlock(state = state, actions = actions)
    }
    // Los ejemplos solo salen antes de interpretar: con el borrador delante estorban.
    if (state.phase == DictationPhase.LISTENING || state.phase == DictationPhase.TYPING) {
        ExamplesBlock(onPick = actions.onInterpret)
    }
}

/** El titulo de la hoja y el boton de cerrar, que es lo que la corona en las cuatro fases. */
@Composable
private fun DictateHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GastonomoText(
            text = title,
            style = GastonomoTheme.typography.sheetTitle,
            color = GastonomoTheme.colors.ink,
        )
        GastonomoIcon(
            name = GastonomoIcons.CLOSE,
            modifier = Modifier
                .size(GastonomoTheme.dimens.minTouchTarget)
                .padding(CLOSE_PADDING)
                .pressable(onClick = onClose),
            tint = GastonomoTheme.colors.ink,
            contentDescription = stringResource(GastonomoStrings.actionClose),
        )
    }
    Spacer(Modifier.height(HEADER_GAP))
}

/** El area que se toca para cerrar es de 44: el icono se queda en 24 dentro de ella. */
private val CLOSE_PADDING = 10.dp
private val HEADER_GAP = 8.dp
