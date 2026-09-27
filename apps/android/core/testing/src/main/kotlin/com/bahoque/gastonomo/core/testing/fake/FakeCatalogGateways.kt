package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CategoryRemoval
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Categorias programables.
 *
 * Archivar y recuperar **cambian de verdad la lista que devuelve [list]**, y con la misma regla
 * que la API: con movimientos se archiva, sin movimientos se borra. Un doble que solo apuntara la
 * llamada dejaria pasar un ViewModel que no recargara, que es justo lo que hay que comprobar.
 */
class FakeCategoryGateway(
    categories: List<Category> = SampleFinance.categories,
    private val failure: ApiError? = null,
) : CategoryGateway {

    private val stored = categories.toMutableList()

    /** El ultimo borrador enviado a create o update. */
    var lastDraft: CategoryDraft? = null
        private set

    /** Los identificadores que se pidio quitar de la lista, archivados o borrados. */
    val removed = mutableListOf<CategoryId>()

    /** Los identificadores que se pidio recuperar. */
    val restored = mutableListOf<CategoryId>()

    override suspend fun list(): Outcome<List<Category>, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(stored.toList())

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> {
        lastDraft = draft
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(stored.first())
    }

    override suspend fun update(
        id: CategoryId,
        draft: CategoryDraft,
    ): Outcome<Category, ApiError> {
        lastDraft = draft
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(stored.first())
    }

    override suspend fun archive(id: CategoryId): Outcome<CategoryRemoval, ApiError> {
        removed += id
        return failure?.let { Outcome.Failure(it) } ?: withCategory(id) { at, category ->
            if (category.movementCount == 0) {
                stored.removeAt(at)
                CategoryRemoval.DELETED
            } else {
                stored[at] = category.copy(archived = true)
                CategoryRemoval.ARCHIVED
            }
        }
    }

    override suspend fun restore(id: CategoryId): Outcome<Category, ApiError> {
        restored += id
        return failure?.let { Outcome.Failure(it) } ?: withCategory(id) { at, category ->
            category.copy(archived = false).also { stored[at] = it }
        }
    }

    /** La de [id], o el 404 que daria la API si fuera de otra cuenta. */
    private fun <V> withCategory(
        id: CategoryId,
        block: (Int, Category) -> V,
    ): Outcome<V, ApiError> {
        val at = stored.indexOfFirst { it.id == id }
        return if (at < 0) Outcome.Failure(ApiError.NotFound) else Outcome.Success(block(at, stored[at]))
    }
}

/** Ajustes programables. */
class FakeSettingsGateway(
    private var settings: UserSettings = UserSettings(),
    private val failure: ApiError? = null,
) : SettingsGateway {

    override suspend fun load(): Outcome<UserSettings, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(settings)

    override suspend fun update(
        displayCurrency: CurrencyCode?,
        language: AppLanguage?,
    ): Outcome<UserSettings, ApiError> {
        failure?.let { return Outcome.Failure(it) }
        settings = settings.copy(
            displayCurrency = displayCurrency ?: settings.displayCurrency,
            language = language ?: settings.language,
        )
        return Outcome.Success(settings)
    }
}

/** Monedas y tasas programables. */
class FakeCurrencyGateway(
    private val catalog: List<CurrencyInfo> = SampleFinance.currencies,
    private val rates: ExchangeRates = SampleFinance.rates,
    private val failure: ApiError? = null,
) : CurrencyGateway {

    override suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(catalog)

    override suspend fun rates(): Outcome<ExchangeRates, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(rates)
}
