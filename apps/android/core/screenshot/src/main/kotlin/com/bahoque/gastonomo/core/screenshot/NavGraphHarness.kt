package com.bahoque.gastonomo.core.screenshot

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * Monta el grafo de una feature sola, con el tema puesto, para poder comprobar sus
 * pantallas sin levantar la aplicacion entera.
 *
 * ```
 * @Test
 * fun `the loans screen is reachable`() {
 *     compose.renderNavGraph(GastonomoRoute.Loans) { actions ->
 *         loansNavGraph(FakeLoansDependencies, actions)
 *     }
 *     compose.onNodeWithText("Préstamos").assertExists()
 * }
 * ```
 *
 * @param recorded recoge las rutas a las que la pantalla pide navegar, para asertar sobre
 *   ellas sin un NavController de verdad.
 */
fun ComposeContentTestRule.renderNavGraph(
    start: GastonomoRoute,
    recorded: MutableList<GastonomoRoute> = mutableListOf(),
    graph: NavGraphBuilder.(GastonomoNavActions) -> Unit,
) {
    setContent {
        GastonomoTheme {
            val navController = rememberNavController()
            val actions = GastonomoNavActions(
                navigateTo = {
                    recorded += it
                    navController.navigate(it)
                },
                switchTab = { recorded += it.route },
                back = { navController.popBackStack() },
                replaceWith = { recorded += it },
            )
            NavHost(navController = navController, startDestination = start) {
                graph(actions)
            }
        }
    }
}
