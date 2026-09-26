package com.bahoque.gastonomo.core.application.usecase.movement

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period

/**
 * Todos los movimientos de un periodo, agotando el cursor.
 *
 * Hace falta la lista **completa** porque de ella salen cuatro vistas del diseno: la
 * tarjeta de balance, la tendencia, los gastos por categoria y el resumen de tres
 * columnas. La lista pagina a 500 como maximo, asi que para "Este ano" o un rango largo
 * hay que recorrer varias paginas antes de dibujar el primer grafico.
 *
 * Se pide **sin filtrar por tipo** a proposito: las pills Todos / Ingresos / Gastos solo
 * filtran la lista de abajo, y el resumen de tres columnas se calcula sobre el periodo
 * entero. Con `kind=out` no se podria pintar la columna de Ingresos.
 */
class LoadPeriodMovements(private val gateway: MovementGateway) {

    suspend operator fun invoke(
        period: Period,
        pageSize: Int = MAX_PAGE_SIZE,
    ): Outcome<List<Movement>, ApiError> {
        val all = mutableListOf<Movement>()
        var cursor: String? = null
        var pages = 0
        do {
            val page = gateway.list(period.from, period.to, null, cursor, pageSize)
            if (page is Outcome.Failure) return page
            val value = (page as Outcome.Success).value
            all += value.items
            cursor = value.nextCursor
            pages++
        } while (cursor != null && pages < MAX_PAGES)
        return Outcome.Success(all)
    }

    companion object {
        /** Lo maximo que acepta la API; un valor mayor se acota en silencio. */
        const val MAX_PAGE_SIZE = 500

        /**
         * Tope de paginas, para que un cursor que no avance no deje la aplicacion dando
         * vueltas. 200 paginas son 100.000 movimientos: mas que cualquier cuenta personal.
         */
        const val MAX_PAGES = 200
    }
}
