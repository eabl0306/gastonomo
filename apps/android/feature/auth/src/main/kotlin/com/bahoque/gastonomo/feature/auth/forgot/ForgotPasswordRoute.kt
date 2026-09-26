package com.bahoque.gastonomo.feature.auth.forgot

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Engancha [ForgotPasswordScreen] a su ViewModel.
 *
 * @param email el correo que venia de Login, con el que se rellena el campo.
 */
@Composable
fun ForgotPasswordRoute(
    dependencies: AuthDependencies,
    email: String,
    onBackToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ForgotPasswordViewModel(dependencies.requestPasswordReset, email) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier) {
        ForgotPasswordScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onSubmit = viewModel::onSubmit,
            onResend = viewModel::onResend,
            onBackToLogin = onBackToLogin,
            onBack = onBack,
        )
        if (state.resentToast) {
            GastonomoToast(
                message = stringResource(R.string.auth_forgot_resent_toast),
                onHide = viewModel::onToastHidden,
            )
        }
    }
}
