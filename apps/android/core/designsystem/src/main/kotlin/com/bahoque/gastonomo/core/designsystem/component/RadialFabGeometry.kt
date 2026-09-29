package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * Una opcion del menu radial.
 *
 * @property icon nombre del Material Symbol.
 * @property label lo que lee un lector de pantalla. No se pinta: el diseno deja solo el icono.
 */
@Immutable
data class RadialOption(
    val icon: String,
    val label: String,
    val onSelect: () -> Unit,
)

/**
 * Donde cae cada opcion y cual esta elegida.
 *
 * Esta aparte del composable porque es aritmetica pura y se puede probar sin pintar nada, que es
 * justo lo que hace falta: lo delicado del menu radial no es como se ve, es que el dedo elija lo
 * que el usuario cree que esta eligiendo.
 */
internal object RadialFabGeometry {

    /**
     * Cuanto hay que moverse desde el centro para que empiece a contar como arrastre.
     *
     * Por debajo de esto, soltar es un toque y el menu se queda abierto para elegir con un
     * segundo toque.
     */
    const val DRAG_THRESHOLD = 14f

    /**
     * Distancia minima del centro para que haya opcion elegida.
     *
     * Cerca del boton no se resalta ninguna: ahi el dedo todavia no ha dicho hacia donde va, y
     * resaltar la mas cercana haria que la eleccion saltara de una a otra con nada de movimiento.
     */
    const val SELECTION_REACH = 46f

    /**
     * Lo alineado que tiene que estar el dedo con una opcion, como coseno del angulo.
     *
     * 0.8 son unos 37 grados. Las opciones estan a 45, asi que entre dos vecinas queda una
     * franja estrecha donde no se elige ninguna: soltar ahi cierra el menu sin hacer nada, que
     * es preferible a ejecutar la que el dedo rozo por poco.
     */
    const val SELECTION_ALIGNMENT = 0.8f

    /**
     * Las direcciones de las tres opciones: arriba, la diagonal y la izquierda.
     *
     * Son vectores unitarios porque lo que decide la eleccion es el angulo, no la distancia.
     */
    val DIRECTIONS: List<Offset> = listOf(
        Offset(0f, -1f),
        Offset(-0.70710677f, -0.70710677f),
        Offset(-1f, 0f),
    )

    /** Cuantas opciones caben en el arco del diseno. */
    val CAPACITY: Int = DIRECTIONS.size

    /**
     * Cual de las opciones esta elegida con el dedo en [offset], o `null` si ninguna.
     *
     * @param offset desplazamiento del dedo desde el centro del boton, en pixeles.
     * @param count cuantas opciones hay.
     * @param reach cuanto hay que alejarse para que cuente, en pixeles.
     */
    fun selected(offset: Offset, count: Int, reach: Float = SELECTION_REACH): Int? {
        val distance = hypot(offset.x, offset.y)
        if (distance <= reach) return null
        return (0 until minOf(count, CAPACITY))
            .map { index -> index to alignment(offset, distance, DIRECTIONS[index]) }
            .maxByOrNull { (_, value) -> value }
            ?.takeIf { (_, value) -> value >= SELECTION_ALIGNMENT }
            ?.first
    }

    /** Si el dedo se ha movido lo bastante como para que soltar no cuente como un toque. */
    fun dragged(offset: Offset): Boolean = hypot(offset.x, offset.y) > DRAG_THRESHOLD

    private fun alignment(offset: Offset, distance: Float, direction: Offset): Float =
        (offset.x * direction.x + offset.y * direction.y) / distance
}
