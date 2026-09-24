package com.bahoque.gastonomo

import androidx.lifecycle.ViewModelProvider
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.application.health.usecase.CheckApiHealth
import com.bahoque.gastonomo.core.common.DefaultDispatcherProvider
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.data.health.createHealthGateway
import com.bahoque.gastonomo.feature.health.HealthViewModel

/**
 * Raiz de composicion. Es el unico sitio del proyecto donde el adaptador de datos se une
 * al caso de uso: ni la feature ni el nucleo conocen a :data:health.
 *
 * Se usa inyeccion manual en vez de Hilt porque con un solo grafo cabe en este fichero,
 * no arrastra procesamiento de anotaciones al build y no obliga a excluir codigo generado
 * del calculo de cobertura.
 */
class AppContainer(
    healthGateway: HealthGateway = createHealthGateway(),
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) {
    private val checkApiHealth = CheckApiHealth(healthGateway)

    val healthViewModelFactory: ViewModelProvider.Factory =
        HealthViewModel.Factory(checkApiHealth, dispatchers)
}
