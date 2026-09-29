package com.bahoque.gastonomo.feature.movements

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * Las pantallas 6 y 7: la lista de Movimientos agrupada por dia y el formulario de Nuevo
 * movimiento.
 *
 * El formulario se abre con un tipo ya elegido, que llega en la ruta: `out` desde el FAB y desde
 * la accion rapida Gasto, `in` desde la accion rapida Ingreso.
 */
fun NavGraphBuilder.movementsNavGraph(
    dependencies: MovementsDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Movements> {
        MovementsRoute(
            dependencies = dependencies,
            onNewMovement = {
                actions.navigateTo(GastonomoRoute.NewMovement(MovementKind.OUT.wire))
            },
            onNewLoan = { actions.navigateTo(GastonomoRoute.NewLoan()) },
            onDictate = { actions.navigateTo(GastonomoRoute.Dictate) },
        )
    }
    composable<GastonomoRoute.NewMovement> { entry ->
        val route = entry.toRoute<GastonomoRoute.NewMovement>()
        NewMovementRoute(
            dependencies = dependencies,
            kind = MovementKind.fromWire(route.kind) ?: MovementKind.OUT,
            onClose = { actions.back() },
            prefill = MovementPrefill.of(route),
        )
    }
}
