package com.bahoque.gastonomo

import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.PendingWritesGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway

/** Los adaptadores que hablan con la API, agrupados para no pasarlos de uno en uno. */
internal data class Gateways(
    val auth: AuthGateway,
    val movements: MovementGateway,
    val loans: LoanGateway,
    val categories: CategoryGateway,
    val settings: SettingsGateway,
    val currencies: CurrencyGateway,

    /** La cola de lo que se escribio sin conexion, para la pantalla que la ensena. */
    val pending: PendingWritesGateway,
)
