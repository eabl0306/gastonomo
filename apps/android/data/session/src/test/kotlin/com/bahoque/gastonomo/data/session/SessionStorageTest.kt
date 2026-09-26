package com.bahoque.gastonomo.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El almacenamiento de la sesion, con el DataStore real de Robolectric. El cifrado del
 * Keystore no se puede ejercitar aqui, asi que se sustituye por uno de mentira y lo que se
 * comprueba es que lo guardado **no es el texto en claro**.
 */
@RunWith(AndroidJUnit4::class)
class SessionStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dataStore = context.gastonomoDataStore

    @After
    fun clean() = runTest {
        DataStoreSessionStore(dataStore, REVERSING_CIPHER).clear()
        DataStoreLocalLock(dataStore).clearPin()
    }

    @Test
    fun `there is no session until one is saved`() = runTest {
        val store = storeOf()
        assertFalse(store.isSignedIn.first())
        assertNull(store.refreshToken())
        assertNull(store.user.first())
    }

    @Test
    fun `saving a session keeps the user and both tokens`() = runTest {
        val store = storeOf()
        store.save(SampleAuth.session)
        assertTrue(store.isSignedIn.first())
        assertEquals("access-token", store.accessToken())
        assertEquals("refresh-token", store.refreshToken())
        assertEquals("Camilo Rojas", store.user.first()?.name)
    }

    @Test
    fun `the tokens are not stored in the clear`() = runTest {
        val store = storeOf()
        store.save(SampleAuth.session)
        val stored = dataStore.data.first()[SessionKeys.REFRESH_TOKEN]
        assertNotEquals("refresh-token", stored)
    }

    @Test
    fun `a refresh replaces both tokens and keeps the user`() = runTest {
        val store = storeOf()
        store.save(SampleAuth.session)
        store.replaceTokens(TokenPair("access-2", 900, "refresh-2"))
        assertEquals("access-2", store.accessToken())
        assertEquals("refresh-2", store.refreshToken())
        assertEquals("Camilo Rojas", store.user.first()?.name)
    }

    @Test
    fun `an expired access token is not handed out`() = runTest {
        val store = storeOf(now = { 0L })
        store.save(SampleAuth.session)
        val later = storeOf(now = { HOUR_IN_MILLIS })
        assertNull(later.accessToken())
        assertEquals("refresh-token", later.refreshToken())
    }

    @Test
    fun `clearing wipes the session`() = runTest {
        val store = storeOf()
        store.save(SampleAuth.session)
        store.clear()
        assertFalse(store.isSignedIn.first())
        assertNull(store.accessToken())
        assertNull(store.user.first())
    }

    @Test
    fun `the pin is stored hashed and verified against the hash`() = runTest {
        val lock = DataStoreLocalLock(dataStore)
        assertFalse(lock.hasPin())
        assertFalse(lock.verifyPin("1234"))
        lock.setPin("1234")
        assertTrue(lock.hasPin())
        assertTrue(lock.verifyPin("1234"))
        assertFalse(lock.verifyPin("4321"))
        assertNotEquals("1234", dataStore.data.first()[SessionKeys.PIN_HASH])
    }

    @Test
    fun `clearing the pin removes the hash and the salt`() = runTest {
        val lock = DataStoreLocalLock(dataStore)
        lock.setPin("1234")
        lock.clearPin()
        assertFalse(lock.hasPin())
        assertNull(dataStore.data.first()[SessionKeys.PIN_SALT])
    }

    @Test
    fun `the device settings start off and can be switched`() = runTest {
        val store = DataStoreDeviceSettingsStore(dataStore)
        assertFalse(store.settings.first().lockEnabled)
        store.setLockEnabled(true)
        store.setHideAmounts(true)
        val settings = store.settings.first()
        assertTrue(settings.lockEnabled)
        assertTrue(settings.hideAmounts)
        store.setLockEnabled(false)
        store.setHideAmounts(false)
    }

    @Test
    fun `two different pins do not share a hash`() = runTest {
        val hasher = PinHasher()
        val salt = hasher.newSalt()
        assertNotEquals(hasher.hash("1234", salt), hasher.hash("4321", salt))
        assertNotEquals(salt, hasher.newSalt())
    }

    private fun storeOf(now: () -> Long = { NOW }) =
        DataStoreSessionStore(dataStore, REVERSING_CIPHER, now)

    private companion object {
        const val NOW = 1_000_000L
        const val HOUR_IN_MILLIS = 3_600_000L

        /**
         * Un cifrado de mentira: el Keystore de verdad no existe bajo Robolectric. Basta
         * para comprobar que lo que se guarda no es el texto en claro y que se recupera.
         */
        val REVERSING_CIPHER = object : KeystoreCipher() {
            override fun encrypt(plain: String): String = plain.reversed()

            override fun decrypt(stored: String): String? = stored.reversed()
        }
    }
}
