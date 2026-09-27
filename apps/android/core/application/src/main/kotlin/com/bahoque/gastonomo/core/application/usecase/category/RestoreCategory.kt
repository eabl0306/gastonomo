package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * Devuelve una categoria archivada a la lista.
 *
 * Hace falta porque el nombre sigue estando cogido mientras la categoria existe: sin esto,
 * archivar por error no tendria vuelta atras desde la aplicacion.
 */
class RestoreCategory(private val gateway: CategoryGateway) {
    suspend operator fun invoke(id: CategoryId): Outcome<Category, ApiError> =
        gateway.restore(id)
}
