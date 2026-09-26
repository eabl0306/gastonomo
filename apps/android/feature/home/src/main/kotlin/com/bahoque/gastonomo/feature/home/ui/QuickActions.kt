package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.feature.home.R

/**
 * Las cuatro acciones rapidas. Tres abren un formulario y la de Abono lleva a Prestamos, que
 * es donde se elige a quien se le abona: un abono no existe sin prestamo.
 */
@Composable
internal fun QuickActions(
    onNewMovement: (MovementKind) -> Unit,
    onNewLoan: () -> Unit,
    onOpenLoans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GastonomoTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        QuickAction(
            label = stringResource(GastonomoStrings.kindIncome),
            icon = GastonomoIcons.SOUTH_WEST,
            tint = colors.green,
            background = colors.greenBg,
            onClick = { onNewMovement(MovementKind.IN) },
        )
        QuickAction(
            label = stringResource(GastonomoStrings.kindExpense),
            icon = GastonomoIcons.NORTH_EAST,
            tint = colors.red,
            background = colors.redBg,
            onClick = { onNewMovement(MovementKind.OUT) },
        )
        QuickAction(
            label = stringResource(R.string.home_quick_loan),
            icon = GastonomoIcons.HANDSHAKE,
            tint = colors.blue,
            background = colors.blueBg,
            onClick = onNewLoan,
        )
        QuickAction(
            label = stringResource(R.string.home_quick_payment),
            icon = GastonomoIcons.ADD_CARD,
            tint = colors.amber,
            background = colors.amberBg,
            onClick = onOpenLoans,
        )
    }
}

@Composable
private fun RowScope.QuickAction(
    label: String,
    icon: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.smallCard)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(GastonomoTheme.dimens.quickAction)
            .clip(shape)
            .background(GastonomoTheme.colors.surface)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.line, shape)
            .pressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = LABEL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(
            background = background,
            size = TILE_SIZE,
            radius = GastonomoTheme.radii.iconTileTiny,
        ) {
            GastonomoIcon(name = icon, tint = tint)
        }
        GastonomoText(
            text = label,
            modifier = Modifier.padding(top = LABEL_GAP),
            style = GastonomoTheme.typography.labelStrong,
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
    }
}

private val GAP = 8.dp
private val TILE_SIZE = 32.dp
private val LABEL_GAP = 6.dp
private val LABEL_PADDING = 4.dp
