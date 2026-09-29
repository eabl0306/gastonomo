package com.bahoque.gastonomo.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavOptionsBuilder

/**
 * Las dos formas de navegar del diseno, para que ninguna feature se las invente.
 *
 * - **Cambiar de tab vacia la pila.** Se va al tab y se quita todo lo que hubiera encima,
 *   asi que volver a un tab nunca deja una pantalla secundaria a medias. Tocar el tab en el
 *   que ya se esta **no hace nada**.
 * - **Una pantalla secundaria se apila.** Sale con `arrow_back`, o con `close` si es un
 *   formulario de creacion.
 *
 * ```
 * navController.switchTab(TopLevelDestination.LOANS)       // desde la tab bar
 * navController.navigate(GastonomoRoute.NewLoan)           // desde el FAB de Prestamos
 * navController.replaceWith(GastonomoRoute.Home)           // al entrar, quitando Login
 * ```
 */
fun NavController.switchTab(destination: TopLevelDestination) {
    // Ya estamos ahi: no se navega. Volver a navegar al mismo sitio vacia la pila y vuelve a
    // crear la pantalla, con su ViewModel de cero, asi que se ve el parpadeo de la recarga y se
    // pierde donde estaba el scroll. La tab bar solo sale en las cuatro principales, asi que
    // desde ella nunca hay una pantalla apilada que hubiera que quitar.
    if (currentDestination?.hasRoute(destination.route::class) == true) return
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Sustituye toda la pila por [route]. Es lo que se hace al entrar (Login o PIN se van) y
 * al cerrar sesion (se va todo lo de dentro).
 */
fun NavController.replaceWith(route: GastonomoRoute, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
        builder()
    }
}
