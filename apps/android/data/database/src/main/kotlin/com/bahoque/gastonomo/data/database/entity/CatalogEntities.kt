package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Las tres tablas de una sola fila: los ajustes de la cuenta, el perfil y la cabecera de la ultima
 * pasada de tasas. Llevan `id` fijo a [SINGLE_ROW] para que insertar por segunda vez reemplace en
 * vez de acumular filas.
 */
internal const val SINGLE_ROW: Int = 1

/** La moneda principal y el idioma de la cuenta. */
@Entity(tableName = "settings")
internal data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "display_currency") val displayCurrency: String,
    @ColumnInfo(name = "language") val language: String,
) {
    fun toDomain(): UserSettings = UserSettings(
        displayCurrency = CurrencyCode(displayCurrency),
        language = AppLanguage.fromWire(language) ?: AppLanguage.DEFAULT,
    )

    companion object {
        fun of(settings: UserSettings): SettingsEntity = SettingsEntity(
            displayCurrency = settings.displayCurrency.value,
            language = settings.language.wire,
        )
    }
}

/** Nombre y correo, que es lo que pinta el saludo y el avatar sin conexion. */
@Entity(tableName = "profile")
internal data class ProfileEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "email") val email: String,
) {
    fun toDomain(): User = User(id = userId, name = name, email = email)

    companion object {
        fun of(user: User): ProfileEntity =
            ProfileEntity(userId = user.id, name = user.name, email = user.email)
    }
}

/** Una moneda del catalogo del sheet. */
@Entity(tableName = "currencies")
internal data class CurrencyEntity(
    @PrimaryKey @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "symbol") val symbol: String,
) {
    fun toDomain(): CurrencyInfo =
        CurrencyInfo(code = CurrencyCode(code), name = name, symbol = symbol)

    companion object {
        fun of(info: CurrencyInfo): CurrencyEntity =
            CurrencyEntity(code = info.code.value, name = info.name, symbol = info.symbol)
    }
}

/**
 * La cabecera de la ultima pasada de tasas: cual es la base, de que dia son y si estan al dia.
 * Las tasas en si van en [ExchangeRateEntity], una fila por moneda.
 */
@Entity(tableName = "exchange_rate_snapshots")
internal data class ExchangeRateSnapshotEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "base") val base: String,
    @ColumnInfo(name = "date") val date: LocalDate?,
    @ColumnInfo(name = "status") val status: RatesStatus,
)

/** Una tasa, en la base de la pasada guardada. El valor es `TEXT`, como todo lo que es numero. */
@Entity(tableName = "exchange_rates")
internal data class ExchangeRateEntity(
    @PrimaryKey @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "rate") val rate: BigDecimal,
)

/** Junta la cabecera y sus tasas en lo que entiende el dominio. */
internal fun ratesOf(
    snapshot: ExchangeRateSnapshotEntity?,
    rates: List<ExchangeRateEntity>,
): ExchangeRates = when (snapshot) {
    null -> ExchangeRates.EMPTY
    else -> ExchangeRates(
        base = CurrencyCode(snapshot.base),
        date = snapshot.date,
        status = snapshot.status,
        rates = rates.associate { CurrencyCode(it.currency) to it.rate },
    )
}

/** Parte unas tasas en la cabecera y sus filas. */
internal fun snapshotOf(rates: ExchangeRates): ExchangeRateSnapshotEntity =
    ExchangeRateSnapshotEntity(base = rates.base.value, date = rates.date, status = rates.status)

internal fun rateRowsOf(rates: ExchangeRates): List<ExchangeRateEntity> =
    rates.rates.map { (code, rate) -> ExchangeRateEntity(currency = code.value, rate = rate) }
