package com.bahoque.gastonomo.feature.auth.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.feature.auth.AuthDependencies

/**
 * Engancha [UnlockScreen] a su ViewModel.
 *
 * Los textos del dialogo del sistema para la huella salen de `strings.xml`, asi que los
 * resuelve la pantalla y los pasa al caso de uso: el nucleo no lee recursos.
 */
@Composable
fun UnlockRoute(
    dependencies: AuthDependencies,
    onUnlocked: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: UnlockViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                UnlockViewModel(
                    unlockWithPin = dependencies.unlockWithPin,
                    unlockWithBiometrics = dependencies.unlockWithBiometrics,
                    signOut = dependencies.signOut,
                    observeStoredUser = dependencies.observeStoredUser,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prompt = BiometricPromptText(
        title = stringResource(GastonomoStrings.biometricTitle),
        subtitle = stringResource(GastonomoStrings.biometricSubtitle),
        negativeButton = stringResource(GastonomoStrings.biometricUsePin),
    )
    LaunchedEffect(state.unlocked) { if (state.unlocked) onUnlocked() }
    UnlockScreen(
        state = state,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onBiometrics = { viewModel.onBiometrics(prompt) },
        onUsePassword = onUsePassword,
        modifier = modifier,
    )
}
