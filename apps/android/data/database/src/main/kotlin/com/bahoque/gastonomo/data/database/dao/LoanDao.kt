package com.bahoque.gastonomo.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Prestamos y sus abonos. El estado, el porcentaje y el orden de la lista no se guardan: los
 * deriva el cliente, porque dependen de la moneda principal del momento.
 */
@Dao
internal interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY opened_on DESC, id DESC")
    fun observeLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY opened_on DESC, id DESC")
    suspend fun loans(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun loan(id: String): LoanEntity?

    @Query("SELECT * FROM loan_payments ORDER BY paid_on DESC, id DESC")
    suspend fun payments(): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE loan_id = :loanId ORDER BY paid_on DESC, id DESC")
    suspend fun paymentsOf(loanId: String): List<LoanPaymentEntity>

    @Upsert
    suspend fun upsertLoans(loans: List<LoanEntity>)

    @Upsert
    suspend fun upsertPayments(payments: List<LoanPaymentEntity>)

    /**
     * Guarda un prestamo con sus abonos de una pieza. En una transaccion porque un prestamo sin
     * sus abonos aparenta un saldo pendiente que no es el que hay.
     */
    @Transaction
    suspend fun upsert(loan: LoanEntity, payments: List<LoanPaymentEntity>) {
        upsertLoans(listOf(loan))
        upsertPayments(payments)
    }

    @Query("DELETE FROM loans")
    suspend fun clear()
}
