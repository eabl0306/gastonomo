package com.bahoque.gastonomo.feature.health

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.usecase.CheckApiHealth
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.HealthReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading before the first answer arrives`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Success(healthyReport()))

        assertEquals(HealthUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `publishes the report when the service answers`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Success(healthyReport()))

        testScheduler.advanceUntilIdle()

        val ready = assertIs<HealthUiState.Ready>(viewModel.uiState.value)
        assertEquals("0.1.0", ready.report.version.value)
    }

    @Test
    fun `a degraded report is still shown`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Success(degradedReport()))

        testScheduler.advanceUntilIdle()

        assertIs<HealthUiState.Ready>(viewModel.uiState.value)
    }

    @Test
    fun `a network failure becomes an unreachable reason`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Failure(HealthCheckError.Unreachable("refused")))

        testScheduler.advanceUntilIdle()

        assertEquals(HealthUiState.Failed(HealthFailureReason.UNREACHABLE), viewModel.uiState.value)
    }

    @Test
    fun `a broken contract becomes an unexpected response reason`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Failure(HealthCheckError.UnexpectedStatus(500)))

        testScheduler.advanceUntilIdle()

        assertEquals(HealthUiState.Failed(HealthFailureReason.UNEXPECTED_RESPONSE), viewModel.uiState.value)
    }

    @Test
    fun `refreshing asks the service again`() = runTest(dispatcher) {
        val viewModel = viewModelWith(Outcome.Failure(HealthCheckError.MalformedResponse("bad")))
        testScheduler.advanceUntilIdle()

        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        assertEquals(HealthUiState.Failed(HealthFailureReason.UNEXPECTED_RESPONSE), viewModel.uiState.value)
    }

    @Test
    fun `the factory only builds its own view model`() {
        val factory = HealthViewModel.Factory(
            CheckApiHealth(gatewayReturning(Outcome.Success(healthyReport()))),
            SingleDispatcherProvider(dispatcher),
        )

        assertIs<HealthViewModel>(factory.create(HealthViewModel::class.java))
    }

    private fun viewModelWith(result: Outcome<HealthReport, HealthCheckError>) =
        HealthViewModel(CheckApiHealth(gatewayReturning(result)), SingleDispatcherProvider(dispatcher))
}
