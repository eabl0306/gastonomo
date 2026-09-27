package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.data.database.dao.PendingWriteDao
import com.bahoque.gastonomo.data.database.decodePendingDraft
import com.bahoque.gastonomo.data.database.encode
import com.bahoque.gastonomo.data.database.entity.PendingWriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * La cola de escrituras sin subir.
 *
 * El orden de [PendingWrite.queuedAt] sale de un contador que solo sube, no del reloj: una hora
 * que se ajusta hacia atras reordenaria la cola, y subir un abono antes que su prestamo no
 * funciona.
 */
class LocalPendingWrites internal constructor(private val dao: PendingWriteDao) {

    suspend fun all(): List<PendingWrite> = dao.all().map { it.toDomain() }

    fun observe(): Flow<List<PendingWrite>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun count(): Int = dao.count()

    /** Mete una escritura al final de la cola. La clave de idempotencia ya viene con ella. */
    suspend fun enqueue(write: PendingWrite) {
        dao.upsert(write.toEntity(queuedAt = dao.nextPosition()))
    }

    /** La quita, porque ya se subio. */
    suspend fun remove(id: String) {
        dao.delete(id)
    }

    /** Deja constancia de un intento fallido pero recuperable. */
    suspend fun markAttempted(id: String) {
        dao.bumpAttempts(id)
    }

    /** La marca como rota: deja de reintentarse y hay que corregirla a mano. */
    suspend fun markFailed(id: String, reason: String) {
        dao.markFailed(id, reason)
    }

    /**
     * Cambia el borrador de una escritura rota, conservando su clave de idempotencia.
     *
     * Conservarla es correcto y esta en el ADR 0012: un 422 no quema la clave, asi que se puede
     * corregir el cuerpo y reintentar con la misma sin arriesgarse a duplicar.
     */
    suspend fun replaceDraft(id: String, draft: PendingDraft) {
        dao.replacePayload(id, draft.encode())
    }

    /** Un prestamo pendiente ya subio: sus abonos pasan a apuntar al identificador de verdad. */
    suspend fun resolveDependency(pendingId: String, loanId: LoanId) {
        dao.resolveDependentsOf(pendingId, loanId.value)
    }
}

private fun PendingWriteEntity.toDomain() = PendingWrite(
    id = id,
    idempotencyKey = IdempotencyKey(idempotencyKey),
    draft = decodePendingDraft(payload),
    loanId = loanId?.let(::LoanId),
    dependsOn = dependsOn,
    queuedAt = queuedAt,
    attempts = attempts,
    failure = failure,
)

private fun PendingWrite.toEntity(queuedAt: Long) = PendingWriteEntity(
    id = id,
    kind = draft.kindName(),
    payload = draft.encode(),
    idempotencyKey = idempotencyKey.value,
    loanId = loanId?.value,
    dependsOn = dependsOn,
    queuedAt = queuedAt,
    attempts = attempts,
    failure = failure,
)

private fun PendingDraft.kindName(): String = when (this) {
    is PendingDraft.Movement -> "movement"
    is PendingDraft.Loan -> "loan"
    is PendingDraft.Payment -> "payment"
}
