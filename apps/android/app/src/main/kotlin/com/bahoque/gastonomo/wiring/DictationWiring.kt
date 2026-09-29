package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.dictation.DictationDependencies

/**
 * Lo que `:feature:dictation` necesita, sacado del grafo de la aplicacion.
 *
 * Guardar un borrador dictado pasa por los mismos casos de uso que los formularios, asi que aqui
 * no hay nada nuevo: el dictado solo anade la interpretacion y el reconocimiento de voz.
 */
internal fun dictationDependencies(useCases: UseCases): DictationDependencies =
    object : DictationDependencies {
        override val parseDictation = useCases.parseDictation
        override val createMovement = useCases.createMovement
        override val createLoan = useCases.createLoan
        override val loadSettings = useCases.loadSettings
        override val loadExchangeRates = useCases.loadExchangeRates
        override val speechToText = useCases.speechToText
        override val messages = sharedMessages
        override val clock = useCases.clock
    }
