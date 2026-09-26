package com.bahoque.gastonomo.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * El unico DataStore del dispositivo. Guarda la sesion cifrada y los ajustes locales.
 *
 * Es una extension de [Context] porque `preferencesDataStore` exige un delegado por
 * proceso: dos instancias del mismo fichero se pisan.
 */
internal val Context.gastonomoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gastonomo.session",
)

/** Las claves del DataStore, en un solo sitio. */
internal object SessionKeys {
    /** El refresh token, cifrado con el Keystore. Es la llave de la cuenta durante 30 dias. */
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token_encrypted")

    /** El access token, tambien cifrado. Caduca en 15 minutos. */
    val ACCESS_TOKEN = stringPreferencesKey("access_token_encrypted")

    /** Cuando caduca el access token, en milisegundos de epoch. */
    val ACCESS_TOKEN_EXPIRES_AT = androidx.datastore.preferences.core.longPreferencesKey(
        "access_token_expires_at",
    )

    val USER_ID = stringPreferencesKey("user_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_EMAIL = stringPreferencesKey("user_email")

    /** El PIN del cerrojo local, hasheado. Nunca en claro. */
    val PIN_HASH = stringPreferencesKey("pin_hash")

    /** La sal del hash del PIN. */
    val PIN_SALT = stringPreferencesKey("pin_salt")

    val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
}
