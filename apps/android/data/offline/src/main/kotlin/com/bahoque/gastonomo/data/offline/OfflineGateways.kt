package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.PendingWritesGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.data.database.local.LocalStore

/**
 * Envuelve los adaptadores de red para que lean de la base local y la red solo la rellene.
 *
 * Los casos de uso reciben exactamente los mismos puertos, asi que ninguna feature se entera de
 * que ahora hay una base de datos detras.
 */
fun offlineMovementGateway(
    remote: MovementGateway,
    store: LocalStore,
    gate: RefreshGate,
    onQueued: () -> Unit = {},
): MovementGateway =
    OfflineMovementGateway(remote, store.movements, gate, OfflineQueue(store, onQueued = onQueued))

fun offlineLoanGateway(
    remote: LoanGateway,
    store: LocalStore,
    gate: RefreshGate,
    onQueued: () -> Unit = {},
): LoanGateway = OfflineLoanGateway(remote, store.loans, gate, OfflineQueue(store, onQueued = onQueued))

fun offlineCategoryGateway(
    remote: CategoryGateway,
    store: LocalStore,
    gate: RefreshGate,
): CategoryGateway = OfflineCategoryGateway(remote, store.categories, gate)

fun offlineSettingsGateway(
    remote: SettingsGateway,
    store: LocalStore,
    gate: RefreshGate,
): SettingsGateway = OfflineSettingsGateway(remote, store.account, gate)

fun offlineCurrencyGateway(
    remote: CurrencyGateway,
    store: LocalStore,
    gate: RefreshGate,
): CurrencyGateway = OfflineCurrencyGateway(remote, store.currencies, gate)

fun offlineAuthGateway(remote: AuthGateway, store: LocalStore, gate: RefreshGate): AuthGateway =
    OfflineAuthGateway(remote, store, store.account, gate)

/**
 * Quien sube lo pendiente. Recibe los adaptadores **de red sin decorar**: con los que leen de la
 * base, subir una escritura volveria a encolarla al primer corte y la cola no bajaria nunca.
 */
fun pendingUploader(
    remoteMovements: MovementGateway,
    remoteLoans: LoanGateway,
    store: LocalStore,
    gate: RefreshGate,
): PendingUploader = QueuePendingUploader(remoteMovements, remoteLoans, store, gate)

/** La cola vista desde la pantalla que la ensena y la corrige. */
fun offlinePendingWrites(store: LocalStore, uploader: PendingUploader): PendingWritesGateway =
    OfflinePendingWrites(store, uploader)
