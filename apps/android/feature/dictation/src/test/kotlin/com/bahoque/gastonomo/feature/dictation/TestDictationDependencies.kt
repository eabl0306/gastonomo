package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.dictation.ParseDictation
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeDictationGateway
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FakeMovementGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSpeechToText
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.sampleDictationDraft

/**
 * Dobles de todo lo que necesita el dictado. Por defecto: hay microfono, la API entiende el texto
 * y guardar funciona.
 */
@Suppress("LongParameterList") // Un parametro por camino de fallo: es lo que deja fallar solo uno.
internal class TestDictationDependencies(
    draft: DictationDraft = sampleDictationDraft(),
    parseFailure: ApiError? = null,
    createFailure: ApiError? = null,
    replay: Boolean = false,
    micAvailable: Boolean = true,
    settings: FakeSettingsGateway = FakeSettingsGateway(),
) : DictationDependencies {

    val dictationGateway = FakeDictationGateway(draft = draft, failure = parseFailure)
    val movementGateway = FakeMovementGateway(failure = createFailure, replay = replay)
    val loanGateway = FakeLoanGateway(failure = createFailure)

    /** El microfono, para mirar cuantas veces se abrio y empujarle eventos. */
    val speech = FakeSpeechToText(available = micAvailable)

    override val clock = FixedClock()

    override val parseDictation = ParseDictation(dictationGateway, clock)
    override val createMovement = CreateMovement(movementGateway)
    override val createLoan = CreateLoan(loanGateway)
    override val loadSettings = LoadSettings(settings)
    override val loadExchangeRates = LoadExchangeRates(FakeCurrencyGateway())
    override val speechToText = speech
    override val messages = TransientMessages()
}
