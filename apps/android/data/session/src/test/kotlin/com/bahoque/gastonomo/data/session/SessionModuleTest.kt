package com.bahoque.gastonomo.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Las fabricas del modulo y lo que se puede comprobar del cerrojo sin un telefono real. */
@RunWith(AndroidJUnit4::class)
class SessionModuleTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `the factories build the adapters the container asks for`() {
        assertNotNull(createSessionStore(context))
        assertNotNull(createLocalLock(context))
        assertNotNull(createDeviceSettingsStore(context))
        assertNotNull(createBiometricLock(context) { null })
    }

    @Test
    fun `without a live activity the prompt is answered as cancelled`() = runTest {
        val lock = createBiometricLock(context) { null }
        val result = lock.authenticate(BiometricPromptText("Entra", "Usa tu huella", "Usar PIN"))
        assertEquals(BiometricResult.Cancelled, result)
    }

    @Test
    fun `a corrupt stored value decrypts to nothing instead of crashing`() {
        assertEquals(null, KeystoreCipher(alias = "gastonomo.test").decrypt("garbage"))
    }
}
