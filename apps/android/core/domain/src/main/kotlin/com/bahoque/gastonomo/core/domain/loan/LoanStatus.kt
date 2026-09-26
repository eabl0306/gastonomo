package com.bahoque.gastonomo.core.domain.loan

/** Estado derivado de un prestamo. La API no lo guarda ni lo calcula. */
enum class LoanStatus {
    /** El saldo es cero. */
    PAID,

    /** Hay abonos pero queda saldo. */
    PARTIAL,

    /** No hay ningun abono. */
    PENDING,
    ;

    val isOpen: Boolean get() = this != PAID
}
