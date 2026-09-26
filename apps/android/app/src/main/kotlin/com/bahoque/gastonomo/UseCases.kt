package com.bahoque.gastonomo

import com.bahoque.gastonomo.core.application.state.UserPreferences
import com.bahoque.gastonomo.core.application.usecase.auth.ChangePassword
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveSignedIn
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveStoredUser
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
import com.bahoque.gastonomo.core.application.usecase.auth.SetUpLocalLock
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.core.application.usecase.category.DeleteCategory
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.category.SaveCategory
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoanDetail
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.loan.RegisterLoanPayment
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.movement.LoadRecentMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.SetHideAmounts
import com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings

/**
 * Todos los casos de uso, construidos una sola vez. Los ficheros de `wiring` reparten de
 * aqui lo que necesita cada feature.
 *
 * Es inyeccion manual a proposito, sin Hilt: con un solo grafo cabe en un fichero, no
 * arrastra procesamiento de anotaciones al build y no obliga a excluir codigo generado del
 * calculo de cobertura.
 */
internal class UseCases(
    gateways: Gateways,
    devices: DeviceAdapters,
    preferences: UserPreferences,
) {

    val signIn = SignIn(gateways.auth, devices.sessionStore)
    val signUp = SignUp(gateways.auth, devices.sessionStore)
    val signOut = SignOut(gateways.auth, devices.sessionStore, devices.localLock)
    val requestPasswordReset = RequestPasswordReset(gateways.auth)
    val changePassword = ChangePassword(gateways.auth)
    val observeSignedIn = ObserveSignedIn(devices.sessionStore)
    val observeStoredUser = ObserveStoredUser(devices.sessionStore)
    val loadProfile = LoadProfile(gateways.auth)
    val unlockWithPin = UnlockWithPin(devices.localLock)
    val unlockWithBiometrics = UnlockWithBiometrics(devices.biometricLock)
    val setUpLocalLock = SetUpLocalLock(devices.localLock, devices.deviceSettings)

    val loadPeriodMovements = LoadPeriodMovements(gateways.movements)
    val loadRecentMovements = LoadRecentMovements(gateways.movements)
    val createMovement = CreateMovement(gateways.movements)

    val loadLoans = LoadLoans(gateways.loans)
    val loadLoanDetail = LoadLoanDetail(gateways.loans)
    val createLoan = CreateLoan(gateways.loans)
    val registerLoanPayment = RegisterLoanPayment(gateways.loans)

    val loadCategories = LoadCategories(gateways.categories)
    val saveCategory = SaveCategory(gateways.categories)
    val deleteCategory = DeleteCategory(gateways.categories)

    val loadSettings = LoadSettings(gateways.settings, preferences)
    val updateSettings = UpdateSettings(gateways.settings, preferences)
    val observeDeviceSettings = ObserveDeviceSettings(devices.deviceSettings)
    val setHideAmounts = SetHideAmounts(devices.deviceSettings)

    val loadCurrencyCatalog = LoadCurrencyCatalog(gateways.currencies)
    val loadExchangeRates = LoadExchangeRates(gateways.currencies)

    val clock = devices.clock
}
