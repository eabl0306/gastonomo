package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.format.GastonomoFormats
import com.bahoque.gastonomo.core.designsystem.theme.LocalContentColor
import com.bahoque.gastonomo.core.designsystem.theme.LocalTextStyle

/**
 * El texto de la aplicacion. Hereda estilo y color del contenedor, asi que una fila no
 * tiene que repetir `style =` y `color =` en cada linea, y una superficie oscura cambia
 * el color de todo lo que lleva dentro de una sola vez.
 *
 * ```
 * GastonomoText("Mercado", style = GastonomoTheme.typography.body)
 * GastonomoText(subtitle, color = GastonomoTheme.colors.muted, maxLines = 1)
 * ```
 */
@Composable
fun GastonomoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = if (color != Color.Unspecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolved),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * La version con estilos dentro del propio texto, para las frases que resaltan una parte:
 * el correo en negrita de "Enviamos un enlace a **{email}**", o el monto original de una
 * fila de movimiento.
 *
 * ```
 * val text = buildAnnotatedString {
 *     append(stringResource(R.string.sent_to))
 *     withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.ink)) { append(email) }
 * }
 * GastonomoText(text = text, style = GastonomoTheme.typography.body, color = colors.muted)
 * ```
 */
@Composable
fun GastonomoText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val resolved = if (color != Color.Unspecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolved),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Los montos llevan cifras de ancho fijo para que una columna de importes no baile al
 * cambiar de digito. Es el `font-variant-numeric: tabular-nums` del diseno.
 *
 * Tambien se leen en voz alta con palabras. El signo del diseno es el menos tipografico U+2212
 * y la mascara del ojo cerrado son seis puntos: ninguno de los dos se lee como lo que significa,
 * asi que el texto que se ve y el que se dicta no son el mismo.
 */
@Composable
fun GastonomoAmountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val spoken = spokenAmountOf(text)
    GastonomoText(
        text = text,
        modifier = modifier.semantics { contentDescription = spoken },
        style = style.withTabularNumbers(),
        color = color,
        maxLines = 1,
    )
}

/** Como se dicta un importe: con el signo y la mascara en palabras. */
@Composable
private fun spokenAmountOf(text: String): String = when {
    GastonomoFormats.MASK in text -> stringResource(R.string.gastonomo_amount_hidden)
    text.startsWith(GastonomoFormats.MINUS) ->
        stringResource(R.string.gastonomo_amount_negative, text.removePrefix(GastonomoFormats.MINUS))
    text.startsWith(GastonomoFormats.PLUS) ->
        stringResource(R.string.gastonomo_amount_positive, text.removePrefix(GastonomoFormats.PLUS))
    else -> text
}

/** Activa las cifras de ancho fijo de la fuente. */
fun TextStyle.withTabularNumbers(): TextStyle = copy(
    fontFeatureSettings = TABULAR_NUMBERS,
)

private const val TABULAR_NUMBERS = "tnum"
