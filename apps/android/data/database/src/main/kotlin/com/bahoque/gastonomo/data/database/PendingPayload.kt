package com.bahoque.gastonomo.data.database

import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.database.local.PendingDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate

/**
 * El borrador de una escritura pendiente, listo para guardar como texto.
 *
 * Va en JSON y no en columnas porque los tres tipos de escritura tienen campos distintos, y una
 * tabla con la union de todos seria mentira en cada fila: la mitad estarian siempre a null.
 *
 * **Los importes viajan como cadena**, igual que en la API y en las columnas de dinero. Un importe
 * en un numero de JSON seria un double, que es justo lo que la regla 1 prohibe.
 */
@Serializable
internal sealed interface PendingPayload {

    @Serializable
    @SerialName("movement")
    data class Movement(
        val occurredOn: String,
        val kind: String,
        val categoryId: String,
        val money: MoneyPayload,
        val note: String?,
    ) : PendingPayload

    @Serializable
    @SerialName("loan")
    data class Loan(
        val person: String,
        val direction: String,
        val openedOn: String,
        val money: MoneyPayload,
        val note: String?,
    ) : PendingPayload

    @Serializable
    @SerialName("payment")
    data class Payment(
        val paidOn: String,
        val money: MoneyPayload,
        val label: String?,
    ) : PendingPayload
}

/** Los cuatro campos de importe, tal cual, sin convertir nada. */
@Serializable
internal data class MoneyPayload(
    val amount: String,
    val currency: String,
    val base: String,
    val baseCurrency: String,
) {
    fun toDomain(): MonetaryAmount = MonetaryAmount(
        amount = BigDecimal(amount),
        currency = CurrencyCode(currency),
        base = BigDecimal(base),
        baseCurrency = CurrencyCode(baseCurrency),
    )

    companion object {
        fun of(money: MonetaryAmount) = MoneyPayload(
            amount = money.amount.toPlainString(),
            currency = money.currency.value,
            base = money.base.toPlainString(),
            baseCurrency = money.baseCurrency.value,
        )
    }
}

/** `ignoreUnknownKeys` porque una fila guardada por una version anterior tiene que seguir leyendose. */
internal val PendingJson = Json { ignoreUnknownKeys = true }

internal fun PendingDraft.encode(): String = PendingJson.encodeToString(toPayload())

internal fun decodePendingDraft(text: String): PendingDraft =
    PendingJson.decodeFromString<PendingPayload>(text).toDraft()

private fun PendingDraft.toPayload(): PendingPayload = when (this) {
    is PendingDraft.Movement -> PendingPayload.Movement(
        occurredOn = value.occurredOn.toString(),
        kind = value.kind.wire,
        categoryId = value.categoryId.value,
        money = MoneyPayload.of(value.money),
        note = value.note,
    )
    is PendingDraft.Loan -> PendingPayload.Loan(
        person = value.person,
        direction = value.direction.wire,
        openedOn = value.openedOn.toString(),
        money = MoneyPayload.of(value.money),
        note = value.note,
    )
    is PendingDraft.Payment -> PendingPayload.Payment(
        paidOn = value.paidOn.toString(),
        money = MoneyPayload.of(value.money),
        label = value.label,
    )
}

private fun PendingPayload.toDraft(): PendingDraft = when (this) {
    is PendingPayload.Movement -> PendingDraft.Movement(
        MovementDraft(
            occurredOn = LocalDate.parse(occurredOn),
            kind = MovementKind.fromWire(kind) ?: MovementKind.OUT,
            categoryId = CategoryId(categoryId),
            money = money.toDomain(),
            note = note,
        ),
    )
    is PendingPayload.Loan -> PendingDraft.Loan(
        LoanDraft(
            person = person,
            direction = LoanDirection.fromWire(direction) ?: LoanDirection.LENT,
            openedOn = LocalDate.parse(openedOn),
            money = money.toDomain(),
            note = note,
        ),
    )
    is PendingPayload.Payment -> PendingDraft.Payment(
        LoanPaymentDraft(
            paidOn = LocalDate.parse(paidOn),
            money = money.toDomain(),
            label = label,
        ),
    )
}
