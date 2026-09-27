package com.bahoque.gastonomo.data.cache

/**
 * El paso del tiempo para la cache, en milisegundos.
 *
 * Es un puerto por dos motivos. El primero es que un test tiene que poder envejecer una
 * entrada sin esperar de verdad. El segundo importa mas: **no puede salir del reloj de
 * pared**. Si el usuario cambia la hora del telefono, o se la ajusta la red, un reloj de
 * pared salta hacia atras, y una entrada que quedara guardada "en el futuro" no caducaria
 * nunca.
 */
fun interface CacheTicker {
    fun nowMillis(): Long
}

/** El tiempo transcurrido desde que arranco la maquina virtual, que nunca va hacia atras. */
object ElapsedTicker : CacheTicker {

    override fun nowMillis(): Long = System.nanoTime() / NANOS_PER_MILLI

    private const val NANOS_PER_MILLI = 1_000_000L
}
