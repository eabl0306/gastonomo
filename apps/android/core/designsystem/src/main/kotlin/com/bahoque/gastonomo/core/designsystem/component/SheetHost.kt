package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Monta un bottom sheet con su animacion de entrada y de salida.
 *
 * Hace falta un envoltorio porque una hoja no puede animar su salida si quien la usa la quita
 * con un `if`: en cuanto la condicion es falsa, la hoja ya no esta compuesta y no hay nada que
 * animar. Este composable se queda compuesto el tiempo que dura la salida.
 *
 * ```
 * GastonomoSheetHost(visible = state.currencySheetOpen) {
 *     CurrencySheet(...)
 * }
 * ```
 *
 * El velo y el panel se animan a la vez: el velo funde con este `AnimatedVisibility` y el panel
 * sube con su propio `animateEnterExit` dentro de [GastonomoBottomSheet]. Si el velo apareciera
 * de golpe se veria un parpadeo oscuro antes de que suba la hoja.
 *
 * La primera composicion no anima, que es lo que hace `AnimatedVisibility` con un `visible` que
 * ya llega a true. Por eso las capturas de Roborazzi siguen saliendo con la hoja entera.
 */
@Composable
fun GastonomoSheetHost(visible: Boolean, content: @Composable () -> Unit) {
    val motion = GastonomoTheme.motion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.enterSpec()),
        exit = fadeOut(motion.exitSpec()),
    ) {
        CompositionLocalProvider(LocalSheetTransition provides this) { content() }
    }
}

/**
 * Lo mismo, para una hoja cuyo contenido sale de un estado que se pone a null al cerrarla.
 *
 * Retiene el ultimo valor no nulo mientras la hoja baja. Sin eso, cerrarla vaciaria el contenido
 * antes de que se viera salir, y lo que bajaria seria una hoja vacia.
 *
 * ```
 * GastonomoSheetHost(state.rangeDraft) { draft ->
 *     DateRangeSheet(from = draft.from, to = draft.to, ...)
 * }
 * ```
 */
@Composable
fun <T : Any> GastonomoSheetHost(value: T?, content: @Composable (T) -> Unit) {
    var retained by remember { mutableStateOf<T?>(null) }
    if (value != null) retained = value
    GastonomoSheetHost(visible = value != null) {
        retained?.let { content(it) }
    }
}

/**
 * El ambito de animacion de la hoja que se esta abriendo o cerrando, puesto por
 * [GastonomoSheetHost] y leido por [GastonomoBottomSheet].
 *
 * Va por CompositionLocal y no por parametro porque entre el host y la hoja hay siempre un
 * componente de por medio ([CurrencySheet], [DateRangeSheet], el sheet de categoria...), y
 * pasarlo a mano obligaria a anadir el mismo parametro a los seis.
 *
 * Cuando es null la hoja se pinta sin animacion. Eso es lo que pasa en las capturas y en los
 * tests que montan una hoja sola, que tienen que ser deterministas.
 */
internal val LocalSheetTransition = compositionLocalOf<AnimatedVisibilityScope?> { null }
