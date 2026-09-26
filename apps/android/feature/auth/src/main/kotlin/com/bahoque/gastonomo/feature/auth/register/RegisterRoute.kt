package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.feature.auth.AuthDependencies
import com.bahoque.gastonomo.feature.auth.R

/**
 * Engancha [RegisterScreen] a su ViewModel.
 *
 * Al crear la cuenta se ensena el toast "Cuenta creada. Tus datos se sincronizan." y se va
 * a Inicio cuando el toast se apaga. El prototipo tiene un unico toast global que sobrevive
 * al cambio de pantalla; aqui vive en esta pantalla, porque un toast montado sobre el
 * NavHost es infraestructura compartida y ninguna feature puede anadirla sola.
 */
@Composable
fun RegisterRoute(
    dependencies: AuthDependencies,
    onRegistered: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RegisterViewModel = viewModel(
        factory = viewModelFactory { initializer { RegisterViewModel(dependencies.signUp) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val callbacks = remember(viewModel, onSignIn, onBack) {
        RegisterCallbacks(
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onToggleTerms = viewModel::onToggleTerms,
            onSubmit = viewModel::onSubmit,
            onSignIn = onSignIn,
            onBack = onBack,
        )
    }
    Box(modifier = modifier) {
        RegisterScreen(state = state, callbacks = callbacks)
        if (state.registered) {
            GastonomoToast(
                message = stringResource(R.string.auth_register_toast),
                onHide = {
                    viewModel.onRegisteredHandled()
                    onRegistered()
                },
            )
        }
    }
}
