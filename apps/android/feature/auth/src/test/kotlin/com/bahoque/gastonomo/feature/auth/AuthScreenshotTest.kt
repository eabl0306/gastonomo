package com.bahoque.gastonomo.feature.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.feature.auth.forgot.ForgotPasswordScreen
import com.bahoque.gastonomo.feature.auth.forgot.ForgotPasswordUiState
import com.bahoque.gastonomo.feature.auth.login.LoginScreen
import com.bahoque.gastonomo.feature.auth.login.LoginUiState
import com.bahoque.gastonomo.feature.auth.register.RegisterCallbacks
import com.bahoque.gastonomo.feature.auth.register.RegisterScreen
import com.bahoque.gastonomo.feature.auth.register.RegisterUiState
import com.bahoque.gastonomo.feature.auth.unlock.UnlockScreen
import com.bahoque.gastonomo.feature.auth.unlock.UnlockUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Las capturas de las cuatro pantallas de entrada, mas el segundo estado de Recuperar.
 *
 * Salen en `feature/auth/build/outputs/roborazzi` y sirven para comparar con el prototipo.
 * Todavia no hay imagen de referencia con la que romper el build ante una regresion visual.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class AuthScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `login`() {
        compose.captureGastonomoScreen("auth-login") {
            LoginScreen(
                state = LoginUiState(email = "camilo@correo.com", password = "secreta1"),
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

    @Test
    fun `register`() {
        compose.captureGastonomoScreen("auth-register") {
            RegisterScreen(
                state = RegisterUiState(
                    name = "Camilo Rojas",
                    email = "camilo@correo.com",
                    password = "secreta1",
                    acceptedTerms = true,
                ),
                callbacks = noCallbacks,
            )
        }
    }

    @Test
    fun `forgot password form`() {
        compose.captureGastonomoScreen("auth-forgot") {
            ForgotPasswordScreen(
                state = ForgotPasswordUiState(email = "camilo@correo.com"),
                onEmailChange = {},
                onSubmit = {},
                onResend = {},
                onBackToLogin = {},
                onBack = {},
            )
        }
    }

    @Test
    fun `forgot password sent`() {
        compose.captureGastonomoScreen("auth-forgot-sent") {
            ForgotPasswordScreen(
                state = ForgotPasswordUiState(sent = true, sentTo = "camilo@correo.com"),
                onEmailChange = {},
                onSubmit = {},
                onResend = {},
                onBackToLogin = {},
                onBack = {},
            )
        }
    }

    @Test
    fun `unlock with pin`() {
        compose.captureGastonomoScreen("auth-unlock") {
            UnlockScreen(
                state = UnlockUiState(name = "Camilo", initials = "CR", digits = 2),
                onDigit = {},
                onBackspace = {},
                onBiometrics = {},
                onUsePassword = {},
            )
        }
    }

    private val noCallbacks = RegisterCallbacks(
        onNameChange = {},
        onEmailChange = {},
        onPasswordChange = {},
        onToggleTerms = {},
        onSubmit = {},
        onSignIn = {},
        onBack = {},
    )
}
