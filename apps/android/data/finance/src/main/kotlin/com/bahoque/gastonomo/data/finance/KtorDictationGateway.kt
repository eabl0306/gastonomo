package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.DictationGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.flatMap
import com.bahoque.gastonomo.data.network.ApiClient
import java.time.LocalDate

/**
 * El dictado contra la API. No guarda nada en local **a proposito**: un borrador no es un
 * registro y no tiene por que sobrevivir a cerrar la hoja.
 */
internal class KtorDictationGateway(private val client: ApiClient) : DictationGateway {

    override suspend fun parse(
        text: String,
        today: LocalDate,
    ): Outcome<DictationDraft, ApiError> = client
        .post<DictationDraftDto>(
            "dictation/parse",
            ParseDictationRequest(text = text, today = today.toString()),
        )
        .flatMap { dto ->
            dto.toDomain()?.let { Outcome.Success(it) } ?: Outcome.Failure(ApiError.Unexpected())
        }
}

/** La fabrica que llama el contenedor de la aplicacion. */
fun createDictationGateway(client: ApiClient): DictationGateway = KtorDictationGateway(client)
