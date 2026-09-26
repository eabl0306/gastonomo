package com.bahoque.gastonomo.feature.categories

import com.bahoque.gastonomo.core.application.usecase.category.DeleteCategory
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.category.SaveCategory

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `CategoriesWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface CategoriesDependencies {
    val loadCategories: LoadCategories

    val saveCategory: SaveCategory

    val deleteCategory: DeleteCategory
}
