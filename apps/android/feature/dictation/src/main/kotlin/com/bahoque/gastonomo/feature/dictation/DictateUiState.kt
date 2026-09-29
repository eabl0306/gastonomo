package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.domain.money.ConversionSuggestion
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.money.Money

/** Las cuatro fases del dictado. */
enum class DictationPhase {
    /** El microfono esta abierto. */
    LISTENING,

    /** Modo escrito: no hay microfono, no hay permiso o el usuario lo prefiere. */
    TYPING,

    /** La API esta interpretando. */
    PROCESSING,

    /** El borrador esta listo para revisar. */
    REVIEW,
}

/**
 * Por que no se puede guardar el borrador, si es el caso.
 *
 * Quien decide que falta es la API ([DictationDraft.missing]), no el cliente: asi el movil y la
 * web no pueden discrepar. `NO_RATE` es lo unico que decide el cliente, porque depende de las
 * tasas que tenga guardadas.
 */
enum class DictationBlocker {
    MISSING_AMOUNT,
    MISSING_PERSON,
    NO_RATE,
}

/**
 * El estado de la hoja de dictado.
 *
 * @property text lo que se va a mandar a la API: el texto definitivo del microfono o lo que el
 *   usuario escribio.
 * @property heard lo que se entiende mientras habla, que se reemplaza en cada emision.
 * @property closing true mientras la hoja baja. Existe para que el cierre se vea salir antes de
 *   que se saque la pantalla de la pila.
 * @property micSupported false si el dispositivo no trae reconocimiento de voz. Entonces **no se
 *   pide el permiso del microfono**: pedirlo seria molestar por algo que no se va a poder usar.
 */
data class DictateUiState(
    val phase: DictationPhase,
    val text: String = "",
    val heard: String = "",
    val micFailure: SpeechFailure? = null,
    val problem: DictationProblem? = null,
    val draft: DictationDraft? = null,
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val saving: Boolean = false,
    val closing: Boolean = false,
    val micSupported: Boolean = true,
) {
    /** La conversion del importe dictado a la moneda principal, o null si no hay importe. */
    val suggestion: ConversionSuggestion?
        get() = draft?.amount?.let { amount ->
            ConversionSuggestion.of(
                entered = Money(amount, draft.currency),
                display = displayCurrency,
                rates = rates,
            )
        }

    /** Los cuatro campos de dinero que se mandarian a la API. */
    val money: MonetaryAmount? get() = suggestion?.takeIf { it.isValid }?.money

    /** True si la equivalencia no es el propio importe y hay que ensenarla debajo. */
    val isForeign: Boolean get() = suggestion?.isForeign == true

    /** Lo que impide guardar, o null si se puede. */
    val blocker: DictationBlocker?
        get() = when {
            draft == null -> null
            MissingField.AMOUNT in draft.missing -> DictationBlocker.MISSING_AMOUNT
            MissingField.PERSON in draft.missing -> DictationBlocker.MISSING_PERSON
            money == null -> DictationBlocker.NO_RATE
            else -> null
        }

    /** Guardar se habilita cuando no falta nada y no hay un guardado en marcha. */
    val canSave: Boolean get() = draft != null && blocker == null && !saving

    /** True si el borrador es un prestamo, que es lo que decide el icono y las filas. */
    val isLoan: Boolean get() = draft?.entry is DictatedEntry.Loan
}
