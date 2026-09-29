package com.bahoque.gastonomo.feature.dictation

import androidx.compose.runtime.Immutable

/**
 * Lo que se puede hacer en la hoja de dictado.
 *
 * Va en un solo objeto para que la hoja reciba un parametro y no ocho, igual que el resto de las
 * pantallas.
 */
@Immutable
data class DictateActions(
    /** Empieza a escuchar, o vuelve a escuchar desde el modo escrito. */
    val onListen: () -> Unit,

    /** Termina de escuchar y espera el resultado del sistema. */
    val onStopListening: () -> Unit,

    /** Pasa al modo escrito porque el usuario lo prefiere. */
    val onTypeInstead: () -> Unit,

    val onText: (String) -> Unit,

    /** Manda a la API el texto escrito, o el del ejemplo que se toco. */
    val onInterpret: (String) -> Unit,

    val onSave: () -> Unit,

    /** Abre el formulario completo con lo que se entendio, para corregirlo. */
    val onEdit: () -> Unit,

    /** Descarta el borrador y vuelve a escuchar. */
    val onRedo: () -> Unit,

    val onClose: () -> Unit,
) {
    companion object {
        /** Unas acciones que no hacen nada, para tests y capturas. */
        val NONE = DictateActions(
            onListen = {},
            onStopListening = {},
            onTypeInstead = {},
            onText = {},
            onInterpret = {},
            onSave = {},
            onEdit = {},
            onRedo = {},
            onClose = {},
        )
    }
}
