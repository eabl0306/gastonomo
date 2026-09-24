package com.bahoque.gastonomo.core.domain.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthReportTest {

    @Test
    fun `report without checks is healthy`() {
        val report = HealthReport(version = VERSION, checks = emptyList())

        assertEquals(HealthStatus.OK, report.status)
        assertTrue(report.isHealthy)
        assertTrue(report.failingChecks.isEmpty())
    }

    @Test
    fun `report with every check ok is healthy`() {
        val checks = listOf(check("database", HealthStatus.OK), check("cache", HealthStatus.OK))

        val report = HealthReport(VERSION, checks)

        assertEquals(HealthStatus.OK, report.status)
        assertTrue(report.isHealthy)
    }

    @Test
    fun `a single failing check degrades the whole report`() {
        val checks = listOf(check("database", HealthStatus.ERROR), check("cache", HealthStatus.OK))

        val report = HealthReport(VERSION, checks)

        assertEquals(HealthStatus.DEGRADED, report.status)
        assertFalse(report.isHealthy)
        assertEquals(listOf("database"), report.failingChecks.map { it.name })
    }

    @Test
    fun `components are looked up by name`() {
        val report = HealthReport(VERSION, listOf(check("database", HealthStatus.OK)))

        assertEquals(HealthStatus.OK, report.componentNamed("database")?.status)
        assertNull(report.componentNamed("queue"))
    }

    @Test
    fun `component rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { check(" ", HealthStatus.OK) }
    }

    @Test
    fun `component rejects a negative latency`() {
        assertFailsWith<IllegalArgumentException> { ComponentHealth("database", HealthStatus.OK, -1) }
    }

    @Test
    fun `component rejects the degraded status`() {
        assertFailsWith<IllegalArgumentException> { check("database", HealthStatus.DEGRADED) }
    }

    private fun check(name: String, status: HealthStatus) = ComponentHealth(name, status, LATENCY_MS)

    private companion object {
        val VERSION = ServiceVersion("1.4.2")
        const val LATENCY_MS = 12L
    }
}
