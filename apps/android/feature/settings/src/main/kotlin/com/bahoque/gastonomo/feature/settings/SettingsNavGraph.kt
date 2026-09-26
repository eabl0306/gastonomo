package com.bahoque.gastonomo.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * La pantalla 12, Ajustes, con los sheets de moneda y de idioma.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero
 * de navegacion compartido.
 *
 * ```
 * // en GastonomoNavHost
 * settingsNavGraph(
 *     dependencies = container.settings,
 *     actions = actions,
 * )
 * ```
 */
fun NavGraphBuilder.settingsNavGraph(
    dependencies: SettingsDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Settings> {
        SettingsRoute(
            dependencies = dependencies,
            onCategories = { actions.navigateTo(GastonomoRoute.Categories) },
            onExport = { actions.navigateTo(GastonomoRoute.Export) },
            onSignedOut = { actions.replaceWith(GastonomoRoute.Login) },
        )
    }
}
