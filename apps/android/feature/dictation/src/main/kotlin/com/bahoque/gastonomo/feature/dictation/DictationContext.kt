package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Lo que el dictado necesita saber antes de abrir el microfono.
 *
 * Ninguna de las dos cosas rompe la pantalla si falla: sin ajustes se habla en castellano y sin
 * tasas solo se puede guardar lo que se dicto en la moneda principal, que es lo normal.
 */
internal data class DictationContext(
    val language: AppLanguage,
    val displayCurrency: CurrencyCode,
    val rates: ExchangeRates,
)

/** Trae el idioma de la cuenta, su moneda principal y las tasas con las que convertir. */
internal class DictationContextLoader(private val dependencies: DictationDependencies) {

    suspend fun load(): DictationContext {
        val settings = dependencies.loadSettings().valueOrNull() ?: UserSettings()
        return DictationContext(
            language = settings.language,
            displayCurrency = settings.displayCurrency,
            rates = dependencies.loadExchangeRates().valueOrNull() ?: ExchangeRates.EMPTY,
        )
    }
}
