package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.SPANISH_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class RegisterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the design does not draw the eye of login here`() {
        render(RegisterUiState(password = "secreta1"))

        compose.onAllNodesWithContentDescription("Mostrar la contraseña").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Ocultar la contraseña").assertCountEquals(0)
    }

    @Test
    fun `it shows the heading, the three fields and the terms`() {
        render(RegisterUiState())

        compose.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
        compose.onNodeWithText("Nombre").assertIsDisplayed()
        compose.onNodeWithText("Correo").assertIsDisplayed()
        compose.onNodeWithText("Contraseña").assertIsDisplayed()
        compose.onNodeWithText("Mínimo 8 caracteres").assertIsDisplayed()
        compose.onNodeWithText("Acepto los términos de uso y la política de privacidad.")
            .assertIsDisplayed()
        compose.onNodeWithText("Crear cuenta").assertIsDisplayed()
    }

    @Test
    fun `the google button of the prototype is not painted`() {
        render(RegisterUiState())

        compose.onNodeWithText("Registrarme con Google").assertDoesNotExist()
    }

    @Test
    fun `an empty meter reads as the minimum`() {
        render(RegisterUiState())

        compose.onNodeWithContentDescription("Fuerza de la contraseña: Mínimo 8 caracteres")
            .assertIsDisplayed()
    }

    @Test
    fun `a strong password reads as safe`() {
        render(RegisterUiState(password = "secreta1"))

        compose.onNodeWithContentDescription("Fuerza de la contraseña: Segura").assertIsDisplayed()
    }

    @Test
    fun `a short password reads as too short`() {
        render(RegisterUiState(password = "abc"))

        compose.onNodeWithContentDescription("Fuerza de la contraseña: Muy corta").assertIsDisplayed()
    }

    @Test
    fun `the error of the terms is painted under the checkbox`() {
        render(RegisterUiState(termsError = AuthMessage.Resource(R.string.auth_error_terms_required)))

        compose.onNodeWithText("Tienes que aceptar los términos para continuar.").assertIsDisplayed()
    }

    @Test
    fun `the error of a field comes from the api without translating`() {
        render(RegisterUiState(emailError = AuthMessage.Literal("Ya existe una cuenta con ese correo.")))

        compose.onNodeWithText("Ya existe una cuenta con ese correo.").assertIsDisplayed()
    }

    @Test
    fun `the whole terms row marks the checkbox`() {
        val pressed = mutableListOf<String>()
        renderWith(RegisterUiState(), pressed)

        compose.onNodeWithText("Acepto los términos de uso y la política de privacidad.")
            .performClick()

        assertEquals(listOf("terms"), pressed)
    }

    @Test
    fun `the button and the footer report what the screen asks for`() {
        val pressed = mutableListOf<String>()
        renderWith(RegisterUiState(), pressed)

        compose.onNodeWithText("Crear cuenta").performClick()
        compose.onNodeWithText("Inicia sesión").performClick()

        assertEquals(listOf("submit", "signIn"), pressed)
    }

    private fun render(state: RegisterUiState) = renderWith(state, mutableListOf())

    private fun renderWith(state: RegisterUiState, pressed: MutableList<String>) {
        compose.setContent {
            GastonomoTheme {
                RegisterScreen(
                    state = state,
                    callbacks = RegisterCallbacks(
                        onNameChange = {},
                        onEmailChange = {},
                        onPasswordChange = {},
                        onToggleTerms = { pressed += "terms" },
                        onSubmit = { pressed += "submit" },
                        onSignIn = { pressed += "signIn" },
                        onBack = { pressed += "back" },
                    ),
                )
            }
        }
    }
}
