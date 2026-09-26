package com.bahoque.gastonomo.core.application.error

/**
 * Un error de un campo concreto, tal como lo manda la API en `errors[]` de un 422.
 *
 * El mensaje **ya viene en castellano** y listo para pintar junto al campo: no se traduce
 * ni se reescribe. Es la unica cadena de la aplicacion que no sale de `strings.xml`.
 */
data class FieldError(val field: String, val message: String)
