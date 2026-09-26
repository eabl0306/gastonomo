package com.bahoque.gastonomo.core.domain.category

import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * Tipo de una categoria. [BOTH] existe porque tres predeterminadas (Prestamo, Abono
 * prestamo y Otros) salen en el grid de gasto y en el de ingreso, y el nombre no puede
 * repetirse. Esta reservado a las predeterminadas: el sheet de crear categoria solo
 * ofrece [OUT] e [IN].
 */
enum class CategoryKind(val wire: String) {
    IN("in"),
    OUT("out"),
    BOTH("both"),
    ;

    /** True si esta categoria se puede ofrecer para un movimiento de tipo [kind]. */
    fun accepts(kind: MovementKind): Boolean = when (this) {
        BOTH -> true
        IN -> kind == MovementKind.IN
        OUT -> kind == MovementKind.OUT
    }

    /** True si el usuario puede elegir este tipo al crear una categoria. */
    fun isSelectableByUsers(): Boolean = this != BOTH

    companion object {
        fun fromWire(wire: String): CategoryKind? = entries.firstOrNull { it.wire == wire }

        fun of(kind: MovementKind): CategoryKind =
            if (kind == MovementKind.IN) IN else OUT
    }
}
