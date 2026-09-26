package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTypography
import com.bahoque.gastonomo.core.domain.loan.LoanStatus

/**
 * La etiqueta de estado de un prestamo: Pagado en verde, Parcial en gris y Pendiente en
 * rojo, cada uno con su fondo.
 *
 * Los colores no se eligen aqui: los da [statusColorsOf] a partir del estado del dominio,
 * para que las tres pantallas que ensenan un estado usen exactamente los mismos.
 */
@Composable
fun StatusBadge(
    label: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(RADIUS))
            .padding(
                horizontal = if (large) LARGE_HORIZONTAL_PADDING else HORIZONTAL_PADDING,
                vertical = if (large) LARGE_VERTICAL_PADDING else VERTICAL_PADDING,
            ),
    ) {
        GastonomoText(
            text = label,
            style = if (large) LARGE_LABEL else LABEL,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** La etiqueta de una tarjeta de prestamo: 11 en negrita. */
private val LABEL = GastonomoTypography().caption.copy(fontWeight = FontWeight.Bold)

/** La del detalle, un punto mas grande. */
private val LARGE_LABEL = GastonomoTypography().secondarySmall.copy(fontWeight = FontWeight.Bold)

/** El par de colores (fondo, texto) de un estado. */
data class BadgeColors(val background: Color, val content: Color)

/** Los colores de los tres estados de un prestamo. */
@Composable
fun statusColorsOf(status: LoanStatus): BadgeColors {
    val colors = GastonomoTheme.colors
    return when (status) {
        LoanStatus.PAID ->
            BadgeColors(colors.greenBg, colors.green)
        LoanStatus.PARTIAL ->
            BadgeColors(colors.partialBg, colors.noteText)
        LoanStatus.PENDING ->
            BadgeColors(colors.redBg, colors.red)
    }
}

private val RADIUS = 8.dp
private val HORIZONTAL_PADDING = 9.dp
private val LARGE_HORIZONTAL_PADDING = 10.dp
private val LARGE_VERTICAL_PADDING = 5.dp
private val VERTICAL_PADDING = 5.dp
