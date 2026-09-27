package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.database.local.LocalStore
import java.time.LocalDate
import java.util.UUID

/**
 * Apunta en la base lo que el usuario registro sin conexion y lo deja en la cola de subida.
 *
 * Guarda **dos cosas por escritura**: el registro provisional, para que aparezca en la lista y
 * cuente en el balance al instante, y la fila de la cola con su clave de idempotencia. Los dos
 * llevan el mismo identificador local, que es lo que permite quitar el provisional cuando llegue
 * el de verdad.
 *
 * Crear un prestamo o un abono genera **tambien** su movimiento, igual que hace la API en una sola
 * transaccion. Aqui se apunta el movimiento provisional para que el balance cuadre en el acto,
 * pero al subir se manda **un solo POST**: el movimiento lo crea la API.
 */
internal class OfflineQueue(
    private val store: LocalStore,
    private val ids: LocalIds = RandomLocalIds,
    private val onQueued: () -> Unit = {},
) {

    /**
     * Apunta un movimiento. Devuelve null si la categoria no esta guardada, que es cuando no se
     * puede construir el registro para ensenarlo: sin haber abierto la aplicacion con red antes,
     * no hay nada con lo que pintarlo.
     */
    suspend fun movement(draft: MovementDraft, key: IdempotencyKey): Movement? {
        val category = store.categories.all().firstOrNull { it.id == draft.categoryId } ?: return null
        val id = ids.next()
        val movement = Movement(
            id = MovementId(id),
            occurredOn = draft.occurredOn,
            kind = draft.kind,
            title = draft.note?.takeIf { it.isNotBlank() } ?: category.name,
            money = draft.money,
            category = category,
            pending = true,
        )
        store.movements.save(movement)
        store.pending.enqueue(PendingWrite(id, key, PendingDraft.Movement(draft)))
        onQueued()
        return movement
    }

    /** Apunta un prestamo y el movimiento que lo acompana. */
    suspend fun loan(draft: LoanDraft, key: IdempotencyKey): Loan? {
        val id = ids.next()
        val loan = Loan(
            id = LoanId(id),
            person = draft.person,
            note = draft.note.orEmpty(),
            direction = draft.direction,
            openedOn = draft.openedOn,
            money = draft.money,
            pending = true,
        )
        store.loans.save(loan)
        store.pending.enqueue(PendingWrite(id, key, PendingDraft.Loan(draft)))
        onQueued()
        saveSideMovement(
            id,
            SideMovement(draft.openedOn, kindOfLoan(draft.direction), draft.money, draft.person),
        ) ?: return null
        return loan
    }

    /** Apunta un abono. Espera a su prestamo si ese prestamo todavia no ha subido. */
    suspend fun payment(id: LoanId, draft: LoanPaymentDraft, key: IdempotencyKey): LoanPayment? {
        val loan = store.loans.byId(id) ?: return null
        val local = ids.next()
        val payment = LoanPayment(
            id = LoanPaymentId(local),
            loanId = id,
            paidOn = draft.paidOn,
            label = draft.label,
            money = draft.money,
            pending = true,
        )
        store.loans.savePayment(payment)
        store.pending.enqueue(
            PendingWrite(
                id = local,
                idempotencyKey = key,
                draft = PendingDraft.Payment(draft),
                loanId = id,
                // Un abono de un prestamo que todavia no existe en el servidor no se puede subir.
                dependsOn = id.value.takeIf { loan.pending },
            ),
        )
        onQueued()
        saveSideMovement(
            local,
            SideMovement(draft.paidOn, kindOfPayment(loan.direction), draft.money, loan.person),
        )
        return payment
    }

    /**
     * El movimiento que la API crea junto al prestamo o al abono. Lleva el identificador de su
     * escritura con un sufijo, que es como se localiza para quitarlo cuando llega el de verdad.
     */
    private suspend fun saveSideMovement(writeId: String, side: SideMovement): Movement? {
        val category = store.categories.all().firstOrNull { it.kind.accepts(side.kind) }
            ?: return null
        val movement = Movement(
            id = MovementId(sideMovementId(writeId)),
            occurredOn = side.on,
            kind = side.kind,
            title = side.title,
            money = side.money,
            category = category,
            pending = true,
        )
        store.movements.save(movement)
        return movement
    }

    /** Lo que hace falta para apuntar el movimiento que acompana a un prestamo o a un abono. */
    private data class SideMovement(
        val on: LocalDate,
        val kind: MovementKind,
        val money: MonetaryAmount,
        val title: String,
    )

    companion object {
        /** El movimiento que acompana a una escritura de prestamo. */
        fun sideMovementId(writeId: String): String = "$writeId$SIDE_SUFFIX"

        private const val SIDE_SUFFIX = ":movement"

        /** Prestar es un gasto; que me presten, un ingreso. */
        fun kindOfLoan(direction: LoanDirection): MovementKind =
            if (direction == LoanDirection.LENT) MovementKind.OUT else MovementKind.IN

        /** Un abono recibido es un ingreso; uno pagado, un gasto. */
        fun kindOfPayment(direction: LoanDirection): MovementKind =
            if (direction == LoanDirection.LENT) MovementKind.IN else MovementKind.OUT
    }
}

/** De donde salen los identificadores locales. Es un puerto para que un test pueda fijarlos. */
internal fun interface LocalIds {
    fun next(): String
}

internal object RandomLocalIds : LocalIds {
    override fun next(): String = UUID.randomUUID().toString()
}
