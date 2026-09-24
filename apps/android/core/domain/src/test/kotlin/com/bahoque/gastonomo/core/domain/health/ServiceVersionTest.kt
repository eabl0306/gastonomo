package com.bahoque.gastonomo.core.domain.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ServiceVersionTest {

    @Test
    fun `accepts plain semantic versions`() {
        assertEquals("1.0.0", ServiceVersion("1.0.0").value)
        assertEquals("0.1.0", ServiceVersion("0.1.0").toString())
    }

    @Test
    fun `accepts pre release and build metadata`() {
        assertEquals("1.2.3-rc.1", ServiceVersion("1.2.3-rc.1").value)
        assertEquals("1.2.3+build.5", ServiceVersion("1.2.3+build.5").value)
    }

    @Test
    fun `rejects anything that is not semver`() {
        assertFailsWith<IllegalArgumentException> { ServiceVersion("1.0") }
        assertFailsWith<IllegalArgumentException> { ServiceVersion("v1.0.0") }
        assertFailsWith<IllegalArgumentException> { ServiceVersion("") }
    }

    @Test
    fun `parseOrNull does not throw`() {
        assertEquals(ServiceVersion("2.0.0"), ServiceVersion.parseOrNull("2.0.0"))
        assertNull(ServiceVersion.parseOrNull("nope"))
    }
}
