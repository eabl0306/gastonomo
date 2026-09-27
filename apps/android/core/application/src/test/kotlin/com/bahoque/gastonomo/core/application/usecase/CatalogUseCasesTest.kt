package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryRemoval
import com.bahoque.gastonomo.core.application.usecase.category.ArchiveCategory
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.category.RestoreCategory
import com.bahoque.gastonomo.core.application.usecase.category.SaveCategory
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.SetHideAmounts
import com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.testing.fake.FakeCategoryGateway
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeDeviceSettingsStore
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogUseCasesTest {

    @Test
    fun `the categories of the user come before the default ones`() = runTest {
        val result = LoadCategories(FakeCategoryGateway())()
        val names = (result as Outcome.Success).value.map { it.name }
        assertEquals("Mascotas", names.first())
        assertEquals(listOf("Alimentación", "Freelance", "Salario", "Servicios"), names.drop(1))
    }

    @Test
    fun `a failed category list is reported`() = runTest {
        assertFalse(LoadCategories(FakeCategoryGateway(failure = ApiError.Network))().isSuccess)
    }

    @Test
    fun `saving without an identifier creates and with one updates`() = runTest {
        val gateway = FakeCategoryGateway()
        SaveCategory(gateway)(DRAFT)
        assertEquals("Mascotas", gateway.lastDraft?.name)
        SaveCategory(gateway)(DRAFT.copy(name = " Gimnasio "), CategoryId("x"))
        assertEquals("Gimnasio", gateway.lastDraft?.name)
    }

    @Test
    fun `a taken name is reported as a conflict`() = runTest {
        val gateway = FakeCategoryGateway(failure = ApiError.Conflict("category_name_taken"))
        val result = SaveCategory(gateway)(DRAFT)
        assertEquals(ApiError.Conflict("category_name_taken"), (result as Outcome.Failure).error)
    }

    @Test
    fun `one with movements comes back archived and one without comes back deleted`() = runTest {
        val unused = SampleFinance.pets.copy(id = CategoryId("gym"), movementCount = 0)
        val gateway = FakeCategoryGateway(SampleFinance.categories + unused)

        val withMovements = ArchiveCategory(gateway)(SampleFinance.pets.id)
        val without = ArchiveCategory(gateway)(unused.id)

        assertEquals(CategoryRemoval.ARCHIVED, (withMovements as Outcome.Success).value)
        assertEquals(CategoryRemoval.DELETED, (without as Outcome.Success).value)
    }

    @Test
    fun `an archived category is not offered, and is once it comes back`() = runTest {
        val gateway = FakeCategoryGateway()
        ArchiveCategory(gateway)(SampleFinance.pets.id)

        val offered = (LoadCategories(gateway)() as Outcome.Success).value

        assertFalse(offered.any { it.id == SampleFinance.pets.id })
        // Y sigue estando, archivada: por eso se puede recuperar.
        val all = (LoadCategories(gateway)(includeArchived = true) as Outcome.Success).value
        assertTrue(all.single { it.id == SampleFinance.pets.id }.archived)

        RestoreCategory(gateway)(SampleFinance.pets.id)
        val again = (LoadCategories(gateway)() as Outcome.Success).value
        assertTrue(again.any { it.id == SampleFinance.pets.id })
    }

    @Test
    fun `the settings come from the account`() = runTest {
        val result = LoadSettings(FakeSettingsGateway())()
        assertEquals(CurrencyCode.COP, (result as Outcome.Success).value.displayCurrency)
    }

    @Test
    fun `the settings can change one field or both`() = runTest {
        val gateway = FakeSettingsGateway()
        val update = UpdateSettings(gateway)
        val currencyOnly = update(displayCurrency = CurrencyCode.USD)
        assertEquals(AppLanguage.SPANISH, (currencyOnly as Outcome.Success).value.language)
        val both = update(displayCurrency = CurrencyCode.EUR, language = AppLanguage.ENGLISH)
        assertEquals(CurrencyCode.EUR, (both as Outcome.Success).value.displayCurrency)
        assertEquals(AppLanguage.ENGLISH, both.value.language)
    }

    @Test
    fun `an empty currency catalog makes the settings patch unavailable, not invalid`() = runTest {
        val gateway = FakeSettingsGateway(failure = ApiError.ServiceUnavailable)
        val result = UpdateSettings(gateway)(displayCurrency = CurrencyCode.USD)
        assertEquals(ApiError.ServiceUnavailable, (result as Outcome.Failure).error)
        assertFalse(LoadSettings(gateway)().isSuccess)
    }

    @Test
    fun `the device settings are observed and the eye is stored`() = runTest {
        val store = FakeDeviceSettingsStore()
        assertFalse(ObserveDeviceSettings(store)().first().hideAmounts)
        SetHideAmounts(store)(true)
        assertTrue(ObserveDeviceSettings(store)().first().hideAmounts)
    }

    @Test
    fun `the currency catalog comes sorted by code`() = runTest {
        val result = LoadCurrencyCatalog(FakeCurrencyGateway())()
        assertEquals(
            listOf("COP", "EUR", "MXN", "USD"),
            (result as Outcome.Success).value.map { it.code.value },
        )
    }

    @Test
    fun `a cold start gives an empty catalog and unavailable rates`() = runTest {
        val empty = FakeCurrencyGateway(catalog = emptyList())
        assertTrue((LoadCurrencyCatalog(empty)() as Outcome.Success).value.isEmpty())
        val unavailable = FakeCurrencyGateway(failure = ApiError.ServiceUnavailable)
        assertEquals(
            ApiError.ServiceUnavailable,
            (LoadExchangeRates(unavailable)() as Outcome.Failure).error,
        )
    }

    @Test
    fun `the rates come with base usd`() = runTest {
        val result = LoadExchangeRates(FakeCurrencyGateway())()
        assertEquals(CurrencyCode.USD, (result as Outcome.Success).value.base)
        assertTrue((result.value.covers(SampleFinance.rates.base)))
    }

    private companion object {
        val DRAFT = CategoryDraft(
            name = " Mascotas ",
            kind = CategoryKind.OUT,
            icon = "pets",
            color = "#2f7fb5",
        )
    }
}
