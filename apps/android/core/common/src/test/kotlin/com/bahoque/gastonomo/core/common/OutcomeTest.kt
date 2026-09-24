package com.bahoque.gastonomo.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutcomeTest {

    @Test
    fun `success carries the value`() {
        val outcome: Outcome<Int, String> = Outcome.Success(7)

        assertTrue(outcome.isSuccess)
        assertEquals(7, outcome.valueOrNull())
        assertNull(outcome.errorOrNull())
    }

    @Test
    fun `failure carries the error`() {
        val outcome: Outcome<Int, String> = Outcome.Failure("boom")

        assertFalse(outcome.isSuccess)
        assertNull(outcome.valueOrNull())
        assertEquals("boom", outcome.errorOrNull())
    }

    @Test
    fun `fold picks the matching branch`() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("bad")

        assertEquals("v2", success.fold({ "v$it" }, { "e$it" }))
        assertEquals("ebad", failure.fold({ "v$it" }, { "e$it" }))
    }

    @Test
    fun `map only transforms the success branch`() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("bad")

        assertEquals(Outcome.Success(4), success.map { it * 2 })
        assertEquals(Outcome.Failure("bad"), failure.map { it * 2 })
    }

    @Test
    fun `flatMap chains outcomes`() {
        val success: Outcome<Int, String> = Outcome.Success(2)

        assertEquals(Outcome.Success("2"), success.flatMap { Outcome.Success(it.toString()) })
        assertEquals(Outcome.Failure("nope"), success.flatMap { Outcome.Failure("nope") })
        assertEquals(Outcome.Failure("bad"), Outcome.Failure("bad").flatMap { Outcome.Success(it) })
    }

    @Test
    fun `mapError only transforms the failure branch`() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("bad")

        assertEquals(Outcome.Success(2), success.mapError { it.length })
        assertEquals(Outcome.Failure(3), failure.mapError { it.length })
    }
}
