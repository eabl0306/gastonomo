package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.application.port.ChangeGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.data.catalog.CategoryDto
import com.bahoque.gastonomo.data.catalog.SettingsDto
import com.bahoque.gastonomo.data.network.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * El feed de cambios contra la API.
 *
 * Vive en este modulo y no en uno propio porque las cinco colecciones del lote son los DTO que ya
 * existen: un movimiento del feed es el mismo movimiento que devuelve `GET /v1/movements`. Un
 * modulo aparte obligaria a declararlos otra vez, y entonces habria dos mapeos del mismo tipo.
 */
internal class KtorChangeGateway(private val client: ApiClient) : ChangeGateway {

    override suspend fun changes(since: String?, limit: Int?): Outcome<ChangeBatch, ApiError> =
        client.get<ChangeBatchDto>("changes${query(since, limit)}").map { it.toDomain() }

    /**
     * El cursor va tal cual, sin tocarlo.
     *
     * Es opaco a proposito: interpretarlo o fabricarlo es la forma de saltarse cambios sin
     * enterarse. Va codificado en la URL porque puede llevar caracteres de base64url.
     */
    private fun query(since: String?, limit: Int?): String {
        val parts = buildList {
            since?.let { add("since=" + it.encodeForQuery()) }
            limit?.let { add("limit=$it") }
        }
        return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
    }

    private fun String.encodeForQuery(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
}

@Serializable
internal data class ChangeBatchDto(
    @SerialName("cursor") val cursor: String,
    @SerialName("hasMore") val hasMore: Boolean = false,
    @SerialName("movements") val movements: List<MovementDto> = emptyList(),
    @SerialName("loans") val loans: List<LoanDto> = emptyList(),
    @SerialName("loanPayments") val loanPayments: List<LoanPaymentDto> = emptyList(),
    @SerialName("categories") val categories: List<CategoryDto> = emptyList(),
    @SerialName("settings") val settings: SettingsDto? = null,
) {
    fun toDomain(): ChangeBatch = ChangeBatch(
        cursor = cursor,
        hasMore = hasMore,
        movements = movements.map { it.toDomain() },
        loans = loans.map { it.toDomain() },
        loanPayments = loanPayments.map { it.toDomain() },
        categories = categories.map { it.toDomain() },
        settings = settings?.toDomain(),
    )
}

/** La fabrica que llama la raiz de composicion. */
fun createChangeGateway(client: ApiClient): ChangeGateway = KtorChangeGateway(client)
