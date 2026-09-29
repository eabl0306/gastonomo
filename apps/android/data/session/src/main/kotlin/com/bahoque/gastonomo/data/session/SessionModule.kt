package com.bahoque.gastonomo.data.session

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.bahoque.gastonomo.core.application.port.BiometricLock
import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.application.port.SpeechToText

/**
 * Las fabricas de los adaptadores de este modulo. El contenedor de la aplicacion las llama
 * y no conoce ninguna clase de dentro: todas son `internal`.
 *
 * ```
 * // en AppContainer
 * val sessionStore = createSessionStore(context)
 * val localLock = createLocalLock(context)
 * ```
 */
fun createSessionStore(context: Context): SessionStore =
    DataStoreSessionStore(context.gastonomoDataStore)

fun createLocalLock(context: Context): LocalLock =
    DataStoreLocalLock(context.gastonomoDataStore)

fun createDeviceSettingsStore(context: Context): DeviceSettingsStore =
    DataStoreDeviceSettingsStore(context.gastonomoDataStore)

/**
 * @param activityProvider devuelve la actividad viva, porque el dialogo del sistema la
 *   necesita. Es un proveedor y no una referencia para no retener una actividad muerta.
 */
fun createBiometricLock(
    context: Context,
    activityProvider: () -> FragmentActivity?,
): BiometricLock = AndroidBiometricLock(context, activityProvider)

/** El reconocimiento de voz del sistema, para el dictado. */
fun createSpeechToText(context: Context): SpeechToText = AndroidSpeechToText(context)
