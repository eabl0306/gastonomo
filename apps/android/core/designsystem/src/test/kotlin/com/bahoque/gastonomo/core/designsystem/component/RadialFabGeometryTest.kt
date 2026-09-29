package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lo delicado del menu radial no es como se ve: es que el dedo elija lo que el usuario cree que
 * esta eligiendo. Por eso la aritmetica vive aparte y se prueba sola.
 */
class RadialFabGeometryTest {

    private val options = RadialFabGeometry.CAPACITY

    private fun selected(x: Float, y: Float): Int? =
        RadialFabGeometry.selected(Offset(x, y), options)

    @Test
    fun `apuntar a cada una de las tres elige la suya`() {
        assertEquals(0, selected(0f, -100f), "arriba")
        assertEquals(1, selected(-70f, -70f), "la diagonal")
        assertEquals(2, selected(-100f, 0f), "la izquierda")
    }

    /**
     * Cerca del boton el dedo todavia no ha dicho hacia donde va. Resaltar la mas cercana haria
     * que la eleccion saltara de una a otra sin apenas moverse.
     */
    @Test
    fun `cerca del centro no hay nada elegido`() {
        assertNull(selected(0f, -10f))
        assertNull(selected(0f, -46f), "justo en el umbral tampoco")
        assertEquals(0, selected(0f, -47f), "un pixel mas alla si")
    }

    /**
     * Entre dos vecinas **no hay hueco**: estan a 45 grados y cada una acepta 37, asi que sus
     * sectores se solapan y gana la mas cercana. Es lo que se quiere, porque el dedo siempre
     * esta apuntando a algo aunque no apunte fino.
     */
    @Test
    fun `entre dos vecinas gana la mas cercana, sin huecos`() {
        // Justo a mitad de camino entre la de arriba y la diagonal, y luego un poco a cada lado.
        assertEquals(0, selected(-37f, -93f), "un pelo hacia arriba")
        assertEquals(1, selected(-41f, -91f), "un pelo hacia la diagonal")
    }

    /**
     * El hueco esta en los **bordes** del arco, pasadas la primera y la ultima. Soltar ahi
     * cierra sin hacer nada, que es preferible a ejecutar la que el dedo rozo por poco.
     */
    @Test
    fun `pasados los bordes del arco no se elige nada`() {
        assertNull(selected(62f, -78f), "por encima y a la derecha de la primera")
        assertNull(selected(-78f, 62f), "por debajo de la ultima")
    }

    /** Hacia donde no hay nada, no hay nada. */
    @Test
    fun `apuntar fuera del arco no elige`() {
        assertNull(selected(100f, 0f), "a la derecha")
        assertNull(selected(0f, 100f), "hacia abajo")
        assertNull(selected(80f, 80f), "abajo a la derecha")
    }

    @Test
    fun `con menos opciones no se elige una que no existe`() {
        assertEquals(0, RadialFabGeometry.selected(Offset(0f, -100f), count = 1))
        assertNull(RadialFabGeometry.selected(Offset(-100f, 0f), count = 1))
        assertNull(RadialFabGeometry.selected(Offset(-100f, 0f), count = 2))
    }

    /**
     * El umbral de arrastre es lo que separa "he tocado" de "he deslizado", y decide si soltar
     * deja el menu abierto o lo cierra.
     */
    @Test
    fun `el arrastre empieza a los catorce pixeles`() {
        assertFalse(RadialFabGeometry.dragged(Offset(0f, 0f)))
        assertFalse(RadialFabGeometry.dragged(Offset(10f, 9f)), "13,45 todavia es un toque")
        assertTrue(RadialFabGeometry.dragged(Offset(0f, 15f)))
    }
}
