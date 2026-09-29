package com.bahoque.gastonomo.core.application.usecase.dictation

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.DictationGateway
import com.bahoque.gastonomo.core.common.Outcome

/**
 * Convierte lo que el usuario dicto en un borrador para revisar.
 *
 * El dia se saca del reloj del dispositivo y no del servidor: "ayer" depende de la zona
 * horaria de quien habla.
 */
class ParseDictation(
    private val gateway: DictationGateway,
    private val clock: Clock,
) {
    suspend operator fun invoke(text: String): Outcome<DictationDraft, ApiError> =
        gateway.parse(text.trim(), clock.today())
}
