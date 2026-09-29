package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.core.testing.fake.sampleDictatedLoan
import com.bahoque.gastonomo.core.testing.fake.sampleDictationDraft
import java.math.BigDecimal

/** Estados del dictado para las capturas y los tests de pantalla. */
internal object DictationSamples {

    /** Escuchando, con media frase entendida. */
    val listening = DictateUiState(
        phase = DictationPhase.LISTENING,
        heard = "Almorcé por 45 mil",
        rates = SampleFinance.rates,
    )

    /** Escuchando, sin haber entendido nada todavia. */
    val hearing = DictateUiState(phase = DictationPhase.LISTENING, rates = SampleFinance.rates)

    /** Modo escrito porque el dispositivo no tiene reconocimiento. */
    val typing = DictateUiState(
        phase = DictationPhase.TYPING,
        text = "Almorcé por 45 mil",
        micFailure = SpeechFailure.UNAVAILABLE,
        rates = SampleFinance.rates,
    )

    /** Interpretando. */
    val processing = DictateUiState(
        phase = DictationPhase.PROCESSING,
        text = "Almorcé por 45 mil",
        rates = SampleFinance.rates,
    )

    /** La tarjeta de revision de un gasto, lista para guardar. */
    val review = DictateUiState(
        phase = DictationPhase.REVIEW,
        text = "Almorcé por 45 mil",
        draft = sampleDictationDraft(),
        rates = SampleFinance.rates,
    )

    /** La de un prestamo. */
    val reviewLoan = DictateUiState(
        phase = DictationPhase.REVIEW,
        text = "Le presté 200 mil a Rosa",
        draft = sampleDictatedLoan(),
        rates = SampleFinance.rates,
    )

    /** La de un gasto en otra moneda, con su equivalencia debajo. */
    val reviewForeign = DictateUiState(
        phase = DictationPhase.REVIEW,
        text = "Pagué 140 dólares del portátil",
        draft = sampleDictationDraft(
            amount = BigDecimal("140"),
            currency = CurrencyCode.USD,
        ),
        rates = SampleFinance.rates,
    )

    /** La de un dictado al que le falto el importe: Guardar apagado y como arreglarlo. */
    val reviewNoAmount = DictateUiState(
        phase = DictationPhase.REVIEW,
        text = "Almuerzo",
        draft = sampleDictationDraft(amount = null, missing = setOf(MissingField.AMOUNT)),
        rates = SampleFinance.rates,
    )

    /** Un gasto en otra moneda sin tasas guardadas: no se puede convertir, asi que no se guarda. */
    val reviewNoRate = DictateUiState(
        phase = DictationPhase.REVIEW,
        text = "Pagué 140 dólares del portátil",
        draft = sampleDictationDraft(
            amount = BigDecimal("140"),
            currency = CurrencyCode.USD,
        ),
        rates = ExchangeRates.EMPTY,
    )
}
