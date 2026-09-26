package com.bahoque.gastonomo.core.domain.loan

/** Preste o me prestaron. Es el valor de `direction` en la API y el de las pills. */
enum class LoanDirection(val wire: String) {
    /** Preste: me deben. Se registra como gasto y cada abono recibido como ingreso. */
    LENT("lent"),

    /** Me prestaron: debo. Se registra como ingreso y cada abono pagado como gasto. */
    BORROWED("borrowed"),
    ;

    companion object {
        fun fromWire(wire: String): LoanDirection? = entries.firstOrNull { it.wire == wire }
    }
}
