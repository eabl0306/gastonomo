package com.bahoque.gastonomo.feature.categories

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * La pantalla 13, Categorias, con el sheet de crear o editar una.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero
 * de navegacion compartido.
 *
 * ```
 * // en GastonomoNavHost
 * categoriesNavGraph(
 *     dependencies = container.categories,
 *     actions = actions,
 * )
 * ```
 */
fun NavGraphBuilder.categoriesNavGraph(
    dependencies: CategoriesDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Categories> {
        CategoriesRoute(dependencies = dependencies, onBack = { actions.back() })
    }
}
