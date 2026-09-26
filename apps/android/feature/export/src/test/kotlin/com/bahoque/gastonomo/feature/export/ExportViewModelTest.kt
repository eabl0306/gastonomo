package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El estado de Exportar y la generacion de los archivos. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it loads the settings, the rates, the loans and the whole history`() = runTest {
        val viewModel = loaded()
        val state = viewModel.state.value
        assertEquals(CurrencyCode.COP, state.displayCurrency)
        assertEquals(SampleFinance.rates, state.rates)
        assertEquals(SampleFinance.loans.size, state.loans.size)
        assertEquals(SampleFinance.movements.size, state.movements.size)
        assertTrue(!state.loading)
    }

    @Test
    fun `it starts on the design defaults`() = runTest {
        val state = loaded().state.value
        assertEquals(ExportFormat.XLSX, state.format)
        assertEquals(ExportSet.entries.toSet(), state.sets)
        assertEquals(ExportRange.ALL, state.range)
        assertEquals(FixedClock.PROTOTYPE_TODAY, state.today)
    }

    @Test
    fun `unticking every set disables the button`() = runTest {
        val viewModel = loaded()
        ExportSet.entries.forEach { viewModel.onSetToggled(it) }
        assertTrue(!viewModel.state.value.canExport)
    }

    @Test
    fun `the row count follows the period pill`() = runTest {
        val viewModel = loaded()
        val all = viewModel.state.value.rowsOf(ExportSet.MOVEMENTS)
        viewModel.onRangeSelected(ExportRange.PERIOD)
        assertTrue(viewModel.state.value.rowsOf(ExportSet.MOVEMENTS) <= all)
    }

    @Test
    fun `exporting to excel produces one workbook with the date in its name`() = runTest {
        val viewModel = loaded()
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        val payload = requireNotNull(viewModel.state.value.payload)
        assertEquals(1, payload.documents.size)
        assertEquals("gastonomo_2026-09-22.xlsx", payload.documents.first().fileName)
        assertEquals(XlsxWriter.MIME_TYPE, payload.documents.first().mimeType)
    }

    @Test
    fun `exporting to csv produces one file per set`() = runTest {
        val viewModel = loaded()
        viewModel.onFormatSelected(ExportFormat.CSV)
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        val payload = requireNotNull(viewModel.state.value.payload)
        assertEquals(
            listOf(
                "gastonomo_movs_2026-09-22.csv",
                "gastonomo_loans_2026-09-22.csv",
                "gastonomo_abonos_2026-09-22.csv",
            ),
            payload.documents.map { it.fileName },
        )
    }

    @Test
    fun `only the ticked sets are exported`() = runTest {
        val viewModel = loaded()
        viewModel.onFormatSelected(ExportFormat.CSV)
        viewModel.onSetToggled(ExportSet.LOANS)
        viewModel.onSetToggled(ExportSet.PAYMENTS)
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        val payload = requireNotNull(viewModel.state.value.payload)
        assertEquals(listOf("gastonomo_movs_2026-09-22.csv"), payload.documents.map { it.fileName })
        assertEquals(SampleFinance.movements.size, payload.rows)
    }

    @Test
    fun `the row count of the payload adds up every set`() = runTest {
        val viewModel = loaded()
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        val state = viewModel.state.value
        val expected = ExportSet.entries.sumOf { state.rowsOf(it) }
        assertEquals(expected, requireNotNull(state.payload).rows)
    }

    @Test
    fun `once shared the payload is dropped and the toast counts the rows`() = runTest {
        val viewModel = loaded()
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        viewModel.onPayloadDelivered(rows = 7)
        assertNull(viewModel.state.value.payload)
        assertEquals(ExportMessage.Ready(7), viewModel.state.value.message)
    }

    @Test
    fun `a failure while sharing is told as an error`() = runTest {
        val viewModel = loaded()
        viewModel.onExport(spanishLabels)
        advanceUntilIdle()
        viewModel.onPayloadFailed()
        assertEquals(ExportMessage.Failed, viewModel.state.value.message)
        assertTrue(ExportMessage.Failed.isError)
    }

    @Test
    fun `a failed load offers to try again and blocks the export`() = runTest {
        val viewModel = ExportViewModel(FakeExportDependencies.offline(dispatcher))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.loadFailed)
    }

    private fun TestScope.loaded(): ExportViewModel {
        val viewModel = ExportViewModel(FakeExportDependencies.working(dispatcher))
        advanceUntilIdle()
        return viewModel
    }
}
