package com.bahoque.gastonomo.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.usecase.CheckApiHealth
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.core.common.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Adaptador driving. Traduce el caso de uso a estado de UI y nunca habla con el adaptador
 * de datos: solo conoce el puerto y el caso de uso.
 */
class HealthViewModel(
    private val checkApiHealth: CheckApiHealth,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            mutableState.value = HealthUiState.Loading
            mutableState.value = checkApiHealth().fold(
                onSuccess = { HealthUiState.Ready(it) },
                onFailure = { HealthUiState.Failed(it.toFailureReason()) },
            )
        }
    }

    /** Fabrica para el composition root: la feature no sabe de donde sale el gateway. */
    class Factory(
        private val checkApiHealth: CheckApiHealth,
        private val dispatchers: DispatcherProvider,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HealthViewModel::class.java)) {
                "Factory only builds HealthViewModel, asked for $modelClass"
            }
            // The require above already proved that T is HealthViewModel, so the cast holds.
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(checkApiHealth, dispatchers) as T
        }
    }
}

private fun HealthCheckError.toFailureReason(): HealthFailureReason = when (this) {
    is HealthCheckError.Unreachable -> HealthFailureReason.UNREACHABLE
    is HealthCheckError.UnexpectedStatus -> HealthFailureReason.UNEXPECTED_RESPONSE
    is HealthCheckError.MalformedResponse -> HealthFailureReason.UNEXPECTED_RESPONSE
}
