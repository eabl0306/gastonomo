package com.bahoque.gastonomo.core.application.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransientMessagesTest {

    @Test
    fun `a posted message waits for the next screen`() {
        val messages = TransientMessages()
        assertNull(messages.message.value)
        messages.post(TransientMessages.MOVEMENT_SAVED_EXPENSE)
        assertEquals(TransientMessages.MOVEMENT_SAVED_EXPENSE, messages.message.value)
    }

    @Test
    fun `consuming it clears it so it is not shown twice`() {
        val messages = TransientMessages()
        messages.post(TransientMessages.MOVEMENT_SAVED_INCOME)
        messages.consume()
        assertNull(messages.message.value)
    }

    @Test
    fun `there is only one message at a time`() {
        val messages = TransientMessages()
        messages.post(TransientMessages.MOVEMENT_SAVED_INCOME)
        messages.post(TransientMessages.MOVEMENT_SAVED_EXPENSE)
        assertEquals(TransientMessages.MOVEMENT_SAVED_EXPENSE, messages.message.value)
    }

    @Test
    fun `a key is its own value`() {
        assertEquals("movement_saved_income", TransientMessages.MOVEMENT_SAVED_INCOME.toString())
    }
}
