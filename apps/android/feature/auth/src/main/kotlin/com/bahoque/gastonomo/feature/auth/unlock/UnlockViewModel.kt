package com.bahoque.gastonomo.feature.auth.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveStoredUser
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Desbloqueo con PIN o huella. No habla con la API: lo que se abre es el refresh token que
 * ya esta guardado y cifrado en este dispositivo.
 *
 * El diseno no dice que pasa cuando el PIN falla. Aqui se cuentan cinco intentos y al
 * agotarlos se cierra la sesion del dispositivo, asi que hay que volver a entrar con la
 * contrasena. Es el precio de un secreto de cuatro digitos: sin limite, probar las diez mil
 * combinaciones es cuestion de minutos y el cerrojo dejaria de proteger nada. El coste es
 * que un tercero puede echar al usuario a proposito, y ese usuario entra con su contrasena.
 */
class UnlockViewModel(
    private val unlockWithPin: UnlockWithPin,
    private val unlockWithBiometrics: UnlockWithBiometrics,
    private val signOut: SignOut,
    observeStoredUser: ObserveStoredUser,
) : ViewModel() {

    private val mutableState = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = mutableState.asStateFlow()

    private var typed: String = ""

    init {
        viewModelScope.launch {
            observeStoredUser().collect { user ->
                mutableState.update {
                    it.copy(name = user?.firstName.orEmpty(), initials = user?.initials.orEmpty())
                }
            }
        }
    }

    fun onDigit(digit: Char) {
        if (!mutableState.value.acceptsInput || typed.length >= UnlockUiState.PIN_LENGTH) return
        typed += digit
        mutableState.update { it.copy(digits = typed.length, message = null) }
        if (typed.length == UnlockUiState.PIN_LENGTH) verify()
    }

    fun onBackspace() {
        if (!mutableState.value.acceptsInput || typed.isEmpty()) return
        typed = typed.dropLast(1)
        mutableState.update { it.copy(digits = typed.length, message = null) }
    }

    fun onBiometrics(prompt: BiometricPromptText) {
        if (!mutableState.value.acceptsInput) return
        mutableState.update { it.copy(checking = true, message = null) }
        viewModelScope.launch {
            val result = unlockWithBiometrics(prompt)
            mutableState.update {
                when (result) {
                    BiometricResult.Succeeded -> it.copy(checking = false, unlocked = true)
                    BiometricResult.Cancelled -> it.copy(checking = false)
                    is BiometricResult.Failed -> it.copy(
                        checking = false,
                        message = AuthMessage.Resource(R.string.auth_unlock_biometrics_failed),
                    )
                }
            }
        }
    }

    /** El PIN correcto entra tras 250 ms, que es lo que tarda el ultimo punto en llenarse. */
    private fun verify() {
        val pin = typed
        mutableState.update { it.copy(checking = true) }
        viewModelScope.launch {
            val correct = unlockWithPin(pin)
            typed = ""
            if (correct) {
                delay(ENTER_DELAY_MILLIS)
                mutableState.update { it.copy(checking = false, digits = 0, unlocked = true) }
            } else {
                onWrongPin()
            }
        }
    }

    private suspend fun onWrongPin() {
        val left = mutableState.value.attemptsLeft - 1
        if (left <= 0) {
            signOut()
            mutableState.update {
                it.copy(
                    checking = false,
                    digits = 0,
                    attemptsLeft = 0,
                    lockedOut = true,
                    message = AuthMessage.Resource(R.string.auth_unlock_locked_out),
                )
            }
            return
        }
        mutableState.update {
            it.copy(
                checking = false,
                digits = 0,
                attemptsLeft = left,
                message = AuthMessage.Plural(R.plurals.auth_unlock_attempts_left, left),
            )
        }
    }

    private companion object {
        /** Los 250 ms que el prototipo espera con los cuatro puntos llenos. */
        const val ENTER_DELAY_MILLIS = 250L
    }
}
