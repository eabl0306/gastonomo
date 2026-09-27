package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId

/**
 * Categorias con cache en memoria delante. Es la lista que mas se repite: la piden el grid de
 * Nuevo movimiento, la pantalla de Categorias y el filtro de Movimientos.
 */
internal class CachedCategoryGateway(
    private val delegate: CategoryGateway,
    private val caches: GatewayCaches,
) : CategoryGateway {

    override suspend fun list(): Outcome<List<Category>, ApiError> =
        caches.categories.read(Unit) { delegate.list() }

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> =
        delegate.create(draft).alsoInvalidating()

    override suspend fun update(
        id: CategoryId,
        draft: CategoryDraft,
    ): Outcome<Category, ApiError> = delegate.update(id, draft).alsoInvalidating()

    override suspend fun delete(id: CategoryId): Outcome<Unit, ApiError> =
        delegate.delete(id).alsoInvalidating()

    private suspend fun <V> Outcome<V, ApiError>.alsoInvalidating(): Outcome<V, ApiError> {
        if (this is Outcome.Success) caches.invalidateAfterCategoryWrite()
        return this
    }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedCategoryGateway(gateway: CategoryGateway, caches: GatewayCaches): CategoryGateway =
    CachedCategoryGateway(gateway, caches)
