package com.bahoque.gastonomo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTabBar
import com.bahoque.gastonomo.core.designsystem.component.LocalOpenSheets
import com.bahoque.gastonomo.core.designsystem.component.OpenSheets
import com.bahoque.gastonomo.core.designsystem.component.pressable
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
    // Las hojas viven dentro de una pantalla y la tab bar se pinta fuera y por encima, asi
    // que una hoja no puede taparla. Contarlas aqui es lo que permite velarla y dejarla sin
    // pulsar mientras hay una abierta: con la barra encendida, tocar un tab navegaba con la
    // hoja puesta.
    val openSheets = remember { OpenSheets() }
    CompositionLocalProvider(LocalOpenSheets provides openSheets) {
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
                Box {
                    GastonomoTabBar(
                        items = tabItems(),
                        selectedIndex = selectedTab,
                        onSelect = { navController.switchTab(TopLevelDestination.entries[it]) },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                    TabBarScrim(visible = openSheets.any)
                }
            }
        }
    }
}

/**
 * El velo de la tab bar mientras hay una hoja abierta.
 *
 * Se funde con las mismas duraciones que la hoja, asi que las dos mitades del velo entran y
 * salen a la vez y se leen como una sola. Y se traga el toque: sin eso se podria cambiar de
 * pestana con una hoja modal delante.
 */
@Composable
private fun BoxScope.TabBarScrim(visible: Boolean) {
    val motion = GastonomoTheme.motion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.enterSpec()),
        exit = fadeOut(motion.exitSpec()),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(GastonomoTheme.colors.scrim)
                .pressable(onClick = {}),
        )
    }
}
