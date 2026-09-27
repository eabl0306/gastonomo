package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.database.dao.MovementDao
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import java.time.LocalDate

/**
 * Los movimientos guardados, en terminos de dominio.
 *
 * Es la cara publica del modulo: fuera de aqui nadie ve una entidad de Room ni un DAO, igual que
 * nadie ve un DTO de Ktor fuera de `:data:network`.
 */
class LocalMovements internal constructor(private val dao: MovementDao) {

    /**
     * Lo guardado de un periodo, del mas reciente al mas antiguo.
     *
     * El filtro por tipo se aplica aqui y no en la consulta porque el resumen de tres columnas se
     * calcula sobre el periodo **sin filtrar**: hace falta traerlo entero de todos modos.
     */
    suspend fun between(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind? = null,
        limit: Int? = null,
    ): List<Movement> = dao.between(from, to)
        .map { it.toDomain() }
        .filter { kind == null || it.kind == kind }
        .let { movements -> limit?.let(movements::take) ?: movements }

    /** Reemplaza lo que hubiera en ese periodo por lo que acaba de traer la red. */
    suspend fun replace(from: LocalDate, to: LocalDate, movements: List<Movement>) {
        dao.replaceBetween(from, to, movements.map(MovementEntity::of))
    }

    /** Guarda uno solo, que es lo que devuelve crear un movimiento. */
    suspend fun save(movement: Movement) {
        dao.upsert(listOf(MovementEntity.of(movement)))
    }

    /** Borra uno por identificador. Se usa para quitar el provisional cuando llega el de verdad. */
    suspend fun forget(id: MovementId) {
        dao.deleteById(id.value)
    }
}
