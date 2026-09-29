package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.MissingField
import com.bahoque.gastonomo.core.application.port.SpeechEvent
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.sampleDictatedLoan
import com.bahoque.gastonomo.core.testing.fake.sampleDictationDraft
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El dictado de punta a punta con dobles: escuchar, interpretar y guardar.
 *
 * Lo que se vigila con mas cuidado es que **ningun fallo del microfono deje sin salida**: los cinco
 * tienen que acabar en el modo escrito, con lo que se hubiera entendido ya puesto en el campo.
 */
class DictateViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `it opens listening when the device has a microphone`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        assertEquals(DictationPhase.LISTENING, viewModel.state.value.phase)
        assertEquals(1, dependencies.speech.starts)
    }

    @Test
    fun `without a microphone it opens in the typed mode and says why`() = runTest {
        val dependencies = TestDictationDependencies(micAvailable = false)
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        assertEquals(DictationPhase.TYPING, viewModel.state.value.phase)
        assertEquals(SpeechFailure.UNAVAILABLE, viewModel.state.value.micFailure)
        assertEquals(0, dependencies.speech.starts)
    }

    @Test
    fun `it asks the system for the language of the account, not ours`() = runTest {
        val english = FakeSettingsGateway(UserSettings(language = AppLanguage.ENGLISH))
        val dependencies = TestDictationDependencies(settings = english)
        DictateViewModel(dependencies)
        advanceUntilIdle()
        assertEquals("en-US", dependencies.speech.lastLanguageTag)
    }

    @Test
    fun `what is heard shows up while speaking and the final text is what gets sent`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        dependencies.speech.emit(SpeechEvent.Partial("gasté 45"))
        advanceUntilIdle()
        assertEquals("gasté 45", viewModel.state.value.heard)
        dependencies.speech.emit(SpeechEvent.Final("gasté 45 mil en almuerzo"))
        advanceUntilIdle()
        assertEquals(listOf("gasté 45 mil en almuerzo"), dependencies.dictationGateway.texts)
        assertEquals(DictationPhase.REVIEW, viewModel.state.value.phase)
    }

    @Test
    fun `stopping asks the system to finish instead of cutting it off`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        dependencies.speech.emit(SpeechEvent.Partial("gasté 45"))
        advanceUntilIdle()
        viewModel.stopListening()
        assertEquals(1, dependencies.speech.finishes)
        // Sigue escuchando: el texto definitivo todavia no ha llegado.
        assertEquals(DictationPhase.LISTENING, viewModel.state.value.phase)
        assertTrue(dependencies.dictationGateway.texts.isEmpty())
    }

    @Test
    fun `every microphone failure lands in the typed mode with what was heard`() = runTest {
        SpeechFailure.entries.forEach { reason ->
            val dependencies = TestDictationDependencies()
            val viewModel = DictateViewModel(dependencies)
            advanceUntilIdle()
            dependencies.speech.emit(SpeechEvent.Partial("gasté 45"))
            advanceUntilIdle()
            dependencies.speech.emit(SpeechEvent.Failed(reason))
            advanceUntilIdle()
            val state = viewModel.state.value
            assertEquals(DictationPhase.TYPING, state.phase, "$reason")
            assertEquals(reason, state.micFailure, "$reason")
            assertEquals("gasté 45", state.text, "$reason")
        }
    }

    @Test
    fun `a final result with nothing in it is treated as not having heard`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        dependencies.speech.emit(SpeechEvent.Final("   "))
        advanceUntilIdle()
        assertEquals(DictationPhase.TYPING, viewModel.state.value.phase)
        assertEquals(SpeechFailure.NO_MATCH, viewModel.state.value.micFailure)
        assertTrue(dependencies.dictationGateway.texts.isEmpty())
    }

    @Test
    fun `preferring to type keeps what was already heard`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        dependencies.speech.emit(SpeechEvent.Partial("gasté 45"))
        advanceUntilIdle()
        viewModel.typeInstead()
        val state = viewModel.state.value
        assertEquals(DictationPhase.TYPING, state.phase)
        assertEquals("gasté 45", state.text)
        assertNull(state.micFailure)
    }

    @Test
    fun `a blank text is not sent to the API`() = runTest {
        val dependencies = TestDictationDependencies(micAvailable = false)
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("   ")
        advanceUntilIdle()
        assertTrue(dependencies.dictationGateway.texts.isEmpty())
        assertEquals(DictationPhase.TYPING, viewModel.state.value.phase)
    }

    @Test
    fun `the day that travels is the one from the device`() = runTest {
        val dependencies = TestDictationDependencies(micAvailable = false)
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("gasté 45 mil")
        advanceUntilIdle()
        assertEquals(dependencies.clock.today(), dependencies.dictationGateway.lastToday)
    }

    @Test
    fun `an account without dictation approved goes back to the typed mode with its message`() =
        runTest {
            val dependencies = TestDictationDependencies(parseFailure = ApiError.NotFound)
            val viewModel = DictateViewModel(dependencies)
            advanceUntilIdle()
            viewModel.interpret("gasté 45 mil")
            advanceUntilIdle()
            val state = viewModel.state.value
            assertEquals(DictationPhase.TYPING, state.phase)
            assertEquals(DictationProblem.NOT_APPROVED, state.problem)
        }

    @Test
    fun `a text the interpreter did not understand says so instead of a generic error`() = runTest {
        val dependencies = TestDictationDependencies(
            parseFailure = ApiError.Validation(fields = emptyList(), detail = "no se entiende"),
        )
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("aaa")
        advanceUntilIdle()
        assertEquals(DictationProblem.UNCLEAR, viewModel.state.value.problem)
    }

    @Test
    fun `saving a movement leaves its toast and closes`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("gasté 45 mil")
        advanceUntilIdle()
        var savedLoan: Boolean? = null
        viewModel.save { savedLoan = it }
        advanceUntilIdle()
        assertEquals(false, savedLoan)
        assertTrue(viewModel.state.value.closing)
        assertEquals(
            TransientMessages.MOVEMENT_SAVED_EXPENSE,
            dependencies.messages.message.value,
        )
    }

    @Test
    fun `saving a loan tells Loans about it, and leaves the toast to that screen`() = runTest {
        val dependencies = TestDictationDependencies(draft = sampleDictatedLoan())
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("le presté 200 mil a Rosa")
        advanceUntilIdle()
        var savedLoan: Boolean? = null
        viewModel.save { savedLoan = it }
        advanceUntilIdle()
        assertEquals(true, savedLoan)
        assertNull(dependencies.messages.message.value)
    }

    @Test
    fun `a retry of the same attempt keeps the same idempotency key`() = runTest {
        val dependencies = TestDictationDependencies(createFailure = ApiError.Network)
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("gasté 45 mil")
        advanceUntilIdle()
        viewModel.save { }
        advanceUntilIdle()
        viewModel.save { }
        advanceUntilIdle()
        val keys = dependencies.movementGateway.keys
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
    }

    @Test
    fun `without an amount it does not save, and says how to fix it`() = runTest {
        val dependencies = TestDictationDependencies(
            draft = sampleDictationDraft(amount = null, missing = setOf(MissingField.AMOUNT)),
        )
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("almuerzo")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSave)
        assertEquals(DictationBlocker.MISSING_AMOUNT, viewModel.state.value.blocker)
        viewModel.save { }
        advanceUntilIdle()
        assertTrue(dependencies.movementGateway.keys.isEmpty())
    }

    @Test
    fun `a loan without a person does not save either`() = runTest {
        val dependencies = TestDictationDependencies(
            draft = sampleDictatedLoan(person = "", missing = setOf(MissingField.PERSON)),
        )
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("le presté 200 mil")
        advanceUntilIdle()
        assertEquals(DictationBlocker.MISSING_PERSON, viewModel.state.value.blocker)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `dictating again throws away the draft and opens the microphone`() = runTest {
        val dependencies = TestDictationDependencies()
        val viewModel = DictateViewModel(dependencies)
        advanceUntilIdle()
        viewModel.interpret("gasté 45 mil")
        advanceUntilIdle()
        assertEquals(DictationPhase.REVIEW, viewModel.state.value.phase)
        viewModel.listen()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(DictationPhase.LISTENING, state.phase)
        assertNull(state.draft)
        assertEquals("", state.text)
        assertEquals(2, dependencies.speech.starts)
    }
}
