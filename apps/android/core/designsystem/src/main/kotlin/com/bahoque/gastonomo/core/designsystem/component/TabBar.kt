package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La tab bar del diseno: 78 dp sobre `bgNav`. El tab activo lleva una pildora lime de
 * 56x30 detras del icono, el icono en FILL 1 y el label en ink 700.
 *
 * Solo sale en Inicio, Movimientos, Prestamos y Ajustes. Las pantallas secundarias no la
 * llevan, y cambiar de tab vacia la pila de navegacion.
 *
 * ```
 * GastonomoTabBar(items = tabs, selectedIndex = current, onSelect = navController::switchTab)
 * ```
 */
@Composable
fun GastonomoTabBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBorder = GastonomoTheme.colors.line
    val borderWidth = with(LocalDensity.current) { GastonomoTheme.dimens.divider.toPx() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GastonomoTheme.colors.bgNav)
            .height(GastonomoTheme.dimens.tabBar)
            // El diseno separa la barra del contenido con una linea de 1, no con el color.
            .drawBehind {
                drawLine(
                    color = topBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = borderWidth,
                )
            }
            .padding(top = TOP_PADDING, bottom = BOTTOM_PADDING, start = SIDE_PADDING, end = SIDE_PADDING),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            TabCell(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabCell(
    item: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.pressable(onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LABEL_GAP),
    ) {
        Box(
            modifier = Modifier
                .size(width = GastonomoTheme.dimens.tabPillWidth, height = GastonomoTheme.dimens.tabPillHeight)
                .then(
                    if (selected) {
                        Modifier.background(
                            GastonomoTheme.colors.lime,
                            RoundedCornerShape(GastonomoTheme.radii.pill),
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            GastonomoIcon(
                name = item.icon,
                size = ICON_SIZE,
                tint = if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.muted,
                filled = selected,
            )
        }
        GastonomoText(
            text = item.label,
            style = if (selected) GastonomoTheme.typography.tabActive else GastonomoTheme.typography.tabInactive,
            color = if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.muted,
            maxLines = 1,
        )
    }
}

private val TOP_PADDING = 6.dp
private val BOTTOM_PADDING = 14.dp
private val SIDE_PADDING = 8.dp
private val ICON_SIZE = 22.dp
private val LABEL_GAP = 3.dp
