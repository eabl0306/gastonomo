package com.bahoque.gastonomo.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.navigation.TopLevelDestination

/**
 * La pantalla 5, Inicio: balance del periodo, tendencia, acciones rapidas, prestamos, gastos
 * por categoria y ultimos movimientos.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero de
 * navegacion compartido.
 *
 * Inicio lleva a cinco sitios y a ninguno le importa de donde viene: Nuevo movimiento con el
 * tipo ya elegido, Nuevo prestamo, Prestamos, Movimientos y Ajustes. Las tres ultimas son
 * pantallas principales, asi que se cambia de tab y se vacia la pila.
 */
fun NavGraphBuilder.homeNavGraph(
    dependencies: HomeDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Home> {
        HomeRoute(
            dependencies = dependencies,
            onNewMovement = { kind -> actions.navigateTo(GastonomoRoute.NewMovement(kind.wire)) },
            onNewLoan = { actions.navigateTo(GastonomoRoute.NewLoan()) },
            onDictate = { actions.navigateTo(GastonomoRoute.Dictate) },
            onOpenLoans = { actions.switchTab(TopLevelDestination.LOANS) },
            onOpenMovements = { actions.switchTab(TopLevelDestination.MOVEMENTS) },
            onOpenSettings = { actions.switchTab(TopLevelDestination.SETTINGS) },
        )
    }
}
