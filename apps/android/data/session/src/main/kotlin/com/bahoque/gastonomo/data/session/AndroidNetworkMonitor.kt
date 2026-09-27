package com.bahoque.gastonomo.data.session

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.bahoque.gastonomo.core.application.port.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * La conectividad del dispositivo, del sistema.
 *
 * Mira `NET_CAPABILITY_VALIDATED` y no solo que haya una red: un wifi de cafeteria con portal
 * cautivo esta conectado y no llega a ninguna parte, y para el usuario eso es estar sin conexion.
 *
 * El flujo emite el estado actual al suscribirse y despues solo cuando cambia, para que la fila de
 * Ajustes no parpadee con cada cambio de celda.
 */
internal class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {

    override fun connected(): Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(manager.isValidated())
            }

            override fun onLost(network: Network) {
                trySend(manager.isValidated())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
        }
        trySend(manager.isValidated())
        manager.registerNetworkCallback(request(), callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    private fun request() = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
}

/** True cuando la red activa no solo existe, sino que el sistema comprobo que sale a internet. */
private fun ConnectivityManager.isValidated(): Boolean =
    getNetworkCapabilities(activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

/** La fabrica que llama el contenedor de la aplicacion. */
fun createNetworkMonitor(context: Context): NetworkMonitor = AndroidNetworkMonitor(context)
