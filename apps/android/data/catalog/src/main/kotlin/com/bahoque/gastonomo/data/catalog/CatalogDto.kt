package com.bahoque.gastonomo.data.catalog

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
internal data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("kind") val kind: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("isDefault") val isDefault: Boolean = false,
    @SerialName("movementCount") val movementCount: Int = 0,
) {
    fun toDomain(): Category = Category(
        id = CategoryId(id),
        name = name,
        kind = CategoryKind.fromWire(kind) ?: CategoryKind.BOTH,
        icon = icon,
        color = color,
        isDefault = isDefault,
        movementCount = movementCount,
    )
}

@Serializable
internal data class CategoryListDto(
    @SerialName("items") val items: List<CategoryDto> = emptyList(),
)

@Serializable
internal data class CategoryRequestDto(
    @SerialName("name") val name: String,
    @SerialName("kind") val kind: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
)

@Serializable
internal data class SettingsDto(
    @SerialName("displayCurrency") val displayCurrency: String,
    @SerialName("language") val language: String,
) {
    fun toDomain(): UserSettings = UserSettings(
        displayCurrency = CurrencyCode.parseOrNull(displayCurrency) ?: CurrencyCode.COP,
        language = AppLanguage.fromWire(language) ?: AppLanguage.DEFAULT,
    )
}

@Serializable
internal data class SettingsPatchDto(
    @SerialName("displayCurrency") val displayCurrency: String? = null,
    @SerialName("language") val language: String? = null,
)

@Serializable
internal data class CurrencyDto(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("symbol") val symbol: String = "",
) {
    fun toDomainOrNull(): CurrencyInfo? = CurrencyCode.parseOrNull(code)?.let {
        CurrencyInfo(code = it, name = name, symbol = symbol)
    }
}

@Serializable
internal data class CurrencyListDto(
    @SerialName("items") val items: List<CurrencyDto> = emptyList(),
)

@Serializable
internal data class ExchangeRatesDto(
    @SerialName("base") val base: String = "USD",
    @SerialName("date") val date: String? = null,
    @SerialName("status") val status: String = "ok",
    @SerialName("rates") val rates: Map<String, String> = emptyMap(),
) {
    /**
     * Las tasas llegan como cadena, igual que los importes. Una moneda con codigo raro o
     * tasa ilegible se descarta en vez de tumbar la conversion entera.
     */
    fun toDomain(): ExchangeRates = ExchangeRates(
        base = CurrencyCode.parseOrNull(base) ?: CurrencyCode.USD,
        date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        status = if (status == STALE) RatesStatus.STALE else RatesStatus.OK,
        rates = rates.mapNotNull { (code, rate) ->
            val currency = CurrencyCode.parseOrNull(code) ?: return@mapNotNull null
            val value = runCatching { BigDecimal(rate) }.getOrNull() ?: return@mapNotNull null
            currency to value
        }.toMap(),
    )
}

/** El valor de `status` con el que la API marca unas tasas viejas. */
private const val STALE = "stale"
