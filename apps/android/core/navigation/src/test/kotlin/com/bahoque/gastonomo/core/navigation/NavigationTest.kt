package com.bahoque.gastonomo.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationTest {

    @Test
    fun `there is a route for each of the thirteen navigable screens`() {
        val routes = listOf(
            GastonomoRoute.Login,
            GastonomoRoute.Register,
            GastonomoRoute.ForgotPassword("camilo@correo.com"),
            GastonomoRoute.Unlock,
            GastonomoRoute.Home,
            GastonomoRoute.Movements,
            GastonomoRoute.NewMovement("out"),
            GastonomoRoute.Loans,
            GastonomoRoute.LoanDetail("l1"),
            GastonomoRoute.NewLoan,
            GastonomoRoute.Settings,
            GastonomoRoute.Categories,
            GastonomoRoute.Export,
        )
        assertEquals(13, routes.size)
        assertTrue(routes.all { it is GastonomoRoute })
    }

    @Test
    fun `a route with arguments carries them`() {
        assertEquals("out", GastonomoRoute.NewMovement("out").kind)
        assertEquals("l1", GastonomoRoute.LoanDetail("l1").loanId)
        assertEquals("camilo@correo.com", GastonomoRoute.ForgotPassword("camilo@correo.com").email)
        assertEquals("", GastonomoRoute.ForgotPassword().email)
    }

    @Test
    fun `the tab bar has the four destinations of the design in order`() {
        assertEquals(
            listOf("home", "swap_vert", "handshake", "settings"),
            TopLevelDestination.entries.map { it.icon },
        )
        assertEquals(
            listOf(
                GastonomoRoute.Home,
                GastonomoRoute.Movements,
                GastonomoRoute.Loans,
                GastonomoRoute.Settings,
            ),
            TopLevelDestination.entries.map { it.route },
        )
    }

    @Test
    fun `the navigation actions forward what the screen asks for`() {
        val visited = mutableListOf<String>()
        val actions = GastonomoNavActions(
            navigateTo = { visited += "navigate:$it" },
            switchTab = { visited += "tab:${it.name}" },
            back = { visited += "back" },
            replaceWith = { visited += "replace:$it" },
        )
        actions.navigateTo(GastonomoRoute.NewLoan)
        actions.switchTab(TopLevelDestination.LOANS)
        actions.back()
        actions.replaceWith(GastonomoRoute.Login)
        assertEquals(
            listOf("navigate:NewLoan", "tab:LOANS", "back", "replace:Login"),
            visited,
        )
    }
}
