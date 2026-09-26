package com.bahoque.gastonomo.feature.movements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.component.tintOfCategoryColor
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * El grid de categorias del formulario: cuatro columnas de celdas de 78.
 *
 * La celda elegida se vuelve blanca, con borde ink de 1,5 y el tile relleno del color de la
 * categoria con el icono en blanco; las demas van en `bgNav` con borde `line` y el tile al 9 por
 * ciento de su color.
 *
 * Se dibuja a mano en filas de cuatro y no con un grid perezoso porque va dentro de una pantalla
 * que ya se desplaza: el grid tendria que medirse dos veces y el formulario no lo necesita.
 */
@Composable
internal fun CategoryGrid(
    categories: List<Category>,
    selected: CategoryId?,
    onSelect: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        categories.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GAP),
            ) {
                row.forEach { category ->
                    CategoryCell(
                        category = category,
                        selected = category.id == selected,
                        onClick = { onSelect(category.id) },
                    )
                }
                // La ultima fila se rellena para que las celdas no se estiren.
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RowScope.CategoryCell(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = categoryColorOf(category.color)
    val shape = RoundedCornerShape(GastonomoTheme.radii.control)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(GastonomoTheme.dimens.categoryCell)
            .clip(shape)
            .background(
                if (selected) GastonomoTheme.colors.surface else GastonomoTheme.colors.bgNav,
            )
            .border(
                width = GastonomoTheme.dimens.selectedBorder,
                color = if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.line,
                shape = shape,
            )
            .pressable(onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = CELL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(
            background = if (selected) color else tintOfCategoryColor(color),
            size = TILE_SIZE,
            radius = GastonomoTheme.radii.iconTileTiny,
        ) {
            GastonomoIcon(
                name = category.icon,
                size = TILE_ICON,
                tint = if (selected) GastonomoTheme.colors.surface else color,
            )
        }
        GastonomoText(
            text = category.name,
            modifier = Modifier.padding(top = LABEL_GAP),
            style = GastonomoTheme.typography.trendLabel.copy(fontWeight = FontWeight.SemiBold),
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
    }
}

/** Las cuatro columnas del diseno. */
private const val COLUMNS = 4

private val GAP = 8.dp
private val TILE_SIZE = 30.dp
private val TILE_ICON = 18.dp
private val LABEL_GAP = 6.dp
private val CELL_PADDING = 4.dp
