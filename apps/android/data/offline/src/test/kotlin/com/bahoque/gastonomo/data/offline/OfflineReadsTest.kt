package com.bahoque.gastonomo.data.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Lo que promete esta capa: **la pantalla lee de la base local y la red solo la rellena**.
 *
 * El test que de verdad importa es el del modo avion: despues de haber abierto la aplicacion con
 * red una vez, apagar la red no cambia lo que se ve.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class OfflineReadsTest {

    private val scope = TestScope()

    @get:Rule
    val offline = OfflineRule(scope.backgroundScope)

    @Test
    fun `with nothing stored it waits for the network and keeps what it brought`() = runTest {
        val page = offline.movements.list(FROM, TO).valueOrNull()!!

        assertEquals(SampleFinance.movements.size, page.items.size)
        assertEquals(1, offline.movementSpy.listCalls)
        assertEquals(SampleFinance.movements.size, offline.store.movements.between(FROM, TO).size)
    }

    @Test
    fun `once stored, reading does not wait for the network`() = runTest {
        offline.movements.list(FROM, TO)
        val before = offline.movementSpy.listCalls

        offline.movements.list(FROM, TO)

        assertEquals(before, offline.movementSpy.listCalls)
    }

    @Test
    fun `in airplane mode the movements are still there`() = runTest {
        offline.movements.list(FROM, TO)
        offline.movementSpy.failure = ApiError.Network
        offline.clock.advance(LATER)

        val page = offline.movements.list(FROM, TO)

        // La red falla y la pantalla no se entera: nunca dependio de ella para pintar.
        assertEquals(SampleFinance.movements.size, page.valueOrNull()!!.items.size)
    }

    @Test
    fun `with nothing stored and no network the failure is reported`() = runTest {
        offline.movementSpy.failure = ApiError.Network

        val result = offline.movements.list(FROM, TO)

        assertEquals(Outcome.Failure(ApiError.Network), result)
    }

    @Test
    fun `a failed refresh does not count, so the next read retries`() = runTest {
        offline.movements.list(FROM, TO)
        offline.clock.advance(LATER)
        offline.movementSpy.failure = ApiError.Network
        offline.movements.list(FROM, TO)
        scope.runCurrent()
        val afterFailure = offline.movementSpy.listCalls

        offline.movementSpy.failure = null
        offline.movements.list(FROM, TO)
        scope.runCurrent()

        assertTrue(offline.movementSpy.listCalls > afterFailure, "no reintento al volver la red")
    }

    @Test
    fun `asking for the last few does not shrink what is stored`() = runTest {
        offline.movements.list(FROM, TO)

        val recent = offline.movements.list(FROM, TO, limit = FEW).valueOrNull()!!

        assertEquals(FEW, recent.items.size)
        // El limite recorta lo que se devuelve, nunca lo que se guarda.
        assertEquals(SampleFinance.movements.size, offline.store.movements.between(FROM, TO).size)
    }

    @Test
    fun `loans come from the base and the pills filter on top of it`() = runTest {
        offline.loans.list()

        val lent = offline.loans.list(LoanDirection.LENT).valueOrNull()!!

        assertEquals(2, lent.size)
        assertEquals(1, offline.loanSpy.listCalls)
    }

    @Test
    fun `a loan detail comes from the base once the list is there`() = runTest {
        offline.loans.list()

        val detail = offline.loans.detail(SampleFinance.partialLoan.id).valueOrNull()!!

        assertEquals(SampleFinance.partialLoan, detail)
        assertEquals(0, offline.loanSpy.detailCalls)
    }

    @Test
    fun `categories, settings and profile also survive without network`() = runTest {
        offline.categories.list()
        offline.settings.load()
        offline.auth.profile()
        offline.categorySpy.failure = ApiError.Network
        offline.settingsSpy.failure = ApiError.Network
        offline.authSpy.failure = ApiError.Network
        offline.clock.advance(LATER)

        assertEquals(SampleFinance.categories.size, offline.categories.list().valueOrNull()!!.size)
        assertTrue(offline.settings.load() is Outcome.Success)
        assertTrue(offline.auth.profile() is Outcome.Success)
    }

    @Test
    fun `the rates that were downloaded keep converting without network`() = runTest {
        offline.currencies.rates()
        offline.currencies.catalog()
        offline.currencySpy.failure = ApiError.Network
        offline.clock.advance(LATER)

        assertEquals(SampleFinance.rates, offline.currencies.rates().valueOrNull())
        // El catalogo vuelve ordenado por codigo, que es como lo guarda la base.
        assertEquals(
            SampleFinance.currencies.sortedBy { it.code.value },
            offline.currencies.catalog().valueOrNull(),
        )
    }

    @Test
    fun `a cold start with no rates keeps asking`() = runTest {
        // 503 mientras la tarea de tasas no consigue su primera pasada: no hay nada que guardar.
        offline.currencySpy.failure = ApiError.ServiceUnavailable

        offline.currencies.rates()
        offline.currencies.rates()

        assertEquals(2, offline.currencySpy.ratesCalls)
    }

    @Test
    fun `the derived numbers do not change for having gone through the base`() = runTest {
        val direct = PeriodSummary.of(SampleFinance.movements, CurrencyCode.COP, SampleFinance.rates)

        offline.movements.list(FROM, TO)
        val fromBase = PeriodSummary.of(
            offline.store.movements.between(FROM, TO),
            CurrencyCode.COP,
            SampleFinance.rates,
        )

        // Guardar y volver a leer no puede mover un centavo: el balance, los ingresos y los gastos
        // se calculan sobre los mismos cuatro campos crudos.
        assertEquals(direct.income, fromBase.income)
        assertEquals(direct.expense, fromBase.expense)
        assertEquals(direct.net, fromBase.net)
    }

    private companion object {
        val FROM: LocalDate = LocalDate.parse("2026-09-01")
        val TO: LocalDate = LocalDate.parse("2026-09-30")
        val LATER = 2.minutes

        /** Menos que los movimientos de ejemplo, para que el limite tenga algo que recortar. */
        const val FEW = 2
    }
}
