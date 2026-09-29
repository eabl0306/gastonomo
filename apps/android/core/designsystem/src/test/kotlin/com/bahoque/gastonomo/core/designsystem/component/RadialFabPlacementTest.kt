package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

    /** El mismo reparto que `GastonomoApp`: contenido con peso y la tab bar debajo. */
    private fun renderShell(name: String) {
        compose.captureGastonomoScreen(name) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(GastonomoTheme.colors.bgApp),
                ) {
                    GastonomoRadialFab(
                        options = options(),
                        closeLabel = "Cerrar",
                        openLabel = "Nuevo registro",
                    )
                }
                GastonomoTabBar(
                    items = listOf(
                        TabItem(GastonomoIcons.SWAP_VERT, "Inicio"),
                        TabItem(GastonomoIcons.HANDSHAKE, "Préstamos"),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                    modifier = Modifier.testTag(TAB_BAR),
                )
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
        // La barra entera, no uno de sus iconos: coloca los iconos con relleno propio.
        val tabBar = compose.onNodeWithTag(TAB_BAR).getUnclippedBoundsInRoot()

        val toSide = SCREEN_WIDTH.dp - fab.right
        val toTabBar = tabBar.top - fab.bottom

        assertTrue(
            abs((toTabBar - toSide).value) < TOLERANCE,
            "el hueco hasta la tab bar es $toTabBar y hasta el lateral $toSide",
        )
    }
}

private const val TAB_BAR = "tab-bar"

/** Medio dp de margen, para el redondeo a pixel de la densidad 3x. */
private const val TOLERANCE = 0.5f
