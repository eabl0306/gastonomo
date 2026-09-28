package com.bahoque.gastonomo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.fragment.app.FragmentActivity
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.feature.auth.authStartDestination

/**
 * La unica actividad. Es una [FragmentActivity] porque el dialogo de huella de
 * androidx.biometric lo exige; con una ComponentActivity a secas no se puede ensenar.
 *
 * El idioma y la moneda principal se leen aqui, por encima del tema, porque afectan a todas
 * las pantallas: el idioma decide los textos y el formato de numeros y fechas, y la moneda el
 * simbolo y los decimales. El valor sale de la API en cuanto una pantalla lee los ajustes.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GastonomoApplication).container
        container.currentActivity = this
        setContent {
            val settings by container.preferences.settings.collectAsState()
            GastonomoTheme(
                language = settings.language,
                displayCurrency = settings.displayCurrency,
            ) {
                val start by produceState<GastonomoRoute?>(initialValue = null) {
                    value = authStartDestination(container.auth)
                }
                // Al abrir se pide solo lo que ha cambiado, no el historico entero. No bloquea a
                // nadie: la aplicacion pinta de la base local y esto la va rellenando.
                LaunchedEffect(Unit) { container.syncOnStart() }
                start?.let { GastonomoApp(container = container, startDestination = it) }
            }
        }
    }

    /**
     * La referencia se borra solo si sigue siendo esta actividad. Si llegan a existir dos
     * instancias y la de arriba se destruye despues de que la de abajo se registre, borrarla a
     * ciegas dejaria el cerrojo sin actividad: el boton de la huella no haria nada y no habria
     * ningun error que mirar.
     */
    override fun onDestroy() {
        val container = (application as GastonomoApplication).container
        if (container.currentActivity === this) container.currentActivity = null
        super.onDestroy()
    }
}
