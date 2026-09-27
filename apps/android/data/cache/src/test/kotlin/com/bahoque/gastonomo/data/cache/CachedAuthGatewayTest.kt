package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El perfil con la cache delante y, sobre todo, la limpieza al cambiar de cuenta.
 *
 * Es el test que de verdad importa de este fichero: sin la limpieza, quien entra despues de
 * que otro cierre sesion en el mismo proceso veria los movimientos del anterior.
 */
class CachedAuthGatewayTest {

    @Test
    fun `the profile is asked once`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.auth.profile()
        wired.auth.profile()

        assertEquals(1, wired.authSpy.profileCalls)
    }

    @Test
    fun `signing out leaves nothing of the account behind`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)
        wired.loans.list()
        wired.categories.list()
        wired.settings.load()
        wired.auth.profile()

        wired.auth.logout()

        wired.movements.list(FROM, TO)
        wired.loans.list()
        wired.categories.list()
        wired.settings.load()
        wired.auth.profile()
        assertEquals(2, wired.movementSpy.listCalls)
        assertEquals(2, wired.loanSpy.listCalls)
        assertEquals(2, wired.categorySpy.listCalls)
        assertEquals(2, wired.settingsSpy.loadCalls)
        assertEquals(2, wired.authSpy.profileCalls)
    }

    @Test
    fun `signing in starts from an empty cache`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)

        // Aunque el cierre de sesion anterior fallara, entrar limpia igual.
        wired.auth.login("camilo@correo.com", "secreta")

        wired.movements.list(FROM, TO)
        assertEquals(2, wired.movementSpy.listCalls)
    }

    @Test
    fun `registering starts from an empty cache`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)

        wired.auth.register(
            RegistrationRequest(
                name = "Camilo",
                email = "camilo@correo.com",
                password = "secreta",
                acceptedTerms = true,
            ),
        )

        wired.movements.list(FROM, TO)
        assertEquals(2, wired.movementSpy.listCalls)
    }

    @Test
    fun `changing the password keeps the cache, because it is still my session`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)

        // Revoca los demas dispositivos, no este.
        wired.auth.changePassword("secreta", "otra-secreta")

        wired.movements.list(FROM, TO)
        assertEquals(1, wired.movementSpy.listCalls)
    }

    @Test
    fun `asking for a password reset changes nothing`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)

        wired.auth.requestPasswordReset("camilo@correo.com")

        wired.movements.list(FROM, TO)
        assertEquals(1, wired.movementSpy.listCalls)
    }

    private companion object {
        val FROM = LocalDate.parse("2026-09-01")
        val TO = LocalDate.parse("2026-09-30")
    }
}
