package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * Crea una categoria o guarda los cambios de una existente. El sheet de categoria es el
 * mismo en los dos casos, asi que el caso de uso tambien.
 *
 * Errores que la pantalla tiene que pintar: 422 con los campos malos, 409
 * `category_name_taken` si el nombre ya existe (sin distinguir tipo ni mayusculas) y 409
 * `category_is_default` si se intento tocar una de las 12 predeterminadas.
 */
class SaveCategory(private val gateway: CategoryGateway) {
    suspend operator fun invoke(
        draft: CategoryDraft,
        id: CategoryId? = null,
    ): Outcome<Category, ApiError> {
        val trimmed = draft.copy(name = draft.name.trim())
        return if (id == null) gateway.create(trimmed) else gateway.update(id, trimmed)
    }
}
