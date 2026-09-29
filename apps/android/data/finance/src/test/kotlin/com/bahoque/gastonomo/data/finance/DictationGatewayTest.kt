package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.ApiConfig
import com.bahoque.gastonomo.data.network.createApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El borrador del dictado leido de la API.
 *
 * Lo que se vigila aqui es que el importe siga siendo decimal exacto, que el dia del dispositivo
 * viaje en el cuerpo y que un cuerpo que no cuadra **no** se convierta en media tarjeta: el JSON
 * lo compone un modelo, asi que puede llegar cualquier cosa.
 */
class DictationGatewayTest {

    @Test
    fun `a dictated movement keeps every digit of its amount`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { MOVEMENT_DRAFT })
        val draft = (gateway.parse("gasté 45 mil", TODAY) as Outcome.Success).value
        assertEquals(BigDecimal("45000.0000"), draft.amount)
        assertEquals(CurrencyCode.COP, draft.currency)
        assertEquals(LocalDate.of(2026, 9, 28), draft.occurredOn)
        val entry = draft.entry as DictatedEntry.Movement
        assertEquals(MovementKind.OUT, entry.kind)
        assertEquals("Alimentación", entry.category.name)
        assertEquals("Almuerzo", entry.note)
    }

    @Test
    fun `the text and the device day travel in the body`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createDictationGateway(clientOf(requests) { MOVEMENT_DRAFT })
        gateway.parse("gasté 45 mil", TODAY)
        val body = (requests.single().body as TextContent).text
        assertTrue(body.contains("\"text\":\"gasté 45 mil\""), body)
        assertTrue(body.contains("\"today\":\"2026-09-28\""), body)
        assertEquals("/v1/dictation/parse", requests.single().url.encodedPath)
    }

    @Test
    fun `a dictated loan carries its direction and its person`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { LOAN_DRAFT })
        val draft = (gateway.parse("le presté 200 mil a Rosa", TODAY) as Outcome.Success).value
        val entry = draft.entry as DictatedEntry.Loan
        assertEquals(LoanDirection.LENT, entry.direction)
        assertEquals("Rosa", entry.person)
    }

    @Test
    fun `what the API says is missing is what comes through`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { NO_AMOUNT })
        val draft = (gateway.parse("almuerzo", TODAY) as Outcome.Success).value
        assertNull(draft.amount)
        assertEquals(setOf(MissingField.AMOUNT), draft.missing)
    }

    @Test
    fun `an unknown missing field is ignored instead of breaking the draft`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { UNKNOWN_MISSING })
        val draft = (gateway.parse("almuerzo", TODAY) as Outcome.Success).value
        assertEquals(setOf(MissingField.AMOUNT), draft.missing)
    }

    @Test
    fun `a kind that does not match the body fails instead of half a card`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { LOAN_KIND_NO_LOAN })
        assertTrue(gateway.parse("algo", TODAY) is Outcome.Failure)
    }

    @Test
    fun `an unreadable date fails instead of a draft dated today by accident`() = runTest {
        val gateway = createDictationGateway(clientOf(mutableListOf()) { BAD_DATE })
        val error = (gateway.parse("algo", TODAY) as Outcome.Failure).error
        assertTrue(error is ApiError.Unexpected)
    }

    @Test
    fun `an account without dictation approved gets a not found`() = runTest {
        val gateway = createDictationGateway(
            createApiClient(
                sessionStore = FakeSessionStore(SampleAuth.session),
                config = ApiConfig(baseUrl = "https://api.test", loggingEnabled = false),
                engine = MockEngine {
                    respond(
                        """{"title":"Not Found","status":404}""",
                        HttpStatusCode.NotFound,
                        headersOf("Content-Type", "application/problem+json"),
                    )
                },
            ),
        )
        assertEquals(ApiError.NotFound, (gateway.parse("algo", TODAY) as Outcome.Failure).error)
    }

    private fun clientOf(requests: MutableList<HttpRequestData>, body: () -> String) =
        createApiClient(
            sessionStore = FakeSessionStore(SampleAuth.session),
            config = ApiConfig(baseUrl = "https://api.test", loggingEnabled = false),
            engine = MockEngine { request ->
                requests += request
                respond(body(), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            },
        )

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 9, 28)

        const val CATEGORY = """{"id":"11111111-1111-4111-8111-111111111111","name":"Alimentación"}"""

        const val MOVEMENT_DRAFT = """{"kind":"movement",
            "movement":{"kind":"out","category":$CATEGORY,"note":"Almuerzo"},
            "amount":"45000.0000","currency":"COP","occurredOn":"2026-09-28","missing":[]}"""

        const val LOAN_DRAFT = """{"kind":"loan",
            "loan":{"direction":"lent","person":"Rosa","note":""},
            "amount":"200000.0000","currency":"COP","occurredOn":"2026-09-28","missing":[]}"""

        const val NO_AMOUNT = """{"kind":"movement",
            "movement":{"kind":"out","category":$CATEGORY,"note":"Almuerzo"},
            "amount":null,"currency":"COP","occurredOn":"2026-09-28","missing":["amount"]}"""

        const val UNKNOWN_MISSING = """{"kind":"movement",
            "movement":{"kind":"out","category":$CATEGORY,"note":"Almuerzo"},
            "amount":null,"currency":"COP","occurredOn":"2026-09-28",
            "missing":["amount","lo_que_sea"]}"""

        const val LOAN_KIND_NO_LOAN = """{"kind":"loan","amount":"1.0000","currency":"COP",
            "occurredOn":"2026-09-28","missing":[]}"""

        const val BAD_DATE = """{"kind":"movement",
            "movement":{"kind":"out","category":$CATEGORY,"note":"Almuerzo"},
            "amount":"1.0000","currency":"COP","occurredOn":"ayer","missing":[]}"""
    }
}
