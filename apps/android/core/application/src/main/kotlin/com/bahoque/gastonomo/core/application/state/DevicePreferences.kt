package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lo ultimo que se leyo de los ajustes de **este telefono**: el cerrojo local y el ojo que
 * oculta los montos. El hermano de [UserPreferences], que guarda los de la cuenta.
 *
 * Hace falta porque leerlos es asincrono y una pantalla que los pinte arranca con el valor por
 * defecto: el interruptor del cerrojo salia apagado y se encendia solo un instante despues,
 * con su animacion, cada vez que se entraba en Ajustes. Con el valor ya leido, la pantalla
 * empieza con el bueno y no hay nada que animar.
 *
 * Lo rellena [com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings]
 * sin que nadie tenga que acordarse: cualquiera que observe los ajustes lo deja al dia. Y
 * alguien los observa siempre, porque el arranque decide con ellos si toca pedir el PIN.
 *
 * Arranca con los valores por defecto del dominio, que es lo que vale antes de la primera
 * lectura y cuando no hay sesion.
 */
class DevicePreferences(initial: DeviceSettings = DeviceSettings()) {

    private val mutable = MutableStateFlow(initial)

    /** Los ajustes del dispositivo vigentes. */
    val settings: StateFlow<DeviceSettings> = mutable.asStateFlow()

    /** Lo llama el caso de uso que los observa, en cada emision. */
    fun set(settings: DeviceSettings) {
        mutable.value = settings
    }
}
