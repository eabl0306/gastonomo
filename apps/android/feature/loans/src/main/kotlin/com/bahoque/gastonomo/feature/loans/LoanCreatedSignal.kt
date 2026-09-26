package com.bahoque.gastonomo.feature.loans

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * El aviso de "se creo un prestamo" que va de Nuevo prestamo a Prestamos.
 *
 * Hace falta por dos razones. La lista vive en su propia entrada de la pila y no se vuelve a
 * cargar al volver, asi que sin esto el prestamo recien creado no saldria. Y el toast
 * "Prestamo creado" del diseno se pinta sobre Prestamos, no sobre el formulario, que ya se
 * cerro.
 *
 * Lo crea el grafo de la feature y lo comparten sus dos pantallas, asi que no es estado
 * global: vive lo que vive el NavHost.
 */
class LoanCreatedSignal {

    private val mutable = MutableStateFlow(0)

    /** Cambia cada vez que se crea un prestamo. */
    val ticks: StateFlow<Int> = mutable.asStateFlow()

    /** Lo llama el formulario cuando la API creo el prestamo de verdad. */
    fun raise() = mutable.update { it + 1 }
}
