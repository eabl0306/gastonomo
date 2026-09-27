package com.bahoque.gastonomo.data.database

import androidx.room.TypeConverter
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Como viajan a SQLite los tipos que Room no conoce.
 *
 * Los dos que importan, y por motivos distintos:
 *
 * - **El dinero va como texto, nunca como `REAL`.** `REAL` es un `double`, y meter un importe en
 *   un double rompe la regla 1 de `CLAUDE.md` en silencio: los numeros pequenos parecen correctos
 *   y el error sale meses despues en un saldo que no cuadra. `toPlainString` conserva ademas la
 *   escala, asi que `"186400.0000"` vuelve con sus cuatro decimales y no como `"186400"`.
 * - **Las fechas de registro van como `YYYY-MM-DD`, nunca como epoch.** Un epoch convierte la
 *   fecha a un instante, y entonces el movimiento salta de dia segun donde este el usuario.
 *
 * Los enums se guardan por su valor `wire`, que es el del contrato de la API y no cambia, en vez
 * de por el nombre de la constante de Kotlin, que es un detalle del codigo.
 */
internal class DatabaseConverters {

    @TypeConverter
    fun fromDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun fromDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromRatesStatus(value: RatesStatus?): String? = value?.name

    @TypeConverter
    fun toRatesStatus(value: String?): RatesStatus? =
        value?.let { name -> RatesStatus.entries.firstOrNull { it.name == name } }
}
