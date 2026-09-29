package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.application.usecase.dictation.ParseDictation
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeDictationGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.sampleDictatedLoan
import com.bahoque.gastonomo.core.testing.fake.sampleDictationDraft
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El dictado del nucleo: manda el texto y el dia del dispositivo, y devuelve el borrador. */
class DictationUseCaseTest {

    @Test
    fun `the day that travels is the one from the device, not the server's`() = runTest {
        val gateway = FakeDictationGateway()
        val clock = FixedClock()
        ParseDictation(gateway, clock)("gasté 45 mil")
        assertEquals(clock.today(), gateway.lastToday)
    }

    @Test
    fun `the text goes trimmed, because a dictation ends in silence`() = runTest {
        val gateway = FakeDictationGateway()
        ParseDictation(gateway, FixedClock())("  gasté 45 mil  ")
        assertEquals(listOf("gasté 45 mil"), gateway.texts)
    }

    @Test
    fun `the draft comes through as the gateway gave it`() = runTest {
        val draft = sampleDictationDraft()
        val result = ParseDictation(FakeDictationGateway(draft), FixedClock())("gasté 45 mil")
        assertEquals(draft, (result as Outcome.Success).value)
    }

    @Test
    fun `a loan keeps its direction and its person`() = runTest {
        val gateway = FakeDictationGateway(sampleDictatedLoan())
        val result = ParseDictation(gateway, FixedClock())("le presté 200 mil a Rosa")
        val entry = (result as Outcome.Success).value.entry as DictatedEntry.Loan
        assertEquals("Rosa", entry.person)
    }

    @Test
    fun `an account without dictation approved gets the failure through`() = runTest {
        val gateway = FakeDictationGateway(failure = ApiError.NotFound)
        val result = ParseDictation(gateway, FixedClock())("gasté 45 mil")
        assertEquals(ApiError.NotFound, (result as Outcome.Failure).error)
    }

    @Test
    fun `what the API says is missing is what the draft carries`() = runTest {
        val draft = sampleDictationDraft(amount = null, missing = setOf(MissingField.AMOUNT))
        val gateway = FakeDictationGateway(draft)
        val parsed = (ParseDictation(gateway, FixedClock())("almuerzo") as Outcome.Success).value
        assertNull(parsed.amount)
        assertTrue(MissingField.AMOUNT in parsed.missing)
    }

    @Test
    fun `the names of the missing fields are the ones the API sends`() {
        assertEquals(MissingField.AMOUNT, MissingField.fromWire("amount"))
        assertEquals(MissingField.PERSON, MissingField.fromWire("person"))
        // Uno que no conocemos se ignora: la API puede anadir otro antes que el cliente.
        assertNull(MissingField.fromWire("lo_que_sea"))
    }
}
