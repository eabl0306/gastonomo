package com.bahoque.gastonomo.core.designsystem.gallery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.component.OverlayGallery
import com.bahoque.gastonomo.core.designsystem.component.OverlaySheetGallery
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/** La segunda captura: app bar, tab bar, FAB, bottom sheet y toast. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS)
class OverlayGalleryScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the chrome of the application renders to a png`() {
        compose.captureGastonomoScreen(
            name = "design-system-chrome",
            language = AppLanguage.ENGLISH,
            displayCurrency = CurrencyCode.USD,
        ) { OverlayGallery() }
        assertTrue(screenshotFile("design-system-chrome").exists())
    }

    @Test
    fun `the sheet and the toast render to a png`() {
        compose.captureGastonomoScreen("design-system-overlays") { OverlaySheetGallery() }
        assertTrue(screenshotFile("design-system-overlays").exists())
    }
}
