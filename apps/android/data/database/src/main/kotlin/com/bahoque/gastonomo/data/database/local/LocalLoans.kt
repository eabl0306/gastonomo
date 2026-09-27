package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.data.database.dao.LoanDao
import com.bahoque.gastonomo.data.database.dao.LoanPaymentDao
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity

/** Los prestamos guardados, con sus abonos ya dentro. */
class LocalLoans internal constructor(
    private val dao: LoanDao,
    private val payments: LoanPaymentDao,
) {

    suspend fun all(direction: LoanDirection? = null): List<Loan> {
        val byLoan = payments.all().groupBy { it.loanId }
        return dao.loans()
            .map { loan -> loan.toDomain(byLoan[loan.id].orEmpty().map { it.toDomain() }) }
            .filter { direction == null || it.direction == direction }
    }

    suspend fun byId(id: LoanId): Loan? = dao.loan(id.value)
        ?.toDomain(payments.of(id.value).map { it.toDomain() })

    /** Guarda un prestamo con sus abonos. Es lo que devuelven crear un prestamo y un abono. */
    suspend fun save(loan: Loan) {
        dao.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of), payments)
    }

    /**
     * Deja la lista como la devuelve la API, sin tocar lo que todavia no ha salido del telefono.
     */
    suspend fun replaceAll(loans: List<Loan>) {
        dao.deleteSyncedNotIn(loans.map { it.id.value })
        loans.forEach { save(it) }
    }

    /** Guarda un abono suelto, que es lo que hace falta al registrar uno sin conexion. */
    suspend fun savePayment(payment: LoanPayment) {
        payments.upsert(listOf(LoanPaymentEntity.of(payment)))
    }

    /** Borra un prestamo pendiente y sus abonos, cuando ya se subio y llego el de verdad. */
    suspend fun forget(id: LoanId) {
        dao.deleteById(id.value)
    }

    /** Borra un abono provisional por identificador. */
    suspend fun forgetPayment(id: LoanPaymentId) {
        payments.deleteById(id.value)
    }
}
