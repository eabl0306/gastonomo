package com.bahoque.gastonomo.core.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation.NavController

/**
 * Lo que una pantalla puede pedirle a la navegacion. Se le pasa a cada grafo de feature
 * para que ninguna pantalla reciba el [NavController] entero: asi no puede inventarse una
 * navegacion que el diseno no tiene.
 *
 * ```
 * // en el grafo de una feature
 * composable<GastonomoRoute.Loans> {
 *     LoansRoute(
 *         onLoanClick = { actions.navigateTo(GastonomoRoute.LoanDetail(it.value)) },
 *         onNewLoan = { actions.navigateTo(GastonomoRoute.NewLoan()) },
 *     )
 * }
 * ```
 */
// Un parametro por verbo de navegacion. Agruparlos en otro objeto solo moveria la lista de sitio:
// lo que hace legible esta clase es justo que la firma diga las cinco cosas que se pueden pedir.
@Suppress("LongParameterList")
@Immutable
class GastonomoNavActions(
    /** Apila una pantalla secundaria. */
    val navigateTo: (GastonomoRoute) -> Unit,
    /** Cambia de tab y vacia la pila. */
    val switchTab: (TopLevelDestination) -> Unit,
    /** Vuelve atras, que es lo que hacen `arrow_back` y `close`. */
    val back: () -> Unit,
    /** Sustituye toda la pila. Se usa al entrar y al cerrar sesion. */
    val replaceWith: (GastonomoRoute) -> Unit,

    /**
     * Cambia la pantalla actual por otra, sin dejarla en la pila.
     *
     * Lo usa el dictado al pasar al formulario para corregir: volver atras desde el formulario
     * tiene que llevar a la pantalla desde la que se dicto, no a un borrador que ya no vale.
     */
    val replaceCurrent: (GastonomoRoute) -> Unit,
) {
    companion object {
        /** Las acciones de un [NavController] de verdad. */
        fun of(navController: NavController): GastonomoNavActions = GastonomoNavActions(
            navigateTo = { navController.navigate(it) },
            switchTab = { navController.switchTab(it) },
            back = { navController.popBackStack() },
            replaceWith = { navController.replaceWith(it) },
            replaceCurrent = { navController.replaceCurrent(it) },
        )
    }
}
