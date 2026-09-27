package com.bahoque.gastonomo.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Movimientos guardados. Las consultas son las dos que piden las pantallas: el periodo entero, del
 * que salen el balance, la tendencia y los graficos, y los ultimos N de Inicio.
 *
 * Las fechas se comparan como texto `YYYY-MM-DD`, que en ese formato ordena igual que la fecha.
 */
@Dao
internal interface MovementDao {

    @Query(
        "SELECT * FROM movements WHERE occurred_on BETWEEN :from AND :to " +
            "ORDER BY occurred_on DESC, id DESC",
    )
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<MovementEntity>>

    @Query(
        "SELECT * FROM movements WHERE occurred_on BETWEEN :from AND :to " +
            "ORDER BY occurred_on DESC, id DESC LIMIT :limit",
    )
    fun observeRecent(from: LocalDate, to: LocalDate, limit: Int): Flow<List<MovementEntity>>

    @Query(
        "SELECT * FROM movements WHERE occurred_on BETWEEN :from AND :to " +
            "ORDER BY occurred_on DESC, id DESC",
    )
    suspend fun between(from: LocalDate, to: LocalDate): List<MovementEntity>

    @Upsert
    suspend fun upsert(movements: List<MovementEntity>)

    /**
     * Reemplaza el contenido de un periodo. Va en una transaccion para que una lista a medias no
     * llegue a verse: entre el borrado y la insercion el balance seria falso.
     */
    @Transaction
    suspend fun replaceBetween(from: LocalDate, to: LocalDate, movements: List<MovementEntity>) {
        deleteBetween(from, to)
        upsert(movements)
    }

    /**
     * Borra lo **ya subido** de un periodo. Nunca lo pendiente: un refresco que se llevara por
     * delante lo que el usuario acaba de registrar sin conexion seria perderle dinero, que es el
     * peor fallo posible de esta aplicacion.
     */
    @Query("DELETE FROM movements WHERE occurred_on BETWEEN :from AND :to AND pending = 0")
    suspend fun deleteBetween(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM movements")
    suspend fun clear()
}
