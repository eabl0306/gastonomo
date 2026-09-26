package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind

/**
 * Lo que se manda a `POST /v1/categories` y a `PATCH /v1/categories/{id}`.
 *
 * [kind] solo puede ser `in` u `out`: `both` esta reservado a las predeterminadas.
 */
data class CategoryDraft(
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: String,
)

/**
 * Categorias. No existe `GET /v1/categories/{id}`: se leen todas de una vez, con su
 * `isDefault` y su conteo de uso.
 */
interface CategoryGateway {

    /** `GET /v1/categories`. Trae las 12 predeterminadas mas las del usuario. */
    suspend fun list(): Outcome<List<Category>, ApiError>

    /** `POST /v1/categories`. */
    suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError>

    /** `PATCH /v1/categories/{id}`. Una predeterminada responde 409 `category_is_default`. */
    suspend fun update(id: CategoryId, draft: CategoryDraft): Outcome<Category, ApiError>

    /** `DELETE /v1/categories/{id}`. Reasigna sus movimientos a Otros. */
    suspend fun delete(id: CategoryId): Outcome<Unit, ApiError>
}
