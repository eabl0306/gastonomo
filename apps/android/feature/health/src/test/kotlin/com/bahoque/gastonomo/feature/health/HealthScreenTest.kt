package com.bahoque.gastonomo.feature.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test de Compose que corre en la JVM con Robolectric: la CI no necesita emulador.
 */
@RunWith(RobolectricTestRunner::class)
class HealthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loading state shows the progress indicator`() {
        composeRule.setContent { HealthScreen(state = HealthUiState.Loading, onRefresh = {}) }

        composeRule.onNodeWithTag(HealthScreenTags.ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(HealthScreenTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun `ready state lists every component of the report`() {
        composeRule.setContent {
            HealthScreen(state = HealthUiState.Ready(healthyReport()), onRefresh = {})
        }

        composeRule.onNodeWithTag(HealthScreenTags.SUMMARY).assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithTag(HealthScreenTags.COMPONENT).fetchSemanticsNodes().size)
    }

    @Test
    fun `failed state shows the error message`() {
        composeRule.setContent {
            HealthScreen(state = HealthUiState.Failed(HealthFailureReason.UNREACHABLE), onRefresh = {})
        }

        composeRule.onNodeWithTag(HealthScreenTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun `the refresh button calls back`() {
        var refreshed = false
        composeRule.setContent { HealthScreen(state = HealthUiState.Loading, onRefresh = { refreshed = true }) }

        composeRule.onNodeWithTag(HealthScreenTags.REFRESH).performClick()

        assertTrue(refreshed)
    }
}
