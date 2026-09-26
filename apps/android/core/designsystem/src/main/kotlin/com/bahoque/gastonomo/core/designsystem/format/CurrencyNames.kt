package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import java.util.Currency
import java.util.Locale

/**
 * El nombre de una moneda en el idioma vigente. Es el equivalente de `Intl.DisplayNames`
 * del prototipo: el sheet de moneda y el chip de los formularios ensenan "Peso colombiano"
 * en espanol y "Colombian Peso" en ingles, aunque la API mande siempre el nombre en ingles.
 *
 * Si el CLDR del dispositivo no conoce el codigo se usa el nombre que mando la API, y si
 * tampoco hay, el propio codigo. Nunca se queda en blanco.
 */
class CurrencyNames internal constructor(private val locale: Locale) {

    /** El nombre localizado de [currency], o [fallback] si el dispositivo no lo conoce. */
    fun of(currency: CurrencyCode, fallback: String = currency.value): String {
        val localized = runCatching {
            Currency.getInstance(currency.value).getDisplayName(locale)
        }.getOrNull()
        val name = localized?.takeIf { it.isNotBlank() && it != currency.value } ?: fallback
        return name.replaceFirstChar { it.uppercase() }
    }
}
