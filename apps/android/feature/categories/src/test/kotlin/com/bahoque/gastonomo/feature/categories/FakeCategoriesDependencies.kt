package com.bahoque.gastonomo.feature.categories

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.usecase.category.ArchiveCategory
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.category.RestoreCategory
import com.bahoque.gastonomo.core.application.usecase.category.SaveCategory
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.testing.fake.FakeCategoryGateway
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/** Dobles de Categorias, con una lista que incluye una del usuario y varias por defecto. */
internal class FakeCategoriesDependencies(
    val gateway: FakeCategoryGateway = FakeCategoryGateway(CATALOG),
) : CategoriesDependencies {
    override val loadCategories = LoadCategories(gateway)
    override val saveCategory = SaveCategory(gateway)
    override val archiveCategory = ArchiveCategory(gateway)
    override val restoreCategory = RestoreCategory(gateway)

    companion object {
        /** Una de tipo `both`, para comprobar que sale en los dos tabs. */
        val shared: Category = SampleFinance.food.copy(
            id = com.bahoque.gastonomo.core.domain.category.CategoryId("otros"),
            name = "Otros",
            kind = CategoryKind.BOTH,
            icon = "more_horiz",
            color = "#7c7669",
            movementCount = 0,
        )

        val CATALOG: List<Category> = SampleFinance.categories + shared

        /** Del usuario y sin usar: al quitarla se borra de verdad, no se archiva. */
        val unused: Category = SampleFinance.pets.copy(
            id = com.bahoque.gastonomo.core.domain.category.CategoryId("suscripciones"),
            name = "Suscripciones",
            icon = "subscriptions",
            movementCount = 0,
        )

        /** Del usuario y ya archivada: es la que se puede recuperar. */
        val archived: Category = SampleFinance.pets.copy(
            id = com.bahoque.gastonomo.core.domain.category.CategoryId("gym"),
            name = "Gimnasio",
            icon = "fitness_center",
            movementCount = 5,
            archived = true,
        )

        /**
         * El catalogo del archivado, aparte del de siempre.
         *
         * Va en su propia constante porque los tests de la lista cuentan filas y afirman quien
         * sale en cada grupo: meter dos categorias mas en [CATALOG] cambiaria esos numeros sin
         * que nadie lo hubiera pedido.
         */
        val CATALOG_WITH_ARCHIVED: List<Category> = CATALOG + unused + archived

        fun working(): FakeCategoriesDependencies = FakeCategoriesDependencies()

        /** Con una sin usar y una ya archivada, que son los dos casos del archivado. */
        fun withArchived(): FakeCategoriesDependencies =
            FakeCategoriesDependencies(FakeCategoryGateway(CATALOG_WITH_ARCHIVED))

        /** El 409 de nombre repetido que manda la API. */
        fun nameTaken(): FakeCategoriesDependencies = FakeCategoriesDependencies(
            FakeCategoryGateway(
                categories = CATALOG,
                failure = ApiError.Conflict(ConflictCode.CATEGORY_NAME_TAKEN),
            ),
        )

        /** El 409 de intentar tocar una predeterminada. */
        fun isDefault(): FakeCategoriesDependencies = FakeCategoriesDependencies(
            FakeCategoryGateway(
                categories = CATALOG,
                failure = ApiError.Conflict(ConflictCode.CATEGORY_IS_DEFAULT),
            ),
        )

        /** Sin red: la pantalla ensena su estado de error con reintento. */
        fun offline(): FakeCategoriesDependencies = FakeCategoriesDependencies(
            FakeCategoryGateway(categories = CATALOG, failure = ApiError.Network),
        )
    }
}
