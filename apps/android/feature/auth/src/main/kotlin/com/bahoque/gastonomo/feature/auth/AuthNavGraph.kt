package com.bahoque.gastonomo.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bahoque.gastonomo.core.navigation.GastonomoNavActions
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.feature.auth.forgot.ForgotPasswordRoute
import com.bahoque.gastonomo.feature.auth.login.LoginRoute
import com.bahoque.gastonomo.feature.auth.register.RegisterRoute
import com.bahoque.gastonomo.feature.auth.unlock.UnlockRoute

/**
 * Las cuatro pantallas de entrada: Login, Registro, Recuperar contrasena y el desbloqueo
 * con PIN o huella.
 *
 * Cada feature aporta su grafo y `:app` solo los compone, asi que nadie edita un fichero de
 * navegacion compartido.
 *
 * Entrar **sustituye toda la pila** con [GastonomoNavActions.replaceWith]: desde Inicio, el
 * boton de atras del sistema no puede volver al Login ni al PIN.
 */
fun NavGraphBuilder.authNavGraph(
    dependencies: AuthDependencies,
    actions: GastonomoNavActions,
) {
    composable<GastonomoRoute.Login> {
        LoginRoute(
            dependencies = dependencies,
            onSignedIn = { actions.replaceWith(GastonomoRoute.Home) },
            onForgotPassword = { actions.navigateTo(GastonomoRoute.ForgotPassword(it)) },
            onRegister = { actions.navigateTo(GastonomoRoute.Register) },
            onUnlockWithLocalLock = { actions.navigateTo(GastonomoRoute.Unlock) },
        )
    }
    composable<GastonomoRoute.Register> {
        RegisterRoute(
            dependencies = dependencies,
            onRegistered = { actions.replaceWith(GastonomoRoute.Home) },
            onSignIn = { actions.replaceWith(GastonomoRoute.Login) },
            onBack = actions.back,
        )
    }
    composable<GastonomoRoute.ForgotPassword> { entry ->
        ForgotPasswordRoute(
            dependencies = dependencies,
            email = entry.toRoute<GastonomoRoute.ForgotPassword>().email,
            onBackToLogin = { actions.replaceWith(GastonomoRoute.Login) },
            onBack = actions.back,
        )
    }
    composable<GastonomoRoute.Unlock> {
        UnlockRoute(
            dependencies = dependencies,
            onUnlocked = { actions.replaceWith(GastonomoRoute.Home) },
            onUsePassword = { actions.replaceWith(GastonomoRoute.Login) },
        )
    }
}
