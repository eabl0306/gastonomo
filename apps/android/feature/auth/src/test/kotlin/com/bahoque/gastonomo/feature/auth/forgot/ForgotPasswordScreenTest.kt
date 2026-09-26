package com.bahoque.gastonomo.feature.auth.forgot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.SPANISH_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class ForgotPasswordScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the form state asks for the email`() {
        render(ForgotPasswordUiState(email = "camilo@correo.com"))

        compose.onNodeWithText("Recupera tu contraseña").assertIsDisplayed()
        compose.onNodeWithText("camilo@correo.com").assertIsDisplayed()
        compose.onNodeWithText("Enviar enlace").assertIsDisplayed()
        compose.onNodeWithText("Revisa tu correo").assertDoesNotExist()
    }

    @Test
    fun `the sent state names the address and the thirty minutes`() {
        render(ForgotPasswordUiState(sent = true, sentTo = "camilo@correo.com"))

        compose.onNodeWithText("Revisa tu correo").assertIsDisplayed()
        compose.onNodeWithText("Enviamos un enlace a camilo@correo.com. Caduca en 30 minutos.")
            .assertIsDisplayed()
        compose.onNodeWithText("Volver a iniciar sesión").assertIsDisplayed()
        compose.onNodeWithText("¿No llegó? Reenviar").assertIsDisplayed()
    }

    @Test
    fun `without an address the sent state falls back to a generic phrase`() {
        render(ForgotPasswordUiState(sent = true, sentTo = ""))

        compose.onNodeWithText("Enviamos un enlace a tu correo. Caduca en 30 minutos.")
            .assertIsDisplayed()
    }

    @Test
    fun `sending and resending report what the screen asks for`() {
        val pressed = mutableListOf<String>()
        renderWith(ForgotPasswordUiState(sent = true, sentTo = "camilo@correo.com"), pressed)

        compose.onNodeWithText("¿No llegó? Reenviar").performClick()
        compose.onNodeWithText("Volver a iniciar sesión").performClick()

        assertEquals(listOf("resend", "backToLogin"), pressed)
    }

    private fun render(state: ForgotPasswordUiState) = renderWith(state, mutableListOf())

    private fun renderWith(state: ForgotPasswordUiState, pressed: MutableList<String>) {
        compose.setContent {
            GastonomoTheme {
                ForgotPasswordScreen(
                    state = state,
                    onEmailChange = {},
                    onSubmit = { pressed += "submit" },
                    onResend = { pressed += "resend" },
                    onBackToLogin = { pressed += "backToLogin" },
                    onBack = { pressed += "back" },
                )
            }
        }
    }
}
