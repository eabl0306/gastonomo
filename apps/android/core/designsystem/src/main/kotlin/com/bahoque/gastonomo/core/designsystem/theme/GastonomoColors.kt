package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Los colores del rediseno, uno a uno y con el nombre que usa el handoff. No hay paleta
 * de Material aqui a proposito: el diseno es hi-fi y cada valor esta fijado.
 *
 * Se accede a ellos con [com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme.colors].
 */
@Immutable
class GastonomoColors {
    /** Texto principal, botones primarios, tarjeta de balance, marco. */
    val ink: Color = Color(0xFF1B1A17)

    /** Texto secundario y labels. */
    val muted: Color = Color(0xFF736D62)

    /** Placeholders y texto terciario. */
    val faint: Color = Color(0xFFA9A397)

    /** Acento: iconos sobre ink, pildora del tab activo, avatar de Ajustes. */
    val lime: Color = Color(0xFFD4EF7A)

    /** Fondo de pantalla. */
    val bgApp: Color = Color(0xFFF5F2EC)

    /** Barra inferior, bottom sheets y celdas de categoria no seleccionadas. */
    val bgNav: Color = Color(0xFFFBF9F5)

    /** Tarjetas e inputs. */
    val surface: Color = Color(0xFFFFFFFF)

    /** Borde de tarjetas. */
    val line: Color = Color(0xFFE8E3D9)

    /** Borde de inputs en reposo. En foco pasa a [ink]. */
    val inputBorder: Color = Color(0xFFE3DED4)

    /** Separador de filas dentro de una tarjeta. */
    val divider: Color = Color(0xFFF0ECE4)

    /** Contenedor del segmented control, botones secundarios y cajas de info. */
    val chipBg: Color = Color(0xFFECE8DF)

    /** Fondo del tile de icono en las filas de Ajustes. */
    val iconTile: Color = Color(0xFFF2EFE8)

    /** Fondo de las barras de progreso. */
    val track: Color = Color(0xFFEFEBE3)

    /** Toggle apagado y handle del bottom sheet. */
    val toggleOff: Color = Color(0xFFD9D4C9)

    /** Borde del radio sin marcar e iconos de candado o vacio. */
    val radioOff: Color = Color(0xFFC9C3B6)

    /** Ingresos y estado Pagado. */
    val green: Color = Color(0xFF2F8A55)

    /** Fondo del estado Pagado. */
    val greenBg: Color = Color(0xFFE6F2EA)

    /** Gastos, estado Pendiente, cerrar sesion y borrar. */
    val red: Color = Color(0xFFC2412F)

    /** Fondo del estado Pendiente. */
    val redBg: Color = Color(0xFFFBEAE7)

    /** Me deben y prestamos otorgados. */
    val blue: Color = Color(0xFF2F6FB0)

    /** Fondo de Me deben. */
    val blueBg: Color = Color(0xFFE3EDF7)

    /** Debo, prestamos recibidos e intereses ganados. */
    val amber: Color = Color(0xFFB67A1C)

    /** Fondo de Debo. */
    val amberBg: Color = Color(0xFFF6ECDA)

    /** Texto de las notas y cajas de info sobre [chipBg]. Tambien el estado Parcial. */
    val noteText: Color = Color(0xFF5D584E)

    /** Fondo del estado Parcial. */
    val partialBg: Color = Color(0xFFEFECE6)

    /** Caja de la nota en el detalle de un prestamo. */
    val loanNote: Color = Color(0xFFF7F5F0)

    /** Velo del bottom sheet. */
    val scrim: Color = Color(0x661B1A17)

    /**
     * El gris del icono de CSV en Exportar. Esta aqui y no en la feature porque un color no se
     * escribe a mano fuera del sistema de diseno, aunque el diseno lo de suelto y no lo use
     * nadie mas.
     */
    val csvGrey: Color = Color(0xFF5B6573)

    /** Colores propios de la tarjeta de balance, que va sobre [ink]. */
    val balance: BalanceCardColors = BalanceCardColors()
}

/**
 * La tarjeta de balance es la unica superficie oscura del diseno y tiene su propia escala
 * de grises, asi que se agrupa aparte en vez de contaminar la paleta general.
 */
@Immutable
class BalanceCardColors {
    /** Labels sobre la tarjeta oscura. */
    val label: Color = Color(0xFFB9B3A6)

    /** Texto terciario sobre la tarjeta oscura. */
    val tertiary: Color = Color(0xFF8B8578)

    /** Fondo de las dos sub tarjetas de Ingresos y Gastos. */
    val subCard: Color = Color(0xFF262521)

    /** Linea cero punteada del grafico de tendencia. */
    val zeroLine: Color = Color(0xFF3A3833)

    /** Flecha de ingresos. */
    val incomeArrow: Color = Color(0xFF8FD49F)

    /** Flecha de gastos. */
    val expenseArrow: Color = Color(0xFFF0A293)
}
