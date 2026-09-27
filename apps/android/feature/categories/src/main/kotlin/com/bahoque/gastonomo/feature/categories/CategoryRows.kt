package com.bahoque.gastonomo.feature.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.ListRow
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.component.tintOfCategoryColor
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.category.Category

/** Una categoria del usuario: tile, nombre, uso y los botones de editar y quitar. */
@Composable
internal fun MyCategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = category.name,
        modifier = modifier,
        subtitle = usesLabel(category.movementCount),
        showDivider = showDivider,
        minHeight = GastonomoTheme.dimens.listRowTall,
        // El diseno sangra esta tarjeta 8 por la derecha: los botones ya ocupan su sitio.
        endPadding = ACTIONS_PADDING,
        leading = { CategoryTile(category) },
        trailing = {
            RowAction(GastonomoIcons.EDIT, stringResource(GastonomoStrings.actionEdit), onEdit)
            RowAction(
                icon = GastonomoIcons.DELETE,
                description = stringResource(GastonomoStrings.actionDelete),
                onClick = onArchive,
                tint = GastonomoTheme.colors.red,
            )
        },
    )
}

/** Una de las 12 predeterminadas: se ensena igual, pero con un candado en vez de botones. */
@Composable
internal fun DefaultCategoryRow(
    category: Category,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = category.name,
        modifier = modifier,
        subtitle = usesLabel(category.movementCount),
        showDivider = showDivider,
        minHeight = GastonomoTheme.dimens.listRowMedium,
        leading = { CategoryTile(category) },
        trailing = {
            GastonomoIcon(
                name = GastonomoIcons.LOCK,
                size = LOCK_SIZE,
                tint = GastonomoTheme.colors.radioOff,
                contentDescription = stringResource(R.string.categories_locked),
            )
        },
    )
}

/**
 * Una archivada: se ensena apagada y con un solo boton, recuperar.
 *
 * No lleva editar: mientras esta archivada no se ofrece, asi que cambiarle el nombre o el color no
 * cambiaria nada de lo que el usuario ve. Se recupera y luego se edita.
 */
@Composable
internal fun ArchivedCategoryRow(
    category: Category,
    onRestore: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = category.name,
        modifier = modifier,
        subtitle = usesLabel(category.movementCount),
        showDivider = showDivider,
        minHeight = GastonomoTheme.dimens.listRowTall,
        endPadding = ACTIONS_PADDING,
        leading = { CategoryTile(category) },
        trailing = {
            RowAction(
                icon = GastonomoIcons.UNARCHIVE,
                description = stringResource(R.string.categories_restore),
                onClick = onRestore,
            )
        },
    )
}

/** El tile de 38 con el icono de la categoria sobre su color al 9 por ciento. */
@Composable
internal fun CategoryTile(category: Category, modifier: Modifier = Modifier) {
    val color = categoryColorOf(category.color)
    IconTile(
        background = tintOfCategoryColor(color),
        modifier = modifier,
        size = GastonomoTheme.dimens.iconTileSmall,
        radius = GastonomoTheme.radii.iconTileSmall,
    ) {
        GastonomoIcon(name = category.icon, size = TILE_ICON_SIZE, tint = color)
    }
}

/** "Sin movimientos", "1 movimiento" o "N movimientos". */
@Composable
internal fun usesLabel(count: Int): String =
    if (count == 0) {
        stringResource(R.string.categories_uses_none)
    } else {
        pluralStringResource(R.plurals.categories_uses, count, count)
    }

@Composable
private fun RowAction(
    icon: String,
    description: String,
    onClick: () -> Unit,
    tint: Color = GastonomoTheme.colors.muted,
) {
    Box(
        modifier = Modifier
            .size(ACTION_SIZE)
            .pressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(name = icon, tint = tint, contentDescription = description)
    }
}

private val ACTIONS_PADDING = 8.dp
private val TILE_ICON_SIZE = 20.dp
private val LOCK_SIZE = 18.dp
private val ACTION_SIZE = 44.dp
