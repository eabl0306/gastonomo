package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Los roles de texto del handoff, con sus tamanos, pesos y tracking exactos. El tracking
 * del diseno esta en em y aqui tambien, para que no dependa del tamano.
 *
 * Los montos piden cifras de ancho fijo, y eso lo aporta
 * [com.bahoque.gastonomo.core.designsystem.component.withTabularNumbers].
 */
@Immutable
class GastonomoTypography {
    /** H1 de las pantallas de autenticacion. */
    val authTitle: TextStyle = style(32.sp, FontWeight.Bold, TIGHT).copy(lineHeight = 35.2.sp)

    /** H1 de Registro y Recuperar, un punto mas pequeno. */
    val authTitleSmall: TextStyle = style(30.sp, FontWeight.Bold, TIGHT).copy(lineHeight = 33.sp)

    /** Titulo de una pantalla que lleva tab bar. */
    val screenTitle: TextStyle = style(26.sp, FontWeight.Bold, SNUG)

    /** Saludo de Inicio. */
    val greeting: TextStyle = style(24.sp, FontWeight.Bold, SNUG)

    /** Monto de la tarjeta de balance. */
    val balance: TextStyle = style(36.sp, FontWeight.Bold, TIGHT)

    /** Monto de los formularios de movimiento y prestamo. */
    val amountInput: TextStyle = style(44.sp, FontWeight.Bold, TIGHT)

    /** Monto del formulario de abono. */
    val amountInputSmall: TextStyle = style(42.sp, FontWeight.Bold, TIGHT)

    /** Monto del detalle de prestamo. */
    val amountDetail: TextStyle = style(34.sp, FontWeight.Bold, TIGHT)

    /**
     * Saludo de la pantalla de desbloqueo, un punto por debajo del de Inicio.
     * El prototipo lo pone en 22 y el de Inicio en 24.
     */
    val unlockGreeting: TextStyle = style(22.sp, FontWeight.Bold, SNUG)

    /** Iniciales de un avatar de 40 o 44. */
    val avatarInitials: TextStyle = style(15.sp, FontWeight.Bold)

    /** Iniciales del avatar de 64 de la pantalla de desbloqueo. */
    val avatarInitialsHuge: TextStyle = style(22.sp, FontWeight.Bold)

    /** Titulo de un bottom sheet. */
    val sheetTitle: TextStyle = style(19.sp, FontWeight.Bold, SLIGHT)

    /** Titulo de la app bar. */
    val appBarTitle: TextStyle = style(17.sp, FontWeight.Bold)

    /** Titulo de seccion dentro de una pantalla. */
    val sectionTitle: TextStyle = style(16.sp, FontWeight.Bold)

    /** Cuerpo y titulo de fila de lista. */
    val body: TextStyle = style(15.sp, FontWeight.SemiBold)

    /**
     * Texto de un segmento del segmented control. Es el mismo en el activo y en el
     * inactivo: el prototipo solo les cambia el color y el fondo, nunca el peso.
     */
    val segment: TextStyle = style(15.sp, FontWeight.Bold)

    /** Lo mismo en el segmented del sheet de categoria, que el diseno pinta un punto menor. */
    val segmentCompact: TextStyle = style(14.sp, FontWeight.Bold)

    /** Cuerpo compacto, el de las filas mas densas. */
    val bodySmall: TextStyle = style(14.sp, FontWeight.Medium)

    /** Label de un campo de formulario. */
    val fieldLabel: TextStyle = style(13.sp, FontWeight.SemiBold)

    /** Texto secundario. */
    val secondary: TextStyle = style(13.sp, FontWeight.Medium)

    /** Texto secundario pequeno, el de los subtitulos de fila. */
    val secondarySmall: TextStyle = style(12.sp, FontWeight.Medium)

    /** Etiqueta de 12 px en semibold: label de accion rapida, encabezado de grupo de dia. */
    val labelStrong: TextStyle = style(12.sp, FontWeight.SemiBold)

    /** Etiqueta de 11 px del eje del grafico de tendencia. */
    val trendLabel: TextStyle = style(11.sp, FontWeight.Medium)

    /** Overline de un grupo de ajustes. El texto va en mayusculas. */
    val overline: TextStyle = style(12.sp, FontWeight.Bold, WIDE)

    /** Label del tab activo. */
    val tabActive: TextStyle = style(11.sp, FontWeight.Bold).copy(textAlign = TextAlign.Center)

    /** Label del tab inactivo. */
    val tabInactive: TextStyle = style(11.sp, FontWeight.Medium).copy(textAlign = TextAlign.Center)

    /** Monto en una fila de lista: 14 en bold, como el titulo que lleva al lado. */
    val amount: TextStyle = style(14.sp, FontWeight.Bold)

    /**
     * Titulo de una fila de movimiento. El diseno la pinta un punto por debajo del resto
     * de las filas, que van en [body].
     */
    val rowTitle: TextStyle = style(14.sp, FontWeight.SemiBold)

    /** Etiqueta de una fila de Ajustes, la unica fila de 15 que el diseno pinta en medium. */
    val settingsLabel: TextStyle = style(15.sp, FontWeight.Medium)

    /** Importe de una fila del grafico de gastos por categoria. */
    val sliceAmount: TextStyle = style(14.sp, FontWeight.SemiBold)

    /** Monto de las tarjetas "Me deben" y "Debo" de Inicio. */
    val loanTotal: TextStyle = style(17.sp, FontWeight.Bold)

    /** Monto destacado de una tarjeta de prestamo. */
    val amountCard: TextStyle = style(20.sp, FontWeight.Bold)

    /** Logo tipografico: la "g" del cuadrado ink. */
    val logoMark: TextStyle = style(18.sp, FontWeight.ExtraBold)

    /** Logo tipografico: la palabra "gastonomo". */
    val logoWord: TextStyle = style(17.sp, FontWeight.Bold)

    /**
     * Label de 11 dp: "Saldo pendiente" y "de {original}" en una tarjeta de prestamo, y las
     * tres columnas de su detalle. Es el texto mas pequeno del diseno.
     */
    val caption: TextStyle = style(11.sp, FontWeight.Medium)

    /** Digito de una tecla del teclado del PIN. */
    val keypadDigit: TextStyle =
        style(26.sp, FontWeight.SemiBold).copy(textAlign = TextAlign.Center)

    private companion object {
        val TIGHT = (-0.03).em
        val SNUG = (-0.02).em
        val SLIGHT = (-0.01).em
        val WIDE = 0.06.em

        fun style(size: TextUnit, weight: FontWeight, tracking: TextUnit = 0.em): TextStyle =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = weight,
                fontSize = size,
                letterSpacing = tracking,
            )
    }
}

/** Familia tipografica del diseno, para quien necesite construir un estilo suelto. */
val GastonomoFontFamily: FontFamily get() = PlusJakartaSans
