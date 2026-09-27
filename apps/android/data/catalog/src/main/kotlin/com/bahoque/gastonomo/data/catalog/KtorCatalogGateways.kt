package com.bahoque.gastonomo.data.catalog

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CategoryRemoval
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.data.network.ApiClient

/** Categorias contra la API. No hay `GET /v1/categories/{id}`: se leen todas de una vez. */
internal class KtorCategoryGateway(private val client: ApiClient) : CategoryGateway {

    override suspend fun list(): Outcome<List<Category>, ApiError> =
        client.get<CategoryListDto>("categories?includeArchived=true")
            .map { list -> list.items.map { it.toDomain() } }

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> =
        client.post<CategoryDto>("categories", draft.toDto()).map { it.toDomain() }

    override suspend fun update(
        id: CategoryId,
        draft: CategoryDraft,
    ): Outcome<Category, ApiError> =
        client.patch<CategoryDto>("categories/${id.value}", draft.toDto()).map { it.toDomain() }

    override suspend fun archive(id: CategoryId): Outcome<CategoryRemoval, ApiError> =
        client.delete<CategoryRemovalDto>("categories/${id.value}").map { it.toDomain() }

    override suspend fun restore(id: CategoryId): Outcome<Category, ApiError> =
        client.post<CategoryDto>("categories/${id.value}/restore", null).map { it.toDomain() }

    private fun CategoryDraft.toDto() = CategoryRequestDto(
        name = name,
        kind = kind.wire,
        icon = icon,
        color = color,
    )
}

/** Ajustes de la cuenta: moneda principal e idioma. */
internal class KtorSettingsGateway(private val client: ApiClient) : SettingsGateway {

    override suspend fun load(): Outcome<UserSettings, ApiError> =
        client.get<SettingsDto>("settings").map { it.toDomain() }

    override suspend fun update(
        displayCurrency: CurrencyCode?,
        language: AppLanguage?,
    ): Outcome<UserSettings, ApiError> = client.patch<SettingsDto>(
        path = "settings",
        body = SettingsPatchDto(displayCurrency?.value, language?.wire),
    ).map { it.toDomain() }
}

/** Catalogo de monedas y tasas. Los clientes no llaman a Frankfurter: lo hace la API. */
internal class KtorCurrencyGateway(private val client: ApiClient) : CurrencyGateway {

    override suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError> =
        client.get<CurrencyListDto>("currencies")
            .map { list -> list.items.mapNotNull { it.toDomainOrNull() } }

    override suspend fun rates(): Outcome<ExchangeRates, ApiError> =
        client.get<ExchangeRatesDto>("exchange-rates").map { it.toDomain() }
}

/** Las fabricas que llama el contenedor de la aplicacion. */
fun createCategoryGateway(client: ApiClient): CategoryGateway = KtorCategoryGateway(client)

fun createSettingsGateway(client: ApiClient): SettingsGateway = KtorSettingsGateway(client)

fun createCurrencyGateway(client: ApiClient): CurrencyGateway = KtorCurrencyGateway(client)
