package com.bahoque.gastonomo.core.application.usecase.category

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.category.Category

/**
 * Las categorias de la cuenta, con su `isDefault` y su conteo de uso. Se ordenan como las
 * ensena Categorias: primero las del usuario y despues las predeterminadas, cada grupo por
 * nombre.
 *
 * Para el grid de Nuevo movimiento hay que filtrar tambien con
 * [com.bahoque.gastonomo.core.domain.category.CategoryKind.accepts]: una categoria se ofrece
 * si su tipo es `both` o coincide con el del movimiento.
 */
class LoadCategories(private val gateway: CategoryGateway) {

    /**
     * @param includeArchived con `false`, que es lo normal, **las archivadas no salen**. Es el
     *   valor por defecto a proposito: una archivada no se puede elegir al registrar, y si quien
     *   pinta la rejilla se olvidara de filtrar, el usuario volveria a ver algo que ya habia
     *   quitado. Solo la pantalla de Categorias pide `true`, que es donde se recuperan.
     */
    suspend operator fun invoke(
        includeArchived: Boolean = false,
    ): Outcome<List<Category>, ApiError> = gateway.list().map { categories ->
        categories
            .filter { includeArchived || !it.archived }
            .sortedWith(compareBy({ it.isDefault }, { it.name.lowercase() }))
    }
}
