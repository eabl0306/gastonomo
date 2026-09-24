package com.bahoque.gastonomo.data.health

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.HealthReport
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Adaptador driven del puerto HealthGateway sobre HTTP.
 *
 * Se eligio Ktor client frente a Retrofit porque su MockEngine permite probar el adaptador
 * entero sin levantar un servidor ni abrir sockets, la API es suspend de serie y no arrastra
 * generacion de codigo. El parseo es explicito con kotlinx-serialization, sin ContentNegotiation,
 * para poder distinguir un cuerpo malformado de un fallo de red.
 */
internal class KtorHealthGateway(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : HealthGateway {

    override suspend fun fetchHealth(): Outcome<HealthReport, HealthCheckError> =
        try {
            readReport(httpClient.get(baseUrl + READY_PATH))
        } catch (cause: IOException) {
            Outcome.Failure(HealthCheckError.Unreachable(cause.message ?: cause.toString()))
        } catch (cause: SerializationException) {
            Outcome.Failure(HealthCheckError.MalformedResponse(cause.message ?: cause.toString()))
        }

    private suspend fun readReport(response: HttpResponse): Outcome<HealthReport, HealthCheckError> {
        val code = response.status.value
        if (code !in REPORTING_STATUSES) return Outcome.Failure(HealthCheckError.UnexpectedStatus(code))
        val report = JSON.decodeFromString(HealthReportDto.serializer(), response.bodyAsText()).toDomain()
        return report?.let { Outcome.Success(it) } ?: Outcome.Failure(MALFORMED_PAYLOAD)
    }

    private companion object {
        const val READY_PATH = "/health/ready"

        /** La API contesta 200 cuando esta sana y 503 cuando esta degradada: ambas traen informe. */
        val REPORTING_STATUSES = setOf(200, 503)

        val MALFORMED_PAYLOAD = HealthCheckError.MalformedResponse("payload does not honour the /health contract")

        val JSON = Json { ignoreUnknownKeys = true }
    }
}
