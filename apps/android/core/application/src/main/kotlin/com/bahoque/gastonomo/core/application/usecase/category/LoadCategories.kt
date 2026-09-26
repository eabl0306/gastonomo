package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.category.Category

/**
 * Todas las categorias de la cuenta, con su `isDefault` y su conteo de uso. Se ordenan
 * como las ensena Categorias: primero las del usuario y despues las predeterminadas, cada
 * grupo por nombre.
 *
 * Para el grid de Nuevo movimiento hay que filtrar con
 * [com.bahoque.gastonomo.core.domain.category.CategoryKind.accepts]: una categoria se
 * ofrece si su tipo es `both` o coincide con el del movimiento.
 */
class LoadCategories(private val gateway: CategoryGateway) {
    suspend operator fun invoke(): Outcome<List<Category>, ApiError> =
        gateway.list().map { categories ->
            categories.sortedWith(compareBy({ it.isDefault }, { it.name.lowercase() }))
        }
}
