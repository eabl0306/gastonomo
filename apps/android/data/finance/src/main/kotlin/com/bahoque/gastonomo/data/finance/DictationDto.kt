package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.port.DictatedCategory
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.network.BigDecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/** Cuerpo de `POST /v1/dictation/parse`. */
@Serializable
internal data class ParseDictationRequest(
    @SerialName("text") val text: String,
    @SerialName("today") val today: String,
)

@Serializable
internal data class DraftCategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
)

@Serializable
internal data class DictatedMovementDto(
    @SerialName("kind") val kind: String,
    @SerialName("category") val category: DraftCategoryDto,
    @SerialName("note") val note: String = "",
)

@Serializable
internal data class DictatedLoanDto(
    @SerialName("direction") val direction: String,
    @SerialName("person") val person: String = "",
    @SerialName("note") val note: String = "",
)

/**
 * El borrador que devuelve la API. El importe viaja como cadena, igual que en todo lo demas,
 * y viene a null cuando no se entendio.
 */
@Serializable
internal data class DictationDraftDto(
    @SerialName("kind") val kind: String,
    @SerialName("movement") val movement: DictatedMovementDto? = null,
    @SerialName("loan") val loan: DictatedLoanDto? = null,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal? = null,
    @SerialName("currency") val currency: String,
    @SerialName("occurredOn") val occurredOn: String,
    @SerialName("missing") val missing: List<String> = emptyList(),
) {
    /**
     * Pasa el cuerpo al borrador del nucleo.
     *
     * Devuelve null si el `kind` no cuadra con el objeto que viene relleno. No deberia pasar,
     * pero el cuerpo lo compone un modelo: ante algo que no se entiende es mejor decir que el
     * dictado no salio que pintar media tarjeta.
     */
    fun toDomain(): DictationDraft? {
        val entry = entryOf() ?: return null
        val currencyCode = currency.takeIf { it.length == CURRENCY_LENGTH } ?: return null
        val date = runCatching { LocalDate.parse(occurredOn) }.getOrNull() ?: return null
        return DictationDraft(
            entry = entry,
            amount = amount,
            currency = CurrencyCode(currencyCode),
            occurredOn = date,
            missing = missing.mapNotNull(MissingField::fromWire).toSet(),
        )
    }

    private fun entryOf(): DictatedEntry? = when (kind) {
        KIND_MOVEMENT -> movement?.let { dto ->
            MovementKind.fromWire(dto.kind)?.let { movementKind ->
                DictatedEntry.Movement(
                    kind = movementKind,
                    category = DictatedCategory(CategoryId(dto.category.id), dto.category.name),
                    note = dto.note,
                )
            }
        }
        KIND_LOAN -> loan?.let { dto ->
            LoanDirection.fromWire(dto.direction)?.let { direction ->
                DictatedEntry.Loan(direction = direction, person = dto.person, note = dto.note)
            }
        }
        else -> null
    }
}

/**
 * Los tres valores fijos del cuerpo.
 *
 * Van fuera de la clase y no en su `companion object` porque **una clase `@Serializable` no puede
 * tener el suyo privado**: kotlinx genera un `Companion` con el acceso al serializador, y
 * declararlo privado lo deja inaccesible. Falla al deserializar, no al compilar.
 */
private const val KIND_MOVEMENT = "movement"
private const val KIND_LOAN = "loan"
private const val CURRENCY_LENGTH = 3
