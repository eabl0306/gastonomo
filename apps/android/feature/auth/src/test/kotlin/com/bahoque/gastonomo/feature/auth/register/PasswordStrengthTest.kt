package com.bahoque.gastonomo.feature.auth.register

import kotlin.test.Test
import kotlin.test.assertEquals

/** Los umbrales son los del prototipo, que el handoff declara fuente de verdad. */
class PasswordStrengthTest {

    @Test
    fun `an empty password fills no bar`() {
        assertEquals(PasswordStrength.EMPTY, PasswordStrength.of(""))
        assertEquals(0, PasswordStrength.EMPTY.filledBars)
    }

    @Test
    fun `under six characters is the first level`() {
        assertEquals(PasswordStrength.VERY_SHORT, PasswordStrength.of("a"))
        assertEquals(PasswordStrength.VERY_SHORT, PasswordStrength.of("abcde"))
    }

    @Test
    fun `six or seven characters is the second level`() {
        assertEquals(PasswordStrength.ALMOST, PasswordStrength.of("abcdef"))
        assertEquals(PasswordStrength.ALMOST, PasswordStrength.of("abcdef1"))
    }

    @Test
    fun `eight characters of a single kind is the third level`() {
        assertEquals(PasswordStrength.GOOD, PasswordStrength.of("abcdefgh"))
        assertEquals(PasswordStrength.GOOD, PasswordStrength.of("12345678"))
    }

    @Test
    fun `eight characters with a letter and a digit is the fourth level`() {
        assertEquals(PasswordStrength.STRONG, PasswordStrength.of("abcdefg1"))
        assertEquals(4, PasswordStrength.STRONG.filledBars)
    }

    @Test
    fun `every level fills one more bar than the previous one`() {
        assertEquals(listOf(0, 1, 2, 3, 4), PasswordStrength.entries.map { it.filledBars })
    }
}
