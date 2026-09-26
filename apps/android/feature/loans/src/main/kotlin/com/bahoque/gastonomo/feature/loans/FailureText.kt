package com.bahoque.gastonomo.feature.loans

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El fallo de un guardado, en rojo y debajo del boton.
 *
 * Los mensajes que vienen de la API ya estan en castellano y se pintan tal cual; los
 * generales los redacta el cliente. Los de campo los ensena cada campo por su cuenta, asi
 * que aqui solo salen los que no tienen un campo donde ponerse.
 */
@Composable
fun FailureText(failure: LoanSaveFailure, modifier: Modifier = Modifier) {
    val message = failure.detail ?: failure.problem?.let { messageOf(it) } ?: return
    GastonomoText(
        text = message,
        modifier = modifier.fillMaxWidth(),
        style = GastonomoTheme.typography.secondarySmall,
        color = GastonomoTheme.colors.red,
    )
}
