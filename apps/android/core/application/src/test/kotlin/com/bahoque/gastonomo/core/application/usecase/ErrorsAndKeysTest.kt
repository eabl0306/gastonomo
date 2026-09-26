package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.application.port.SystemClock
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ErrorsAndKeysTest {

    @Test
    fun `a validation error finds the message of a field`() {
        val error = ApiError.Validation(
            fields = listOf(FieldError("amount", "El importe tiene que ser mayor que cero.")),
        )
        assertEquals("El importe tiene que ser mayor que cero.", error.messageFor("amount"))
        assertNull(error.messageFor("currency"))
    }

    @Test
    fun `the conflict codes are the ones the api sends`() {
        assertEquals("idempotency_key_reused", ConflictCode.IDEMPOTENCY_KEY_REUSED)
        assertEquals("idempotency_key_in_progress", ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS)
        assertEquals("category_name_taken", ConflictCode.CATEGORY_NAME_TAKEN)
        assertEquals("category_is_default", ConflictCode.CATEGORY_IS_DEFAULT)
    }

    @Test
    fun `a new key is different every time`() {
        assertNotEquals(IdempotencyKey.newKey(), IdempotencyKey.newKey())
    }

    @Test
    fun `a key is neither empty nor longer than the api accepts`() {
        assertFailsWith<IllegalArgumentException> { IdempotencyKey("  ") }
        assertFailsWith<IllegalArgumentException> {
            IdempotencyKey("x".repeat(IdempotencyKey.MAX_LENGTH + 1))
        }
        assertEquals("k", IdempotencyKey("k").toString())
    }

    @Test
    fun `a result knows whether it was a replay`() {
        assertFalse(IdempotentResult(value = 1, isReplay = false).isReplay)
    }

    @Test
    fun `the system clock gives a real date`() {
        assertEquals(LocalDate.now(), SystemClock.today())
    }

    @Test
    fun `the pin of the design has four digits`() {
        assertEquals(4, LocalLock.PIN_LENGTH)
    }
}
