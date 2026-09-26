package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El estado vacio del diseno: un icono en `radioOff` y una linea de texto en muted.
 *
 * ```
 * EmptyState(icon = GastonomoIcons.RECEIPT_LONG, message = stringResource(R.string.no_movements))
 * ```
 */
@Composable
fun EmptyState(
    icon: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VERTICAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        GastonomoIcon(icon, size = ICON_SIZE, tint = GastonomoTheme.colors.radioOff)
        GastonomoText(
            text = message,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.muted,
        )
    }
}

private val ICON_SIZE = 32.dp
private val VERTICAL_PADDING = 28.dp
private val GAP = 10.dp
