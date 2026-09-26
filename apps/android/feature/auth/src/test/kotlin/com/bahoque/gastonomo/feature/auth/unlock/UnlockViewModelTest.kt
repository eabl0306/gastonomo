package com.bahoque.gastonomo.feature.auth.unlock

import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveStoredUser
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeBiometricLock
import com.bahoque.gastonomo.core.testing.fake.FakeLocalLock
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnlockViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    private val prompt = BiometricPromptText("Desbloquea", "Usa tu huella", "Usar PIN")

    @Test
    fun `it greets with the stored user, without touching the api`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway = gateway)
        advanceUntilIdle()

        assertEquals("Camilo", viewModel.state.value.name)
        assertEquals("CR", viewModel.state.value.initials)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `the dots fill up as the digits are typed`() = runTest {
        val viewModel = viewModelWith()

        viewModel.onDigit('1')
        viewModel.onDigit('2')

        assertEquals(2, viewModel.state.value.digits)
    }

    @Test
    fun `backspace takes one digit away`() = runTest {
        val viewModel = viewModelWith()

        viewModel.onDigit('1')
        viewModel.onDigit('2')
        viewModel.onBackspace()

        assertEquals(1, viewModel.state.value.digits)
    }

    @Test
    fun `the fourth digit of the right pin unlocks`() = runTest {
        val viewModel = viewModelWith()

        "1234".forEach(viewModel::onDigit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.unlocked)
        assertEquals(0, viewModel.state.value.digits)
    }

    @Test
    fun `a wrong pin clears the dots and says how many tries are left`() = runTest {
        val viewModel = viewModelWith()

        "9999".forEach(viewModel::onDigit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.unlocked)
        assertEquals(0, state.digits)
        assertEquals(4, state.attemptsLeft)
        assertTrue(state.message is AuthMessage.Plural)
    }

    @Test
    fun `five wrong pins wipe the session of the device`() = runTest {
        val sessionStore = FakeSessionStore(SampleAuth.session)
        val localLock = FakeLocalLock(RIGHT_PIN)
        val viewModel = viewModelWith(localLock = localLock, sessionStore = sessionStore)

        repeat(UnlockUiState.MAXIMUM_ATTEMPTS) {
            "9999".forEach(viewModel::onDigit)
            advanceUntilIdle()
        }

        assertTrue(viewModel.state.value.lockedOut)
        assertEquals(0, viewModel.state.value.attemptsLeft)
        assertNull(sessionStore.saved)
        assertFalse(localLock.hasPin())
    }

    @Test
    fun `once locked out the keypad stops responding`() = runTest {
        val viewModel = viewModelWith()

        repeat(UnlockUiState.MAXIMUM_ATTEMPTS) {
            "9999".forEach(viewModel::onDigit)
            advanceUntilIdle()
        }
        viewModel.onDigit('1')

        assertEquals(0, viewModel.state.value.digits)
    }

    @Test
    fun `the fingerprint goes straight in`() = runTest {
        val viewModel = viewModelWith()

        viewModel.onBiometrics(prompt)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.unlocked)
    }

    @Test
    fun `cancelling the fingerprint leaves the screen as it was`() = runTest {
        val viewModel = viewModelWith(
            biometricLock = FakeBiometricLock(result = BiometricResult.Cancelled),
        )

        viewModel.onBiometrics(prompt)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.unlocked)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `a fingerprint that fails suggests the pin`() = runTest {
        val viewModel = viewModelWith(
            biometricLock = FakeBiometricLock(result = BiometricResult.Failed("sensor sucio")),
        )

        viewModel.onBiometrics(prompt)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.unlocked)
        assertNotNull(viewModel.state.value.message)
    }

    @Test
    fun `a fifth digit is ignored`() = runTest {
        val viewModel = viewModelWith()

        "12345".forEach(viewModel::onDigit)

        assertEquals(UnlockUiState.PIN_LENGTH, viewModel.state.value.digits)
    }

    private fun viewModelWith(
        localLock: FakeLocalLock = FakeLocalLock(RIGHT_PIN),
        sessionStore: FakeSessionStore = FakeSessionStore(SampleAuth.session),
        biometricLock: FakeBiometricLock = FakeBiometricLock(),
        gateway: FakeAuthGateway = FakeAuthGateway(),
    ) = UnlockViewModel(
        unlockWithPin = UnlockWithPin(localLock),
        unlockWithBiometrics = UnlockWithBiometrics(biometricLock),
        signOut = SignOut(gateway, sessionStore, localLock),
        observeStoredUser = ObserveStoredUser(sessionStore),
    )

    private companion object {
        const val RIGHT_PIN = "1234"
    }
}
