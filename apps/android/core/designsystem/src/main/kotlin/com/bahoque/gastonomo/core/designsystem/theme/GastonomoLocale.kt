package com.bahoque.gastonomo.core.designsystem.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.util.Locale

/**
 * Pone los recursos en el idioma elegido en Ajustes, que no tiene por que ser el del
 * telefono: el diseno ofrece Espanol e English dentro de la aplicacion.
 *
 * `stringResource` lee de `LocalResources`, que por defecto se calcula a partir de
 * `LocalContext` y `LocalConfiguration`. Se proveen los tres para que ninguna pantalla tenga
 * que enterarse: siguen llamando a `stringResource` igual.
 *
 * La configuracion se copia de la vigente, asi que un cambio de tamano o de densidad del
 * sistema sigue llegando; lo unico que se sustituye es el idioma.
 *
 * **Cuidado con lo que se calcula a partir de `LocalContext`.** El contexto que se provee aqui
 * es un contexto de configuracion, y **no es la actividad**: no es un `ContextWrapper`, asi que
 * no lleva a ella. `LocalActivity` no se provee en ningun sitio, se deriva de `LocalContext`, asi
 * que sustituirlo lo dejaba en null en toda la aplicacion. Quien quisiera abrir algo del sistema
 * se quedaba sin actividad, y eso no da un error: da una pantalla que no hace nada, o un cierre si
 * lo intenta con el contexto equivocado. Por eso se vuelve a fijar con el valor de fuera, que es
 * donde todavia se resuelve bien.
 *
 * Lo mismo pasa con el registro de resultados de actividad, que es lo que usan los permisos del
 * sistema: tambien se busca subiendo por `LocalContext`, y sin fijarlo pedir el microfono cerraba
 * la aplicacion en vez de ensenar el dialogo.
 */
// El reparto por idioma del bundle esta desactivado en app/build.gradle.kts, que es donde se
// configura. Lint no lo ve desde aqui porque este es un modulo de libreria.
@SuppressLint("AppBundleLocaleChanges")
@Composable
internal fun ProvideLocalizedResources(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Se lee ANTES de sustituir LocalContext, que es de donde sale.
    val activity = LocalActivity.current
    val current = LocalConfiguration.current
    val configuration = remember(current, language) {
        Configuration(current).apply { setLocale(Locale.forLanguageTag(language.localeTag)) }
    }
    val localized = remember(context, configuration) {
        context.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration,
        LocalResources provides localized.resources,
        LocalActivity provides activity,
    ) {
        ProvideActivityResultRegistry(activity, content)
    }
}

/**
 * Fija el registro de resultados de actividad, que es por donde pasan los permisos del sistema.
 *
 * Sin actividad no hay registro y no se provee nada: es lo que ocurre en las capturas y en los
 * tests que montan un composable suelto, donde tampoco hay permisos que pedir.
 */
@Composable
private fun ProvideActivityResultRegistry(activity: Activity?, content: @Composable () -> Unit) {
    val owner = activity as? ActivityResultRegistryOwner
    if (owner == null) {
        content()
        return
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner, content = content)
}
