package com.bahoque.gastonomo.feature.loans

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailRoute
import com.bahoque.gastonomo.feature.loans.form.LoanPrefill
import com.bahoque.gastonomo.feature.loans.form.NewLoanRoute
import com.bahoque.gastonomo.feature.loans.list.LoansRoute

/**
 * Las pantallas 8, 9 y 10: la lista de Prestamos, el detalle y el formulario de Nuevo
 * prestamo. La 11, Registrar abono, no esta aqui: es un bottom sheet que se abre encima de
 * las dos primeras sin navegar, tal como dice el diseno.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero de
 * navegacion compartido.
 *
 * ```
 * // en GastonomoNavHost
 * loansNavGraph(container.loans, actions, loanCreated)
 * ```
 *
 * @param created el aviso de que se creo un prestamo. Llega de fuera porque lo levantan dos
 *   pantallas, el formulario y el dictado, y el dictado es de otro modulo.
 */
fun NavGraphBuilder.loansNavGraph(
    dependencies: LoansDependencies,
    actions: GastonomoNavActions,
    created: LoanCreatedSignal,
) {
    composable<GastonomoRoute.Loans> {
        LoansRoute(
            dependencies = dependencies,
            createdSignal = created,
            onOpenLoan = { actions.navigateTo(GastonomoRoute.LoanDetail(it.value)) },
            onNewLoan = { actions.navigateTo(GastonomoRoute.NewLoan()) },
            onNewMovement = { actions.navigateTo(GastonomoRoute.NewMovement(MOVEMENT_OUT)) },
            onDictate = { actions.navigateTo(GastonomoRoute.Dictate) },
        )
    }
    composable<GastonomoRoute.LoanDetail> { entry ->
        LoanDetailRoute(
            dependencies = dependencies,
            loanId = LoanId(entry.toRoute<GastonomoRoute.LoanDetail>().loanId),
            onBack = { actions.back() },
        )
    }
    composable<GastonomoRoute.NewLoan> { entry ->
        NewLoanRoute(
            dependencies = dependencies,
            prefill = LoanPrefill.of(entry.toRoute<GastonomoRoute.NewLoan>()),
            onCreated = { replay ->
                // Un reintento idempotente no vuelve a avisar: el toast ya se pinto.
                if (!replay) created.raise()
                actions.back()
            },
            onClose = { actions.back() },
        )
    }
}

/**
 * El tipo con el que se abre el formulario de movimiento desde el boton de registro rapido.
 *
 * Se escribe aqui y no se lee de `MovementKind` porque eso es de otra feature, y una feature no
 * conoce a otra: lo que comparten son las rutas.
 */
private const val MOVEMENT_OUT = "out"
