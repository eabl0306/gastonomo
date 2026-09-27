package com.bahoque.gastonomo.core.application.port

import kotlinx.coroutines.flow.Flow

/**
 * Si el dispositivo cree que tiene salida a internet.
 *
 * Es del dispositivo, no de la cuenta: no dice si la API responde, dice si hay por donde
 * intentarlo. Lo otro se sabe intentandolo, y eso ya lo cuentan los fallos de cada lectura.
 *
 * Hace falta como puerto porque hay dos cosas que dependen de ello y no se pueden deducir de un
 * fallo: la fila de Ajustes, que tiene que decir la verdad **antes** de que el usuario toque nada,
 * y el chip de moneda de los formularios, que se apaga sin conexion porque guardar un importe en
 * otra moneda sin conocer su equivalencia rompe la regla de los cuatro campos.
 */
fun interface NetworkMonitor {
    fun connected(): Flow<Boolean>
}
