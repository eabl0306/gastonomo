package com.bahoque.gastonomo.feature.auth.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** El divisor "o" que separa el boton de entrar del desbloqueo local. */
@Composable
internal fun AuthDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        DividerLine(Modifier.weight(1f))
        GastonomoText(
            text = label,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.faint,
        )
        DividerLine(Modifier.weight(1f))
    }
}

@Composable
private fun DividerLine(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(GastonomoTheme.dimens.divider)
            .background(GastonomoTheme.colors.inputBorder),
    )
}

/**
 * El fallo que no es de un campo: credenciales, red o limite de peticiones. Va sobre el
 * boton principal, en rojo y con el tamano de un error de campo.
 */
@Composable
internal fun AuthFormError(message: String, modifier: Modifier = Modifier) {
    GastonomoText(
        text = message,
        style = GastonomoTheme.typography.secondarySmall,
        color = GastonomoTheme.colors.red,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ERROR_BOTTOM_PADDING),
    )
}

private val GAP = 12.dp
private val ERROR_BOTTOM_PADDING = 10.dp
