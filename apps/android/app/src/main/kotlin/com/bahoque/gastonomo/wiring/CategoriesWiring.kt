package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.categories.CategoriesDependencies

/**
 * Lo que `:feature:categories` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun categoriesDependencies(useCases: UseCases): CategoriesDependencies =
    object : CategoriesDependencies {
        override val loadCategories = useCases.loadCategories
        override val saveCategory = useCases.saveCategory
        override val archiveCategory = useCases.archiveCategory
        override val restoreCategory = useCases.restoreCategory
    }
