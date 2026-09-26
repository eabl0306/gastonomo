package com.bahoque.gastonomo.core.designsystem.gallery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.component.ComponentGallery
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * La captura de ejemplo. Renderiza el catalogo de componentes a PNG desde la JVM, sin
 * emulador, y comprueba que el fichero existe: si Roborazzi deja de escribir, este test
 * falla en vez de pasar en silencio.
 *
 * Es el mismo camino que van a usar las capturas de las 14 pantallas.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS)
class ComponentGalleryScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the component gallery renders to a png`() {
        compose.captureGastonomoScreen(SCREENSHOT_NAME) { ComponentGallery() }
        val file = screenshotFile(SCREENSHOT_NAME)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        assertTrue(file.length() > 0, "Roborazzi wrote an empty file")
    }

    private companion object {
        const val SCREENSHOT_NAME = "design-system-gallery"
    }
}
