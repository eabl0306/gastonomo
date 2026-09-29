package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.SCREEN_WIDTH
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Donde queda el boton de registro rapido **dentro del armazon de verdad**.
 *
 * Existe porque el resto de tests lo pintan solo, en una caja que ocupa la pantalla entera, y
 * asi no se ve el fallo: en la aplicacion la tab bar es el **hermano siguiente** del contenido
 * (`GastonomoApp`, una `Column` con el NavHost en `weight(1f)`), de modo que la caja del boton
 * termina donde empieza la barra. Un hueco pensado para pasar por encima de la barra se suma al
 * alto de la barra y el boton queda flotando muy arriba.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class RadialFabPlacementTest {

    @get:Rule
    val compose = createComposeRule()

    private fun options() = listOf(
        RadialOption(GastonomoIcons.SWAP_VERT, "Nuevo movimiento") {},
        RadialOption(GastonomoIcons.HANDSHAKE, "Nuevo préstamo") {},
        RadialOption(GastonomoIcons.MIC, "Dictar") {},
    )

    /**
     * El armazon de verdad, el mismo que monta la aplicacion.
     *
     * La region de contenido va etiquetada porque es lo que hay que medir: su borde inferior es
     * exactamente donde empieza la tab bar. Medir la barra por uno de sus iconos daria otra cosa,
     * porque los coloca con relleno propio.
     */
    private fun renderShell(name: String) {
        compose.captureGastonomoScreen(name) {
            GastonomoShell(
                tabs = listOf(
                    TabItem(GastonomoIcons.SWAP_VERT, "Inicio"),
                    TabItem(GastonomoIcons.HANDSHAKE, "Préstamos"),
                ),
                selectedTab = 0,
                onSelectTab = {},
            ) {
                Box(
                    modifier = Modifier
                        .testTag(CONTENT)
                        .fillMaxSize()
                        .background(GastonomoTheme.colors.bgApp),
                ) {
                    GastonomoRadialFab(
                        options = options(),
                        closeLabel = "Cerrar",
                        openLabel = "Nuevo registro",
                    )
                }
            }
        }
    }

    /**
     * El hueco hasta la tab bar es el mismo que hasta el lateral.
     *
     * Es lo que pidio el diseno y lo unico que se puede comprobar sin ojo: que las dos
     * distancias coincidan. Con un numero escrito para otra disposicion, el de abajo salia
     * cuatro veces mas grande.
     */
    @Test
    fun `el boton guarda la misma distancia con la tab bar que con el lateral`() {
        renderShell("radial-fab-armazon")

        val fab = compose.onNodeWithContentDescription("Nuevo registro").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag(CONTENT).getUnclippedBoundsInRoot()

        val toSide = SCREEN_WIDTH.dp - fab.right
        // El borde inferior del contenido es donde empieza la tab bar.
        val toTabBar = content.bottom - fab.bottom

        assertTrue(
            abs((toTabBar - toSide).value) < TOLERANCE,
            "el hueco hasta la tab bar es $toTabBar y hasta el lateral $toSide",
        )
    }
}

private const val CONTENT = "contenido"

/** Medio dp de margen, para el redondeo a pixel de la densidad 3x. */
private const val TOLERANCE = 0.5f
