package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.database.local.LocalMovements
import java.time.LocalDate

/**
 * Movimientos leidos de la base local, con la red solo rellenandola.
 *
 * Lo que consigue: en modo avion, despues de haber abierto la aplicacion con red una vez, la
 * pantalla ensena los movimientos igual, porque nunca dependio de la red para pintarlos.
 *
 * La paginacion la resuelve este adaptador y no el caso de uso: agota el cursor, guarda el periodo
 * entero y devuelve una sola pagina sin cursor. Asi `limit` solo recorta lo que se devuelve y
 * nunca decide lo que se guarda, que es como Inicio pidiendo los cuatro ultimos borraria el mes.
 */
internal class OfflineMovementGateway(
    private val remote: MovementGateway,
    private val local: LocalMovements,
    private val gate: RefreshGate,
) : MovementGateway {

    override suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind?,
        cursor: String?,
        limit: Int?,
    ): Outcome<MovementPage, ApiError> {
        // Un cursor explicito es una peticion a la red, no una lectura de lo guardado.
        if (cursor != null) return remote.list(from, to, kind, cursor, limit)
        val key = PeriodKey(from, to)
        val stored = local.between(from, to, kind, limit)
        if (stored.isNotEmpty() || gate.isFresh(key)) {
            gate.inBackground(key) { refresh(from, to, key) }
            return Outcome.Success(MovementPage(stored, null))
        }
        // No hay nada guardado y nunca se pidio: aqui si hay que esperar a la red.
        return refresh(from, to, key).map { MovementPage(local.between(from, to, kind, limit), null) }
    }

    override suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> {
        val result = remote.create(draft, key)
        if (result is Outcome.Success) {
            local.save(result.value.value)
            // El movimiento cambia el contador de uso de su categoria y puede venir de un abono.
            gate.forget()
        }
        return result
    }

    /** Trae el periodo entero y lo reemplaza. Devuelve el fallo tal cual si la red no responde. */
    private suspend fun refresh(
        from: LocalDate,
        to: LocalDate,
        key: PeriodKey,
    ): Outcome<Unit, ApiError> {
        val all = mutableListOf<Movement>()
        var cursor: String? = null
        var pages = 0
        do {
            val page = remote.list(from, to, null, cursor, PAGE_SIZE)
            if (page is Outcome.Failure) return page
            val value = (page as Outcome.Success).value
            all += value.items
            cursor = value.nextCursor
            pages++
        } while (cursor != null && pages < MAX_PAGES)
        local.replace(from, to, all)
        gate.markFresh(key)
        return Outcome.Success(Unit)
    }

    /** Un periodo. Es la unidad que se refresca: el periodo entero o nada. */
    private data class PeriodKey(val from: LocalDate, val to: LocalDate)

    private companion object {
        /** Lo maximo que acepta la API. */
        const val PAGE_SIZE = 500

        /** Tope de paginas, para que un cursor que no avance no deje la aplicacion dando vueltas. */
        const val MAX_PAGES = 200
    }
}
