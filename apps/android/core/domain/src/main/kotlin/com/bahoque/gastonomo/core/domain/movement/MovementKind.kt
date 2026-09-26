package com.bahoque.gastonomo.core.domain.movement

/** Ingreso o gasto. Es el valor de `kind` en la API y el de las pills de Movimientos. */
enum class MovementKind(val wire: String) {
    IN("in"),
    OUT("out"),
    ;

    val isIncome: Boolean get() = this == IN

    companion object {
        fun fromWire(wire: String): MovementKind? = entries.firstOrNull { it.wire == wire }
    }
}
