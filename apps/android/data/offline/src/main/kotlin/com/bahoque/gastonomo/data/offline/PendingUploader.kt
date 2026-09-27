package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.data.database.local.LocalStore

/**
 * Sube lo que quedo pendiente, en el orden en el que el usuario lo registro.
 *
 * Recibe los adaptadores **de red sin decorar**: si recibiera los que leen de la base, subir una
 * escritura pendiente volveria a encolarla al primer corte, y la cola no bajaria nunca.
 *
 * Tres reglas:
 *
 * - **La clave de idempotencia sale de la fila, nunca se genera.** Es lo unico que evita que un
 *   reintento despues de un timeout que en realidad si llego cree el gasto dos veces.
 * - **Un fallo de red para la tanda.** Si no hay red, lo siguiente tampoco va a subir, y seguir
 *   solo sirve para gastar intentos.
 * - **Un abono espera a su prestamo.** Mientras el prestamo no exista en el servidor, su abono no
 *   tiene a que engancharse.
 */
/**
 * Quien sube lo pendiente. Es una interfaz para que el worker se pueda probar con un doble: montar
 * la base y la red entera solo para comprobar que reintenta seria probar otra cosa.
 */
interface PendingUploader {

    /** Vacia la cola hasta donde pueda. Devuelve si conviene volver a intentarlo mas tarde. */
    suspend fun uploadAll(): UploadOutcome
}

internal class QueuePendingUploader(
    private val movements: MovementGateway,
    private val loans: LoanGateway,
    private val store: LocalStore,
    private val gate: RefreshGate,
) : PendingUploader {

    override suspend fun uploadAll(): UploadOutcome {
        // Las que esperan a otra escritura o estan rotas no se tocan: la primera subira en la
        // vuelta siguiente y la segunda hasta que el usuario la corrija.
        val ready = store.pending.all().filter { !it.needsFixing && it.dependsOn == null }
        var uploaded = 0
        for (write in ready) {
            val result = send(write)
            if (result is Retry) {
                // Sin red, lo siguiente tampoco va a subir: seguir solo gasta intentos.
                store.pending.markAttempted(write.id)
                break
            }
            if (result is Broken) store.pending.markFailed(write.id, result.reason) else uploaded++
        }
        if (uploaded > 0) gate.forget()
        return UploadOutcome(uploaded = uploaded, retryLater = store.pending.count() > 0)
    }

    private suspend fun send(write: PendingWrite): SendResult = when (val draft = write.draft) {
        is PendingDraft.Movement -> sendMovement(write, draft)
        is PendingDraft.Loan -> sendLoan(write, draft)
        is PendingDraft.Payment -> sendPayment(write, draft)
    }

    private suspend fun sendMovement(
        write: PendingWrite,
        draft: PendingDraft.Movement,
    ): SendResult = when (val result = movements.create(draft.value, write.idempotencyKey)) {
        is Outcome.Success -> {
            store.movements.forget(MovementId(write.id))
            store.movements.save(result.value.value)
            store.pending.remove(write.id)
            Sent
        }
        is Outcome.Failure -> classify(result.error)
    }

    private suspend fun sendLoan(write: PendingWrite, draft: PendingDraft.Loan): SendResult =
        when (val result = loans.create(draft.value, write.idempotencyKey)) {
            is Outcome.Success -> {
                store.loans.forget(LoanId(write.id))
                store.loans.save(result.value.value)
                // El movimiento del prestamo lo creo la API: el provisional sobra.
                store.movements.forget(MovementId(OfflineQueue.sideMovementId(write.id)))
                store.pending.resolveDependency(write.id, result.value.value.id)
                store.pending.remove(write.id)
                Sent
            }
            is Outcome.Failure -> classify(result.error)
        }

    private suspend fun sendPayment(
        write: PendingWrite,
        draft: PendingDraft.Payment,
    ): SendResult {
        val loanId = write.loanId ?: return Broken(NO_LOAN)
        return when (val result = loans.addPayment(loanId, draft.value, write.idempotencyKey)) {
            is Outcome.Success -> {
                store.loans.forgetPayment(LoanPaymentId(write.id))
                store.movements.forget(MovementId(OfflineQueue.sideMovementId(write.id)))
                store.pending.remove(write.id)
                Sent
            }
            is Outcome.Failure -> classify(result.error)
        }
    }

    /**
     * Que hacer con cada fallo. Los tres casos del ADR 0012 estan aqui:
     *
     * - `idempotency_key_in_progress`: hay otro intento en vuelo, asi que se espera.
     * - `idempotency_key_reused`: la misma clave con otro cuerpo. Es un fallo nuestro, no del
     *   usuario, y reintentarlo en bucle no lo arregla.
     * - Un 422 no quema la clave: se puede corregir el cuerpo y reenviar con la misma.
     */
    private fun classify(error: ApiError): SendResult = when (error) {
        is ApiError.Network -> Retry
        is ApiError.Unauthorized -> Retry
        is ApiError.ServiceUnavailable -> Retry
        is ApiError.TooManyRequests -> Retry
        is ApiError.Conflict ->
            if (error.code == ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS) Retry else Broken(reasonOf(error))
        is ApiError.Validation -> Broken(error.fields.firstOrNull()?.message ?: INVALID)
        else -> Broken(UNEXPECTED)
    }

    private fun reasonOf(error: ApiError.Conflict): String = error.detail ?: error.code ?: UNEXPECTED

    private companion object {
        const val NO_LOAN = "El abono no sabe a que prestamo pertenece"
        const val INVALID = "La API rechazo el registro"
        const val UNEXPECTED = "No se pudo enviar"
    }
}

/** Como acabo una tanda de subida. */
data class UploadOutcome(val uploaded: Int, val retryLater: Boolean)

private sealed interface SendResult

private data object Sent : SendResult

private data object Retry : SendResult

private data class Broken(val reason: String) : SendResult
