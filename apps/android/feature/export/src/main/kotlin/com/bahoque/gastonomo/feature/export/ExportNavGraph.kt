package com.bahoque.gastonomo.feature.export

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * La pantalla 14, Exportar datos, que genera el CSV o el Excel en el cliente y lo entrega
 * por la hoja de compartir del sistema.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero
 * de navegacion compartido.
 *
 * ```
 * // en GastonomoNavHost
 * exportNavGraph(
 *     dependencies = container.export,
 *     actions = actions,
 * )
 * ```
 */
fun NavGraphBuilder.exportNavGraph(
    dependencies: ExportDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Export> {
        ExportRoute(dependencies = dependencies, onBack = { actions.back() })
    }
}
