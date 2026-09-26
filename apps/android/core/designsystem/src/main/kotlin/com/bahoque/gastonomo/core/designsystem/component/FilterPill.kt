package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoFontFamily
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Un chip de filtro: alto 36 y radio 18. Activo va en ink con texto blanco; inactivo en
 * blanco con borde `line` y texto ink.
 *
 * ```
 * FilterPillRow {
 *     FilterPill(all, selected = filter == ALL) { onFilter(ALL) }
 *     FilterPill(income, selected = filter == IN) { onFilter(IN) }
 * }
 * ```
 *
 * @param compact el chip de 13 px y padding 14 de los presets del sheet de rango y de
 *   Exportar. Los filtros de una pantalla usan 14 px y padding 16.
 */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.pill)
    Box(
        modifier = modifier
            .height(GastonomoTheme.dimens.pill)
            .clip(shape)
            .background(if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.surface)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.line, shape)
                },
            )
            .pressable(onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = if (compact) COMPACT_PADDING else HORIZONTAL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = label,
            style = if (compact) COMPACT_LABEL else LABEL,
            color = if (selected) GastonomoTheme.colors.surface else GastonomoTheme.colors.ink,
            maxLines = 1,
        )
    }
}

/**
 * La fila de pills, con la separacion del diseno. Si no caben, pasan a la linea siguiente:
 * el diseno las pone en un contenedor con `flex-wrap`, y una pill como
 * "Periodo actual . Septiembre 2026" no cabe al lado de otra en un telefono estrecho.
 */
@Composable
fun FilterPillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        content()
    }
}

private val LABEL = TextStyle(
    fontFamily = GastonomoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
)
private val COMPACT_LABEL = LABEL.copy(fontSize = 13.sp)

private val HORIZONTAL_PADDING = 16.dp
private val COMPACT_PADDING = 14.dp
private val GAP = 6.dp
