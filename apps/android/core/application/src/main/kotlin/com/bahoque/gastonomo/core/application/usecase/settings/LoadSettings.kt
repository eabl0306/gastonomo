package com.bahoque.gastonomo.core.application.usecase.settings

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.application.state.UserPreferences
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * La moneda principal y el idioma de la cuenta.
 *
 * Al leerlos los publica en [preferences], asi que la interfaz entera queda en el idioma y la
 * moneda de la cuenta en cuanto cualquier pantalla los pide. Sin esto, elegir English en
 * Ajustes se guardaba en la API y no cambiaba nada de lo que se ve.
 */
class LoadSettings(
    private val gateway: SettingsGateway,
    private val preferences: UserPreferences = UserPreferences(),
) {
    suspend operator fun invoke(): Outcome<UserSettings, ApiError> =
        gateway.load().also { result -> result.valueOrNull()?.let(preferences::set) }
}
