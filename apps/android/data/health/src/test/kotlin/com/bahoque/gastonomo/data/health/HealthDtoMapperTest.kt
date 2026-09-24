package com.bahoque.gastonomo.data.health

import com.bahoque.gastonomo.core.domain.health.HealthStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthDtoMapperTest {

    @Test
    fun `a report without checks maps to an empty healthy report`() {
        val report = HealthReportDto(status = "ok", version = "1.0.0").toDomain()

        assertEquals(HealthStatus.OK, report?.status)
        assertEquals(emptyList(), report?.checks)
    }

    @Test
    fun `the server status is ignored and recomputed from the checks`() {
        val dto = HealthReportDto(
            status = "ok",
            version = "1.0.0",
            checks = listOf(ComponentHealthDto("database", "error", 10)),
        )

        assertEquals(HealthStatus.DEGRADED, dto.toDomain()?.status)
    }

    @Test
    fun `a degraded component status is rejected`() {
        assertNull(ComponentHealthDto("database", "degraded", 1).toDomain())
    }

    @Test
    fun `a blank component name is rejected`() {
        assertNull(ComponentHealthDto(" ", "ok", 1).toDomain())
    }

    @Test
    fun `a negative latency is rejected`() {
        assertNull(ComponentHealthDto("database", "ok", -5).toDomain())
    }
}
