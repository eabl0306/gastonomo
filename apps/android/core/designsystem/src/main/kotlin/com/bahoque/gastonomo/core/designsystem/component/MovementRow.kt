package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La fila de un movimiento: el tile de su categoria, el titulo con su subtitulo y el importe
 * con signo a la derecha.
 *
 * Es la misma en los "Ultimos movimientos" de Inicio y en la lista de Movimientos; lo unico
 * que cambia es el subtitulo, que en Inicio lleva tambien la fecha. El importe va en verde
 * cuando es un ingreso y en ink cuando es un gasto, que es lo que pide el diseno: el rojo se
 * reserva para el resumen y para el monto de un formulario.
 *
 * ```
 * MovementRow(
 *     title = movement.title,
 *     subtitle = movement.category.name,
 *     categoryIcon = movement.category.icon,
 *     categoryColor = categoryColorOf(movement.category.color),
 *     amount = formats.signed(shown, movement.kind.isIncome, displayCurrency),
 *     amountColor = if (movement.kind.isIncome) colors.green else colors.ink,
 *     showDivider = index != last,
 * )
 * ```
 */
@Composable
fun MovementRow(
    title: String,
    subtitle: String,
    categoryIcon: String,
    categoryColor: Color,
    amount: String,
    amountColor: Color,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = CARD_PADDING,
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        titleStyle = GastonomoTheme.typography.rowTitle,
        showDivider = showDivider,
        horizontalPadding = horizontalPadding,
        leading = {
            IconTile(background = tintOfCategoryColor(categoryColor)) {
                GastonomoIcon(name = categoryIcon, tint = categoryColor)
            }
        },
        trailing = {
            GastonomoAmountText(
                text = amount,
                style = GastonomoTheme.typography.amount,
                color = amountColor,
            )
        },
    )
}

/** El padding de una fila dentro de una tarjeta de lista de movimientos. */
private val CARD_PADDING = 16.dp
