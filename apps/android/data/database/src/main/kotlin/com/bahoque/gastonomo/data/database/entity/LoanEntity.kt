package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import java.time.LocalDate

/**
 * Un prestamo. Los abonos viven en su propia tabla porque son muchos por prestamo y se leen
 * ordenados; el dominio los recibe embebidos, y de juntarlos se encarga el DAO.
 *
 * El indice es por direccion porque las pills de Prestamos filtran por ella.
 */
@Entity(
    tableName = "loans",
    indices = [Index(value = ["direction"])],
)
internal data class LoanEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "person") val person: String,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "direction") val direction: String,
    @ColumnInfo(name = "opened_on") val openedOn: LocalDate,
    @Embedded val money: MoneyColumns,
    @ColumnInfo(name = "pending", defaultValue = "0") val pending: Boolean = false,
) {
    fun toDomain(payments: List<LoanPayment>): Loan = Loan(
        id = LoanId(id),
        person = person,
        note = note,
        direction = LoanDirection.fromWire(direction) ?: LoanDirection.LENT,
        openedOn = openedOn,
        money = money.toDomain(),
        payments = payments,
        pending = pending,
    )

    companion object {
        fun of(loan: Loan): LoanEntity = LoanEntity(
            id = loan.id.value,
            person = loan.person,
            note = loan.note,
            direction = loan.direction.wire,
            openedOn = loan.openedOn,
            money = MoneyColumns.of(loan.money),
            pending = loan.pending,
        )
    }
}

/**
 * Un abono de un prestamo.
 *
 * La clave ajena borra en cascada: un prestamo que ya no esta no puede dejar abonos sueltos, que
 * es como se acaba sumando dinero que no existe. El indice cubre el unico acceso que hay, que es
 * "los abonos de este prestamo, del mas reciente al mas antiguo".
 */
@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["loan_id", "paid_on"],
            orders = [Index.Order.ASC, Index.Order.DESC],
        ),
    ],
)
internal data class LoanPaymentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "loan_id") val loanId: String,
    @ColumnInfo(name = "paid_on") val paidOn: LocalDate,
    @ColumnInfo(name = "label") val label: String?,
    @Embedded val money: MoneyColumns,
    @ColumnInfo(name = "pending", defaultValue = "0") val pending: Boolean = false,
) {
    fun toDomain(): LoanPayment = LoanPayment(
        id = LoanPaymentId(id),
        loanId = LoanId(loanId),
        paidOn = paidOn,
        label = label,
        money = money.toDomain(),
        pending = pending,
    )

    companion object {
        fun of(payment: LoanPayment): LoanPaymentEntity = LoanPaymentEntity(
            id = payment.id.value,
            loanId = payment.loanId.value,
            paidOn = payment.paidOn,
            label = payment.label,
            money = MoneyColumns.of(payment.money),
            pending = payment.pending,
        )
    }
}
