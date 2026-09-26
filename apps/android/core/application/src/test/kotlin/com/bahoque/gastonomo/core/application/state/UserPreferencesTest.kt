package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El idioma y la moneda principal los aplica el tema, que esta por encima de cualquier
 * ViewModel. Estos tres tests son los que sostienen que elegir English en Ajustes cambia la
 * interfaz entera y no solo la pantalla que lo guardo.
 */
class UserPreferencesTest {

    @Test
    fun `it starts with the defaults of the domain, before there is a session`() {
        assertEquals(CurrencyCode.COP, UserPreferences().settings.value.displayCurrency)
        assertEquals(AppLanguage.SPANISH, UserPreferences().settings.value.language)
    }

    @Test
    fun `reading the settings publishes them, so no screen needs an extra request`() = runTest {
        val preferences = UserPreferences()
        val gateway = FakeSettingsGateway()
        gateway.update(displayCurrency = CurrencyCode.EUR, language = AppLanguage.ENGLISH)

        LoadSettings(gateway, preferences)()

        assertEquals(CurrencyCode.EUR, preferences.settings.value.displayCurrency)
        assertEquals(AppLanguage.ENGLISH, preferences.settings.value.language)
    }

    @Test
    fun `saving publishes what the api confirmed, and a failure changes nothing`() = runTest {
        val preferences = UserPreferences()
        UpdateSettings(FakeSettingsGateway(), preferences)(language = AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, preferences.settings.value.language)

        val broken = FakeSettingsGateway(failure = ApiError.ServiceUnavailable)
        UpdateSettings(broken, preferences)(language = AppLanguage.SPANISH)
        assertEquals(AppLanguage.ENGLISH, preferences.settings.value.language)
    }
}
