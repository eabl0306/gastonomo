package com.bahoque.gastonomo.data.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

/**
 * Los importes viajan como cadena (`"186400.0000"`) para que nadie los meta en un Double y
 * pierda centavos. Esta es la unica traduccion de esas cadenas a [BigDecimal] de toda la
 * aplicacion.
 *
 * Se usa como `@Serializable(with = BigDecimalAsStringSerializer::class)` en cada campo de
 * importe de los DTO.
 */
object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}
