package com.bahoque.gastonomo.data.health

/** Cuerpos JSON de ejemplo, copiados del contrato compartido con la API. */
internal object HealthResponses {

    val HEALTHY = """
        {"status":"ok","checks":[{"name":"database","status":"ok","latencyMs":3}],"version":"0.1.0"}
    """.trimIndent()

    val DEGRADED = """
        {"status":"degraded","checks":[{"name":"database","status":"error","latencyMs":812}],"version":"0.1.0"}
    """.trimIndent()

    val UNKNOWN_COMPONENT_STATUS = """
        {"status":"ok","checks":[{"name":"database","status":"weird","latencyMs":3}],"version":"0.1.0"}
    """.trimIndent()

    val BAD_VERSION = """
        {"status":"ok","checks":[{"name":"database","status":"ok","latencyMs":3}],"version":"nightly"}
    """.trimIndent()

    const val NOT_JSON = "<html>502</html>"
}
