package com.bahoque.gastonomo.core.application.health.usecase

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.flatMap
import com.bahoque.gastonomo.core.domain.health.HealthReport

/**
 * Caso de uso "comprobar salud del servicio". Pide el informe al puerto y exige
 * que traiga el chequeo de base de datos que promete el contrato compartido.
 */
class CheckApiHealth(private val gateway: HealthGateway) {

    suspend operator fun invoke(): Outcome<HealthReport, HealthCheckError> =
        gateway.fetchHealth().flatMap(::requireContractedChecks)

    private fun requireContractedChecks(report: HealthReport): Outcome<HealthReport, HealthCheckError> =
        if (report.componentNamed(DATABASE_COMPONENT) == null) {
            Outcome.Failure(HealthCheckError.MalformedResponse("missing '$DATABASE_COMPONENT' check"))
        } else {
            Outcome.Success(report)
        }

    private companion object {
        const val DATABASE_COMPONENT = "database"
    }
}
