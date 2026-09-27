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

/** Que paso al quitar una categoria de la lista. Lo dice la API, no se adivina. */
enum class CategoryRemoval {
    /** Algun movimiento la usa: se archivo y sigue ahi. */
    ARCHIVED,

    /** No la usaba nadie: se borro de verdad. */
    DELETED,
}

/**
 * Categorias. No existe `GET /v1/categories/{id}`: se leen todas de una vez, con su
 * `isDefault` y su conteo de uso.
 */
interface CategoryGateway {

    /**
     * `GET /v1/categories?includeArchived=true`. Las 12 predeterminadas, las del usuario y
     * **tambien las archivadas**.
     *
     * Siempre se piden todas, en una sola forma de consulta, porque la respuesta se guarda en la
     * base local reemplazando la tabla entera. Si unas veces se pidieran solo las activas y otras
     * todas, la ultima llamada decidiria que hay guardado y la pantalla de Categorias veria
     * aparecer y desaparecer la seccion de archivadas. Filtrar es de quien las lee.
     */
    suspend fun list(): Outcome<List<Category>, ApiError>

    /** `POST /v1/categories`. */
    suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError>

    /** `PATCH /v1/categories/{id}`. Una predeterminada responde 409 `category_is_default`. */
    suspend fun update(id: CategoryId, draft: CategoryDraft): Outcome<Category, ApiError>

    /**
     * `DELETE /v1/categories/{id}`. Archiva la categoria, o la borra si no la usa ningun
     * movimiento. **No toca los movimientos** en ninguno de los dos casos.
     */
    suspend fun archive(id: CategoryId): Outcome<CategoryRemoval, ApiError>

    /** `POST /v1/categories/{id}/restore`. Devuelve una archivada a la lista. */
    suspend fun restore(id: CategoryId): Outcome<Category, ApiError>
}
