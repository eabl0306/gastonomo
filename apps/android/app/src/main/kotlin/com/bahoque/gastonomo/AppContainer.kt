package com.bahoque.gastonomo

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.application.port.SystemClock
import com.bahoque.gastonomo.core.application.state.UserPreferences
import com.bahoque.gastonomo.data.auth.createAuthGateway
import com.bahoque.gastonomo.data.catalog.createCategoryGateway
import com.bahoque.gastonomo.data.catalog.createCurrencyGateway
import com.bahoque.gastonomo.data.catalog.createSettingsGateway
import com.bahoque.gastonomo.data.finance.createLoanGateway
import com.bahoque.gastonomo.data.finance.createMovementGateway
import com.bahoque.gastonomo.data.network.createApiClient
import com.bahoque.gastonomo.data.session.createBiometricLock
import com.bahoque.gastonomo.data.session.createDeviceSettingsStore
import com.bahoque.gastonomo.data.session.createLocalLock
import com.bahoque.gastonomo.data.session.createSessionStore
import com.bahoque.gastonomo.feature.auth.AuthDependencies
import com.bahoque.gastonomo.feature.categories.CategoriesDependencies
import com.bahoque.gastonomo.feature.export.ExportDependencies
import com.bahoque.gastonomo.feature.home.HomeDependencies
import com.bahoque.gastonomo.feature.loans.LoansDependencies
import com.bahoque.gastonomo.feature.movements.MovementsDependencies
import com.bahoque.gastonomo.feature.settings.SettingsDependencies
import com.bahoque.gastonomo.wiring.authDependencies
import com.bahoque.gastonomo.wiring.categoriesDependencies
import com.bahoque.gastonomo.wiring.exportDependencies
import com.bahoque.gastonomo.wiring.homeDependencies
import com.bahoque.gastonomo.wiring.loansDependencies
import com.bahoque.gastonomo.wiring.movementsDependencies
import com.bahoque.gastonomo.wiring.settingsDependencies
import com.bahoque.gastonomo.wiring.sharedUserPreferences

/**
 * Raiz de composicion. Es el unico sitio del proyecto donde los adaptadores de `:data` se
 * unen a los casos de uso: ninguna feature ni el nucleo conocen a `:data`.
 *
 * Cada feature recibe su propio objeto de dependencias, construido en su fichero de
 * `wiring`. Asi ninguna feature depende de otra y anadir una dependencia toca un solo
 * fichero.
 */
class AppContainer(context: Context) {

    /**
     * La actividad viva, que el dialogo de huella necesita. La registra [MainActivity] al
     * crearse y la borra al destruirse, para no retener una actividad muerta.
     */
    var currentActivity: FragmentActivity? = null

    private val devices = DeviceAdapters(
        sessionStore = createSessionStore(context),
        localLock = createLocalLock(context),
        deviceSettings = createDeviceSettingsStore(context),
        biometricLock = createBiometricLock(context) { currentActivity },
        clock = SystemClock,
    )

    private val apiClient = createApiClient(devices.sessionStore)

    private val gateways = Gateways(
        auth = createAuthGateway(apiClient, devices.sessionStore),
        movements = createMovementGateway(apiClient),
        loans = createLoanGateway(apiClient),
        categories = createCategoryGateway(apiClient),
        settings = createSettingsGateway(apiClient),
        currencies = createCurrencyGateway(apiClient),
    )

    private val useCases = UseCases(gateways, devices, sharedUserPreferences)

    /**
     * El idioma y la moneda principal de la cuenta, que el tema aplica a toda la interfaz.
     * Los casos de uso de ajustes lo rellenan en cuanto una pantalla lee los ajustes.
     */
    val preferences: UserPreferences = sharedUserPreferences

    /** La sesion guardada, que decide con que pantalla arranca la aplicacion. */
    val sessionStore: SessionStore get() = devices.sessionStore

    val auth: AuthDependencies = authDependencies(useCases)
    val home: HomeDependencies = homeDependencies(useCases)
    val movements: MovementsDependencies = movementsDependencies(useCases)
    val loans: LoansDependencies = loansDependencies(useCases)
    val settings: SettingsDependencies = settingsDependencies(useCases)
    val categories: CategoriesDependencies = categoriesDependencies(useCases)
    val export: ExportDependencies = exportDependencies(useCases)
}
