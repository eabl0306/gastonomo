package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.DictatedCategory
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.DictationGateway
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.application.port.SpeechEvent
import com.bahoque.gastonomo.core.application.port.SpeechSession
import com.bahoque.gastonomo.core.application.port.SpeechToText
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.math.BigDecimal
import java.time.LocalDate

/** El dictado contra la API, programable. */
class FakeDictationGateway(
    private val draft: DictationDraft = sampleDictationDraft(),
    private val failure: ApiError? = null,
) : DictationGateway {

    /** Los textos con los que se llamo, en orden. */
    val texts = mutableListOf<String>()

    /** El dia que se mando en la ultima llamada. */
    var lastToday: LocalDate? = null
        private set

    override suspend fun parse(
        text: String,
        today: LocalDate,
    ): Outcome<DictationDraft, ApiError> {
        texts += text
        lastToday = today
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(draft)
    }
}

/**
 * El reconocimiento de voz, programable.
 *
 * Los eventos se empujan a mano con [emit], que es lo que permite probar la fase de escuchar sin
 * microfono: se emite un parcial, se comprueba lo que se ve y se emite el definitivo.
 */
class FakeSpeechToText(private val available: Boolean = true) : SpeechToText {

    private val events = Channel<SpeechEvent>(Channel.UNLIMITED)

    /** Cuantas veces se abrio el microfono. */
    var starts: Int = 0
        private set

    /** Cuantas veces se pidio terminar. */
    var finishes: Int = 0
        private set

    /** El idioma con el que se abrio la ultima escucha. */
    var lastLanguageTag: String? = null
        private set

    override fun isAvailable(): Boolean = available

    override fun start(languageTag: String): SpeechSession {
        starts++
        lastLanguageTag = languageTag
        return object : SpeechSession {
            override val events: Flow<SpeechEvent> = this@FakeSpeechToText.events.receiveAsFlow()

            override fun finish() {
                finishes++
            }
        }
    }

    /** Empuja un evento a quien este escuchando. */
    suspend fun emit(event: SpeechEvent) {
        events.send(event)
    }
}

/** Un borrador de movimiento, el caso normal. */
fun sampleDictationDraft(
    amount: BigDecimal? = BigDecimal("45000"),
    currency: CurrencyCode = CurrencyCode.COP,
    occurredOn: LocalDate = LocalDate.parse("2026-09-28"),
    missing: Set<MissingField> = emptySet(),
): DictationDraft = DictationDraft(
    entry = DictatedEntry.Movement(
        kind = MovementKind.OUT,
        category = DictatedCategory(
            id = CategoryId("11111111-1111-4111-8111-111111111111"),
            name = "Alimentación",
        ),
        note = "Almuerzo",
    ),
    amount = amount,
    currency = currency,
    occurredOn = occurredOn,
    missing = missing,
)

/** Un borrador de prestamo. */
fun sampleDictatedLoan(
    person: String = "Rosa",
    missing: Set<MissingField> = emptySet(),
): DictationDraft = DictationDraft(
    entry = DictatedEntry.Loan(
        direction = LoanDirection.LENT,
        person = person,
        note = "",
    ),
    amount = BigDecimal("200000"),
    currency = CurrencyCode.COP,
    occurredOn = LocalDate.parse("2026-09-28"),
    missing = missing,
)
