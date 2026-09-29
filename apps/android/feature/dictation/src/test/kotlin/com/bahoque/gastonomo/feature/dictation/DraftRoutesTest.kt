package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.testing.fake.sampleDictatedLoan
import com.bahoque.gastonomo.core.testing.fake.sampleDictationDraft
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * "Editar" abre el formulario completo con lo que se entendio.
 *
 * Lo que se vigila es que **no se pierda nada por el camino**: si el interprete acerto el importe y
 * fallo la categoria, el importe tiene que seguir ahi. Volver a teclearlo todo seria peor que no
 * haber dictado.
 */
class DraftRoutesTest {

    @Test
    fun `a dictated movement opens its form with everything it understood`() {
        val route = formRouteOf(sampleDictationDraft()) as GastonomoRoute.NewMovement
        assertEquals("out", route.kind)
        assertEquals("45000", route.amount)
        assertEquals("COP", route.currency)
        assertEquals("11111111-1111-4111-8111-111111111111", route.categoryId)
        assertEquals("Almuerzo", route.note)
        assertEquals("2026-09-28", route.occurredOn)
    }

    @Test
    fun `a dictated loan opens its form with the person and the direction`() {
        val route = formRouteOf(sampleDictatedLoan()) as GastonomoRoute.NewLoan
        assertEquals("Rosa", route.person)
        assertEquals("lent", route.direction)
        assertEquals("200000", route.amount)
        assertEquals("2026-09-28", route.openedOn)
    }

    @Test
    fun `without an amount the field opens empty, which is what has to be filled in`() {
        val draft = sampleDictationDraft(amount = null, missing = setOf(MissingField.AMOUNT))
        val route = formRouteOf(draft) as GastonomoRoute.NewMovement
        assertEquals("", route.amount)
        // Lo demas si se conserva: fallar el importe no puede borrar la categoria.
        assertEquals("11111111-1111-4111-8111-111111111111", route.categoryId)
    }

    @Test
    fun `the amount travels with a decimal point and no thousands separator`() {
        val draft = sampleDictationDraft(
            amount = BigDecimal("12.5000"),
            currency = CurrencyCode.USD,
        )
        assertEquals("12.5", (formRouteOf(draft) as GastonomoRoute.NewMovement).amount)
    }

    @Test
    fun `an amount with more decimals than its currency is rounded to it`() {
        // COP no tiene decimales: 45000.4 no puede llegar al campo como 45000.4.
        val draft = sampleDictationDraft(amount = BigDecimal("45000.4"))
        assertEquals("45000", (formRouteOf(draft) as GastonomoRoute.NewMovement).amount)
    }
}
