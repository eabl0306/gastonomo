package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Categorias, ajustes y catalogo con la cache delante.
 *
 * El caso que mas importa es el ultimo: **cambiar la moneda principal no vuelve a pedir los
 * registros**. Los importes guardados son los cuatro campos que devolvio la API y la
 * conversion la hace el cliente al pintar, asi que lo que ya esta en memoria sigue valiendo.
 */
class CachedCatalogGatewaysTest {

    @Test
    fun `the category list is asked once`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.categories.list()
        wired.categories.list()

        assertEquals(1, wired.categorySpy.listCalls)
    }

    @Test
    fun `creating a category refreshes its list`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.categories.list()

        wired.categories.create(Drafts.category)

        wired.categories.list()
        assertEquals(2, wired.categorySpy.listCalls)
    }

    @Test
    fun `renaming a category refreshes the movements that show it`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)

        wired.categories.update(CATEGORY, Drafts.category)

        wired.movements.list(FROM, TO)
        assertEquals(2, wired.movementSpy.listCalls)
    }

    @Test
    fun `deleting a category refreshes the movements it reassigned`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)
        wired.categories.list()

        wired.categories.delete(CATEGORY)

        wired.movements.list(FROM, TO)
        wired.categories.list()
        // Borrar reasigna los movimientos a Otros: una lista ya cargada pintaria un tile que
        // ya no existe.
        assertEquals(2, wired.movementSpy.listCalls)
        assertEquals(2, wired.categorySpy.listCalls)
    }

    @Test
    fun `a rejected category write does not throw the cache away`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.categories.list()
        wired.categorySpy.failure = ApiError.Conflict(code = "category_name_taken")

        wired.categories.create(Drafts.category)

        wired.categorySpy.failure = null
        wired.categories.list()
        assertEquals(1, wired.categorySpy.listCalls)
    }

    @Test
    fun `the settings are asked once`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.settings.load()
        wired.settings.load()

        assertEquals(1, wired.settingsSpy.loadCalls)
    }

    @Test
    fun `changing the main currency refreshes the settings but not the records`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.settings.load()
        wired.movements.list(FROM, TO)
        wired.loans.list()

        wired.settings.update(displayCurrency = CurrencyCode.USD)

        wired.settings.load()
        wired.movements.list(FROM, TO)
        wired.loans.list()
        assertEquals(2, wired.settingsSpy.loadCalls)
        assertEquals(1, wired.movementSpy.listCalls)
        assertEquals(1, wired.loanSpy.listCalls)
    }

    @Test
    fun `the currency catalog and the rates are asked once`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.currencies.catalog()
        wired.currencies.catalog()
        wired.currencies.rates()
        wired.currencies.rates()

        assertEquals(1, wired.currencySpy.catalogCalls)
        assertEquals(1, wired.currencySpy.ratesCalls)
    }

    @Test
    fun `a cold start keeps retrying the rates`() = runTest {
        val wired = WiredCaches(backgroundScope)
        // 503 mientras la tarea de tasas no consigue su primera pasada. No es un fallo del
        // cliente: hay que reintentar, asi que no se puede guardar.
        wired.currencySpy.failure = ApiError.ServiceUnavailable

        wired.currencies.rates()
        wired.currencies.rates()

        assertEquals(2, wired.currencySpy.ratesCalls)
    }

    private companion object {
        val FROM = LocalDate.parse("2026-09-01")
        val TO = LocalDate.parse("2026-09-30")
        val CATEGORY = SampleFinance.pets.id
    }
}
