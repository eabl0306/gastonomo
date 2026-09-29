package com.bahoque.gastonomo.core.navigation

import kotlinx.serialization.Serializable

/**
 * Las rutas de las 14 pantallas del rediseno, como tipos serializables: navigation-compose
 * las resuelve sin cadenas ni claves de argumento.
 *
 * Viven aqui, en un modulo que no es de nadie, para que una feature pueda navegar a la
 * pantalla de otra sin depender de ella. `:feature:home` necesita ir a Movimientos y
 * `:feature:loans` a Nuevo prestamo; ninguno de los dos conoce al otro.
 *
 * ```
 * // dentro de un NavGraphBuilder de una feature
 * composable<GastonomoRoute.Movements> { MovementsRoute(...) }
 * // y para navegar
 * navController.navigate(GastonomoRoute.LoanDetail(loanId = loan.id.value))
 * ```
 *
 * La pantalla 11, Registrar abono, no esta aqui: es un bottom sheet que se abre encima de
 * Prestamos o del detalle sin navegar, tal como dice el diseno.
 */
sealed interface GastonomoRoute {

    /** 1. Login. */
    @Serializable
    data object Login : GastonomoRoute

    /** 2. Registro. */
    @Serializable
    data object Register : GastonomoRoute

    /**
     * 3. Recuperar contrasena.
     *
     * @property email el correo que el usuario ya habia escrito en Login, con el que se
     *   rellena el campo. Vacio si se llega sin haberlo escrito.
     */
    @Serializable
    data class ForgotPassword(val email: String = "") : GastonomoRoute

    /** 4. Desbloqueo con PIN o huella. */
    @Serializable
    data object Unlock : GastonomoRoute

    /** 5. Inicio. Lleva tab bar. */
    @Serializable
    data object Home : GastonomoRoute

    /** 6. Movimientos. Lleva tab bar. */
    @Serializable
    data object Movements : GastonomoRoute

    /**
     * 7. Nuevo movimiento.
     *
     * Los campos de relleno vienen vacios salvo cuando se llega desde el dictado, que abre el
     * formulario con lo que se entendio para poder corregirlo. Van sueltos y como texto porque
     * asi lo quiere navigation-compose: son argumentos de una ruta, no un objeto de dominio.
     *
     * @property kind el tipo con el que se abre el formulario: `out` desde el FAB y desde
     *   la accion rapida Gasto, `in` desde la accion rapida Ingreso. Es el `wire` de
     *   [com.bahoque.gastonomo.core.domain.movement.MovementKind].
     * @property amount importe con punto decimal y sin separador de miles.
     * @property currency codigo ISO 4217. Vacio significa la moneda principal de la cuenta.
     * @property categoryId la categoria ya elegida.
     * @property occurredOn la fecha en `YYYY-MM-DD`. Vacia significa hoy.
     */
    @Serializable
    data class NewMovement(
        val kind: String,
        val amount: String = "",
        val currency: String = "",
        val categoryId: String = "",
        val note: String = "",
        val occurredOn: String = "",
    ) : GastonomoRoute

    /** 8. Prestamos. Lleva tab bar. */
    @Serializable
    data object Loans : GastonomoRoute

    /** 9. Detalle de prestamo. */
    @Serializable
    data class LoanDetail(val loanId: String) : GastonomoRoute

    /**
     * 10. Nuevo prestamo.
     *
     * Igual que [NewMovement]: los campos vienen vacios salvo cuando se llega desde el dictado.
     *
     * @property direction `lent` o `borrowed`. Vacio abre el formulario como esta por defecto.
     */
    @Serializable
    data class NewLoan(
        val person: String = "",
        val direction: String = "",
        val amount: String = "",
        val currency: String = "",
        val note: String = "",
        val openedOn: String = "",
    ) : GastonomoRoute

    /**
     * Dictar un registro.
     *
     * Es una pantalla de la pila y no una hoja dentro de otra, aunque se vea como un modal,
     * porque la abren tres pantallas distintas. Como hoja habria que montarla en las tres y
     * las tres tendrian que conocer la feature del dictado.
     */
    @Serializable
    data object Dictate : GastonomoRoute

    /** 12. Ajustes. Lleva tab bar. */
    @Serializable
    data object Settings : GastonomoRoute

    /** 13. Categorias. */
    @Serializable
    data object Categories : GastonomoRoute

    /** 14. Exportar datos. */
    @Serializable
    data object Export : GastonomoRoute

    /**
     * Registros sin enviar. No esta en el rediseno: sale de que registrar sin conexion sea
     * posible, y hace falta porque un registro que la API rechaza no puede desaparecer en
     * silencio.
     */
    @Serializable
    data object PendingWrites : GastonomoRoute
}
