package com.bahoque.gastonomo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.bahoque.gastonomo.AppContainer
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.feature.auth.authNavGraph
import com.bahoque.gastonomo.feature.categories.categoriesNavGraph
import com.bahoque.gastonomo.feature.export.exportNavGraph
import com.bahoque.gastonomo.feature.home.homeNavGraph
import com.bahoque.gastonomo.feature.loans.loansNavGraph
import com.bahoque.gastonomo.feature.movements.movementsNavGraph
import com.bahoque.gastonomo.feature.settings.settingsNavGraph

/**
 * Las 14 pantallas del rediseno. Este fichero solo compone los grafos que aporta cada
 * feature: no declara ninguna ruta, asi que anadir una pantalla no obliga a tocarlo.
 */
@Composable
fun GastonomoNavHost(
    navController: NavHostController,
    container: AppContainer,
    startDestination: GastonomoRoute,
    modifier: Modifier = Modifier,
) {
    val actions = GastonomoNavActions.of(navController)
    // Las animaciones se deciden en un sitio, mirando las dos pantallas de la transicion. Lo que
    // cada una significa esta en GastonomoTransitions.
    val motion = GastonomoTheme.motion
    val transitions = remember(motion) { GastonomoTransitions(motion) }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = transitions.enter,
        exitTransition = transitions.exit,
        popEnterTransition = transitions.popEnter,
        popExitTransition = transitions.popExit,
    ) {
        authNavGraph(container.auth, actions)
        homeNavGraph(container.home, actions)
        movementsNavGraph(container.movements, actions)
        loansNavGraph(container.loans, actions)
        settingsNavGraph(container.settings, actions)
        categoriesNavGraph(container.categories, actions)
        exportNavGraph(container.export, actions)
    }
}
