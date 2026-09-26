package com.bahoque.gastonomo.feature.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La caja de informacion del formulario de prestamo: fondo de chip, icono `info` y el texto
 * que explica que la API tambien creara el movimiento.
 */
@Composable
fun InfoBox(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GastonomoTheme.colors.chipBg, RoundedCornerShape(RADIUS))
            .padding(horizontal = PADDING_X, vertical = PADDING_Y),
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.Top,
    ) {
        GastonomoIcon(GastonomoIcons.INFO, size = ICON_SIZE, tint = GastonomoTheme.colors.noteText)
        GastonomoText(
            text = text,
            style = GastonomoTheme.typography.secondary.copy(lineHeight = LINE_HEIGHT),
            color = GastonomoTheme.colors.noteText,
        )
    }
}

private val RADIUS = 14.dp
private val PADDING_X = 14.dp
private val PADDING_Y = 12.dp
private val GAP = 10.dp
private val ICON_SIZE = 18.dp
private val LINE_HEIGHT = 19.sp
