package com.bahoque.gastonomo.core.designsystem.component

/**
 * Desde donde se abrio el sheet de moneda. Decide su titulo y su subtitulo, que el diseno
 * cambia segun lo que el usuario esta eligiendo.
 */
enum class CurrencySheetOrigin {
    /** Desde Ajustes: se elige la moneda en la que se ensena toda la aplicacion. */
    Settings,

    /** Desde un formulario: se elige la moneda de un solo registro. */
    Record,
}
