package com.bahoque.gastonomo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                start?.let { GastonomoApp(container = container, startDestination = it) }
            }
        }
    }

    override fun onDestroy() {
        (application as GastonomoApplication).container.currentActivity = null
        super.onDestroy()
    }
}
