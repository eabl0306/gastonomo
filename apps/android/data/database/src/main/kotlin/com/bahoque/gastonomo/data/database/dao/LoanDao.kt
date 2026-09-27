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

    @Upsert
    suspend fun upsertLoans(loans: List<LoanEntity>)

    /**
     * Guarda un prestamo con sus abonos de una pieza. En una transaccion porque un prestamo sin
     * sus abonos aparenta un saldo pendiente que no es el que hay.
     *
     * Los abonos ya subidos de ese prestamo se reemplazan; los pendientes se quedan, que es lo que
     * el usuario acaba de registrar sin conexion.
     */
    @Transaction
    suspend fun upsert(loan: LoanEntity, payments: List<LoanPaymentEntity>, dao: LoanPaymentDao) {
        upsertLoans(listOf(loan))
        dao.deleteSyncedOf(loan.id)
        dao.upsert(payments)
    }

    /**
     * Quita los prestamos ya subidos que la API ya no lista. Los pendientes no se tocan: todavia
     * no existen en el servidor, asi que no puede listarlos.
     */
    @Query("DELETE FROM loans WHERE pending = 0 AND id NOT IN (:keep)")
    suspend fun deleteSyncedNotIn(keep: List<String>)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM loans")
    suspend fun clear()

    /** Borra los prestamos ya subidos y deja los pendientes. Para la resincronizacion completa. */
    @Query("DELETE FROM loans WHERE pending = 0")
    suspend fun deleteSynced()
}

/**
 * Los abonos. Van en su propio DAO y no dentro de [LoanDao] porque se leen y se escriben por su
 * cuenta: un abono registrado sin conexion se guarda solo, sin su prestamo.
 */
@Dao
internal interface LoanPaymentDao {

    @Query("SELECT * FROM loan_payments ORDER BY paid_on DESC, id DESC")
    suspend fun all(): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE loan_id = :loanId ORDER BY paid_on DESC, id DESC")
    suspend fun of(loanId: String): List<LoanPaymentEntity>

    @Upsert
    suspend fun upsert(payments: List<LoanPaymentEntity>)

    /** Los ya subidos de un prestamo. Los pendientes se quedan: todavia no existen en la API. */
    @Query("DELETE FROM loan_payments WHERE loan_id = :loanId AND pending = 0")
    suspend fun deleteSyncedOf(loanId: String)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Borra los abonos ya subidos y deja los pendientes. Para la resincronizacion completa. */
    @Query("DELETE FROM loan_payments WHERE pending = 0")
    suspend fun deleteSyncedPayments()
}
