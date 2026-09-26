package com.bahoque.gastonomo.feature.categories

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/** Lo que se puede tocar en Categorias, agrupado para no pasar seis lambdas sueltas. */
@Immutable
data class CategoriesActions(
    val onBack: () -> Unit,
    val onTab: (MovementKind) -> Unit,
    val onOpenSheet: (Category?) -> Unit,
    val onDelete: (Category) -> Unit,
    val onRetry: () -> Unit,
) {
    companion object {
        /** Acciones que no hacen nada, para las capturas y los tests de pintado. */
        val NONE = CategoriesActions({}, {}, {}, {}, {})
    }
}
