package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.math.BigDecimal
import java.time.LocalDate

/** La categoria que la API eligio para un movimiento dictado. Siempre una que existe. */
data class DictatedCategory(val id: CategoryId, val name: String)

/** Lo propio de cada uno de los dos registros que se pueden dictar. */
sealed interface DictatedEntry {

    /** Un movimiento, con su tipo y su categoria ya resueltos. */
    data class Movement(
        val kind: MovementKind,
        val category: DictatedCategory,
        val note: String,
    ) : DictatedEntry

    /** Un prestamo. [person] viene vacia si no se entendio, y entonces sale en `missing`. */
    data class Loan(
        val direction: LoanDirection,
        val person: String,
        val note: String,
    ) : DictatedEntry
}

/** Lo que el usuario tiene que completar a mano para poder guardar. */
enum class MissingField {
    /** No se entendio ningun importe. */
    AMOUNT,

    /** Un prestamo sin persona. */
    PERSON,
    ;

    companion object {
        /** Lee el valor de la API. Un nombre que no conocemos se ignora. */
        fun fromWire(wire: String): MissingField? = when (wire) {
            "amount" -> AMOUNT
            "person" -> PERSON
            else -> null
        }
    }
}

/**
 * El borrador que devuelve `POST /v1/dictation/parse`.
 *
 * **No se ha creado nada todavia.** Guardar es otra llamada, a `POST /v1/movements` o a
 * `POST /v1/loans`, con su clave de idempotencia: el historico es de solo anadir y no puede
 * depender de que un modelo acierte a la primera.
 *
 * Quien decide que falta es la API, no el cliente ([missing]): asi el movil y la web no
 * pueden discrepar sobre cuando se puede guardar.
 *
 * @property amount null cuando no se entendio ningun importe.
 */
data class DictationDraft(
    val entry: DictatedEntry,
    val amount: BigDecimal?,
    val currency: CurrencyCode,
    val occurredOn: LocalDate,
    val missing: Set<MissingField>,
)

/** El dictado contra la API. */
interface DictationGateway {

    /**
     * `POST /v1/dictation/parse`.
     *
     * [today] es el dia del dispositivo y va en el cuerpo porque **la zona horaria es del
     * cliente**: el servidor no sabe si para quien dicta ya es manana.
     *
     * Una cuenta sin el dictado aprobado recibe 404, igual que cualquier recurso que no es
     * suyo.
     */
    suspend fun parse(text: String, today: LocalDate): Outcome<DictationDraft, ApiError>
}
