package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import java.math.BigDecimal

/**
 * Los cuatro campos de importe de todo registro de dinero, tal como llegan de la API: lo que
 * teclo el usuario y su equivalencia al registrar.
 *
 * Se guardan **sin recalcular nada**. La conversion para mostrar la hace el cliente al pintar, con
 * las tasas del momento, y la equivalencia guardada puede llevar una tasa que el usuario edito a
 * mano: recalcularla al guardar la perderia.
 *
 * Las cuatro columnas son `TEXT`. Ver [com.bahoque.gastonomo.data.database.DatabaseConverters].
 */
internal data class MoneyColumns(
    @ColumnInfo(name = "amount") val amount: BigDecimal,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "base") val base: BigDecimal,
    @ColumnInfo(name = "base_currency") val baseCurrency: String,
) {
    fun toDomain(): MonetaryAmount = MonetaryAmount(
        amount = amount,
        currency = CurrencyCode(currency),
        base = base,
        baseCurrency = CurrencyCode(baseCurrency),
    )

    companion object {
        fun of(money: MonetaryAmount): MoneyColumns = MoneyColumns(
            amount = money.amount,
            currency = money.currency.value,
            base = money.base,
            baseCurrency = money.baseCurrency.value,
        )
    }
}
