package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import java.time.LocalDate

/**
 * Todo lo que pinta Ajustes, en un solo valor inmutable.
 *
 * Solo [settings] se sincroniza con la cuenta. [device] es del telefono y [cloudSync] y
 * [rates] son estado del cliente: no hay campo en la API para ninguno de los dos.
 */
data class SettingsUiState(
    val loading: Boolean = true,
    val user: User? = null,
    val settings: UserSettings = UserSettings(),
    val device: DeviceSettings = DeviceSettings(),
    val categoryCount: Int = 0,
    val currencies: List<CurrencyInfo> = emptyList(),
    val rates: RatesIndicator = RatesIndicator.Loading,
    val cloudSync: CloudSyncState = CloudSyncState.SYNCED,

    /** Cuantos registros siguen sin salir del telefono. */
    val pendingCount: Int = 0,
    val sheet: SettingsSheet? = null,
    val currencyQuery: String = "",
    val pin: String = "",
    val message: SettingsMessage? = null,
)

/** Lo que ensena la fila "Tasas de cambio", que es de solo lectura. */
sealed interface RatesIndicator {

    /** Mientras `GET /v1/exchange-rates` esta en vuelo. */
    data object Loading : RatesIndicator

    /** Tasas al dia: la fila ensena "Frankfurter . {fecha}" en verde. */
    data class Ready(val date: LocalDate?) : RatesIndicator

    /** Tasas viejas o 503: la fila ensena "Sin conexion". */
    data object Offline : RatesIndicator
}

/** El indicador "Sincronizacion en la nube". Es conectividad del cliente, no un campo de la API. */
enum class CloudSyncState {
    /** La ultima lectura fue bien. */
    SYNCED,

    /** La ultima lectura no pudo hablar con la API. */
    PAUSED,
}

/** Las tres hojas que abre Ajustes. */
enum class SettingsSheet {
    CURRENCY,
    LANGUAGE,

    /** Crear el PIN al encender el cerrojo. El diseno no tiene pantalla para esto. */
    PIN,
}

/** Lo que Ajustes cuenta por el toast. Se resuelve a texto en la pantalla, no en el ViewModel. */
sealed interface SettingsMessage {

    /** True si es un fallo: el toast cambia el icono en vez de mentir con un check. */
    val isError: Boolean get() = false

    /** "Mostrando todo en {COD}". */
    data class CurrencyChanged(val currency: CurrencyCode) : SettingsMessage

    /** El cerrojo local quedo encendido con su PIN nuevo. */
    data object LockEnabled : SettingsMessage

    /** El cerrojo local quedo apagado y el PIN borrado. */
    data object LockDisabled : SettingsMessage

    /** `PATCH /v1/settings` respondio 503: el catalogo de monedas aun no esta. */
    data object CatalogNotReady : SettingsMessage {
        override val isError: Boolean get() = true
    }

    /** Cualquier otro fallo al guardar. */
    data object SaveFailed : SettingsMessage {
        override val isError: Boolean get() = true
    }
}
