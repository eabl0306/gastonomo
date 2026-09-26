package com.bahoque.gastonomo.core.navigation

import android.content.Context
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Las dos reglas de navegacion del diseno, comprobadas sobre un NavController de verdad:
 * cambiar de tab vacia la pila, y entrar sustituye toda la pila.
 */
@RunWith(AndroidJUnit4::class)
class NavControllerActionsTest {

    private lateinit var controller: TestNavHostController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        controller = TestNavHostController(context)
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.graph = controller.createGraph(startDestination = GastonomoRoute.Home) {
            composable<GastonomoRoute.Home> { }
            composable<GastonomoRoute.Loans> { }
            composable<GastonomoRoute.NewLoan> { }
            composable<GastonomoRoute.Login> { }
        }
    }

    @Test
    fun `switching tab empties the stack`() {
        controller.navigate(GastonomoRoute.NewLoan)
        assertEquals(3, controller.currentBackStack.value.size)
        controller.switchTab(TopLevelDestination.LOANS)
        assertEquals(2, controller.currentBackStack.value.size)
        assertEquals(routeOf(GastonomoRoute.Loans), controller.currentDestination?.route)
    }

    @Test
    fun `replacing leaves only the new screen`() {
        controller.navigate(GastonomoRoute.NewLoan)
        controller.replaceWith(GastonomoRoute.Login)
        assertEquals(routeOf(GastonomoRoute.Login), controller.currentDestination?.route)
        assertEquals(2, controller.currentBackStack.value.size)
    }

    @Test
    fun `the actions of a real controller navigate it`() {
        val actions = GastonomoNavActions.of(controller)
        actions.navigateTo(GastonomoRoute.NewLoan)
        assertEquals(routeOf(GastonomoRoute.NewLoan), controller.currentDestination?.route)
        actions.back()
        assertEquals(routeOf(GastonomoRoute.Home), controller.currentDestination?.route)
        actions.switchTab(TopLevelDestination.LOANS)
        assertEquals(routeOf(GastonomoRoute.Loans), controller.currentDestination?.route)
        actions.replaceWith(GastonomoRoute.Login)
        assertEquals(routeOf(GastonomoRoute.Login), controller.currentDestination?.route)
    }

    private fun routeOf(route: GastonomoRoute): String = route::class.qualifiedName.orEmpty()
}
