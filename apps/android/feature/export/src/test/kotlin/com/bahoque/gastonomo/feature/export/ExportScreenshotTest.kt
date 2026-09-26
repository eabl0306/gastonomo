package com.bahoque.gastonomo.feature.export

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/** La captura de Exportar datos. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class ExportScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the export screen renders to a png`() {
        compose.captureGastonomoScreen(NAME) {
            ExportScreen(state = loadedExportState(), actions = ExportActions.NONE)
        }
        assertTrue(screenshotFile(NAME).exists())
    }

    private companion object {
        const val NAME = "export"
    }
}
