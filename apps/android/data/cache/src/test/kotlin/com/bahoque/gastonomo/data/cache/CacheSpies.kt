package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.time.Duration

/** Un reloj que el test mueve a mano, para envejecer una entrada sin esperar. */
internal class TestTicker(private var now: Long = 0L) : CacheTicker {

    override fun nowMillis(): Long = now

    fun advance(by: Duration) {
        now += by.inWholeMilliseconds
    }
}

/**
 * Espias que cuentan cuantas veces se llamo a la red. Son lo que hace comprobable lo unico
 * que promete la cache: que volver a una pantalla no vuelve a pedir lo mismo.
 *
 * Cada uno lleva un `failure` que el test puede encender para probar que un fallo no se
 * guarda, y los de lectura devuelven un valor que se puede cambiar entre llamadas para
 * distinguir "me sirvio lo guardado" de "salio a la red".
 */
internal class SpyMovementGateway : MovementGateway {

    var listCalls: Int = 0
        private set
    var page: MovementPage = MovementPage(SampleFinance.movements, null)
    var failure: ApiError? = null

    override suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind?,
        cursor: String?,
        limit: Int?,
    ): Outcome<MovementPage, ApiError> {
        listCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(page)
    }

    override suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> =
        failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(IdempotentResult(SampleFinance.groceries, false))
}

internal class SpyLoanGateway : LoanGateway {

    var listCalls: Int = 0
        private set
    var detailCalls: Int = 0
        private set
    var failure: ApiError? = null

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> {
        listCalls++
        return failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(SampleFinance.loans.filter { direction == null || it.direction == direction })
    }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> {
        detailCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.partialLoan)
    }

    override suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> =
        failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(IdempotentResult(SampleFinance.partialLoan, false))

    override suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> =
        failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(IdempotentResult(SampleFinance.payment, false))
}

internal class SpyCategoryGateway : CategoryGateway {

    var listCalls: Int = 0
        private set
    var failure: ApiError? = null

    override suspend fun list(): Outcome<List<Category>, ApiError> {
        listCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.categories)
    }

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.pets)

    override suspend fun update(id: CategoryId, draft: CategoryDraft): Outcome<Category, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.pets)

    override suspend fun delete(id: CategoryId): Outcome<Unit, ApiError> =
        failure?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
}

internal class SpySettingsGateway : SettingsGateway {

    var loadCalls: Int = 0
        private set
    private var settings = UserSettings()

    override suspend fun load(): Outcome<UserSettings, ApiError> {
        loadCalls++
        return Outcome.Success(settings)
    }

    override suspend fun update(
        displayCurrency: CurrencyCode?,
        language: AppLanguage?,
    ): Outcome<UserSettings, ApiError> {
        settings = settings.copy(
            displayCurrency = displayCurrency ?: settings.displayCurrency,
            language = language ?: settings.language,
        )
        return Outcome.Success(settings)
    }
}

internal class SpyCurrencyGateway : CurrencyGateway {

    var catalogCalls: Int = 0
        private set
    var ratesCalls: Int = 0
        private set
    var failure: ApiError? = null

    override suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError> {
        catalogCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.currencies)
    }

    override suspend fun rates(): Outcome<ExchangeRates, ApiError> {
        ratesCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleFinance.rates)
    }
}

internal class SpyAuthGateway : AuthGateway {

    var profileCalls: Int = 0
        private set

    override suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError> =
        Outcome.Success(SampleAuth.session)

    override suspend fun login(email: String, password: String): Outcome<Session, ApiError> =
        Outcome.Success(SampleAuth.session)

    override suspend fun logout(): Outcome<Unit, ApiError> = Outcome.Success(Unit)

    override suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError> =
        Outcome.Success(Unit)

    override suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError> =
        Outcome.Success(Unit)

    override suspend fun profile(): Outcome<User, ApiError> {
        profileCalls++
        return Outcome.Success(SampleAuth.user)
    }
}

/**
 * Los seis decoradores sobre una sola instancia de caches, igual que quedan en la raiz de
 * composicion. Tienen que compartirla para poder comprobar la invalidacion que cruza
 * recursos: un movimiento nuevo deja desfasada tambien la lista de categorias.
 */
internal class WiredCaches(scope: CoroutineScope, ticker: CacheTicker = TestTicker()) {

    val movementSpy = SpyMovementGateway()
    val loanSpy = SpyLoanGateway()
    val categorySpy = SpyCategoryGateway()
    val settingsSpy = SpySettingsGateway()
    val currencySpy = SpyCurrencyGateway()
    val authSpy = SpyAuthGateway()

    private val caches = GatewayCaches(ticker = ticker, scope = scope)

    val movements: MovementGateway = cachedMovementGateway(movementSpy, caches)
    val loans: LoanGateway = cachedLoanGateway(loanSpy, caches)
    val categories: CategoryGateway = cachedCategoryGateway(categorySpy, caches)
    val settings: SettingsGateway = cachedSettingsGateway(settingsSpy, caches)
    val currencies: CurrencyGateway = cachedCurrencyGateway(currencySpy, caches)
    val auth: AuthGateway = cachedAuthGateway(authSpy, caches)
}

/** Lo minimo que hay que mandar para crear algo. El contenido no importa aqui. */
internal object Drafts {

    val movement = MovementDraft(
        occurredOn = LocalDate.parse("2026-09-22"),
        kind = MovementKind.OUT,
        categoryId = SampleFinance.food.id,
        money = MonetaryAmount.single(BigDecimal("1000"), CurrencyCode.COP),
    )

    val loan = LoanDraft(
        person = "Rosa",
        direction = LoanDirection.LENT,
        openedOn = LocalDate.parse("2026-09-22"),
        money = MonetaryAmount.single(BigDecimal("1000"), CurrencyCode.COP),
    )

    val payment = LoanPaymentDraft(
        paidOn = LocalDate.parse("2026-09-22"),
        money = MonetaryAmount.single(BigDecimal("500"), CurrencyCode.COP),
    )

    val category = CategoryDraft(name = "Mascotas", kind = CategoryKind.OUT, icon = "pets", color = "#2f7fb5")

    val key = IdempotencyKey("key-1")
}
