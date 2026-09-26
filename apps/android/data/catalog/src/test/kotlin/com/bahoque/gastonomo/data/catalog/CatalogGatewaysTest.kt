package com.bahoque.gastonomo.data.catalog

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.ApiConfig
import com.bahoque.gastonomo.data.network.createApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogGatewaysTest {

    @Test
    fun `the categories arrive with their default flag and their usage count`() = runTest {
        val gateway = createCategoryGateway(clientOf(mutableListOf()) { respondJson(CATEGORY_LIST) })
        val categories = (gateway.list() as Outcome.Success).value
        assertEquals(2, categories.size)
        assertTrue(categories.first().isDefault)
        assertEquals(12, categories.first().movementCount)
        assertEquals(CategoryKind.BOTH, categories.last().kind)
    }

    @Test
    fun `creating a category sends the four fields of the sheet`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createCategoryGateway(clientOf(requests) { respondJson(CATEGORY) })
        gateway.create(DRAFT)
        val body = requests.single().bodyText()
        assertTrue(body.contains("\"name\":\"Mascotas\""))
        assertTrue(body.contains("\"kind\":\"out\""))
        assertTrue(body.contains("\"icon\":\"pets\""))
        assertTrue(body.contains("\"color\":\"#2f7fb5\""))
    }

    @Test
    fun `editing a default category is a conflict with its code`() = runTest {
        val gateway = createCategoryGateway(
            clientOf(mutableListOf()) {
                respond(
                    """{"status":409,"code":"category_is_default"}""",
                    HttpStatusCode.Conflict,
                    headersOf("Content-Type", "application/problem+json"),
                )
            },
        )
        val error = (gateway.update(CategoryId("c1"), DRAFT) as Outcome.Failure).error
        assertEquals("category_is_default", (error as ApiError.Conflict).code)
    }

    @Test
    fun `deleting a category answers without a body`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createCategoryGateway(clientOf(requests) { respond("", HttpStatusCode.NoContent) })
        assertTrue(gateway.delete(CategoryId("c1")).isSuccess)
        assertEquals("/v1/categories/c1", requests.single().url.encodedPath)
    }

    @Test
    fun `the settings map the currency and the language`() = runTest {
        val gateway = createSettingsGateway(clientOf(mutableListOf()) { respondJson(SETTINGS) })
        val settings = (gateway.load() as Outcome.Success).value
        assertEquals(CurrencyCode.COP, settings.displayCurrency)
        assertEquals(AppLanguage.SPANISH, settings.language)
    }

    @Test
    fun `patching only sends the fields that change`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createSettingsGateway(clientOf(requests) { respondJson(SETTINGS) })
        gateway.update(language = AppLanguage.ENGLISH)
        val body = requests.single().bodyText()
        assertTrue(body.contains("\"language\":\"en\""))
        assertTrue(!body.contains("displayCurrency"))
    }

    @Test
    fun `an empty currency catalog makes the patch unavailable`() = runTest {
        val gateway = createSettingsGateway(
            clientOf(mutableListOf()) {
                respond(
                    """{"status":503}""",
                    HttpStatusCode.ServiceUnavailable,
                    headersOf("Content-Type", "application/problem+json"),
                )
            },
        )
        assertEquals(
            ApiError.ServiceUnavailable,
            (gateway.update(displayCurrency = CurrencyCode.USD) as Outcome.Failure).error,
        )
    }

    @Test
    fun `the currency catalog arrives ready to show`() = runTest {
        val gateway = createCurrencyGateway(clientOf(mutableListOf()) { respondJson(CURRENCIES) })
        val catalog = (gateway.catalog() as Outcome.Success).value
        assertEquals(listOf("COP", "EUR"), catalog.map { it.code.value })
        assertEquals("€", catalog.last().symbol)
    }

    @Test
    fun `a currency with a broken code is dropped instead of breaking the list`() = runTest {
        val gateway = createCurrencyGateway(clientOf(mutableListOf()) { respondJson(BROKEN_CURRENCIES) })
        assertEquals(1, (gateway.catalog() as Outcome.Success).value.size)
    }

    @Test
    fun `the rates arrive as strings and become decimals`() = runTest {
        val gateway = createCurrencyGateway(clientOf(mutableListOf()) { respondJson(RATES) })
        val rates = (gateway.rates() as Outcome.Success).value
        assertEquals(CurrencyCode.USD, rates.base)
        assertEquals(LocalDate.of(2026, 9, 25), rates.date)
        assertEquals(RatesStatus.OK, rates.status)
        assertEquals(BigDecimal("3316.44"), rates.rates[CurrencyCode.COP])
    }

    @Test
    fun `stale rates are served all the same and marked`() = runTest {
        val gateway = createCurrencyGateway(clientOf(mutableListOf()) { respondJson(STALE_RATES) })
        val rates = (gateway.rates() as Outcome.Success).value
        assertEquals(RatesStatus.STALE, rates.status)
        assertEquals(null, rates.date)
    }

    @Test
    fun `an unreadable rate is dropped instead of breaking the conversion`() = runTest {
        val gateway = createCurrencyGateway(clientOf(mutableListOf()) { respondJson(BROKEN_RATES) })
        val rates = (gateway.rates() as Outcome.Success).value
        assertEquals(setOf(CurrencyCode.USD), rates.rates.keys)
    }

    private fun clientOf(
        requests: MutableList<HttpRequestData>,
        handler: MockHandler,
    ) = createApiClient(
        sessionStore = FakeSessionStore(SampleAuth.session),
        config = ApiConfig(baseUrl = "https://api.test", loggingEnabled = false),
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
    )

    private fun HttpRequestData.bodyText(): String = (body as? TextContent)?.text.orEmpty()

    private companion object {
        val DRAFT = CategoryDraft("Mascotas", CategoryKind.OUT, "pets", "#2f7fb5")

        const val CATEGORY = """{"id":"c1","name":"Alimentación","kind":"out","icon":"restaurant",
            "color":"#cf5f35","isDefault":true,"movementCount":12}"""

        const val CATEGORY_LIST = """{"items":[$CATEGORY,{"id":"c2","name":"Otros","kind":"both",
            "icon":"more_horiz","color":"#7c7669","isDefault":true,"movementCount":0}]}"""

        const val SETTINGS = """{"displayCurrency":"COP","language":"es"}"""

        const val CURRENCIES = """{"items":[{"code":"COP","name":"Colombian Peso","symbol":"$"},
            {"code":"EUR","name":"Euro","symbol":"€"}]}"""

        const val BROKEN_CURRENCIES = """{"items":[{"code":"COP","name":"Colombian Peso"},
            {"code":"nope","name":"Broken"}]}"""

        const val RATES = """{"base":"USD","date":"2026-09-25","status":"ok",
            "rates":{"USD":"1","COP":"3316.44"}}"""

        const val STALE_RATES = """{"base":"USD","status":"stale","rates":{"USD":"1"}}"""

        const val BROKEN_RATES = """{"base":"USD","status":"ok",
            "rates":{"USD":"1","COP":"not a number","zz":"2"}}"""

        fun MockRequestHandleScope.respondJson(body: String) =
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    }
}

/** Lo que el MockEngine hace con cada peticion. El tipo completo no cabe en una linea. */
private typealias MockHandler =
    suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
