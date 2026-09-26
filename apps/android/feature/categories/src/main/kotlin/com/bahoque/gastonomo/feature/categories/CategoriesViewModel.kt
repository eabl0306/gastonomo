package com.bahoque.gastonomo.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El estado de Categorias.
 *
 * El nombre no se puede repetir con **ninguna** categoria de la cuenta, sin distinguir tipo
 * ni mayusculas. Se comprueba aqui para poder avisar mientras se escribe, y ademas se
 * atiende el 409 de la API, que es quien manda.
 */
class CategoriesViewModel(private val dependencies: CategoriesDependencies) : ViewModel() {

    private val mutableState = MutableStateFlow(CategoriesUiState())

    val state: StateFlow<CategoriesUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = dependencies.loadCategories()
            mutableState.update { state ->
                when (result) {
                    is Outcome.Success ->
                        state.copy(categories = result.value, loading = false, loadFailed = false)
                    is Outcome.Failure -> state.copy(loading = false, loadFailed = true)
                }
            }
        }
    }

    fun onTabSelected(kind: MovementKind) = mutableState.update { it.copy(tab = kind) }

    fun onMessageShown() = mutableState.update { it.copy(message = null) }

    /** Abre el sheet vacio con el tipo del tab activo, o con una categoria ya cargada. */
    fun onOpenSheet(category: Category?) {
        mutableState.update {
            it.copy(form = category?.toForm() ?: CategoryForm(kind = it.tab))
        }
    }

    fun onCloseSheet() = mutableState.update { it.copy(form = null) }

    /**
     * Cada cambio del sheet llega entero, ya con el campo tocado. Aqui se recorta el nombre
     * al maximo que acepta la API y se recalcula si choca con otra categoria.
     */
    fun onFormChanged(form: CategoryForm) {
        mutableState.update { state ->
            val name = form.name.take(Category.MAX_NAME_LENGTH)
            state.copy(
                form = form.copy(
                    name = name,
                    duplicate = state.categories.hasName(name, ignoring = form.id),
                    fieldError = null,
                ),
            )
        }
    }

    /**
     * Guarda el borrador. Al terminar se recarga la lista y el tab salta al tipo de la
     * categoria, que es lo que hace el prototipo.
     */
    fun onSave() {
        val form = mutableState.value.form?.takeIf { it.canSave } ?: return
        mutableState.update { it.copy(form = form.copy(saving = true)) }
        viewModelScope.launch {
            val draft = CategoryDraft(
                name = form.trimmedName,
                kind = CategoryKind.of(form.kind),
                icon = form.icon,
                color = form.color,
            )
            when (val result = dependencies.saveCategory(draft, form.id)) {
                is Outcome.Success -> {
                    mutableState.update { it.saved(form) }
                    load()
                }
                is Outcome.Failure -> mutableState.update { it.failedToSave(result.error) }
            }
        }
    }

    /** Borra una categoria del usuario. La API reasigna sus movimientos a Otros. */
    fun onDelete(category: Category) {
        viewModelScope.launch {
            when (val result = dependencies.deleteCategory(category.id)) {
                is Outcome.Success -> {
                    mutableState.update {
                        it.copy(message = CategoriesMessage.Deleted(category.name))
                    }
                    load()
                }
                is Outcome.Failure -> mutableState.update { it.failedToSave(result.error) }
            }
        }
    }
}
