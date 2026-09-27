package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.RatesStatus

/**
 * La carga de Ajustes: perfil, ajustes de cuenta, catalogo de monedas, tasas y el conteo de
 * categorias. Vive aparte del ViewModel porque son cinco lecturas independientes y ninguna
 * necesita saber nada de la pantalla.
 *
 * Ninguna lectura que falle tumba la pantalla: lo que no llega se queda como estaba y, si el
 * fallo fue de red, el indicador de sincronizacion pasa a Pausada.
 */
internal class SettingsLoader(private val dependencies: SettingsDependencies) {

    /** Aplica las cinco lecturas sobre [base] y devuelve el estado ya cargado. */
    suspend operator fun invoke(base: SettingsUiState): SettingsUiState {
        var next = base
        next = withProfile(next)
        next = withAccountSettings(next)
        next = withCatalog(next)
        next = withRates(next)
        next = withCategoryCount(next)
        return next.copy(loading = false)
    }

    private suspend fun withProfile(state: SettingsUiState): SettingsUiState =
        when (val result = dependencies.loadProfile()) {
            is Outcome.Success -> state.copy(user = result.value)
            is Outcome.Failure -> state.pausedIfOffline(result.error)
        }

    private suspend fun withAccountSettings(state: SettingsUiState): SettingsUiState =
        when (val result = dependencies.loadSettings()) {
            is Outcome.Success ->
                state.copy(settings = result.value, reachedApi = true)
            is Outcome.Failure -> state.pausedIfOffline(result.error)
        }

    private suspend fun withCatalog(state: SettingsUiState): SettingsUiState =
        when (val result = dependencies.loadCurrencyCatalog()) {
            is Outcome.Success -> state.copy(currencies = result.value)
            is Outcome.Failure -> state.pausedIfOffline(result.error)
        }

    private suspend fun withRates(state: SettingsUiState): SettingsUiState {
        val indicator = when (val result = dependencies.loadExchangeRates()) {
            is Outcome.Success ->
                if (result.value.status == RatesStatus.OK) {
                    RatesIndicator.Ready(result.value.date)
                } else {
                    RatesIndicator.Offline
                }
            is Outcome.Failure -> RatesIndicator.Offline
        }
        return state.copy(rates = indicator)
    }

    private suspend fun withCategoryCount(state: SettingsUiState): SettingsUiState =
        when (val result = dependencies.loadCategories()) {
            is Outcome.Success -> state.copy(categoryCount = result.value.size)
            is Outcome.Failure -> state.pausedIfOffline(result.error)
        }
}

/**
 * La fila de sincronizacion pasa a "Sin conexion" cuando no se pudo hablar con la API. Un 422 o
 * un 404 no son falta de conexion, asi que no la tocan.
 */
internal fun SettingsUiState.pausedIfOffline(error: ApiError): SettingsUiState =
    if (error is ApiError.Network) copy(reachedApi = false) else this

/**
 * El estado tras un fallo al guardar: el aviso que toca y, si fue de red, la sincronizacion
 * en Pausada. Un 503 es el catalogo de monedas vacio, no un fallo del usuario.
 */
internal fun SettingsUiState.failedWith(error: ApiError): SettingsUiState {
    val message = if (error is ApiError.ServiceUnavailable) {
        SettingsMessage.CatalogNotReady
    } else {
        SettingsMessage.SaveFailed
    }
    return copy(message = message, sheet = null).pausedIfOffline(error)
}
