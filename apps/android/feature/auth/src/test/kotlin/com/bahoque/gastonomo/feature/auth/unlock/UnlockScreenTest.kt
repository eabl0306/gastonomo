package com.bahoque.gastonomo.feature.auth.unlock

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
class UnlockScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val camilo = UnlockUiState(name = "Camilo", initials = "CR")

    @Test
    fun `it greets by name and shows the keypad`() {
        render(camilo)

        compose.onNodeWithText("CR").assertIsDisplayed()
        compose.onNodeWithText("Hola, Camilo").assertIsDisplayed()
        compose.onNodeWithText("Ingresa tu PIN o usa tu huella").assertIsDisplayed()
        listOf("1", "5", "9", "0").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Usar contraseña").assertIsDisplayed()
    }

    @Test
    fun `the fingerprint and the backspace keys have a name for the screen reader`() {
        render(camilo)

        compose.onNodeWithContentDescription("Entrar con huella").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar el último dígito").assertIsDisplayed()
    }

    @Test
    fun `the dots say how many digits are typed`() {
        render(camilo.copy(digits = 2))

        compose.onNodeWithContentDescription("PIN de cuatro dígitos, 2 escritos").assertIsDisplayed()
    }

    @Test
    fun `a wrong pin replaces the prompt with the warning`() {
        render(camilo.copy(message = AuthMessage.Resource(R.string.auth_unlock_wrong_pin)))

        compose.onNodeWithText("PIN incorrecto. Inténtalo de nuevo.").assertIsDisplayed()
    }

    @Test
    fun `the plural of the tries left is resolved`() {
        render(camilo.copy(message = AuthMessage.Plural(R.plurals.auth_unlock_attempts_left, 3)))

        compose.onNodeWithText("PIN incorrecto. Te quedan 3 intentos.").assertIsDisplayed()
    }

    @Test
    fun `with a single try left it reads in singular`() {
        render(camilo.copy(message = AuthMessage.Plural(R.plurals.auth_unlock_attempts_left, 1)))

        compose.onNodeWithText("PIN incorrecto. Te queda 1 intento.").assertIsDisplayed()
    }

    @Test
    fun `locked out it explains that the password is the only way`() {
        render(
            camilo.copy(
                lockedOut = true,
                attemptsLeft = 0,
                message = AuthMessage.Resource(R.string.auth_unlock_locked_out),
            ),
        )

        compose.onNodeWithText("Demasiados intentos. Entra con tu contraseña.").assertIsDisplayed()
        compose.onNodeWithText("Usar contraseña").assertIsDisplayed()
    }

    @Test
    fun `every key reports what it is`() {
        val pressed = mutableListOf<String>()
        renderWith(camilo, pressed)

        compose.onNodeWithText("7").performClick()
        compose.onNodeWithContentDescription("Borrar el último dígito").performClick()
        compose.onNodeWithContentDescription("Entrar con huella").performClick()
        compose.onNodeWithText("Usar contraseña").performClick()

        assertEquals(listOf("digit:7", "backspace", "biometrics", "usePassword"), pressed)
    }

    private fun render(state: UnlockUiState) = renderWith(state, mutableListOf())

    private fun renderWith(state: UnlockUiState, pressed: MutableList<String>) {
        compose.setContent {
            GastonomoTheme {
                UnlockScreen(
                    state = state,
                    onDigit = { pressed += "digit:$it" },
                    onBackspace = { pressed += "backspace" },
                    onBiometrics = { pressed += "biometrics" },
                    onUsePassword = { pressed += "usePassword" },
                )
            }
        }
    }
}
