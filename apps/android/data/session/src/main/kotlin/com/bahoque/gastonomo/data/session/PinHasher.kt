package com.bahoque.gastonomo.data.session

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Hashea el PIN con PBKDF2-HMAC-SHA256 y una sal por dispositivo.
 *
 * Un PIN de cuatro digitos tiene diez mil combinaciones, asi que el hash no lo protege de
 * una busqueda exhaustiva; lo que evita es que quien lea el almacenamiento lo lea en claro.
 * La proteccion de verdad la da el Keystore, que es quien guarda el token que el PIN abre.
 */
@OptIn(ExperimentalEncodingApi::class)
internal class PinHasher(private val random: SecureRandom = SecureRandom()) {

    /** Una sal nueva, en base64. */
    fun newSalt(): String = Base64.encode(ByteArray(SALT_BYTES).also(random::nextBytes))

    /** El hash del PIN con esa sal, en base64. */
    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.decode(salt), ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return Base64.encode(factory.generateSecret(spec).encoded)
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
