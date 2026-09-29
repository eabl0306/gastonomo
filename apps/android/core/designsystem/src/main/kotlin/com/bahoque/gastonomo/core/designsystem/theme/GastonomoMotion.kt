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

    /**
     * Lo que se mueve dentro de un control sin entrar ni salir, como el fondo del segmento
     * activo. Es corto a proposito: una animacion larga en un control pequeno estorba.
     */
    val selectionMillis: Int = millis(SELECTION_MILLIS)

    /** Emphasized decelerate de M3: arranca rapido y frena al final. Para lo que entra. */
    val entering: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Emphasized accelerate de M3: arranca suave y acelera al irse. Para lo que sale. */
    val leaving: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** Standard de M3, para lo que se mueve dentro de la pantalla sin entrar ni salir. */
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /**
     * Lo que tarda cada opcion del menu radial en salir del boton.
     *
     * Es mas larga que el resto a proposito: la curva se pasa de largo y vuelve, y con menos
     * tiempo el rebote no se ve, solo se nota un tiron.
     */
    val radialMillis: Int = millis(RADIAL_MILLIS)

    /**
     * El retraso entre una opcion y la siguiente al abrir.
     *
     * Al **cerrar no se escalona**: salen en abanico y vuelven juntas. Escalonar el cierre hace
     * que el menu parezca pegajoso justo cuando el usuario ya decidio irse.
     */
    val radialStaggerMillis: Int = millis(RADIAL_STAGGER_MILLIS)

    /**
     * La curva del menu radial, que se pasa de largo y vuelve.
     *
     * Es la unica de la aplicacion que no es de Material: viene del prototipo, y ese rebote es
     * lo que hace que las opciones se lean como si salieran despedidas del boton.
     */
    val overshoot: Easing = CubicBezierEasing(0.3f, 1.35f, 0.5f, 1.0f)

    /** El spec de una opcion del menu radial, con su retraso. */
    fun <T> radialSpec(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(radialMillis, delayMillis = delayMillis, easing = overshoot)

    /** El spec de lo que entra. */
    fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(enterMillis, easing = entering)

    /** El spec de lo que sale. */
    fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(exitMillis, easing = leaving)

    /** El spec de lo que se mueve dentro de la pantalla. */
    fun <T> selectionSpec(): FiniteAnimationSpec<T> = tween(selectionMillis, easing = standard)

    private fun millis(value: Int): Int = if (enabled) value else 0

    private companion object {
        const val ENTER_MILLIS = 240
        const val EXIT_MILLIS = 180
        const val SELECTION_MILLIS = 170

        /** Lo que tarda una opcion del menu radial en salir del boton. */
        const val RADIAL_MILLIS = 240

        /** El retraso entre una opcion y la siguiente al abrir. */
        const val RADIAL_STAGGER_MILLIS = 35
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
