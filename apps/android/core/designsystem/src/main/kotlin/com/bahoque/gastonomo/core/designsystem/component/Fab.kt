package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El FAB del diseno: ink, alto 56, radio 20, icono `add` en lime mas una palabra.
 *
 * Quien lo usa lo coloca abajo a la derecha (right 20, bottom 92) dentro del Box de la
 * pantalla; el composable no se posiciona solo.
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     // contenido
 *     GastonomoFab(
 *         text = stringResource(R.string.new_movement_short),
 *         onClick = ::openForm,
 *         modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp),
 *     )
 * }
 * ```
 */
@Composable
fun GastonomoFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String = GastonomoIcons.ADD,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.fab)
    Row(
        modifier = modifier
            .height(GastonomoTheme.dimens.fab)
            .shadow(GastonomoTheme.shadows.fab, shape)
            .background(GastonomoTheme.colors.ink, shape)
            .pressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        GastonomoIcon(icon, size = ICON_SIZE, tint = GastonomoTheme.colors.lime)
        GastonomoText(
            text = text,
            style = GastonomoTheme.typography.body,
            color = GastonomoTheme.colors.surface,
            maxLines = 1,
        )
    }
}

private val HORIZONTAL_PADDING = 18.dp
private val ICON_SIZE = 22.dp
private val GAP = 8.dp
