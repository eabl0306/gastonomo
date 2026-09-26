package com.bahoque.gastonomo.core.domain.user

import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import kotlin.test.Test
import kotlin.test.assertEquals

class UserTest {

    @Test
    fun `the initials are the first letter of the first two words`() {
        assertEquals("CR", user("Camilo Rojas").initials)
        assertEquals("CR", user("  camilo  rojas  perez ").initials)
    }

    @Test
    fun `a single name gives a single initial`() {
        assertEquals("C", user("Camilo").initials)
    }

    @Test
    fun `without a name the initial comes from the email`() {
        assertEquals("C", user("").initials)
    }

    @Test
    fun `the greeting uses the first name`() {
        assertEquals("Camilo", user("Camilo Rojas").firstName)
        assertEquals("", user("").firstName)
    }

    @Test
    fun `a session carries the tokens and the user`() {
        val session = Session(
            accessToken = "access",
            expiresInSeconds = 900,
            refreshToken = "refresh",
            user = user("Camilo Rojas"),
        )
        assertEquals("Camilo", session.user.firstName)
        val rotated = TokenPair("access2", 900, "refresh2")
        assertEquals("refresh2", rotated.refreshToken)
    }

    private fun user(name: String) = User(id = "1", name = name, email = "camilo@correo.com")
}
