package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Alturas, espaciados y tamanos fijos del diseno. */
@Immutable
class GastonomoDimens {
    /** Padding horizontal de una pantalla corriente. */
    val screenPadding: Dp = 20.dp

    /** Padding horizontal de las pantallas de autenticacion. */
    val authScreenPadding: Dp = 24.dp

    /** Separacion habitual entre tarjetas. */
    val cardGap: Dp = 12.dp

    /** Separacion minima entre tarjetas. */
    val cardGapTight: Dp = 10.dp

    /** Separacion maxima entre tarjetas. */
    val cardGapLoose: Dp = 14.dp

    /** Alto del boton primario. */
    val primaryButton: Dp = 56.dp

    /** Alto del boton secundario. */
    val secondaryButton: Dp = 54.dp

    /** Alto de un campo de texto. */
    val textField: Dp = 54.dp

    /** Alto del selector de mes y lado del boton de rango. */
    val monthPicker: Dp = 44.dp

    /** Alto de un chip o pill. */
    val pill: Dp = 36.dp

    /** Alto de la tab bar, sin contar el home indicator. */
    val tabBar: Dp = 78.dp

    /** Alto de la app bar de las pantallas secundarias. */
    val appBar: Dp = 52.dp

    /** Alto de una fila de lista corriente. */
    val listRow: Dp = 56.dp

    /** Alto de una fila de lista con subtitulo. */
    val listRowTall: Dp = 62.dp

    /** Alto de una fila de categoria predeterminada. */
    val listRowMedium: Dp = 58.dp

    /** Lado del tile de icono de una fila de lista. */
    val iconTile: Dp = 40.dp

    /** Lado del tile de icono de una fila de categoria. */
    val iconTileSmall: Dp = 38.dp

    /** Lado del tile de icono de una fila de Ajustes. */
    val iconTileTiny: Dp = 32.dp

    /** Lado del tile de icono de una tarjeta de formato de Exportar. */
    val iconTileMedium: Dp = 36.dp

    /** Lado del tile de la vista previa del sheet de categoria. */
    val iconTileLarge: Dp = 48.dp

    /** Lado de un circulo de color del sheet de categoria. */
    val colorSwatch: Dp = 34.dp

    /** Lado del avatar del header de Inicio. */
    val avatar: Dp = 44.dp

    /** Lado del avatar de la tarjeta de perfil de Ajustes. */
    val avatarLarge: Dp = 52.dp

    /** Lado del avatar del PIN y del detalle de prestamo. */
    val avatarHuge: Dp = 64.dp

    /** Lado del checkbox. */
    val checkbox: Dp = 22.dp

    /** Ancho del toggle. */
    val toggleWidth: Dp = 44.dp

    /** Alto del toggle. */
    val toggleHeight: Dp = 26.dp

    /** Lado del handle del toggle. */
    val toggleHandle: Dp = 20.dp

    /** Ancho del handle del bottom sheet. */
    val sheetHandleWidth: Dp = 40.dp

    /** Alto del handle del bottom sheet. */
    val sheetHandleHeight: Dp = 4.dp

    /** Alto del FAB. */
    val fab: Dp = 56.dp

    /** Alto de una celda del grid de categorias. */
    val categoryCell: Dp = 78.dp

    /** Alto de una tarjeta de accion rapida. */
    val quickAction: Dp = 76.dp

    /** Ancho de una tecla del teclado del PIN. */
    val keypadKeyWidth: Dp = 80.dp

    /** Alto de una tecla del teclado del PIN. */
    val keypadKeyHeight: Dp = 68.dp

    /** Ancho de la pildora del tab activo. */
    val tabPillWidth: Dp = 56.dp

    /** Alto de la pildora del tab activo. */
    val tabPillHeight: Dp = 30.dp

    /** Alto de la barra de progreso de una tarjeta de prestamo. */
    val progressBar: Dp = 6.dp

    /** Alto de la barra de progreso del detalle de prestamo. */
    val progressBarTall: Dp = 8.dp

    /** Alto de la barra apilada de gastos por categoria. */
    val stackedBar: Dp = 12.dp

    /** Grosor de un divisor. */
    val divider: Dp = 1.dp

    /** Grosor del borde de un control seleccionado. */
    val selectedBorder: Dp = 1.5.dp

    /** Objetivo tactil minimo. */
    val minTouchTarget: Dp = 44.dp
}
