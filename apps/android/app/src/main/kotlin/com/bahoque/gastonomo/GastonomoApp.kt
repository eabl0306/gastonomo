package com.bahoque.gastonomo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bahoque.gastonomo.core.designsystem.component.GastonomoShell
import com.bahoque.gastonomo.core.designsystem.component.NO_TAB
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.navigation.TopLevelDestination
import com.bahoque.gastonomo.core.navigation.switchTab
import com.bahoque.gastonomo.core.navigation.tabItems
import com.bahoque.gastonomo.navigation.GastonomoNavHost
import com.bahoque.gastonomo.navigation.isModal
import com.bahoque.gastonomo.navigation.selectedTabIndex

/**
 * El armazon de la aplicacion: el NavHost de las 14 pantallas y la tab bar, que solo sale
 * en las cuatro pantallas principales.
 *
 * Cambiar de tab vacia la pila, asi que volver a un tab nunca deja una pantalla secundaria
 * a medias.
 */
@Composable
fun GastonomoApp(container: AppContainer, startDestination: GastonomoRoute) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = tabBehindModals(backStackEntry?.destination)
    GastonomoShell(
        tabs = tabItems(),
        selectedTab = selectedTab,
        onSelectTab = { navController.switchTab(TopLevelDestination.entries[it]) },
    ) {
        GastonomoNavHost(
            navController = navController,
            container = container,
            startDestination = startDestination,
        )
    }
}

/**
 * El tab que hay que ensenar marcado, contando con que un modal se abre encima.
 *
 * El dictado es una ruta y no lleva tab bar, pero se pinta encima de una de las cuatro pantallas
 * que si la llevan. Sin recordar cual era, la barra desaparecia al abrir el dictado y volvia al
 * cerrarlo, que es exactamente lo que un modal no debe hacer.
 */
@Composable
private fun tabBehindModals(destination: NavDestination?): Int {
    val current = selectedTabIndex(destination)
    var last by remember { mutableIntStateOf(0) }
    if (current >= 0) last = current
    return if (current >= 0 || isModal(destination)) last else NO_TAB
}
