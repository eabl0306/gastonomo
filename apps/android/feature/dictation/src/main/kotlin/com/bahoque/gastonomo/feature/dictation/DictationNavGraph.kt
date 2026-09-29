package com.bahoque.gastonomo.feature.dictation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * La pantalla del dictado.
 *
 * Es una ruta y no una hoja dentro de otra pantalla aunque se vea como un modal, porque la abren
 * tres pantallas distintas: Inicio, Movimientos y Prestamos. Como hoja habria que montarla en las
 * tres, y una feature no conoce a otra.
 *
 * ```
 * // en GastonomoNavHost
 * dictationNavGraph(container.dictation, actions, onLoanCreated = created::raise)
 * ```
 *
 * @param onLoanCreated se llama cuando el dictado creo un prestamo. Lo necesita Prestamos para
 *   recargar su lista y pintar su aviso, igual que cuando se crea desde el formulario.
 */
fun NavGraphBuilder.dictationNavGraph(
    dependencies: DictationDependencies,
    actions: GastonomoNavActions,
    onLoanCreated: () -> Unit,
) {
    composable<GastonomoRoute.Dictate> {
        DictateRoute(
            dependencies = dependencies,
            onClose = { actions.back() },
            onSaved = { isLoan -> if (isLoan) onLoanCreated() },
            // Corregir **sustituye** el dictado en la pila: volver atras desde el formulario tiene
            // que llevar a la pantalla desde la que se dicto, no a un borrador que ya no vale.
            onEdit = { actions.replaceCurrent(formRouteOf(it)) },
        )
    }
}
