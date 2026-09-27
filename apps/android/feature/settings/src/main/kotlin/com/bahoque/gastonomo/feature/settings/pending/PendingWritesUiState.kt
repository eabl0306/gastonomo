package com.bahoque.gastonomo.feature.settings.pending

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.application.port.PendingWrite

/**
 * Lo que ensena la pantalla de registros sin enviar.
 *
 * @property editing el identificador de la escritura que se esta corrigiendo, o null.
 * @property amount el importe que el usuario esta tecleando en la que corrige.
 * @property confirming el identificador de la que se va a descartar, mientras se pregunta.
 */
@Immutable
data class PendingWritesUiState(
    val writes: List<PendingWrite> = emptyList(),
    val editing: String? = null,
    val amount: String = "",
    val confirming: String? = null,
) {
    val isEmpty: Boolean get() = writes.isEmpty()
}
