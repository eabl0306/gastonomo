package com.bahoque.gastonomo.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * El dinero sobrevive al viaje de ida y vuelta, **con su escala**.
 *
 * Es el test que justifica que las columnas de importe sean `TEXT`. Con `REAL` casi todos estos
 * importes volverian pareciendo correctos y alguno volveria mal, y el fallo no saldria aqui sino
 * meses despues en un saldo que no cuadra.
 */
@RunWith(AndroidJUnit4::class)
internal class MoneyRoundTripTest {

    @get:Rule
    val local = LocalDatabaseRule()

    @Test
    fun `amounts a double would ruin come back identical`() = runTest {
        val dao = local.database.movements()
        val stored = AMOUNTS.mapIndexed { index, amount ->
            MovementEntity.of(movementOf(index, amount))
        }

        dao.upsert(stored)

        val read = dao.between(DAY, DAY).associateBy { it.id }
        stored.forEach { expected ->
            val actual = read.getValue(expected.id)
            assertEquals(expected.money.amount, actual.money.amount, "importe ${expected.id}")
            assertEquals(expected.money.base, actual.money.base, "equivalencia ${expected.id}")
        }
    }

    @Test
    fun `the scale survives, so four decimals stay four decimals`() = runTest {
        val dao = local.database.movements()
        val exact = BigDecimal("186400.0000")

        dao.upsert(listOf(MovementEntity.of(movementOf(0, exact))))

        val read = dao.between(DAY, DAY).single().money.amount
        assertEquals(exact, read)
        assertEquals(exact.scale(), read.scale())
        assertEquals("186400.0000", read.toPlainString())
    }

    @Test
    fun `the whole movement comes back, category and all`() = runTest {
        val dao = local.database.movements()
        val movement = movementOf(0, BigDecimal("1.005"))

        dao.upsert(listOf(MovementEntity.of(movement)))

        assertEquals(movement, dao.between(DAY, DAY).single().toDomain())
    }

    private fun movementOf(index: Int, amount: BigDecimal) = Movement(
        id = MovementId("m$index"),
        occurredOn = DAY,
        kind = MovementKind.OUT,
        title = "Mercado",
        money = MonetaryAmount(
            amount = amount,
            currency = CurrencyCode.USD,
            base = amount.multiply(BigDecimal("3199.41")),
            baseCurrency = CurrencyCode.COP,
        ),
        category = CATEGORY,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.parse("2026-09-15")

        val CATEGORY = Category(
            id = CategoryId("food"),
            name = "Alimentación",
            kind = CategoryKind.OUT,
            icon = "restaurant",
            color = "#cf5f35",
            isDefault = true,
            movementCount = 12,
        )

        /**
         * Importes elegidos para que un `double` falle: los dos primeros no tienen representacion
         * binaria exacta, el tercero se pasa de los enteros exactos de un double y el cuarto tiene
         * mas cifras significativas de las que caben.
         */
        val AMOUNTS = listOf(
            BigDecimal("0.1"),
            BigDecimal("1.005"),
            BigDecimal("9007199254740993"),
            BigDecimal("123456789012345678.9012"),
            BigDecimal("186400.0000"),
        )
    }
}
