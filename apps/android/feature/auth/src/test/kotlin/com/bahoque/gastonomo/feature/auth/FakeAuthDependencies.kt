package com.bahoque.gastonomo.feature.auth

import com.bahoque.gastonomo.core.application.usecase.auth.ObserveSignedIn
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveStoredUser
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeBiometricLock
import com.bahoque.gastonomo.core.testing.fake.FakeDeviceSettingsStore
import com.bahoque.gastonomo.core.testing.fake.FakeLocalLock
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore

/**
 * Dobles de todo lo que necesita esta feature, para montarla sin red ni dispositivo.
 *
 * @param session la sesion guardada, o null para simular que no hay ninguna.
 * @param lockEnabled el cerrojo local del dispositivo, que decide si al arrancar se pasa
 *   por la pantalla de PIN.
 */
internal class FakeAuthDependencies(
    session: Session? = null,
    lockEnabled: Boolean = false,
    pin: String? = "1234",
    gateway: FakeAuthGateway = FakeAuthGateway(),
) : AuthDependencies {

    private val sessionStore = FakeSessionStore(session)
    private val localLock = FakeLocalLock(pin)
    private val deviceSettings = FakeDeviceSettingsStore(DeviceSettings(lockEnabled = lockEnabled))

    override val signIn = SignIn(gateway, sessionStore)
    override val signUp = SignUp(gateway, sessionStore)
    override val requestPasswordReset = RequestPasswordReset(gateway)
    override val unlockWithPin = UnlockWithPin(localLock)
    override val unlockWithBiometrics = UnlockWithBiometrics(FakeBiometricLock())
    override val signOut = SignOut(gateway, sessionStore, localLock)
    override val observeSignedIn = ObserveSignedIn(sessionStore)
    override val observeStoredUser = ObserveStoredUser(sessionStore)
    override val observeDeviceSettings = ObserveDeviceSettings(deviceSettings)
}
