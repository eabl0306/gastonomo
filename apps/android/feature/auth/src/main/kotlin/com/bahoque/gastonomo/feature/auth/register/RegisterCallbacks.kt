package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.runtime.Immutable

/**
 * Lo que la pantalla de Registro puede pedir. Son siete acciones y van agrupadas porque
 * pasarlas de una en una dispararia el limite de cinco parametros sin hacer la firma mas
 * clara.
 */
@Immutable
data class RegisterCallbacks(
    val onNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onToggleTerms: () -> Unit,
    val onSubmit: () -> Unit,
    val onSignIn: () -> Unit,
    val onBack: () -> Unit,
)
