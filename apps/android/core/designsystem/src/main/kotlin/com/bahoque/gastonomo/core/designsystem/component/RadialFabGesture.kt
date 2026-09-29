package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope

/** Ninguna opcion resaltada. */
internal const val NO_OPTION = -1

/** Lo que el menu radial recuerda: si esta abierto y sobre que opcion esta el dedo. */
@Stable
internal class RadialFabState {
    var open: Boolean by mutableStateOf(false)
    var hovered: Int by mutableIntStateOf(NO_OPTION)

    fun close() {
        open = false
        hovered = NO_OPTION
    }
}

/**
 * Cuenta el menu abierto como una hoja mas.
 *
 * La tab bar se pinta fuera de la pantalla y por encima, asi que el velo del menu no la tapa. Sin
 * esto quedaba encendida y pulsable con el menu delante.
 */
@Composable
internal fun DimTabBarWhileOpen(open: Boolean) {
    val sheets = LocalOpenSheets.current
    DisposableEffect(sheets, open) {
        if (open) sheets?.opened()
        onDispose { if (open) sheets?.closed() }
    }
}

/**
 * Lo que hace falta para leer el gesto, ya en pixeles.
 *
 * @property center medio lado del boton, para medir desde su centro y no desde su esquina.
 * @property selection cuanto hay que alejarse para que empiece a contar una opcion.
 */
internal data class RadialMetrics(val center: Float, val selection: Float, val count: Int)

/**
 * El gesto entero: abrir al tocar, resaltar segun el angulo y decidir al soltar.
 *
 * Lo que distingue las dos formas de usarlo es [Tracking.dragged]: si el dedo casi no se movio,
 * levantar fue un toque y el menu se queda abierto para elegir con un segundo toque. Si se movio,
 * fue un gesto, y soltar fuera del arco cierra sin hacer nada.
 */
internal suspend fun PointerInputScope.radialGestures(
    state: RadialFabState,
    metrics: RadialMetrics,
    onSelect: (Int) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    val wasOpen = state.open
    state.open = true
    state.hovered = NO_OPTION
    val tracking = track(down.id, state, metrics)
    val chosen = tracking.selected
    state.close()
    when {
        chosen != NO_OPTION -> onSelect(chosen)
        // Un toque limpio con el menu cerrado lo deja abierto: es el menu de dos toques.
        !tracking.dragged && !wasOpen -> state.open = true
        else -> Unit
    }
}

/** Como acabo el recorrido del dedo. */
internal data class Tracking(val dragged: Boolean, val selected: Int)

/** Sigue al dedo hasta que levanta, resaltando la opcion a la que apunta. */
private suspend fun AwaitPointerEventScope.track(
    pointer: PointerId,
    state: RadialFabState,
    metrics: RadialMetrics,
): Tracking {
    var dragged = false
    var selected = NO_OPTION
    var pressed = true
    while (pressed) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
        if (change == null) {
            pressed = false
        } else {
            val offset = Offset(
                change.position.x - metrics.center,
                change.position.y - metrics.center,
            )
            if (RadialFabGeometry.dragged(offset)) dragged = true
            selected =
                RadialFabGeometry.selected(offset, metrics.count, metrics.selection) ?: NO_OPTION
            state.hovered = selected
            pressed = change.pressed
        }
    }
    return Tracking(dragged, selected)
}
