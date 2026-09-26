package com.bahoque.gastonomo.feature.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.feature.auth.AuthDependencies

/**
 * Engancha [LoginScreen] a su ViewModel y traduce lo que la pantalla pide en navegacion.
 *
 * Al entrar se sustituye toda la pila, para que `atras` desde Inicio no vuelva al Login.
 */
@Composable
fun LoginRoute(
    dependencies: AuthDependencies,
    onSignedIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onRegister: () -> Unit,
    onUnlockWithLocalLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory { initializer { LoginViewModel(dependencies.signIn) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.onSignedInHandled()
            onSignedIn()
        }
    }
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = { onForgotPassword(state.email) },
        onUnlockWithLocalLock = onUnlockWithLocalLock,
        onRegister = onRegister,
        modifier = modifier,
    )
}
