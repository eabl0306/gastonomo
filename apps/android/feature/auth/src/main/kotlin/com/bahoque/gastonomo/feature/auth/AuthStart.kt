package com.bahoque.gastonomo.feature.auth

import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import kotlinx.coroutines.flow.first

/**
 * Con que pantalla arranca la aplicacion. Son tres casos y los decide este modulo porque
 * las tres puertas son suyas:
 *
 * - sin sesion guardada, **Login**;
 * - con sesion y el cerrojo local encendido, **desbloqueo con PIN o huella**;
 * - con sesion y sin cerrojo, **Inicio**.
 *
 * El cerrojo es local: protege el refresh token guardado en el dispositivo, no se manda a
 * la API y no se sincroniza, asi que el mismo usuario puede tener cerrojo en un telefono y
 * no en otro.
 */
suspend fun authStartDestination(dependencies: AuthDependencies): GastonomoRoute {
    if (!dependencies.observeSignedIn().first()) return GastonomoRoute.Login
    val locked = dependencies.observeDeviceSettings().first().lockEnabled
    return if (locked) GastonomoRoute.Unlock else GastonomoRoute.Home
}
