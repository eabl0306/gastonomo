package com.bahoque.gastonomo.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Lo que se ensena cuando una lectura falla: el motivo y un boton para reintentar.
 *
 * El diseno no lo dibuja, asi que se resuelve con los componentes que ya existen y con el
 * mismo aire de un estado vacio.
 */
@Composable
fun LoanProblemState(
    problem: LoanProblem,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = VERTICAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        GastonomoText(
            text = messageOf(problem),
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.muted,
        )
        SecondaryButton(
            text = stringResource(GastonomoStrings.actionRetry),
            onClick = onRetry,
            modifier = Modifier.width(BUTTON_WIDTH),
        )
    }
}

private val VERTICAL_PADDING = 28.dp
private val GAP = 12.dp
private val BUTTON_WIDTH = 200.dp
