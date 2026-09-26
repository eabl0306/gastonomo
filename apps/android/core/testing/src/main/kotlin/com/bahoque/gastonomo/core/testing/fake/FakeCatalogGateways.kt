package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
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

/** Categorias programables. */
class FakeCategoryGateway(
    private val categories: List<Category> = SampleFinance.categories,
    private val failure: ApiError? = null,
) : CategoryGateway {

    /** El ultimo borrador enviado a create o update. */
    var lastDraft: CategoryDraft? = null
        private set

    /** Los identificadores borrados. */
    val deleted = mutableListOf<CategoryId>()

    override suspend fun list(): Outcome<List<Category>, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(categories)

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> {
        lastDraft = draft
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(categories.first())
    }

    override suspend fun update(
        id: CategoryId,
        draft: CategoryDraft,
    ): Outcome<Category, ApiError> {
        lastDraft = draft
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(categories.first())
    }

    override suspend fun delete(id: CategoryId): Outcome<Unit, ApiError> {
        deleted += id
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
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
