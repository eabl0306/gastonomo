package com.bahoque.gastonomo.feature.auth

import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Las tres puertas de entrada de la aplicacion. */
class AuthStartTest {

    @Test
    fun `without a stored session it starts at login`() = runTest {
        val dependencies = FakeAuthDependencies(session = null, lockEnabled = true)

        assertEquals(GastonomoRoute.Login, authStartDestination(dependencies))
    }

    @Test
    fun `with a session and the local lock on it starts at the pin screen`() = runTest {
        val dependencies = FakeAuthDependencies(session = SampleAuth.session, lockEnabled = true)

        assertEquals(GastonomoRoute.Unlock, authStartDestination(dependencies))
    }

    @Test
    fun `with a session and no lock it goes straight home`() = runTest {
        val dependencies = FakeAuthDependencies(session = SampleAuth.session, lockEnabled = false)

        assertEquals(GastonomoRoute.Home, authStartDestination(dependencies))
    }
}
