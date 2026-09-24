package com.bahoque.gastonomo

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.ComponentHealth
import com.bahoque.gastonomo.core.domain.health.HealthReport
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import com.bahoque.gastonomo.core.domain.health.ServiceVersion
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Comprueba que la raiz de composicion levanta la pantalla sin tocar la red.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @BeforeTest
    fun replaceContainer() {
        val application = ApplicationProvider.getApplicationContext<GastonomoApplication>()
        application.container = AppContainer(healthGateway = FAKE_GATEWAY)
    }

    @Test
    fun `the activity reaches the resumed state with the health screen`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(false, activity.isFinishing)
            }
        }
    }

    private companion object {
        val REPORT = HealthReport(
            version = ServiceVersion("0.1.0"),
            checks = listOf(ComponentHealth("database", HealthStatus.OK, 3)),
        )
        val FAKE_GATEWAY = HealthGateway { Outcome.Success(REPORT) }
    }
}
