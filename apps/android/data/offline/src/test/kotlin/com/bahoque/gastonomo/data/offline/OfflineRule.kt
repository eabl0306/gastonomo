package com.bahoque.gastonomo.data.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bahoque.gastonomo.data.database.local.LocalStore
import com.bahoque.gastonomo.data.database.local.createInMemoryLocalStore
import kotlinx.coroutines.CoroutineScope
import org.junit.rules.ExternalResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Un reloj que el test mueve a mano, para dejar pasar el plazo de refresco sin esperar. */
internal class TestClock(private var now: Long = 0L) : RefreshClock {

    override fun nowMillis(): Long = now

    fun advance(by: Duration) {
        now += by.inWholeMilliseconds
    }
}

/**
 * Los seis adaptadores sobre una base local de verdad, en memoria, y espias en el sitio de la red.
 *
 * La base es real a proposito: lo que se prueba aqui es que leer de disco y refrescar por detras
 * hace lo que dice, y un doble de la base no probaria eso.
 */
internal class OfflineRule(private val scope: CoroutineScope) : ExternalResource() {

    lateinit var store: LocalStore
        private set

    val clock = TestClock()
    val movementSpy = SpyMovementGateway()
    val loanSpy = SpyLoanGateway()
    val categorySpy = SpyCategoryGateway()
    val settingsSpy = SpySettingsGateway()
    val currencySpy = SpyCurrencyGateway()
    val authSpy = SpyAuthGateway()

    lateinit var gate: RefreshGate
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = createInMemoryLocalStore(context)
        gate = RefreshGate(clock = clock, scope = scope, every = WINDOW)
    }

    override fun after() {
        store.close()
    }

    val movements by lazy { offlineMovementGateway(movementSpy, store, gate) }
    val loans by lazy { offlineLoanGateway(loanSpy, store, gate) }
    val categories by lazy { offlineCategoryGateway(categorySpy, store, gate) }
    val settings by lazy { offlineSettingsGateway(settingsSpy, store, gate) }
    val currencies by lazy { offlineCurrencyGateway(currencySpy, store, gate) }
    val auth by lazy { offlineAuthGateway(authSpy, store, gate) }

    private companion object {
        val WINDOW = 45.seconds
    }
}
