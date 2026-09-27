package com.bahoque.gastonomo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTabBar
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.navigation.TopLevelDestination
import com.bahoque.gastonomo.core.navigation.switchTab
import com.bahoque.gastonomo.navigation.GastonomoNavHost
import com.bahoque.gastonomo.navigation.selectedTabIndex
import com.bahoque.gastonomo.navigation.tabItems

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
    val selectedTab = selectedTabIndex(backStackEntry?.destination)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GastonomoTheme.colors.bgApp)
            .statusBarsPadding(),
    ) {
        GastonomoNavHost(
            navController = navController,
            container = container,
            startDestination = startDestination,
            // Recortado a su propia region: un bottom sheet vive dentro de la pantalla, y al
            // bajarse se salia por debajo. La tab bar es el hermano siguiente de esta Column,
            // asi que le pasaba por encima y el boton del sheet se veia asomar detras de ella.
            modifier = Modifier.weight(1f).clipToBounds(),
        )
        if (selectedTab >= 0) {
            GastonomoTabBar(
                items = tabItems(),
                selectedIndex = selectedTab,
                onSelect = { navController.switchTab(TopLevelDestination.entries[it]) },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}
