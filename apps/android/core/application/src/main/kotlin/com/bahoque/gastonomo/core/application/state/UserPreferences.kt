package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * La ultima moneda principal e idioma que la API confirmo, para que la interfaz entera los
 * aplique y no solo la pantalla que los leyo.
 *
 * Hace falta porque el idioma decide los textos y el formato de numeros y fechas de **todas**
 * las pantallas, y la moneda principal decide el simbolo y los decimales. Eso lo pinta el
 * tema, que esta por encima de cualquier ViewModel, asi que el valor no puede vivir en uno.
 *
 * Lo rellenan [com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings] y
 * [com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings], que son los dos
 * unicos sitios de donde sale un [UserSettings] de verdad. Cualquier pantalla que lea los
 * ajustes lo actualiza sin saberlo, asi que no hay una llamada de red extra al arrancar.
 *
 * ```
 * // en la raiz de composicion
 * val settings by preferences.settings.collectAsState()
 * GastonomoTheme(language = settings.language, displayCurrency = settings.displayCurrency) { ... }
 * ```
 *
 * Arranca con los valores por defecto del dominio, que es lo que se ve el primer fotograma y
 * mientras no haya sesion.
 */
class UserPreferences(initial: UserSettings = UserSettings()) {

    private val mutable = MutableStateFlow(initial)

    /** Los ajustes vigentes. Cambian para toda la interfaz a la vez. */
    val settings: StateFlow<UserSettings> = mutable.asStateFlow()

    /** Lo llaman los casos de uso de ajustes cuando la API responde. */
    fun set(settings: UserSettings) {
        mutable.value = settings
    }
}
