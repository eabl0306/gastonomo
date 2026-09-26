package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.application.usecase.auth.ChangePassword
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveSignedIn
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
import com.bahoque.gastonomo.core.application.usecase.auth.SetUpLocalLock
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeBiometricLock
import com.bahoque.gastonomo.core.testing.fake.FakeDeviceSettingsStore
import com.bahoque.gastonomo.core.testing.fake.FakeLocalLock
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthUseCasesTest {

    @Test
    fun `signing in saves the session`() = runTest {
        val store = FakeSessionStore()
        val result = SignIn(FakeAuthGateway(), store)("  camilo@correo.com ", "secret")
        assertTrue(result.isSuccess)
        assertEquals(SampleAuth.session, store.saved)
    }

    @Test
    fun `signing in trims the email before sending it`() = runTest {
        val gateway = FakeAuthGateway()
        SignIn(gateway, FakeSessionStore())(" camilo@correo.com ", "secret")
        assertEquals("camilo@correo.com" to "secret", gateway.lastCredentials)
    }

    @Test
    fun `a failed sign in leaves no session behind`() = runTest {
        val store = FakeSessionStore()
        val gateway = FakeAuthGateway(sessionResult = Outcome.Failure(ApiError.InvalidCredentials))
        val result = SignIn(gateway, store)("camilo@correo.com", "wrong")
        assertFalse(result.isSuccess)
        assertNull(store.saved)
    }

    @Test
    fun `signing up trims the name and the email and saves the session`() = runTest {
        val gateway = FakeAuthGateway()
        val store = FakeSessionStore()
        SignUp(gateway, store)(
            RegistrationRequest(" Camilo Rojas ", " camilo@correo.com ", "secret123", true),
        )
        assertEquals("Camilo Rojas", gateway.lastRegistration?.name)
        assertEquals("camilo@correo.com", gateway.lastRegistration?.email)
        assertEquals(SampleAuth.session, store.saved)
    }

    @Test
    fun `a failed sign up leaves no session behind`() = runTest {
        val store = FakeSessionStore()
        val gateway = FakeAuthGateway(
            sessionResult = Outcome.Failure(ApiError.Validation(emptyList())),
        )
        SignUp(gateway, store)(RegistrationRequest("Camilo", "c@c.com", "short", true))
        assertNull(store.saved)
    }

    @Test
    fun `signing out revokes the token and wipes the device`() = runTest {
        val gateway = FakeAuthGateway()
        val store = FakeSessionStore(SampleAuth.session)
        val lock = FakeLocalLock(pin = "1234")
        SignOut(gateway, store, lock)()
        assertEquals(listOf("logout"), gateway.calls)
        assertNull(store.saved)
        assertFalse(lock.hasPin())
    }

    @Test
    fun `the device is wiped even when the api does not answer`() = runTest {
        val gateway = FakeAuthGateway(unitResult = Outcome.Failure(ApiError.Network))
        val store = FakeSessionStore(SampleAuth.session)
        SignOut(gateway, store, FakeLocalLock())()
        assertNull(store.saved)
    }

    @Test
    fun `asking for a password reset trims the email`() = runTest {
        val gateway = FakeAuthGateway()
        RequestPasswordReset(gateway)(" camilo@correo.com ")
        assertEquals(listOf("forgot:camilo@correo.com"), gateway.calls)
    }

    @Test
    fun `changing the password goes straight to the api`() = runTest {
        val gateway = FakeAuthGateway()
        assertTrue(ChangePassword(gateway)("old", "new").isSuccess)
        assertEquals(listOf("change"), gateway.calls)
    }

    @Test
    fun `the profile comes from the api`() = runTest {
        assertEquals(SampleAuth.user, (LoadProfile(FakeAuthGateway())() as Outcome.Success).value)
    }

    @Test
    fun `being signed in is whether there is a session stored`() = runTest {
        assertTrue(ObserveSignedIn(FakeSessionStore(SampleAuth.session))().first())
        assertFalse(ObserveSignedIn(FakeSessionStore())().first())
    }

    @Test
    fun `the pin only unlocks when it has four digits and matches`() = runTest {
        val unlock = UnlockWithPin(FakeLocalLock(pin = "1234"))
        assertTrue(unlock("1234"))
        assertFalse(unlock("4321"))
        assertFalse(unlock("123"))
    }

    @Test
    fun `the fingerprint answer comes from the device`() = runTest {
        val unlock = UnlockWithBiometrics(FakeBiometricLock(result = BiometricResult.Cancelled))
        assertEquals(BiometricResult.Cancelled, unlock(PROMPT))
    }

    @Test
    fun `turning the lock on needs a four digit pin`() = runTest {
        val lock = FakeLocalLock()
        val settings = FakeDeviceSettingsStore()
        val setUp = SetUpLocalLock(lock, settings)
        assertFalse(setUp(enabled = true, pin = "12"))
        assertFalse(setUp(enabled = true, pin = null))
        assertFalse(settings.current.lockEnabled)
        assertTrue(setUp(enabled = true, pin = "1234"))
        assertTrue(settings.current.lockEnabled)
        assertTrue(lock.verifyPin("1234"))
    }

    @Test
    fun `turning the lock off clears the pin`() = runTest {
        val lock = FakeLocalLock(pin = "1234")
        val settings = FakeDeviceSettingsStore()
        assertTrue(SetUpLocalLock(lock, settings)(enabled = false))
        assertFalse(lock.hasPin())
        assertFalse(settings.current.lockEnabled)
    }

    private companion object {
        val PROMPT = BiometricPromptText("Desbloquea", "Usa tu huella", "Usar PIN")
    }
}
