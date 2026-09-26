package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La fila de lista del diseno: tile de icono, titulo con subtitulo y valor a la derecha,
 * con divisor debajo salvo en la ultima.
 *
 * El tile y el valor son slots, porque cada pantalla pinta cosas distintas ahi: un icono
 * de categoria, un avatar con inicial, un monto, un chevron o un par de botones.
 *
 * [endPadding] es para la tarjeta de "Mis categorias", la unica que el diseno sangra
 * distinto por la derecha (8) porque sus dos botones ya traen su propio margen.
 *
 * ```
 * ListRow(
 *     title = movement.title,
 *     subtitle = subtitle,
 *     showDivider = index != last,
 *     leading = { IconTile(tint) { GastonomoIcon(category.icon, tint = color) } },
 *     trailing = { GastonomoAmountText(amount, style = typography.amount) },
 * )
 * ```
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = GastonomoTheme.typography.body,
    showDivider: Boolean = true,
    minHeight: Dp = GastonomoTheme.dimens.listRow,
    horizontalPadding: Dp = ROW_PADDING,
    endPadding: Dp = horizontalPadding,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
                .padding(
                    start = horizontalPadding,
                    end = endPadding,
                    top = VERTICAL_PADDING,
                    bottom = VERTICAL_PADDING,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                GastonomoText(
                    text = title,
                    style = titleStyle,
                    color = GastonomoTheme.colors.ink,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    GastonomoText(
                        text = subtitle,
                        style = GastonomoTheme.typography.secondarySmall,
                        color = GastonomoTheme.colors.muted,
                        maxLines = 1,
                    )
                }
            }
            trailing?.invoke(this)
        }
        if (showDivider) {
            RowDivider(horizontalPadding = horizontalPadding, endPadding = endPadding)
        }
    }
}

/** El divisor de dentro de una tarjeta, sangrado como en el diseno. */
@Composable
fun RowDivider(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ROW_PADDING,
    endPadding: Dp = horizontalPadding,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = endPadding)
            .height(GastonomoTheme.dimens.divider)
            .background(GastonomoTheme.colors.divider),
    )
}

/** Padding horizontal de una fila dentro de una tarjeta: el `padding:0 16px` del diseno. */
val ROW_PADDING = 16.dp

private val VERTICAL_PADDING = 12.dp
private val GAP = 12.dp
