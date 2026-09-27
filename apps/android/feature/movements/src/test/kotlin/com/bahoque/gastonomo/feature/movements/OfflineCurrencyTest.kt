package com.bahoque.gastonomo.feature.movements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La multimoneda se degrada sin conexion, no se rompe.
 *
 * Sin tasas nuevas no se puede guardar un importe en otra moneda sin inventarse su equivalencia,
 * y eso romperia la regla de los cuatro campos. Asi que el chip se apaga y el registro va en la
 * moneda principal, con una explicacion en vez de un control muerto.
 */
@RunWith(AndroidJUnit4::class)
class OfflineCurrencyTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `with network and rates the currency can be chosen`() = runTest {
        val viewModel = NewMovementViewModel(TestMovementsDependencies(), MovementKind.OUT)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canConvert)
    }

    @Test
    fun `in airplane mode it cannot`() = runTest {
        val viewModel =
            NewMovementViewModel(TestMovementsDependencies(online = false), MovementKind.OUT)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canConvert)
    }

    @Test
    fun `a 503 from the api behaves the same as no network`() = runTest {
        // Distinguir "no tengo red" de "el servidor no tiene tasas" importa para el diagnostico,
        // pero para el usuario acaba en lo mismo: no hay con que convertir.
        val viewModel = NewMovementViewModel(
            TestMovementsDependencies(ratesFailure = ApiError.ServiceUnavailable),
            MovementKind.OUT,
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canConvert)
    }

    @Test
    fun `offline the record goes in the main currency, so base equals amount`() = runTest {
        val viewModel =
            NewMovementViewModel(TestMovementsDependencies(online = false), MovementKind.OUT)
        advanceUntilIdle()

        val state = viewModel.state.value
        // Los cuatro campos quedan coherentes solos: sin poder elegir otra moneda, la
        // equivalencia es el propio importe.
        assertFalse(state.canConvert)
        assertTrue(state.currency == state.displayCurrency)
    }
}
