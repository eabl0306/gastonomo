package com.bahoque.gastonomo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.bahoque.gastonomo.AppContainer
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
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
