package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToggle
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.ListRow
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Una fila de Ajustes: tile de 32 con el icono, etiqueta y un valor a la derecha. El valor
 * lleva chevron cuando la fila abre algo.
 */
@Composable
internal fun SettingsValueRow(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color = GastonomoTheme.colors.muted,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    ListRow(
        title = label,
        modifier = modifier,
        titleStyle = GastonomoTheme.typography.settingsLabel,
        showDivider = showDivider,
        onClick = onClick,
        leading = { SettingsIcon(icon) },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VALUE_GAP),
            ) {
                if (value != null) {
                    GastonomoText(
                        text = value,
                        style = GastonomoTheme.typography.bodySmall,
                        color = valueColor,
                        maxLines = 1,
                    )
                }
                if (showChevron) {
                    GastonomoIcon(
                        name = GastonomoIcons.CHEVRON_RIGHT,
                        size = CHEVRON_SIZE,
                        tint = GastonomoTheme.colors.faint,
                    )
                }
            }
        },
    )
}

/**
 * La fila del cerrojo local, la unica de Ajustes que lleva toggle.
 *
 * La fila entera cambia el toggle, no solo el control de 44x26: el minimo tactil del diseno
 * son 44 dp y el toggle mide 26 de alto.
 */
@Composable
internal fun SettingsToggleRow(
    icon: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    ListRow(
        title = label,
        modifier = modifier,
        titleStyle = GastonomoTheme.typography.settingsLabel,
        showDivider = showDivider,
        onClick = { onCheckedChange(!checked) },
        leading = { SettingsIcon(icon) },
        trailing = { GastonomoToggle(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun SettingsIcon(icon: String) {
    IconTile(
        background = GastonomoTheme.colors.iconTile,
        size = GastonomoTheme.dimens.iconTileTiny,
        radius = GastonomoTheme.radii.iconTileTiny,
    ) {
        GastonomoIcon(name = icon, size = ICON_SIZE, tint = GastonomoTheme.colors.ink)
    }
}

private val ICON_SIZE = 18.dp
private val CHEVRON_SIZE = 18.dp
private val VALUE_GAP = 6.dp
