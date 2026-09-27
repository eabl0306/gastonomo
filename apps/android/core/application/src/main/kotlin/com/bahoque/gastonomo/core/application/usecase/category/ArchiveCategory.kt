package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CategoryRemoval
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * Quita una categoria del usuario de la lista.
 *
 * La API la archiva si algun movimiento la usa, y la borra solo si no la usa ninguno. En los
 * dos casos **los movimientos se quedan como estaban**: ninguno cambia de categoria.
 *
 * Devuelve cual de las dos cosas paso, que es lo que decide el aviso que lee el usuario.
 */
class ArchiveCategory(private val gateway: CategoryGateway) {
    suspend operator fun invoke(id: CategoryId): Outcome<CategoryRemoval, ApiError> =
        gateway.archive(id)
}
