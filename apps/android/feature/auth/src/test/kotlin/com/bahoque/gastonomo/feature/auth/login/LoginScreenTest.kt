package com.bahoque.gastonomo.feature.auth.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows the logo, the heading and the two ways in`() {
        render(LoginUiState())

        compose.onNodeWithText("gastonomo").assertIsDisplayed()
        compose.onNodeWithText("Bienvenido de nuevo").assertIsDisplayed()
        compose.onNodeWithText("Entra para ver tus cuentas sincronizadas.").assertIsDisplayed()
        compose.onNodeWithText("Entrar").assertIsDisplayed()
        compose.onNodeWithText("Entrar con huella o PIN").assertIsDisplayed()
        compose.onNodeWithText("¿No tienes cuenta?").assertIsDisplayed()
    }

    @Test
    fun `the google button of the prototype is not painted`() {
        render(LoginUiState())

        compose.onNodeWithText("Continuar con Google").assertDoesNotExist()
    }

    @Test
    fun `a hidden password offers to show it`() {
        render(LoginUiState(password = "secreta1"))

        compose.onNodeWithContentDescription("Mostrar la contraseña").assertIsDisplayed()
    }

    @Test
    fun `a visible password offers to hide it`() {
        render(LoginUiState(password = "secreta1", passwordVisible = true))

        compose.onNodeWithContentDescription("Ocultar la contraseña").assertIsDisplayed()
    }

    @Test
    fun `the error of a field is painted under it with the text of the api`() {
        render(LoginUiState(emailError = AuthMessage.Literal("El correo no es valido.")))

        compose.onNodeWithText("El correo no es valido.").assertIsDisplayed()
    }

    @Test
    fun `the form error is painted over the button`() {
        render(LoginUiState(formError = AuthMessage.Resource(R.string.auth_error_invalid_credentials)))

        compose.onNodeWithText("Correo o contraseña incorrectos.").assertIsDisplayed()
    }

    @Test
    fun `every button reports what the screen asks for`() {
        val pressed = mutableListOf<String>()
        compose.setContent {
            GastonomoTheme {
                LoginScreen(
                    state = LoginUiState(),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = { pressed += "eye" },
                    onSubmit = { pressed += "submit" },
                    onForgotPassword = { pressed += "forgot" },
                    onUnlockWithLocalLock = { pressed += "unlock" },
                    onRegister = { pressed += "register" },
                )
            }
        }

        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").performClick()
        compose.onNodeWithText("Entrar con huella o PIN").performClick()
        compose.onNodeWithText("Regístrate").performClick()

        assertEquals(listOf("submit", "forgot", "unlock", "register"), pressed)
    }

    @Test
    fun `while it is sending the button does nothing`() {
        var submits = 0
        compose.setContent {
            GastonomoTheme {
                LoginScreen(
                    state = LoginUiState(submitting = true),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = { submits++ },
                    onForgotPassword = {},
                    onUnlockWithLocalLock = {},
                    onRegister = {},
                )
            }
        }

        compose.onNodeWithText("Entrar").performClick()

        assertEquals(0, submits)
    }

    private fun render(state: LoginUiState) {
        compose.setContent {
            GastonomoTheme {
                LoginScreen(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = {},
                    onForgotPassword = {},
                    onUnlockWithLocalLock = {},
                    onRegister = {},
                )
            }
        }
    }
}
