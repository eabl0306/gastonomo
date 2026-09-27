package com.bahoque.gastonomo.feature.categories

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * Lo que pinta Categorias: la lista entera de la cuenta, que tab esta activo y, si hay una
 * hoja abierta, el borrador que se esta editando.
 *
 * Las categorias se guardan sin partir: el tab decide cuales se ensenan, y una de tipo
 * `both` (Prestamo, Abono prestamo y Otros) sale en los dos.
 *
 * Esta lista es la unica de la aplicacion que llega **con las archivadas dentro**, porque es la
 * unica pantalla donde se pueden recuperar. Los tres grupos las reparten y ninguno las mezcla.
 */
data class CategoriesUiState(
    val loading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val tab: MovementKind = MovementKind.OUT,
    val form: CategoryForm? = null,
    val message: CategoriesMessage? = null,
    val loadFailed: Boolean = false,
) {
    /** Las del usuario que caben en el tab activo, que son las unicas editables. */
    val mine: List<Category> get() = active.filter { !it.isDefault && it.kind.accepts(tab) }

    /** Las 12 predeterminadas que caben en el tab activo. */
    val defaults: List<Category> get() = active.filter { it.isDefault && it.kind.accepts(tab) }

    /**
     * Las archivadas del tab activo. Solo se pueden recuperar: ni se editan ni se vuelven a
     * archivar.
     */
    val archived: List<Category> get() = categories.filter { it.archived && it.kind.accepts(tab) }

    /**
     * El conteo que ensena cada segmento del control de arriba.
     *
     * Cuenta solo las activas: es el numero de categorias que se pueden elegir al registrar, que
     * es lo que el usuario entiende por "cuantas tengo".
     */
    fun countOf(kind: MovementKind): Int = active.count { it.kind.accepts(kind) }

    private val active: List<Category> get() = categories.filterNot { it.archived }
}

/**
 * El borrador del sheet de categoria. Sirve para crear y para editar: la unica diferencia
 * es si lleva [id].
 */
data class CategoryForm(
    val id: CategoryId? = null,
    val name: String = "",
    val kind: MovementKind = MovementKind.OUT,
    val icon: String = Category.ICON_CHOICES.first(),
    val color: String = Category.COLOR_CHOICES[DEFAULT_COLOR_INDEX],
    val duplicate: Boolean = false,
    val fieldError: String? = null,
    val saving: Boolean = false,
) {
    /** True si se esta editando una categoria que ya existe. */
    val isEditing: Boolean get() = id != null

    /** El nombre sin espacios de sobra, que es lo que se manda a la API. */
    val trimmedName: String get() = name.trim()

    /** Se puede guardar con un nombre no vacio que no choque con otro. */
    val canSave: Boolean get() = trimmedName.isNotEmpty() && !duplicate && !saving

    /** True si el nombre esta marcado, sea por la comprobacion local o por la API. */
    val hasNameProblem: Boolean get() = duplicate || fieldError != null

    private companion object {
        /** El azul con el que el prototipo abre el sheet. */
        const val DEFAULT_COLOR_INDEX = 3
    }
}

/** Lo que Categorias cuenta por el toast. El texto se resuelve en la pantalla. */
sealed interface CategoriesMessage {

    /** True si es un fallo: el toast cambia el icono en vez de mentir con un check. */
    val isError: Boolean get() = false

    data class Created(val name: String) : CategoriesMessage

    data object Updated : CategoriesMessage

    /** Se archivo: sigue ahi porque algun movimiento la usa. */
    data class Archived(val name: String) : CategoriesMessage

    /** No la usaba nadie, asi que se borro de verdad. */
    data class Deleted(val name: String) : CategoriesMessage

    data class Restored(val name: String) : CategoriesMessage

    /** 409 `category_is_default`: se intento tocar una de las 12. */
    data object IsDefault : CategoriesMessage {
        override val isError: Boolean get() = true
    }

    data object SaveFailed : CategoriesMessage {
        override val isError: Boolean get() = true
    }
}
