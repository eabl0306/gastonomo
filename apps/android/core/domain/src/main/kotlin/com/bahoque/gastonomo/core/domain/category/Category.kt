package com.bahoque.gastonomo.core.domain.category

/**
 * Una categoria de la cuenta. Cada movimiento la trae completa, asi que la fila de la
 * pantalla pinta su tile sin cruzar nada.
 *
 * @property icon nombre de un Material Symbol, el mismo que usa el prototipo.
 * @property color color del tile, en `#rrggbb`.
 * @property isDefault las 12 predeterminadas: no se pueden editar ni borrar.
 * @property movementCount alimenta el texto "Sin movimientos / 1 movimiento / N movimientos".
 */
data class Category(
    val id: CategoryId,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val movementCount: Int = 0,
) {
    companion object {
        /** Maximo de caracteres del nombre, el mismo que valida la API. */
        const val MAX_NAME_LENGTH = 22

        /** Los 16 iconos del sheet de categoria, en el orden del grid de 8x2. */
        val ICON_CHOICES: List<String> = (
            "shopping_cart pets fitness_center local_cafe flight checkroom child_care " +
                "sports_esports card_giftcard local_bar spa build savings subscriptions " +
                "local_gas_station volunteer_activism"
            ).split(' ')

        /** Los 8 colores del sheet de categoria, en orden. */
        val COLOR_CHOICES: List<String> =
            "#cf5f35 #c48f12 #2f8a55 #2f7fb5 #3f6fc4 #7a52c7 #b84a86 #5b6573".split(' ')

        /** Nombre de la categoria a la que se reasignan los movimientos al borrar una. */
        const val FALLBACK_NAME = "Otros"
    }
}

/** Identificador de una categoria. Es un UUID en la API. */
@JvmInline
value class CategoryId(val value: String) {
    override fun toString(): String = value
}
