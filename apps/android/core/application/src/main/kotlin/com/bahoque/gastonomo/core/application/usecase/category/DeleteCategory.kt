package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * Borra una categoria del usuario. La API reasigna sus movimientos a Otros en la misma
 * transaccion, asi que no hay que hacer nada mas que recargar la lista.
 */
class DeleteCategory(private val gateway: CategoryGateway) {
    suspend operator fun invoke(id: CategoryId): Outcome<Unit, ApiError> = gateway.delete(id)
}
