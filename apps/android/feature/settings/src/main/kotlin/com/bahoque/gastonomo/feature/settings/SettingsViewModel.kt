package com.bahoque.gastonomo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.application.state.ResyncResult
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El estado de Ajustes y lo que se puede hacer en la pantalla.
 *
 * Solo la moneda y el idioma van a `PATCH /v1/settings`. El toggle del cerrojo no toca la
 * API: protege un secreto de este telefono y se queda aqui.
 */
class SettingsViewModel(private val dependencies: SettingsDependencies) : ViewModel() {

    private val loader = SettingsLoader(dependencies)
    private val mutableState = MutableStateFlow(SettingsUiState())

    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            dependencies.observeDeviceSettings().collect { device ->
                mutableState.update { it.copy(device = device) }
            }
        }
        viewModelScope.launch {
            dependencies.observePendingWrites().collect { writes ->
                mutableState.update { it.copy(pendingCount = writes.size) }
            }
        }
        viewModelScope.launch {
            dependencies.observeConnection().collect { online ->
                mutableState.update { it.copy(online = online) }
            }
        }
        viewModelScope.launch {
            // Si la aplicacion murio a la mitad de una resincronizacion, la marca sigue puesta.
            if (dependencies.syncStore.fullResyncPending()) {
                mutableState.update { it.copy(resync = ResyncProgress.UNFINISHED) }
            }
        }
        load()
    }

    /** Vuelve a pedirlo todo. Es lo que hace el reintento. */
    fun load() {
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val loaded = loader(mutableState.value)
            mutableState.update { loaded }
        }
    }

    /**
     * Borra lo local y lo baja otra vez.
     *
     * El caso de uso comprueba lo pendiente antes de borrar nada; si hay algo sin subir vuelve sin
     * haber tocado la base, y la hoja ya lo dice. Un fallo a la mitad deja la marca puesta, y la
     * fila lo cuenta: el siguiente arranque la termina.
     */
    fun resync() {
        if (mutableState.value.resync == ResyncProgress.RUNNING) return
        mutableState.update { it.copy(resync = ResyncProgress.RUNNING) }
        viewModelScope.launch {
            val message = when (val result = dependencies.resyncAccount()) {
                ResyncResult.Done -> SettingsMessage.ResyncDone
                is ResyncResult.HasPendingWrites -> null
                is ResyncResult.Failed -> SettingsMessage.ResyncFailed
            }
            val unfinished = dependencies.syncStore.fullResyncPending()
            mutableState.update {
                it.copy(
                    sheet = null,
                    resync = if (unfinished) ResyncProgress.UNFINISHED else ResyncProgress.IDLE,
                    message = message ?: it.message,
                )
            }
            if (message == SettingsMessage.ResyncDone) load()
        }
    }

    /** Abre una hoja, o la cierra con null. Cambiar de hoja limpia lo que se estaba buscando. */
    fun showSheet(sheet: SettingsSheet?) {
        mutableState.update { it.copy(sheet = sheet, currencyQuery = "", pin = "") }
    }

    fun onCurrencyQuery(query: String) = mutableState.update { it.copy(currencyQuery = query) }

    fun onMessageShown() = mutableState.update { it.copy(message = null) }

    /**
     * Cambia la moneda principal o el idioma. Son una sola funcion porque el camino es el mismo, y
     * `PATCH /v1/settings` ya acepta los dos campos sueltos.
     *
     * Cambiar la moneda **no toca ningun registro guardado**: solo como se ensenan.
     */
    fun onSettingChanged(currency: CurrencyCode? = null, language: AppLanguage? = null) {
        val previous = mutableState.value.settings.displayCurrency
        viewModelScope.launch {
            when (val result = dependencies.updateSettings(currency, language)) {
                is Outcome.Success -> mutableState.update {
                    it.copy(
                        settings = result.value,
                        sheet = null,
                        // Solo se avisa del cambio de moneda, y solo si cambio de verdad.
                        message = currency
                            ?.takeIf { it != previous }
                            ?.let(SettingsMessage::CurrencyChanged),
                    )
                }
                is Outcome.Failure ->
                    mutableState.update { it.failedWith(result.error) }
            }
        }
    }

    /**
     * El toggle del cerrojo local. Encenderlo exige un PIN, asi que abre la hoja que lo
     * pide; apagarlo borra el PIN guardado sin preguntar nada.
     */
    fun onLockToggled(enabled: Boolean) {
        if (enabled) {
            showSheet(SettingsSheet.PIN)
            return
        }
        viewModelScope.launch {
            dependencies.setUpLocalLock(enabled = false)
            mutableState.update { it.copy(message = SettingsMessage.LockDisabled) }
        }
    }

    /** Los digitos teclados en la hoja del PIN, ya recortados a los cuatro del diseno. */
    fun onPinChanged(digits: String) {
        val clean = digits.filter { it.isDigit() }.take(LocalLock.PIN_LENGTH)
        mutableState.update { it.copy(pin = clean) }
    }

    fun confirmPin() {
        val pin = mutableState.value.pin
        viewModelScope.launch {
            val saved = dependencies.setUpLocalLock(enabled = true, pin = pin)
            mutableState.update {
                if (saved) {
                    it.copy(sheet = null, pin = "", message = SettingsMessage.LockEnabled)
                } else {
                    it.copy(message = SettingsMessage.SaveFailed)
                }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            dependencies.signOut()
            onDone()
        }
    }
}
