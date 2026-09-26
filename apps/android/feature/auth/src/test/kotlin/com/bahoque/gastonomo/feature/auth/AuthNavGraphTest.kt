package com.bahoque.gastonomo.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Que las cuatro pantallas de entrada se alcanzan y que se navega entre ellas. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class AuthNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the login screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.Login) { actions ->
            authNavGraph(FakeAuthDependencies(), actions)
        }
        compose.onNodeWithText("Bienvenido de nuevo").assertIsDisplayed()
    }

    @Test
    fun `the register screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.Register) { actions ->
            authNavGraph(FakeAuthDependencies(), actions)
        }
        compose.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
    }

    @Test
    fun `the forgot password screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.ForgotPassword()) { actions ->
            authNavGraph(FakeAuthDependencies(), actions)
        }
        compose.onNodeWithText("Recupera tu contraseña").assertIsDisplayed()
    }

    @Test
    fun `the unlock screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.Unlock) { actions ->
            authNavGraph(FakeAuthDependencies(session = SampleAuth.session), actions)
        }
        compose.onNodeWithText("Ingresa tu PIN o usa tu huella").assertIsDisplayed()
    }

    @Test
    fun `login stacks the register screen and register replaces the stack with login`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Login, recorded) { actions ->
            authNavGraph(FakeAuthDependencies(), actions)
        }

        compose.onNodeWithText("Regístrate").performClick()
        compose.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
        compose.onNodeWithText("Inicia sesión").performClick()

        // `replaceWith` no apila: sustituye. El doble de navegacion solo lo anota.
        assertEquals(listOf(GastonomoRoute.Register, GastonomoRoute.Login), recorded)
    }

    @Test
    fun `login carries the typed email over to the recovery screen`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Login, recorded) { actions ->
            authNavGraph(FakeAuthDependencies(), actions)
        }

        // El primer campo con accion de escritura es el de correo.
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("camilo@correo.com")
        compose.onNodeWithText("¿Olvidaste tu contraseña?").performClick()

        assertEquals(
            GastonomoRoute.ForgotPassword("camilo@correo.com"),
            recorded.filterIsInstance<GastonomoRoute.ForgotPassword>().single(),
        )
    }

    @Test
    fun `the fingerprint button of login opens the unlock screen`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Login, recorded) { actions ->
            authNavGraph(FakeAuthDependencies(session = SampleAuth.session), actions)
        }

        compose.onNodeWithText("Entrar con huella o PIN").performClick()

        assertTrue(GastonomoRoute.Unlock in recorded)
    }

    @Test
    fun `the use password link of the unlock screen replaces the stack with login`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Unlock, recorded) { actions ->
            authNavGraph(FakeAuthDependencies(session = SampleAuth.session), actions)
        }

        compose.onNodeWithText("Usar contraseña").performClick()

        assertTrue(GastonomoRoute.Login in recorded)
    }
}
