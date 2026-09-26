package com.bahoque.gastonomo.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Cifra y descifra con una clave AES-256 que vive en el Android Keystore y **nunca sale de
 * el**: el proceso solo maneja un identificador.
 *
 * Se eligio esto en vez de `EncryptedSharedPreferences` porque esa biblioteca
 * (`androidx.security:security-crypto`) esta obsoleta y su ultima version estable arrastra
 * Tink; con DataStore mas Keystore el cifrado son treinta lineas y no hay dependencia que
 * se quede vieja.
 *
 * El formato guardado es `base64(iv):base64(cifrado)`. El IV va delante porque GCM necesita
 * uno distinto por escritura y hay que recuperarlo para descifrar.
 */
@OptIn(ExperimentalEncodingApi::class)
internal open class KeystoreCipher(private val alias: String = KEY_ALIAS) {

    /** Devuelve el texto cifrado, listo para guardar. */
    open fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain.toByteArray())
        return Base64.encode(cipher.iv) + SEPARATOR + Base64.encode(encrypted)
    }

    /** Devuelve el texto en claro, o null si el guardado esta corrupto o la clave cambio. */
    open fun decrypt(stored: String): String? = runCatching {
        val (iv, payload) = stored.split(SEPARATOR).let { it[0] to it[1] }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(iv)))
        String(cipher.doFinal(Base64.decode(payload)))
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "gastonomo.session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
    }
}
