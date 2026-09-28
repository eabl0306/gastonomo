package com.bahoque.gastonomo.feature.export

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * La pantalla entera, no solo lo que pinta: tocar "Exportar a Excel" tiene que acabar en la hoja
 * de compartir del sistema.
 *
 * Cerraba la aplicacion. La hoja se abria desde `LocalContext`, y en esta aplicacion
 * `LocalContext` no es la actividad: `GastonomoTheme` lo sustituye por un contexto de
 * configuracion para que los textos salgan en el idioma de Ajustes. Los tests que ya habia
 * pintaban `ExportScreen` con un estado a mano, asi que nunca llegaban a este paso.
 *
 * El grafo se renderiza dentro de `GastonomoTheme` a proposito: sin el, `LocalContext` si seria
 * la actividad y el test no probaria nada.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class ExportRouteTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `exporting opens the share sheet instead of closing the application`() {
        compose.renderNavGraph(GastonomoRoute.Export) { actions ->
            exportNavGraph(FakeExportDependencies.working(Dispatchers.Unconfined), actions)
        }

        compose.onNodeWithText("Exportar a Excel").performClick()
        compose.waitForIdle()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val shared = shadowOf(application).nextStartedActivity
        assertNotNull(shared, "no se abrio ninguna hoja de compartir")
        assertEquals(Intent.ACTION_CHOOSER, shared.action)
    }
}
