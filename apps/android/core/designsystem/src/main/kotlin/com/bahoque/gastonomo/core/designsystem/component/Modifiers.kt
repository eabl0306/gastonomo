package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * La respuesta al toque del diseno: `transform: scale(.98)` mientras el dedo esta abajo.
 * No hay borroso circular de Material en ninguna pantalla.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = PRESSED_SCALE,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "pressScale",
    )
    scale(scale)
}

/**
 * Un area pulsable con el `scale(.98)` del diseno y sin ningun otro efecto.
 *
 * @param enabled false deja el control visible y pulsable pero sin efecto, que es lo que
 *   pide el diseno para el boton primario deshabilitado: opacidad 0.4 y nada mas.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: androidx.compose.ui.semantics.Role? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Escala del control pulsado. */
const val PRESSED_SCALE = 0.98f

/** Opacidad de un control deshabilitado. */
const val DISABLED_ALPHA = 0.4f
