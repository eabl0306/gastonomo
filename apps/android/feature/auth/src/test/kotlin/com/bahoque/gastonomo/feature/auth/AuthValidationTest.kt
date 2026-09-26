package com.bahoque.gastonomo.feature.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthValidationTest {

    @Test
    fun `a normal address is valid`() {
        assertTrue(AuthValidation.isValidEmail("camilo@correo.com"))
        assertTrue(AuthValidation.isValidEmail("camilo.rojas+etiqueta@sub.correo.com.co"))
    }

    @Test
    fun `the surrounding whitespace does not invalidate it`() {
        assertTrue(AuthValidation.isValidEmail("  camilo@correo.com  "))
    }

    @Test
    fun `an address without arroba, without domain or with a space is not valid`() {
        assertFalse(AuthValidation.isValidEmail("camilo.correo.com"))
        assertFalse(AuthValidation.isValidEmail("camilo@correo"))
        assertFalse(AuthValidation.isValidEmail("camilo@correo.c"))
        assertFalse(AuthValidation.isValidEmail("cami lo@correo.com"))
        assertFalse(AuthValidation.isValidEmail(""))
    }

    @Test
    fun `the password needs the eight characters the api asks for`() {
        assertFalse(AuthValidation.isLongEnough("1234567"))
        assertTrue(AuthValidation.isLongEnough("12345678"))
    }
}
