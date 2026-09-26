package com.bahoque.gastonomo.core.domain.prototype

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Los datos semilla del prototipo, copiados de `MOVS` y `LOANS` del bloque de script de
 * `Gastonomo.dc.html`. Existen para poder comprobar que el dominio calcula exactamente lo
 * mismo que el prototipo pinta, que es el criterio de aceptacion del rediseno.
 *
 * No se tocan: si cambian, dejan de comparar nada.
 */
internal object PrototypeSeed {

    val COP: CurrencyCode = CurrencyCode("COP")
    val USD: CurrencyCode = CurrencyCode("USD")

    /** Las 31 filas de `MOVS`, en el orden del prototipo. */
    val movements: List<Movement> = buildList {
        add(out("2026-06-28", "Prestamo", "Prestamo - Rosa", "600000"))
        add(out("2026-06-28", "Prestamo", "Prestamo - Nevera", "1000000"))
        add(income("2026-06-28", "Abono prestamo", "Abono - Nevera", "100000"))
        add(income("2026-06-28", "Abono prestamo", "Abono - Mama", "100000"))
        add(income("2026-07-01", "Salario", "Salario", "2800000"))
        add(out("2026-07-01", "Vivienda", "Arriendo", "1200000"))
        add(out("2026-07-02", "Prestamo", "Prestamo - Collar Johana", "150000"))
        add(income("2026-07-02", "Abono prestamo", "Abono - Nevera", "70000"))
        add(out("2026-07-14", "Alimentacion", "Mercado", "198000"))
        add(out("2026-07-20", "Servicios", "Internet y luz", "139000"))
        add(income("2026-07-29", "Abono prestamo", "Abono - Rosa", "600000"))
        add(income("2026-07-29", "Abono prestamo", "Abono - Hermana Evila", "200000"))
        add(income("2026-08-01", "Salario", "Salario", "2800000"))
        add(out("2026-08-01", "Vivienda", "Arriendo", "1200000"))
        add(income("2026-08-01", "Abono prestamo", "Abono - Nevera", "200000"))
        add(income("2026-08-06", "Abono prestamo", "Abono - Rosa (detalles)", "50000"))
        add(income("2026-08-06", "Abono prestamo", "Abono - Rosa (intereses)", "50000"))
        add(out("2026-08-11", "Alimentacion", "Mercado", "210500"))
        add(out("2026-08-18", "Salud", "Consulta medica", "95000"))
        add(out("2026-08-21", "Servicios", "Internet y luz", "141000"))
        add(out("2026-08-25", "Transporte", "Gasolina", "80000"))
        addAll(september())
    }

    /** Las 10 filas de septiembre de 2026, el periodo que ensena el prototipo al abrir. */
    fun september(): List<Movement> = listOf(
        income("2026-09-01", "Salario", "Salario", "2800000"),
        out("2026-09-01", "Vivienda", "Arriendo", "1200000"),
        out("2026-09-03", "Entretenimiento", "Cine", "38000"),
        // La unica fila en otra moneda: 140 USD con equivalencia guardada de 450.000 COP.
        Movement(
            id = MovementId("m-usd"),
            occurredOn = LocalDate.parse("2026-09-05"),
            kind = MovementKind.IN,
            title = "Diseno web - cliente EE. UU.",
            money = MonetaryAmount(BigDecimal("140"), USD, BigDecimal("450000"), COP),
            category = category("Freelance", CategoryKind.IN),
        ),
        out("2026-09-10", "Servicios", "Internet y luz", "142000"),
        out("2026-09-12", "Transporte", "Gasolina", "60000"),
        out("2026-09-15", "Alimentacion", "Mercado", "186400"),
        income("2026-09-19", "Abono prestamo", "Abono - Collar Johana", "100000"),
        out("2026-09-20", "Alimentacion", "Almuerzo", "32000"),
        out("2026-09-08", "Mascotas", "Concentrado Luna", "74000"),
    )

    /** Las cinco filas de `LOANS`, todas prestadas. */
    val loans: List<Loan> = listOf(
        loan(
            id = "1",
            person = "Collar Johana",
            note = "",
            amount = "150000",
            openedOn = "2026-07-02",
            payments = listOf("2026-09-19" to "100000"),
        ),
        loan(
            id = "2",
            person = "Nevera",
            note = "",
            amount = "1000000",
            openedOn = "2026-06-28",
            payments = listOf(
                "2026-06-28" to "100000",
                "2026-07-02" to "70000",
                "2026-08-01" to "200000",
            ),
        ),
        loan(
            id = "3",
            person = "Hermana Evila",
            note = "Telefono",
            amount = "600000",
            openedOn = "2026-06-20",
            payments = listOf("2026-07-29" to "200000"),
        ),
        loan(
            id = "4",
            person = "Mama",
            note = "",
            amount = "210000",
            openedOn = "2026-06-10",
            payments = listOf("2026-06-28" to "100000"),
        ),
        loan(
            id = "5",
            person = "Rosa",
            note = "",
            amount = "600000",
            openedOn = "2026-06-28",
            payments = listOf(
                "2026-07-29" to "600000",
                "2026-08-06" to "50000",
                "2026-08-06" to "50000",
            ),
        ),
    )

    private var nextId = 100

    private fun income(date: String, category: String, title: String, amount: String) =
        movement(date, MovementKind.IN, category, CategoryKind.IN, title, amount)

    private fun out(date: String, category: String, title: String, amount: String) =
        movement(date, MovementKind.OUT, category, CategoryKind.OUT, title, amount)

    @Suppress("LongParameterList") // Es una fila del prototipo: fecha, tipo, categoria, titulo e importe.
    private fun movement(
        date: String,
        kind: MovementKind,
        categoryName: String,
        categoryKind: CategoryKind,
        title: String,
        amount: String,
    ) = Movement(
        id = MovementId("m-${nextId++}"),
        occurredOn = LocalDate.parse(date),
        kind = kind,
        title = title,
        money = MonetaryAmount.single(BigDecimal(amount), COP),
        category = category(categoryName, categoryKind),
    )

    private fun category(name: String, kind: CategoryKind) = Category(
        id = CategoryId("c-$name"),
        name = name,
        kind = kind,
        icon = "shopping_cart",
        color = "#cf5f35",
        isDefault = true,
    )

    @Suppress("LongParameterList") // Es una fila de LOANS del prototipo.
    private fun loan(
        id: String,
        person: String,
        note: String,
        amount: String,
        openedOn: String,
        payments: List<Pair<String, String>>,
    ) = Loan(
        id = LoanId(id),
        person = person,
        note = note,
        direction = LoanDirection.LENT,
        openedOn = LocalDate.parse(openedOn),
        money = MonetaryAmount.single(BigDecimal(amount), COP),
        payments = payments.mapIndexed { index, (date, value) ->
            LoanPayment(
                id = LoanPaymentId("$id-$index"),
                loanId = LoanId(id),
                paidOn = LocalDate.parse(date),
                label = null,
                money = MonetaryAmount.single(BigDecimal(value), COP),
            )
        },
    )
}
