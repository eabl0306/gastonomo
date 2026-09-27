package com.bahoque.gastonomo.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.CorrectPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.DiscardPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.ObservePendingWrites
import com.bahoque.gastonomo.core.application.usecase.sync.RetryPendingWrites
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.testing.fake.FakePendingWritesGateway
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.feature.settings.pending.PendingWritesActions
import com.bahoque.gastonomo.feature.settings.pending.PendingWritesScreen
import com.bahoque.gastonomo.feature.settings.pending.PendingWritesUiState
import com.bahoque.gastonomo.feature.settings.pending.PendingWritesViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los registros que no han salido del telefono.
 *
 * Existe por una razon concreta: un registro que la API rechaza **no puede desaparecer en
 * silencio**. Si desapareciera, el usuario creeria que ese gasto esta y no estaria en ninguna
 * parte.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class PendingWritesTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `it shows what is still waiting`() = runTest {
        val model = viewModelWith(WAITING)
        advanceUntilIdle()

        assertEquals(listOf(WAITING), model.state.value.writes)
    }

    @Test
    fun `correcting the amount leaves the row fixed and still its own`() = runTest {
        val gateway = FakePendingWritesGateway(listOf(BROKEN))
        val model = viewModelWith(gateway = gateway)
        advanceUntilIdle()

        model.onEdit(BROKEN)
        model.onAmount("2000")
        model.onSave()
        advanceUntilIdle()

        val corrected = gateway.corrected.getValue(BROKEN.id) as PendingDraft.Movement
        assertEquals(BigDecimal("2000"), corrected.value.money.amount)
        // La fila sigue siendo la misma, con su clave, y deja de estar rota. Que la clave no se
        // toque al guardar lo comprueba el test de `:data:database`, que es donde se guarda.
        val stored = model.state.value.writes.single()
        assertEquals(BROKEN.idempotencyKey, stored.idempotencyKey)
        assertNull(stored.failure)
    }

    @Test
    fun `the amount only takes digits`() = runTest {
        val model = viewModelWith(BROKEN)
        advanceUntilIdle()

        model.onEdit(BROKEN)
        model.onAmount("1a2b.5")

        assertEquals("12.5", model.state.value.amount)
    }

    @Test
    fun `discarding asks first, because it deletes what the user registered`() = runTest {
        val gateway = FakePendingWritesGateway(listOf(BROKEN))
        val model = viewModelWith(gateway = gateway)
        advanceUntilIdle()

        model.onAskDiscard(BROKEN)
        assertEquals(BROKEN.id, model.state.value.confirming)

        model.onCancelDiscard()
        advanceUntilIdle()
        assertNull(model.state.value.confirming)
        assertTrue(model.state.value.writes.isNotEmpty())
    }

    @Test
    fun `confirming the discard takes it out`() = runTest {
        val model = viewModelWith(BROKEN)
        advanceUntilIdle()

        model.onAskDiscard(BROKEN)
        model.onConfirmDiscard()
        advanceUntilIdle()

        assertTrue(model.state.value.writes.isEmpty())
    }

    @Test
    fun `retrying asks for another round`() = runTest {
        val gateway = FakePendingWritesGateway(listOf(WAITING))
        val model = viewModelWith(gateway = gateway)

        model.onRetry()
        advanceUntilIdle()

        assertEquals(1, gateway.retries)
    }

    @Test
    fun `with nothing waiting it says so`() {
        compose.setContent {
            GastonomoTheme { PendingWritesScreen(PendingWritesUiState(), NO_ACTIONS) }
        }

        compose.onNodeWithText("No hay nada esperando", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a rejected write says why and offers a way out`() {
        compose.setContent {
            GastonomoTheme {
                PendingWritesScreen(PendingWritesUiState(writes = listOf(BROKEN)), NO_ACTIONS)
            }
        }

        compose.onNodeWithText(REASON).assertIsDisplayed()
        compose.onNodeWithText("Descartar").assertIsDisplayed()
    }

    @Test
    fun `one that is only waiting offers nothing to fix`() {
        var asked = 0
        compose.setContent {
            GastonomoTheme {
                PendingWritesScreen(
                    state = PendingWritesUiState(writes = listOf(WAITING)),
                    actions = NO_ACTIONS.copy(onAskDiscard = { asked++ }),
                )
            }
        }

        compose.onNodeWithText("Esperando conexión").assertIsDisplayed()
        assertEquals(0, asked)
    }

    @Test
    fun `retrying from the screen calls back`() {
        var retried = 0
        compose.setContent {
            GastonomoTheme {
                PendingWritesScreen(
                    state = PendingWritesUiState(writes = listOf(WAITING)),
                    actions = NO_ACTIONS.copy(onRetry = { retried++ }),
                )
            }
        }

        compose.onNodeWithText("Reintentar ahora").performClick()

        assertEquals(1, retried)
    }

    private fun viewModelWith(
        vararg writes: PendingWrite,
        gateway: FakePendingWritesGateway = FakePendingWritesGateway(writes.toList()),
    ) = PendingWritesViewModel(
        observe = ObservePendingWrites(gateway),
        correct = CorrectPendingWrite(gateway),
        discard = DiscardPendingWrite(gateway),
        retry = RetryPendingWrites(gateway),
    )

    private companion object {
        const val REASON = "El importe tiene que ser mayor que cero"

        val DRAFT = MovementDraft(
            occurredOn = LocalDate.parse("2026-09-22"),
            kind = MovementKind.OUT,
            categoryId = SampleFinance.food.id,
            money = MonetaryAmount.single(BigDecimal("1000"), CurrencyCode.COP),
            note = "Mercado",
        )

        val WAITING = PendingWrite(
            id = "w1",
            idempotencyKey = IdempotencyKey("key-1"),
            draft = PendingDraft.Movement(DRAFT),
        )

        val BROKEN = WAITING.copy(id = "w2", failure = REASON)

        val NO_ACTIONS = PendingWritesActions(
            onBack = {},
            onEdit = {},
            onAmount = {},
            onCancelEdit = {},
            onSave = {},
            onAskDiscard = {},
            onCancelDiscard = {},
            onConfirmDiscard = {},
            onRetry = {},
        )
    }
}
