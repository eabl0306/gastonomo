package com.bahoque.gastonomo.feature.categories

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * Las transformaciones de estado de Categorias, fuera del ViewModel: son funciones puras y
 * asi se pueden leer, y probar, sin montar el ViewModel entero.
 */

/** True si alguna categoria ya se llama asi, sin distinguir mayusculas y sin contar [ignoring]. */
internal fun List<Category>.hasName(name: String, ignoring: CategoryId?): Boolean {
    val needle = name.trim().lowercase()
    if (needle.isEmpty()) return false
    return any { it.id != ignoring && it.name.lowercase() == needle }
}

/** Cierra el sheet, salta al tab del tipo guardado y prepara el aviso. */
internal fun CategoriesUiState.saved(form: CategoryForm): CategoriesUiState = copy(
    form = null,
    tab = form.kind,
    message = if (form.isEditing) {
        CategoriesMessage.Updated
    } else {
        CategoriesMessage.Created(form.trimmedName)
    },
)

/**
 * Un fallo al guardar. Cuando el problema es del campo se marca en el propio sheet y el
 * toast se calla: contar lo mismo dos veces tapa el formulario.
 */
internal fun CategoriesUiState.failedToSave(error: ApiError): CategoriesUiState {
    val nameTaken = error is ApiError.Conflict && error.code == ConflictCode.CATEGORY_NAME_TAKEN
    val fieldError = (error as? ApiError.Validation)?.messageFor(NAME_FIELD)
    val shownInTheField = nameTaken || fieldError != null
    return copy(
        form = form?.copy(saving = false, duplicate = nameTaken, fieldError = fieldError),
        message = when {
            shownInTheField -> null
            error is ApiError.Conflict && error.code == ConflictCode.CATEGORY_IS_DEFAULT ->
                CategoriesMessage.IsDefault
            else -> CategoriesMessage.SaveFailed
        },
    )
}

/** El borrador con el que se abre el sheet al editar una categoria del usuario. */
internal fun Category.toForm(): CategoryForm = CategoryForm(
    id = id,
    name = name,
    kind = if (kind == CategoryKind.IN) MovementKind.IN else MovementKind.OUT,
    icon = icon,
    color = color,
)

/** El campo que marca la API cuando el nombre no vale. */
private const val NAME_FIELD = "name"
