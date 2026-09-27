package com.bahoque.gastonomo.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Los tiempos y las curvas de las animaciones. Son tokens como los colores: una duracion no se
 * escribe a mano fuera de este modulo.
 *
 * Las curvas son las de Material 3, escritas por valor. No se usa `MotionScheme` porque todavia
 * es experimental en Material 3 y ataria el sistema de diseno a una API que puede cambiar de
 * firma; los numeros, en cambio, son los de la especificacion y no se mueven.
 *
 * Los tiempos son cortos a proposito. Estas animaciones existen para tapar el hueco de la carga,
 * asi que una que dure mas de 300 ms consigue justo lo contrario de lo que se busca: que la
 * aplicacion parezca lenta.
 *
 * @property enabled false cuando el sistema tiene las animaciones apagadas. Entonces **todas las
 *   duraciones son cero**, asi que un composable nuevo respeta la preferencia por el hecho de
 *   leer el token, sin que quien lo escriba tenga que acordarse.
 */
@Immutable
class GastonomoMotion(val enabled: Boolean = true) {

    /** Lo que entra en pantalla. */
    val enterMillis: Int = millis(ENTER_MILLIS)

    /** Lo que sale. Sale mas rapido de lo que entra: lo que se va no merece atencion. */
    val exitMillis: Int = millis(EXIT_MILLIS)

    /** Bottom sheets, modales y el indicador de los tabs. */
    val overlayMillis: Int = millis(OVERLAY_MILLIS)

    /** Emphasized decelerate de M3: arranca rapido y frena al final. Para lo que entra. */
    val entering: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Emphasized accelerate de M3: arranca suave y acelera al irse. Para lo que sale. */
    val leaving: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** Standard de M3, para lo que se mueve dentro de la pantalla sin entrar ni salir. */
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** El spec de lo que entra. */
    fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(enterMillis, easing = entering)

    /** El spec de lo que sale. */
    fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(exitMillis, easing = leaving)

    /** El spec de un overlay o de un movimiento dentro de la pantalla. */
    fun <T> overlaySpec(): FiniteAnimationSpec<T> = tween(overlayMillis, easing = standard)

    private fun millis(value: Int): Int = if (enabled) value else 0

    private companion object {
        const val ENTER_MILLIS = 240
        const val EXIT_MILLIS = 180
        const val OVERLAY_MILLIS = 220
    }
}

/**
 * Si el sistema tiene las animaciones encendidas.
 *
 * Se lee de `TRANSITION_ANIMATION_SCALE`, que es lo que pone a cero tanto "Quitar animaciones"
 * de accesibilidad como la escala de animacion de las opciones de desarrollador. Si el ajuste no
 * existe se asume que si: es lo que hace el sistema.
 */
internal fun systemAnimationsEnabled(context: Context): Boolean = Settings.Global.getFloat(
    context.contentResolver,
    Settings.Global.TRANSITION_ANIMATION_SCALE,
    ANIMATIONS_ON,
) > ANIMATIONS_OFF

private const val ANIMATIONS_ON = 1f
private const val ANIMATIONS_OFF = 0f
