package com.bahoque.gastonomo.feature.export

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** Lo que Exportar ensena, renderizado en la JVM. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class ExportScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows the two formats, the three sets and the two pills`() {
        setContent(loadedExportState())
        compose.onNodeWithText("Excel").assertIsDisplayed()
        compose.onNodeWithText("CSV").assertIsDisplayed()
        compose.onNodeWithText("Movimientos").assertIsDisplayed()
        compose.onNodeWithText("Préstamos").assertIsDisplayed()
        compose.onNodeWithText("Abonos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Todo el historial").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `every set says how many rows it exports`() {
        setContent(loadedExportState())
        // Los movimientos de ejemplo son 4, y los prestamos y los abonos 3 cada uno.
        compose.onNodeWithText("4 filas").assertIsDisplayed()
        compose.onAllNodesWithText("3 filas").assertCountEquals(2)
    }

    @Test
    fun `the summary box names the workbook`() {
        setContent(loadedExportState())
        compose.onNodeWithText("gastonomo_2026-09-22.xlsx").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `with csv and one set the summary names that file`() {
        setContent(
            loadedExportState().copy(
                format = ExportFormat.CSV,
                sets = setOf(ExportSet.MOVEMENTS),
            ),
        )
        compose.onNodeWithText("gastonomo_movs_2026-09-22.csv")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Exportar CSV").assertIsDisplayed()
    }

    @Test
    fun `with nothing ticked the summary asks for a set`() {
        setContent(loadedExportState().copy(sets = emptySet()))
        compose.onNodeWithText("Selecciona al menos un conjunto")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `the current period pill carries the month`() {
        setContent(loadedExportState())
        compose.onNodeWithText("Periodo actual · Septiembre 2026")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `ticking a set reports it`() {
        val toggled = mutableListOf<ExportSet>()
        setContent(
            loadedExportState(),
            ExportActions({}, {}, { toggled += it }, {}, {}, {}),
        )
        compose.onNodeWithText("Préstamos").performClick()
        assertEquals(listOf(ExportSet.LOANS), toggled)
    }

    @Test
    fun `a failed load offers to try again`() {
        var retried = 0
        setContent(
            loadedExportState().copy(loadFailed = true),
            ExportActions({}, {}, {}, {}, {}, { retried++ }),
        )
        compose.onNodeWithText("No se pudieron cargar tus datos.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertEquals(1, retried)
    }

    private fun setContent(
        state: ExportUiState,
        actions: ExportActions = ExportActions.NONE,
    ) {
        compose.setContent {
            GastonomoTheme { ExportScreen(state = state, actions = actions) }
        }
    }
}
