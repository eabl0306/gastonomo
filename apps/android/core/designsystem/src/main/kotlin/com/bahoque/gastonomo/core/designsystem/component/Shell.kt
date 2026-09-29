package com.bahoque.gastonomo.core.designsystem.component

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** Lo que devuelve [selectedTab] cuando la pantalla no lleva tab bar. */
const val NO_TAB = -1

/**
 * El reparto de la aplicacion: el contenido arriba y la tab bar debajo, como hermanos.
 *
 * Vive aqui y no en `:app` porque lo usan dos sitios: la aplicacion de verdad y las capturas de
 * pantalla. Cuando estaba solo en `:app`, las capturas pintaban la pantalla sola, en una caja de
 * 844 dp, y ensenaban 78 dp de contenido que en el telefono tapa la barra. Eso ya escondio un
 * fallo: el boton de registro rapido quedaba 92 dp por encima de la barra en vez de 20, y
 * ninguna captura lo delataba porque en ninguna habia barra.
 *
 * Tres detalles del reparto que no son decorativos:
 *
 * - **El contenido va recortado a su region.** Un bottom sheet vive dentro de la pantalla, y al
 *   bajarse se salia por debajo; como la tab bar es el hermano siguiente, el boton del sheet se
 *   veia asomar detras de ella.
 * - **La barra se vela mientras hay una hoja abierta**, y deja de responder. Sin eso se podia
 *   cambiar de pestana con una hoja modal delante. El contador vive aqui y las hojas lo
 *   encuentran por [LocalOpenSheets].
 * - **Con [NO_TAB] no hay barra.** El diseno solo la pone en las cuatro pantallas principales.
 *
 * ```
 * GastonomoShell(tabs = tabItems(), selectedTab = current, onSelectTab = navController::switchTab) {
 *     GastonomoNavHost(...)
 * }
 * ```
 */
@Composable
fun GastonomoShell(
    tabs: List<TabItem>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val openSheets = remember { OpenSheets() }
    CompositionLocalProvider(LocalOpenSheets provides openSheets) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(GastonomoTheme.colors.bgApp)
                .statusBarsPadding(),
        ) {
            Box(modifier = Modifier.weight(1f).clipToBounds()) { content() }
            if (selectedTab != NO_TAB) {
                Box {
                    GastonomoTabBar(
                        items = tabs,
                        selectedIndex = selectedTab,
                        onSelect = onSelectTab,
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
