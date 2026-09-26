package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Las cuatro sombras del diseno, traducidas a la elevacion de Compose.
 *
 * La sombra CSS del handoff lleva desenfoque y desplazamiento propios y Compose solo
 * expone una elevacion, asi que cada valor es la elevacion que mas se parece al
 * desenfoque original, no una conversion exacta. Esta es la unica desviacion consciente
 * del sistema de diseno.
 */
@Immutable
class GastonomoShadows {
    /** FAB: `0 10px 24px -8px rgba(0,0,0,.4)`. */
    val fab: Dp = 12.dp

    /** Toast: `0 12px 30px -10px rgba(0,0,0,.5)`. */
    val toast: Dp = 16.dp

    /** Segmento activo del segmented control: `0 1px 3px rgba(0,0,0,.08)`. */
    val activeSegment: Dp = 2.dp

    /** Handle del toggle: `0 1px 2px rgba(0,0,0,.2)`. */
    val toggleHandle: Dp = 1.dp
}
