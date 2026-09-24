package com.bahoque.gastonomo.core.domain.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthStatusTest {

    @Test
    fun `wire values follow the shared contract`() {
        assertEquals("ok", HealthStatus.OK.wireValue)
        assertEquals("degraded", HealthStatus.DEGRADED.wireValue)
        assertEquals("error", HealthStatus.ERROR.wireValue)
    }

    @Test
    fun `only ok is healthy`() {
        assertTrue(HealthStatus.OK.isHealthy)
        assertFalse(HealthStatus.DEGRADED.isHealthy)
        assertFalse(HealthStatus.ERROR.isHealthy)
    }

    @Test
    fun `parsing is case insensitive and trims`() {
        assertEquals(HealthStatus.OK, HealthStatus.fromWire(" OK "))
        assertEquals(HealthStatus.ERROR, HealthStatus.fromWire("Error"))
    }

    @Test
    fun `unknown wire values are rejected`() {
        assertNull(HealthStatus.fromWire("broken"))
        assertNull(HealthStatus.fromWire(""))
    }
}
