package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.data.database.dao.LoanDao
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity

/** Los prestamos guardados, con sus abonos ya dentro. */
class LocalLoans internal constructor(private val dao: LoanDao) {

    suspend fun all(direction: LoanDirection? = null): List<Loan> {
        val payments = dao.payments().groupBy { it.loanId }
        return dao.loans()
            .map { loan -> loan.toDomain(payments[loan.id].orEmpty().map { it.toDomain() }) }
            .filter { direction == null || it.direction == direction }
    }

    suspend fun byId(id: LoanId): Loan? = dao.loan(id.value)
        ?.toDomain(dao.paymentsOf(id.value).map { it.toDomain() })

    /** Guarda un prestamo con sus abonos. Es lo que devuelven crear un prestamo y un abono. */
    suspend fun save(loan: Loan) {
        dao.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of))
    }

    suspend fun replaceAll(loans: List<Loan>) {
        dao.clear()
        loans.forEach { save(it) }
    }
}
