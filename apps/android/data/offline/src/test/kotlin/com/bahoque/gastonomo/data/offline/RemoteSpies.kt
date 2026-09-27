package com.bahoque.gastonomo.data.offline

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
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import java.time.LocalDate

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
    var created: Movement = SampleFinance.groceries
    var replay: Boolean = false
    var createCalls: Int = 0
        private set

    /** Las claves con las que se llamo a create, en orden. Es lo que delata un duplicado. */
    val keys = mutableListOf<IdempotencyKey>()
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
    ): Outcome<IdempotentResult<Movement>, ApiError> {
        createCalls++
        keys += key
        return failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(IdempotentResult(created, replay))
    }
}

internal class SpyLoanGateway : LoanGateway {

    var listCalls: Int = 0
        private set
    var detailCalls: Int = 0
        private set
    var failure: ApiError? = null

    /** Los abonos que devuelve el detalle, para poder simular que la API ya tiene el nuevo. */
    var detailPayments: List<LoanPayment> = SampleFinance.partialLoan.payments

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> {
        listCalls++
        return failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(SampleFinance.loans.filter { direction == null || it.direction == direction })
    }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> {
        detailCalls++
        return failure?.let { Outcome.Failure(it) }
            ?: Outcome.Success(SampleFinance.partialLoan.copy(payments = detailPayments))
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
    var failure: ApiError? = null
    private var settings = UserSettings()

    override suspend fun load(): Outcome<UserSettings, ApiError> {
        loadCalls++
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(settings)
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
    var failure: ApiError? = null

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
        return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(SampleAuth.user)
    }
}
