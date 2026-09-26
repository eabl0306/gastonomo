package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El checkbox del diseno: 22x22 con radio 7 y borde ink. Marcado se rellena de ink con el
 * check en lime.
 */
@Composable
fun GastonomoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.checkbox)
    Box(
        modifier = modifier
            .size(GastonomoTheme.dimens.checkbox)
            .background(if (checked) GastonomoTheme.colors.ink else GastonomoTheme.colors.surface, shape)
            .border(BORDER, GastonomoTheme.colors.ink, shape)
            .pressable(onClick = { onCheckedChange(!checked) }, role = Role.Checkbox),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            GastonomoIcon(GastonomoIcons.CHECK, size = CHECK_SIZE, tint = GastonomoTheme.colors.lime)
        }
    }
}

/**
 * El toggle del diseno: 44x26, encendido en ink, handle blanco de 20 con su sombra.
 */
@Composable
fun GastonomoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val travel = GastonomoTheme.dimens.toggleWidth -
        GastonomoTheme.dimens.toggleHandle - HANDLE_INSET * 2
    val offset = animateDpAsState(if (checked) travel else 0.dp, label = "toggleHandle")
    Box(
        modifier = modifier
            .width(GastonomoTheme.dimens.toggleWidth)
            .height(GastonomoTheme.dimens.toggleHeight)
            .background(
                if (checked) GastonomoTheme.colors.ink else GastonomoTheme.colors.toggleOff,
                CircleShape,
            )
            .pressable(onClick = { onCheckedChange(!checked) }, role = Role.Switch),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                // La version con lambda lee el estado en la fase de layout: asi mover el
                // handle no recompone la fila entera de Ajustes.
                .offset { IntOffset(x = (HANDLE_INSET + offset.value).roundToPx(), y = 0) }
                .size(GastonomoTheme.dimens.toggleHandle)
                .shadow(GastonomoTheme.shadows.toggleHandle, CircleShape)
                .background(GastonomoTheme.colors.surface, CircleShape),
        )
    }
}

/**
 * El radio de las tarjetas-radio de Exportar y de Idioma: circulo de 22 con borde
 * `radioOff`, y relleno de ink con el check en lime al marcarse.
 *
 * El check va en lime y no en blanco porque es lo que hace el prototipo, que es la fuente
 * de verdad de los valores exactos.
 */
@Composable
fun GastonomoRadio(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(GastonomoTheme.dimens.checkbox)
            .background(if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.surface, CircleShape)
            .border(
                BORDER,
                if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.radioOff,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            GastonomoIcon(GastonomoIcons.CHECK, size = CHECK_SIZE, tint = GastonomoTheme.colors.lime)
        }
    }
}

private val BORDER = 1.5.dp
private val CHECK_SIZE = 14.dp
private val HANDLE_INSET = 3.dp
