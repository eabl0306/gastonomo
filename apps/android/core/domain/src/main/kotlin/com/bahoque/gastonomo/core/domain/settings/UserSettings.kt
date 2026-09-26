package com.bahoque.gastonomo.core.domain.settings

import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * Lo que se sincroniza con la cuenta: la moneda principal y el idioma. Nada mas.
 *
 * El bloqueo con huella o PIN y el ojo que oculta los montos son del dispositivo, no de
 * la cuenta: viven en [DeviceSettings].
 */
data class UserSettings(
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val language: AppLanguage = AppLanguage.DEFAULT,
)

/**
 * Ajustes que se quedan en el telefono. El cerrojo protege un secreto de este dispositivo
 * y `hideAmounts` es estado efimero de la tarjeta de balance.
 */
data class DeviceSettings(
    val lockEnabled: Boolean = false,
    val hideAmounts: Boolean = false,
)
