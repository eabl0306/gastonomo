package com.bahoque.gastonomo.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.CurrencyEntity
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import com.bahoque.gastonomo.data.database.entity.ProfileEntity
import com.bahoque.gastonomo.data.database.entity.SettingsEntity
import com.bahoque.gastonomo.data.database.entity.rateRowsOf
import com.bahoque.gastonomo.data.database.entity.ratesOf
import com.bahoque.gastonomo.data.database.entity.snapshotOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Que lo guardado vuelva como se guardo y en el orden en el que lo piden las pantallas.
 */
@RunWith(AndroidJUnit4::class)
internal class LocalStorageTest {

    @get:Rule
    val local = LocalDatabaseRule()

    @Test
    fun `the period comes back newest first`() = runTest {
        val dao = local.database.movements()
        dao.upsert(SampleFinance.movements.map(MovementEntity::of))

        val dates = dao.between(FIRST, LAST).map { it.occurredOn }

        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `the period filters by range and does not bring the neighbours`() = runTest {
        val dao = local.database.movements()
        dao.upsert(SampleFinance.movements.map(MovementEntity::of))

        val inside = dao.between(LocalDate.parse("2026-09-10"), LAST)

        assertEquals(2, inside.size)
    }

    @Test
    fun `home asks for the last few and gets exactly those`() = runTest {
        val dao = local.database.movements()
        dao.upsert(SampleFinance.movements.map(MovementEntity::of))

        // El limite es menor que lo guardado a proposito: con cuatro de cuatro el test pasaria
        // aunque el LIMIT no estuviera.
        val recent = dao.observeRecent(FIRST, LAST, FEW).first()

        assertEquals(FEW, recent.size)
        assertEquals(
            listOf(LocalDate.parse("2026-09-15"), LocalDate.parse("2026-09-10")),
            recent.map { it.occurredOn },
        )
    }

    @Test
    fun `replacing a period leaves only what came in`() = runTest {
        val dao = local.database.movements()
        dao.upsert(SampleFinance.movements.map(MovementEntity::of))

        dao.replaceBetween(FIRST, LAST, listOf(MovementEntity.of(SampleFinance.groceries)))

        assertEquals(listOf("Mercado"), dao.between(FIRST, LAST).map { it.title })
    }

    @Test
    fun `a loan comes back with its payments`() = runTest {
        val dao = local.database.loans()
        val loan = SampleFinance.partialLoan

        dao.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of))

        val stored = dao.loan(loan.id.value)!!
        assertEquals(loan, stored.toDomain(dao.paymentsOf(loan.id.value).map { it.toDomain() }))
    }

    @Test
    fun `payments of a loan come back newest first`() = runTest {
        val dao = local.database.loans()
        val loan = SampleFinance.paidLoan

        dao.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of))

        val dates = dao.paymentsOf(loan.id.value).map { it.paidOn }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `a loan that goes takes its payments with it`() = runTest {
        val dao = local.database.loans()
        val loan = SampleFinance.partialLoan
        dao.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of))

        dao.clear()

        // Sin el borrado en cascada quedarian abonos de un prestamo que ya no existe, sumando
        // dinero que no esta.
        assertTrue(dao.payments().isEmpty())
    }

    @Test
    fun `categories come back with the users first and then by name`() = runTest {
        val dao = local.database.categories()
        dao.replaceAll(SampleFinance.categories.map(CategoryEntity::of))

        val names = dao.observeAll().first().map { it.name }

        assertEquals("Mascotas", names.first())
        assertEquals(SampleFinance.categories.size, names.size)
    }

    @Test
    fun `the settings are one row, not one per save`() = runTest {
        val dao = local.database.account()

        dao.upsertSettings(SettingsEntity.of(UserSettings()))
        dao.upsertSettings(
            SettingsEntity.of(UserSettings(CurrencyCode.USD, AppLanguage.ENGLISH)),
        )

        val stored = dao.settings()!!.toDomain()
        assertEquals(CurrencyCode.USD, stored.displayCurrency)
        assertEquals(AppLanguage.ENGLISH, stored.language)
    }

    @Test
    fun `the profile is what paints the greeting without a connection`() = runTest {
        val dao = local.database.account()

        dao.upsertProfile(ProfileEntity.of(SampleAuth.user))

        assertEquals(SampleAuth.user, dao.profile()!!.toDomain())
    }

    @Test
    fun `the rates come back whole, header and all`() = runTest {
        val dao = local.database.currencies()
        val rates = SampleFinance.rates

        dao.replaceRates(snapshotOf(rates), rateRowsOf(rates))

        assertEquals(rates, ratesOf(dao.snapshot(), dao.rates()))
    }

    @Test
    fun `replacing the rates does not leave the old ones behind`() = runTest {
        val dao = local.database.currencies()
        dao.replaceRates(snapshotOf(SampleFinance.rates), rateRowsOf(SampleFinance.rates))
        val shorter = ExchangeRates(
            base = CurrencyCode.USD,
            date = LocalDate.parse("2026-09-23"),
            status = RatesStatus.OK,
            rates = mapOf(CurrencyCode.USD to BigDecimal("1")),
        )

        dao.replaceRates(snapshotOf(shorter), rateRowsOf(shorter))

        // Una moneda que desaparece del catalogo no puede dejar su tasa colgada: convertir con
        // ella seria usar un numero que la API ya no da por bueno.
        assertEquals(shorter, ratesOf(dao.snapshot(), dao.rates()))
    }

    @Test
    fun `with nothing stored the rates are the empty ones`() = runTest {
        val dao = local.database.currencies()

        assertEquals(ExchangeRates.EMPTY, ratesOf(dao.snapshot(), dao.rates()))
        assertNull(dao.snapshot())
    }

    @Test
    fun `the currency catalog comes back sorted by code`() = runTest {
        val dao = local.database.currencies()

        dao.upsertCurrencies(SampleFinance.currencies.map(CurrencyEntity::of))

        val stored = dao.currencies()
        assertEquals(stored.map { it.code }.sorted(), stored.map { it.code })
        assertEquals(
            SampleFinance.currencies.sortedBy { it.code.value },
            stored.map { it.toDomain() },
        )
    }

    @Test
    fun `the real file opens and keeps what it was given`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = GastonomoDatabase.open(context)
        try {
            database.account().upsertProfile(ProfileEntity.of(SampleAuth.user))

            assertEquals(SampleAuth.user, database.account().profile()!!.toDomain())
        } finally {
            database.close()
            context.deleteDatabase(GastonomoDatabase.FILE_NAME)
        }
    }

    private companion object {
        val FIRST: LocalDate = LocalDate.parse("2026-09-01")
        val LAST: LocalDate = LocalDate.parse("2026-09-30")

        /** Menos que los movimientos de ejemplo, para que el limite tenga algo que recortar. */
        const val FEW = 2
    }
}
