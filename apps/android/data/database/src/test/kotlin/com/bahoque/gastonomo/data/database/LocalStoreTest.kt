package com.bahoque.gastonomo.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.data.database.local.LocalStore
import com.bahoque.gastonomo.data.database.local.createInMemoryLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La cara publica del modulo, que es lo unico que ve el resto de la aplicacion: dominio dentro,
 * dominio fuera, y ni una entidad de Room asomando.
 */
@RunWith(AndroidJUnit4::class)
class LocalStoreTest {

    private lateinit var store: LocalStore

    @Before
    fun open() {
        store = createInMemoryLocalStore(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun close() {
        store.close()
    }

    @Test
    fun `a period goes in and comes back whole`() = runTest {
        store.movements.replace(FROM, TO, SampleFinance.movements)

        assertEquals(SampleFinance.movements.size, store.movements.between(FROM, TO).size)
    }

    @Test
    fun `the kind filters and the limit trims, in that order`() = runTest {
        store.movements.replace(FROM, TO, SampleFinance.movements)

        val expenses = store.movements.between(FROM, TO, MovementKind.OUT)
        val trimmed = store.movements.between(FROM, TO, limit = 1)

        assertTrue(expenses.all { it.kind == MovementKind.OUT })
        assertEquals(2, expenses.size)
        assertEquals(1, trimmed.size)
    }

    @Test
    fun `replacing a period leaves only what came in`() = runTest {
        store.movements.replace(FROM, TO, SampleFinance.movements)

        store.movements.replace(FROM, TO, listOf(SampleFinance.groceries))

        assertEquals(listOf(SampleFinance.groceries), store.movements.between(FROM, TO))
    }

    @Test
    fun `saving one movement adds it without touching the rest`() = runTest {
        store.movements.replace(FROM, TO, SampleFinance.movements)

        store.movements.save(SampleFinance.groceries.copy(title = "Otro"))

        assertEquals(SampleFinance.movements.size, store.movements.between(FROM, TO).size)
        assertTrue(store.movements.between(FROM, TO).any { it.title == "Otro" })
    }

    @Test
    fun `loans come back with their payments and can be filtered by direction`() = runTest {
        store.loans.replaceAll(SampleFinance.loans)

        val lent = store.loans.all(LoanDirection.LENT)

        assertEquals(2, lent.size)
        assertEquals(SampleFinance.partialLoan, store.loans.byId(SampleFinance.partialLoan.id))
    }

    @Test
    fun `a loan that is not there is null, not an empty loan`() = runTest {
        assertNull(store.loans.byId(SampleFinance.partialLoan.id))
    }

    @Test
    fun `replacing the loans does not leave the old ones behind`() = runTest {
        store.loans.replaceAll(SampleFinance.loans)

        store.loans.replaceAll(listOf(SampleFinance.pendingLoan))

        assertEquals(listOf(SampleFinance.pendingLoan), store.loans.all())
    }

    @Test
    fun `categories, settings, profile and rates go in and come back`() = runTest {
        store.categories.replaceAll(SampleFinance.categories)
        store.account.saveSettings(UserSettings(CurrencyCode.USD, AppLanguage.ENGLISH))
        store.account.saveProfile(SampleAuth.user)
        store.currencies.saveCatalog(SampleFinance.currencies)
        store.currencies.saveRates(SampleFinance.rates)

        assertEquals(SampleFinance.categories.size, store.categories.all().size)
        assertEquals(CurrencyCode.USD, store.account.settings()!!.displayCurrency)
        assertEquals(SampleAuth.user, store.account.profile())
        assertEquals(SampleFinance.currencies.size, store.currencies.catalog().size)
        assertEquals(SampleFinance.rates, store.currencies.rates())
    }

    @Test
    fun `with nothing stored the rates are the empty ones and the rest is empty too`() = runTest {
        assertEquals(ExchangeRates.EMPTY, store.currencies.rates())
        assertTrue(store.categories.all().isEmpty())
        assertTrue(store.currencies.catalog().isEmpty())
        assertNull(store.account.settings())
        assertNull(store.account.profile())
    }

    @Test
    fun `clearing leaves nothing of the account`() = runTest {
        store.movements.replace(FROM, TO, SampleFinance.movements)
        store.loans.replaceAll(SampleFinance.loans)
        store.categories.replaceAll(SampleFinance.categories)
        store.account.saveProfile(SampleAuth.user)

        store.clear()

        assertTrue(store.movements.between(FROM, TO).isEmpty())
        assertTrue(store.loans.all().isEmpty())
        assertTrue(store.categories.all().isEmpty())
        assertNull(store.account.profile())
    }

    private companion object {
        val FROM: LocalDate = LocalDate.parse("2026-09-01")
        val TO: LocalDate = LocalDate.parse("2026-09-30")
    }
}
