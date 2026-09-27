package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
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
 *
 * Con [pending] lleva la marca de que todavia no ha salido del telefono. Es deliberadamente
 * discreta: el movimiento ya cuenta para el balance, asi que no es un aviso, es una nota.
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
    pending: Boolean = false,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pending) {
                    GastonomoIcon(
                        name = GastonomoIcons.CLOUD_UPLOAD,
                        size = PENDING_ICON,
                        tint = GastonomoTheme.colors.muted,
                        contentDescription = stringResource(GastonomoStrings.pendingUpload),
                    )
                    Spacer(Modifier.width(PENDING_GAP))
                }
                GastonomoAmountText(
                    text = amount,
                    style = GastonomoTheme.typography.amount,
                    color = amountColor,
                )
            }
        },
    )
}

/** El padding de una fila dentro de una tarjeta de lista de movimientos. */
private val CARD_PADDING = 16.dp

private val PENDING_ICON = 16.dp
private val PENDING_GAP = 6.dp
