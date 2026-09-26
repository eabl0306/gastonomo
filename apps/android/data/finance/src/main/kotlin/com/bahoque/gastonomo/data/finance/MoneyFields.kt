package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.data.network.BigDecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Los cuatro campos de dinero de un registro, compartidos por movimientos, prestamos y
 * abonos. Los importes viajan como cadena y se leen a [BigDecimal], nunca a Double.
 */
@Serializable
internal data class MoneyFields(
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
) {
    /**
     * Si la respuesta no trae `base` y `baseCurrency`, la equivalencia es el propio importe.
     * Es el caso del usuario que teclea en su moneda principal.
     */
    fun toDomain(): MonetaryAmount {
        val currencyCode = CurrencyCode(currency)
        val baseCode = baseCurrency?.let(::CurrencyCode) ?: currencyCode
        return MonetaryAmount(
            amount = amount,
            currency = currencyCode,
            base = base ?: amount,
            baseCurrency = baseCode,
        )
    }

    companion object {
        /**
         * Lo que se manda al crear. `base` y `baseCurrency` se omiten **los dos juntos**
         * cuando la equivalencia es el propio importe: mandar uno solo es un 422.
         */
        fun of(money: MonetaryAmount): MoneyFields {
            val sameCurrency = money.currency == money.baseCurrency &&
                money.amount.compareTo(money.base) == 0
            return MoneyFields(
                amount = money.amount,
                currency = money.currency.value,
                base = if (sameCurrency) null else money.base,
                baseCurrency = if (sameCurrency) null else money.baseCurrency.value,
            )
        }
    }
}
