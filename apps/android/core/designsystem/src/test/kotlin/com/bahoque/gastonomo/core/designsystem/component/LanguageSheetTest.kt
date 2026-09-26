package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** El sheet de idioma: dos tarjetas-radio con el nombre de cada idioma en su propia lengua. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class LanguageSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it offers spanish and english with their region`() {
        render()
        compose.onNodeWithText("Idioma").assertIsDisplayed()
        compose.onNodeWithText("Español").assertIsDisplayed()
        compose.onNodeWithText("Colombia · es-CO").assertIsDisplayed()
        compose.onNodeWithText("English").assertIsDisplayed()
        compose.onNodeWithText("United States · en-US").assertIsDisplayed()
    }

    @Test
    fun `picking one reports it`() {
        val picked = mutableListOf<AppLanguage>()
        render(onSelect = { picked += it })
        compose.onNodeWithText("English").performClick()
        assertEquals(listOf(AppLanguage.ENGLISH), picked)
    }

    private fun render(onSelect: (AppLanguage) -> Unit = {}) {
        compose.setContent {
            GastonomoTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LanguageSheet(
                        selected = AppLanguage.SPANISH,
                        onSelect = onSelect,
                        onDismiss = {},
                    )
                }
            }
        }
    }
}
