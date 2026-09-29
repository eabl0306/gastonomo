package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Los radios del diseno, con el nombre de lo que redondean. */
@Immutable
class GastonomoRadii {
    /** Bottom sheet, solo las dos esquinas de arriba. */
    val sheet: Dp = 28.dp

    /** Tarjeta de balance y tarjeta del detalle de prestamo. */
    val balanceCard: Dp = 26.dp

    /** Tarjeta de prestamo y tarjeta de categorias. */
    val loanCard: Dp = 22.dp

    /** Tarjeta de perfil de Ajustes. */
    val profileCard: Dp = 24.dp

    /** Tecla del teclado del PIN. */
    val keypadKey: Dp = 22.dp

    /** Listas y tarjetas corrientes. */
    val card: Dp = 20.dp

    /** Tile de 64 del estado enviado de Recuperar, y el FAB de una palabra. */
    val fab: Dp = 20.dp

    /** El FAB de registro rapido, que es un cuadrado redondeado y no un circulo. */
    val fabRadial: Dp = 22.dp

    /** Tarjetas pequenas: acciones rapidas, caja de conversion. */
    val smallCard: Dp = 18.dp

    /** Chips y pills. */
    val pill: Dp = 18.dp

    /** Inputs, botones grandes y celdas de categoria. */
    val control: Dp = 16.dp

    /** Sub tarjetas de la tarjeta de balance. */
    val subCard: Dp = 16.dp

    /** Selector de mes y boton de rango. */
    val monthPicker: Dp = 14.dp

    /** Tile de la vista previa del sheet de categoria. */
    val previewTile: Dp = 15.dp

    /** Tile de icono de 40 en una fila de lista. */
    val iconTile: Dp = 13.dp

    /** Celda del grid de iconos del sheet de categoria y tile de 36 de Exportar. */
    val iconChoice: Dp = 11.dp

    /** Tile de icono de 38 en una fila de categoria. */
    val iconTileSmall: Dp = 12.dp

    /** Cuadrado del logo. */
    val logo: Dp = 11.dp

    /** Tile de icono mas pequeno. */
    val iconTileTiny: Dp = 10.dp

    /** Checkbox de 22. */
    val checkbox: Dp = 7.dp

    /** Barra apilada del grafico de gastos por categoria. */
    val bar: Dp = 4.dp
}
