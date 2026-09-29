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
import kotlin.test.assertSame

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
        controller.navigate(GastonomoRoute.NewLoan())
        assertEquals(3, controller.currentBackStack.value.size)
        controller.switchTab(TopLevelDestination.LOANS)
        assertEquals(2, controller.currentBackStack.value.size)
        assertEquals(routeOf(GastonomoRoute.Loans), currentRoute())
    }

    /**
     * Tocar el tab en el que ya se esta se veia: la pantalla parpadeaba porque se volvia a crear
     * con su ViewModel de cero y pedia sus datos otra vez.
     */
    @Test
    fun `switching to the tab we are already on leaves the screen untouched`() {
        controller.switchTab(TopLevelDestination.LOANS)
        val entry = controller.currentBackStackEntry
        val stack = controller.currentBackStack.value.size

        controller.switchTab(TopLevelDestination.LOANS)

        assertSame(entry, controller.currentBackStackEntry, "la pantalla se volvio a crear")
        assertEquals(stack, controller.currentBackStack.value.size)
    }

    /**
     * Corregir un dictado sustituye la pantalla: volver atras desde el formulario tiene que llevar
     * a la pantalla desde la que se dicto, no a un borrador que ya no vale.
     */
    @Test
    fun `replacing the current screen does not leave it in the stack`() {
        controller.navigate(GastonomoRoute.Loans)
        val before = controller.currentBackStack.value.size
        controller.replaceCurrent(GastonomoRoute.NewLoan())
        assertEquals(routeOf(GastonomoRoute.NewLoan()), currentRoute())
        assertEquals(before, controller.currentBackStack.value.size)
        controller.popBackStack()
        assertEquals(routeOf(GastonomoRoute.Home), currentRoute())
    }

    @Test
    fun `replacing leaves only the new screen`() {
        controller.navigate(GastonomoRoute.NewLoan())
        controller.replaceWith(GastonomoRoute.Login)
        assertEquals(routeOf(GastonomoRoute.Login), currentRoute())
        assertEquals(2, controller.currentBackStack.value.size)
    }

    @Test
    fun `the actions of a real controller navigate it`() {
        val actions = GastonomoNavActions.of(controller)
        actions.navigateTo(GastonomoRoute.NewLoan())
        assertEquals(routeOf(GastonomoRoute.NewLoan()), currentRoute())
        actions.back()
        assertEquals(routeOf(GastonomoRoute.Home), currentRoute())
        actions.switchTab(TopLevelDestination.LOANS)
        assertEquals(routeOf(GastonomoRoute.Loans), currentRoute())
        actions.replaceWith(GastonomoRoute.Login)
        assertEquals(routeOf(GastonomoRoute.Login), currentRoute())
    }

    /**
     * El nombre de la ruta, sin los argumentos.
     *
     * Con rutas tipadas, `route` es el nombre cualificado de la clase mas la plantilla de sus
     * argumentos detras: `...NewLoan?person={person}&...`. Lo que se compara es la pantalla.
     */
    private fun routeOf(route: GastonomoRoute): String = route::class.qualifiedName.orEmpty()

    private fun currentRoute(): String? =
        controller.currentDestination?.route?.substringBefore('?')
}
