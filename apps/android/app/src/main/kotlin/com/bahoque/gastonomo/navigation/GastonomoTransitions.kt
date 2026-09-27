package com.bahoque.gastonomo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoMotion
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.navigation.TopLevelDestination

/** Que papel tiene una pantalla en la navegacion, que es lo que decide como se mueve. */
internal enum class ScreenKind {
    /** Una de las cuatro con tab bar. */
    TAB,

    /** Un formulario de creacion. Entra con `close`, no con `arrow_back`. */
    FORM,

    /** Cualquier otra: se apila encima de la anterior. */
    PUSHED,
}

/** Como se mueven las dos pantallas de una transicion. */
internal enum class NavMotion {
    /** Fundido, sin desplazamiento. */
    FADE,

    /** Desplazamiento horizontal. */
    PUSH,

    /** De abajo arriba, como un modal. */
    SHEET,
}

/**
 * Las cuatro animaciones que se le pasan al `NavHost`, ya resueltas con los tokens de movimiento.
 *
 * Se decide aqui, en un sitio, y no `composable` por `composable` en los siete grafos de feature:
 * la regla depende de **las dos** pantallas de la transicion, y repartirla garantizaba que un dia
 * cambiar de tab se animara como entrar en un detalle.
 */
internal class GastonomoTransitions(private val motion: GastonomoMotion) {

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        entering(motionOf(), popping = false)
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        leaving(motionOf(), popping = false)
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        entering(motionOf(), popping = true)
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        leaving(motionOf(), popping = true)
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.motionOf(): NavMotion =
        navMotionOf(screenKindOf(initialState.destination), screenKindOf(targetState.destination))

    private fun entering(nav: NavMotion, popping: Boolean): EnterTransition = when {
        !motion.enabled -> EnterTransition.None
        nav == NavMotion.FADE -> fadeIn(motion.enterSpec())
        // Al volver de un formulario, lo que hay debajo no se movio: solo reaparece.
        nav == NavMotion.SHEET && popping -> fadeIn(motion.enterSpec())
        nav == NavMotion.SHEET -> slideInVertically(motion.enterSpec()) { it } + fadeIn(motion.enterSpec())
        popping -> slideInHorizontally(motion.enterSpec()) { -it / PARALLAX } + fadeIn(motion.enterSpec())
        else -> slideInHorizontally(motion.enterSpec()) { it } + fadeIn(motion.enterSpec())
    }

    private fun leaving(nav: NavMotion, popping: Boolean): ExitTransition = when {
        !motion.enabled -> ExitTransition.None
        nav == NavMotion.FADE -> fadeOut(motion.exitSpec())
        // El formulario se va hacia abajo; la pantalla que tapaba se queda quieta debajo.
        nav == NavMotion.SHEET && popping -> slideOutVertically(motion.exitSpec()) { it } + fadeOut(motion.exitSpec())
        nav == NavMotion.SHEET -> ExitTransition.None
        popping -> slideOutHorizontally(motion.exitSpec()) { it } + fadeOut(motion.exitSpec())
        else -> slideOutHorizontally(motion.exitSpec()) { -it / PARALLAX } + fadeOut(motion.exitSpec())
    }

    private companion object {
        /**
         * La que se queda detras se desplaza una fraccion de lo que se desplaza la que entra. Que
         * las dos recorran lo mismo hace que parezca que la pila entera se mueve de lado.
         */
        const val PARALLAX = 3
    }
}

/**
 * Que animacion toca al ir de [from] a [to].
 *
 * Cambiar de tab **no** desliza: las cuatro principales son hermanas y no hay jerarquia entre
 * ellas. Si cambiar de tab se viera igual que entrar en un detalle, el gesto de volver dejaria de
 * ser predecible.
 */
internal fun navMotionOf(from: ScreenKind, to: ScreenKind): NavMotion = when {
    from == ScreenKind.FORM || to == ScreenKind.FORM -> NavMotion.SHEET
    from == ScreenKind.TAB && to == ScreenKind.TAB -> NavMotion.FADE
    else -> NavMotion.PUSH
}

/**
 * El papel de un destino, deducido de su ruta.
 *
 * Con rutas tipadas, `route` es el nombre cualificado de la clase mas los argumentos detras si los
 * lleva, igual que en [selectedTabIndex].
 */
internal fun screenKindOf(destination: NavDestination?): ScreenKind {
    val name = destination?.route?.substringBefore('/')?.substringBefore('?')
    return when (name) {
        in FORM_ROUTES -> ScreenKind.FORM
        in TAB_ROUTES -> ScreenKind.TAB
        else -> ScreenKind.PUSHED
    }
}

/** Los dos formularios de creacion, que son los que el diseno abre con `close`. */
private val FORM_ROUTES: Set<String?> = setOf(
    GastonomoRoute.NewMovement::class.qualifiedName,
    GastonomoRoute.NewLoan::class.qualifiedName,
)

private val TAB_ROUTES: Set<String?> =
    TopLevelDestination.entries.map { it.route::class.qualifiedName }.toSet()
