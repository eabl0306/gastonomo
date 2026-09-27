package com.bahoque.gastonomo.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bahoque.gastonomo.data.database.entity.PendingWriteEntity
import kotlinx.coroutines.flow.Flow

/** La cola de escrituras sin subir, en el orden en el que el usuario las registro. */
@Dao
internal interface PendingWriteDao {

    @Query("SELECT * FROM pending_writes ORDER BY queued_at ASC")
    suspend fun all(): List<PendingWriteEntity>

    @Query("SELECT * FROM pending_writes ORDER BY queued_at ASC")
    fun observeAll(): Flow<List<PendingWriteEntity>>

    @Query("SELECT COUNT(*) FROM pending_writes")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(write: PendingWriteEntity)

    @Query("DELETE FROM pending_writes WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * El prestamo del que dependian ya subio: los abonos pasan a apuntar a su identificador de
     * verdad y dejan de esperar. Va en una sola sentencia para que no exista un instante en el que
     * el abono este suelto sin prestamo al que apuntar.
     */
    @Query("UPDATE pending_writes SET loan_id = :loanId, depends_on = null WHERE depends_on = :pendingId")
    suspend fun resolveDependentsOf(pendingId: String, loanId: String)

    @Query("UPDATE pending_writes SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpAttempts(id: String)

    @Query("UPDATE pending_writes SET failure = :reason WHERE id = :id")
    suspend fun markFailed(id: String, reason: String)

    @Query("UPDATE pending_writes SET payload = :payload, failure = null WHERE id = :id")
    suspend fun replacePayload(id: String, payload: String)

    /**
     * La siguiente posicion de la cola. Es un contador que solo sube y no el reloj: una hora que se
     * ajusta hacia atras reordenaria la cola, y un abono antes que su prestamo no se puede subir.
     */
    @Query("SELECT COALESCE(MAX(queued_at), 0) + 1 FROM pending_writes")
    suspend fun nextPosition(): Long
}
