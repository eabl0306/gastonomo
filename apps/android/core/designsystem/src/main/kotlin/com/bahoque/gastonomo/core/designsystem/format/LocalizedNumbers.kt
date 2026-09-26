package com.bahoque.gastonomo.core.designsystem.format

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Los formateadores numericos del idioma, cacheados por numero de decimales.
 *
 * [NumberFormat] no es seguro entre hilos, asi que cada acceso va sincronizado: las
 * pantallas formatean desde el hilo principal, pero un caso de uso que prepare una
 * exportacion lo hace desde otro.
 */
internal class LocalizedNumbers(private val locale: Locale) {

    private val lock = Any()
    private val cache = mutableMapOf<Int, NumberFormat>()

    /** El separador decimal del idioma: "," en es-CO y "." en en-US. */
    val decimalSeparator: Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /** Formatea con exactamente [decimals] decimales y agrupacion de miles. */
    fun fixed(value: BigDecimal, decimals: Int): String = synchronized(lock) {
        cache.getOrPut(decimals) { exact(decimals) }.format(value)
    }

    /** Formatea con hasta [decimals] decimales, sin rellenar con ceros. */
    fun upTo(value: BigDecimal, decimals: Int): String = synchronized(lock) {
        cache.getOrPut(-decimals - 1) { atMost(decimals) }.format(value)
    }

    private fun exact(decimals: Int): NumberFormat =
        numberFormat().apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }

    private fun atMost(decimals: Int): NumberFormat =
        numberFormat().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = decimals
        }

    private fun numberFormat(): NumberFormat =
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            isGroupingUsed = true
            // Sin esto un importe grande pierde cifras al formatear un BigDecimal.
            isParseBigDecimal = true
        }
}
