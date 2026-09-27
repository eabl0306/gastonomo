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
import com.bahoque.gastonomo.data.database.local.createLocalStore
import com.bahoque.gastonomo.data.finance.createLoanGateway
import com.bahoque.gastonomo.data.finance.createMovementGateway
import com.bahoque.gastonomo.data.network.createApiClient
import com.bahoque.gastonomo.data.offline.PendingUploader
import com.bahoque.gastonomo.data.offline.RefreshGate
import com.bahoque.gastonomo.data.offline.offlineAuthGateway
import com.bahoque.gastonomo.data.offline.offlineCategoryGateway
import com.bahoque.gastonomo.data.offline.offlineCurrencyGateway
import com.bahoque.gastonomo.data.offline.offlineLoanGateway
import com.bahoque.gastonomo.data.offline.offlineMovementGateway
import com.bahoque.gastonomo.data.offline.offlineSettingsGateway
import com.bahoque.gastonomo.data.offline.pendingUploader
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
import com.bahoque.gastonomo.sync.PendingUploadWorker
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

    private val appContext = context.applicationContext

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

    /** La base local, que es la unica fuente de verdad de lo que se pinta. */
    private val store = createLocalStore(context)

    /**
     * Quien decide cuando vale la pena volver a pedir algo a la red. No guarda datos: eso es cosa
     * de [store]. Es uno solo para los seis adaptadores porque una escritura cruza recursos.
     */
    private val refresh = RefreshGate()

    /**
     * Cada adaptador de red va envuelto en el que lee de la base local. Los casos de uso reciben
     * el mismo puerto que antes, asi que ninguna feature se entera de que hay una base de datos
     * detras ni de que la red puede no estar.
     */
    // Los adaptadores de red sin decorar. Los guarda porque quien sube lo pendiente tiene que
    // hablar con la red directamente: con los decorados, subir una escritura la volveria a encolar
    // al primer corte y la cola no bajaria nunca.
    private val remoteMovements = createMovementGateway(apiClient)
    private val remoteLoans = createLoanGateway(apiClient)

    /** Sube lo que quedo pendiente. Lo llama el worker en cuanto hay red. */
    val uploader: PendingUploader = pendingUploader(remoteMovements, remoteLoans, store, refresh)

    private val gateways = Gateways(
        auth = offlineAuthGateway(
            createAuthGateway(apiClient, devices.sessionStore),
            store,
            refresh,
        ),
        movements = offlineMovementGateway(remoteMovements, store, refresh, ::onQueued),
        loans = offlineLoanGateway(remoteLoans, store, refresh, ::onQueued),
        categories = offlineCategoryGateway(createCategoryGateway(apiClient), store, refresh),
        settings = offlineSettingsGateway(createSettingsGateway(apiClient), store, refresh),
        currencies = offlineCurrencyGateway(createCurrencyGateway(apiClient), store, refresh),
    )

    /** Algo quedo sin subir: se pide el trabajo, que arrancara cuando haya red. */
    private fun onQueued() {
        PendingUploadWorker.schedule(appContext)
    }

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
