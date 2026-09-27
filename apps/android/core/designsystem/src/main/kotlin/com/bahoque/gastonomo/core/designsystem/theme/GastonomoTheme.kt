package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.bahoque.gastonomo.core.designsystem.format.GastonomoFormats
import com.bahoque.gastonomo.core.designsystem.format.LocalGastonomoFormats
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage

/**
 * Raiz del sistema de diseno. Envuelve toda la aplicacion y publica los tokens por
 * CompositionLocal: ninguna pantalla declara un color ni un tamano suelto.
 *
 * Se usa asi, y los tokens se leen con el objeto [GastonomoTheme]:
 * ```
 * GastonomoTheme(language = AppLanguage.SPANISH, displayCurrency = CurrencyCode("COP")) {
 *     Text("Hola", style = GastonomoTheme.typography.body, color = GastonomoTheme.colors.ink)
 * }
 * ```
 *
 * No hay modo oscuro. El diseno entregado es uno solo y meter un segundo esquema seria
 * inventar valores que nadie ha decidido.
 */
@Composable
fun GastonomoTheme(
    language: AppLanguage = AppLanguage.SPANISH,
    displayCurrency: CurrencyCode = CurrencyCode.COP,
    content: @Composable () -> Unit,
) {
    val colors = remember { GastonomoColors() }
    val context = LocalContext.current
    // La preferencia del sistema se lee una vez y queda en los tokens, asi que cualquier
    // composable la respeta por leer la duracion.
    val motion = remember(context) { GastonomoMotion(systemAnimationsEnabled(context)) }
    val formats = remember(language, displayCurrency) {
        GastonomoFormats(language = language, displayCurrency = displayCurrency)
    }
    val selection = remember(colors) {
        TextSelectionColors(handleColor = colors.ink, backgroundColor = colors.lime)
    }
    CompositionLocalProvider(
        LocalGastonomoColors provides colors,
        LocalGastonomoTypography provides remember { GastonomoTypography() },
        LocalGastonomoRadii provides remember { GastonomoRadii() },
        LocalGastonomoDimens provides remember { GastonomoDimens() },
        LocalGastonomoShadows provides remember { GastonomoShadows() },
        LocalGastonomoMotion provides motion,
        LocalGastonomoFormats provides formats,
        LocalTextSelectionColors provides selection,
        LocalIndication provides NoIndication,
        LocalContentColor provides colors.ink,
        LocalTextStyle provides GastonomoTypography().body,
    ) {
        ProvideLocalizedResources(language = language, content = content)
    }
}

/** Punto unico de acceso a los tokens. Solo se puede leer dentro de un [GastonomoTheme]. */
object GastonomoTheme {
    val colors: GastonomoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoColors.current

    val typography: GastonomoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoTypography.current

    val radii: GastonomoRadii
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoRadii.current

    val dimens: GastonomoDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoDimens.current

    val shadows: GastonomoShadows
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoShadows.current

    /** Duraciones y curvas. Valen cero si el sistema tiene las animaciones apagadas. */
    val motion: GastonomoMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoMotion.current

    /** Formato de dinero, tasas y fechas para el idioma y la moneda principal vigentes. */
    val formats: GastonomoFormats
        @Composable
        @ReadOnlyComposable
        get() = LocalGastonomoFormats.current
}

/** Color de texto e icono heredado. Lo cambia cualquier contenedor con fondo oscuro. */
val LocalContentColor = compositionLocalOf { GastonomoColors().ink }

/** Estilo de texto heredado, para no repetir `style =` en cada Text de una fila. */
val LocalTextStyle = compositionLocalOf { GastonomoTypography().body }

internal val LocalGastonomoColors = staticCompositionLocalOf { GastonomoColors() }
internal val LocalGastonomoTypography = staticCompositionLocalOf { GastonomoTypography() }
internal val LocalGastonomoRadii = staticCompositionLocalOf { GastonomoRadii() }
internal val LocalGastonomoDimens = staticCompositionLocalOf { GastonomoDimens() }
internal val LocalGastonomoShadows = staticCompositionLocalOf { GastonomoShadows() }
internal val LocalGastonomoMotion = staticCompositionLocalOf { GastonomoMotion() }

/**
 * Reemplaza el borroso circular de Material. El diseno no lo usa: sus controles responden
 * con `scale(.98)`, que es lo que hace
 * [com.bahoque.gastonomo.core.designsystem.component.pressScale].
 */
internal object NoIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(
        interactionSource: androidx.compose.foundation.interaction.InteractionSource,
    ): androidx.compose.ui.node.DelegatableNode = object : androidx.compose.ui.Modifier.Node() {}

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

/** Aplica un color a un estilo sin reescribirlo entero. */
fun TextStyle.withColor(color: Color): TextStyle = copy(color = color)
