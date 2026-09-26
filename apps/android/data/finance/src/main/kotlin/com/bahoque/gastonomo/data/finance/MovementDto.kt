package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.network.BigDecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/** Una categoria embebida en un movimiento. Viaja completa para pintar la fila de una vez. */
@Serializable
internal data class EmbeddedCategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("kind") val kind: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
) {
    fun toDomain(): Category = Category(
        id = CategoryId(id),
        name = name,
        kind = CategoryKind.fromWire(kind) ?: CategoryKind.BOTH,
        icon = icon,
        color = color,
        isDefault = false,
    )
}

@Serializable
internal data class MovementDto(
    @SerialName("id") val id: String,
    @SerialName("occurredOn") val occurredOn: String,
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
    @SerialName("category") val category: EmbeddedCategoryDto,
    @SerialName("loanId") val loanId: String? = null,
    @SerialName("loanPaymentId") val loanPaymentId: String? = null,
) {
    fun toDomain(): Movement = Movement(
        id = MovementId(id),
        occurredOn = LocalDate.parse(occurredOn),
        kind = MovementKind.fromWire(kind) ?: MovementKind.OUT,
        title = title,
        money = MoneyFields(amount, currency, base, baseCurrency).toDomain(),
        category = category.toDomain(),
        loanId = loanId,
        loanPaymentId = loanPaymentId,
    )
}

@Serializable
internal data class MovementPageDto(
    @SerialName("items") val items: List<MovementDto> = emptyList(),
    @SerialName("nextCursor") val nextCursor: String? = null,
)

@Serializable
internal data class CreateMovementDto(
    @SerialName("occurredOn") val occurredOn: String,
    @SerialName("kind") val kind: String,
    @SerialName("categoryId") val categoryId: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
    @SerialName("note") val note: String? = null,
)
