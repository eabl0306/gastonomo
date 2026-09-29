package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.SpeechToText
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.dictation.ParseDictation
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings

/**
 * Lo que necesita el dictado. Lo implementa la raiz de composicion en `DictationWiring.kt`:
 * este modulo no conoce a ningun adaptador.
 *
 * Guardar no es una llamada nueva: son los mismos casos de uso que usan los formularios, con su
 * clave de idempotencia. Un borrador dictado no es un registro, y crearlo tiene que pasar por
 * donde pasa todo lo demas.
 */
interface DictationDependencies {

    val parseDictation: ParseDictation

    val createMovement: CreateMovement

    val createLoan: CreateLoan

    /** El idioma de la cuenta y la moneda principal, para hablarle al sistema y para convertir. */
    val loadSettings: LoadSettings

    val loadExchangeRates: LoadExchangeRates

    /** El reconocimiento de voz del sistema. */
    val speechToText: SpeechToText

    /** El toast que sale al guardar, encima de la pantalla desde la que se dicto. */
    val messages: TransientMessages

    val clock: Clock
}
