package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.common.DefaultDispatcherProvider
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.time.LocalDate

/**
 * Todas las caches de la aplicacion y las reglas de que invalida que.
 *
 * Estan juntas porque la invalidacion **cruza recursos**: registrar un movimiento cambia la
 * lista de movimientos y tambien el contador de uso que devuelve `GET /v1/categories`, y
 * crear un prestamo o un abono crea ademas su propio movimiento. Si cada decorador tuviera su
 * cache privada esas reglas quedarian repartidas, y la primera que alguien olvidara dejaria
 * una pantalla ensenando algo que ya no es verdad.
 *
 * Vive tanto como el proceso, igual que la raiz de composicion que la construye.
 *
 * @param scope donde corren las revalidaciones en segundo plano. Es un [SupervisorJob] para
 *   que un fallo de una no cancele las demas.
 */
class GatewayCaches(
    private val ticker: CacheTicker = ElapsedTicker,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io),
) {

    /**
     * Se rellena sola al construir cada cache, para que [clear] no pueda dejarse una fuera.
     * Declarada antes que las caches a proposito: las propiedades se inicializan en orden.
     */
    private val registry = mutableListOf<MemoryCache<*, *>>()

    internal val movements = cache<MovementQuery, MovementPage>(CachePolicy.WRITEABLE)
    internal val loans = cache<LoanDirection?, List<Loan>>(CachePolicy.WRITEABLE)
    internal val loanDetail = cache<LoanId, Loan>(CachePolicy.WRITEABLE)
    internal val categories = cache<Unit, List<Category>>(CachePolicy.WRITEABLE)
    internal val settings = cache<Unit, UserSettings>(CachePolicy.WRITEABLE)
    internal val profile = cache<Unit, User>(CachePolicy.WRITEABLE)
    internal val currencies = cache<Unit, List<CurrencyInfo>>(CachePolicy.CATALOG)
    internal val rates = cache<Unit, ExchangeRates>(CachePolicy.CATALOG)

    /**
     * Lo que queda desfasado al crear dinero: el movimiento, el prestamo que lo genero y el
     * contador de uso de la categoria.
     */
    internal suspend fun invalidateAfterMoneyWrite() {
        movements.invalidate()
        loans.invalidate()
        loanDetail.invalidate()
        categories.invalidate()
    }

    /**
     * Lo que queda desfasado al crear, editar o borrar una categoria. Los movimientos entran
     * porque borrar una categoria reasigna los suyos a Otros, asi que una lista ya cargada se
     * quedaria pintando el tile de una categoria que no existe.
     */
    internal suspend fun invalidateAfterCategoryWrite() {
        categories.invalidate()
        movements.invalidate()
    }

    /**
     * Todo, porque cambio la cuenta. Sin esto, quien entra despues de que otro cierre sesion
     * en el mismo proceso veria los movimientos del anterior.
     *
     * Tira tambien el catalogo de monedas y las tasas, que no son de nadie en particular. Es
     * una peticion de mas al entrar, y a cambio no hay que razonar sobre de quien es lo que
     * queda en memoria.
     */
    internal suspend fun clear() {
        registry.forEach { it.invalidate() }
    }

    private fun <K, V : Any> cache(policy: CachePolicy): MemoryCache<K, V> =
        MemoryCache<K, V>(policy, ticker, scope).also { registry += it }
}

/**
 * La clave de una pagina de movimientos: los cinco parametros de la consulta. El periodo
 * entra en la clave porque cada mes es una respuesta distinta, y el limite tambien porque
 * Inicio pide los cuatro ultimos y Movimientos pide el periodo entero.
 */
internal data class MovementQuery(
    val from: LocalDate,
    val to: LocalDate,
    val kind: MovementKind?,
    val cursor: String?,
    val limit: Int?,
)
