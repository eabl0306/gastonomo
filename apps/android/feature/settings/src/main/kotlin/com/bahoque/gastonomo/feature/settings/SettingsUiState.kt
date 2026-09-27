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
    /** Si la ultima lectura llego a la API. False solo cuando fallo por red. */
    val reachedApi: Boolean = true,

    /** Si el dispositivo tiene por donde salir a internet. */
    val online: Boolean = true,

    /** Cuantos registros siguen sin salir del telefono. */
    val pendingCount: Int = 0,

    /** Como va la resincronizacion completa. */
    val resync: ResyncProgress = ResyncProgress.IDLE,
    val sheet: SettingsSheet? = null,
    val currencyQuery: String = "",
    val pin: String = "",
    val message: SettingsMessage? = null,
) {

    /**
     * Lo que dice la fila. Se deriva, no se guarda: guardarlo permitiria que dijera una cosa
     * mientras la cola dice otra, que es exactamente como empezo este issue.
     */
    val cloudSync: CloudSyncState
        get() = when {
            pendingCount > 0 && online -> CloudSyncState.SYNCING
            pendingCount > 0 -> CloudSyncState.PENDING
            online && reachedApi -> CloudSyncState.SYNCED
            else -> CloudSyncState.OFFLINE
        }

    /**
     * Si se puede registrar en otra moneda. Sin red no: guardar un importe sin conocer su
     * equivalencia rompe la regla de los cuatro campos, y las tasas guardadas pueden ser viejas.
     */
    val canConvert: Boolean get() = online && rates is RatesIndicator.Ready
}

/** Lo que ensena la fila "Tasas de cambio", que es de solo lectura. */
sealed interface RatesIndicator {

    /** Mientras `GET /v1/exchange-rates` esta en vuelo. */
    data object Loading : RatesIndicator

    /** Tasas al dia: la fila ensena "Frankfurter . {fecha}" en verde. */
    data class Ready(val date: LocalDate?) : RatesIndicator

    /** Tasas viejas o 503: la fila ensena "Sin conexion". */
    data object Offline : RatesIndicator
}

/**
 * El indicador "Sincronizacion en la nube". Es del cliente, no un campo de la API.
 *
 * Sale de dos cosas que el usuario no puede ver por su cuenta: **cuantas escrituras siguen sin
 * salir del telefono** y **si hay red**. Antes era un texto fijo que decia "Al dia" pasara lo que
 * pasara, que es justo lo que destapo que la aplicacion tenia que funcionar sin conexion.
 */
enum class CloudSyncState {
    /** Nada pendiente y la API responde. */
    SYNCED,

    /** Hay cola y hay red: se esta vaciando. */
    SYNCING,

    /** Hay cola y no hay por donde subirla. Es lo que el usuario querria saber antes de
     *  desinstalar la aplicacion o cambiar de telefono. */
    PENDING,

    /** Sin red y sin nada pendiente. No hay nada que perder, pero tampoco nada que bajar. */
    OFFLINE,
}

/** Las tres hojas que abre Ajustes. */
enum class SettingsSheet {
    CURRENCY,
    LANGUAGE,

    /** Crear el PIN al encender el cerrojo. El diseno no tiene pantalla para esto. */
    PIN,

    /** Confirmacion de borrar lo local y bajarlo otra vez. */
    RESYNC,
}

/**
 * Como va la resincronizacion completa.
 *
 * [UNFINISHED] sale de la base local, no de esta sesion: si la aplicacion murio a la mitad, la
 * marca sigue puesta al volver. Es lo que evita confundir medio historico con el historico entero.
 */
enum class ResyncProgress {
    IDLE,
    RUNNING,

    /** Quedo a medias. El siguiente arranque la termina. */
    UNFINISHED,
}

/** Lo que Ajustes cuenta por el toast. Se resuelve a texto en la pantalla, no en el ViewModel. */
sealed interface SettingsMessage {

    /** True si es un fallo: el toast cambia el icono en vez de mentir con un check. */
    val isError: Boolean get() = false

    /** "Mostrando todo en {COD}". */
    data class CurrencyChanged(val currency: CurrencyCode) : SettingsMessage

    /** La resincronizacion completa acabo. */
    data object ResyncDone : SettingsMessage

    /** Se corto a la mitad. La marca sigue puesta y el siguiente arranque la termina. */
    data object ResyncFailed : SettingsMessage {
        override val isError: Boolean get() = true
    }

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
