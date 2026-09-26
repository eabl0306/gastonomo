package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.data.network.BigDecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
internal data class LoanPaymentDto(
    @SerialName("id") val id: String,
    @SerialName("loanId") val loanId: String,
    @SerialName("paidOn") val paidOn: String,
    @SerialName("label") val label: String? = null,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
) {
    fun toDomain(): LoanPayment = LoanPayment(
        id = LoanPaymentId(id),
        loanId = LoanId(loanId),
        paidOn = LocalDate.parse(paidOn),
        label = label,
        money = MoneyFields(amount, currency, base, baseCurrency).toDomain(),
    )
}

@Serializable
internal data class LoanDto(
    @SerialName("id") val id: String,
    @SerialName("person") val person: String,
    @SerialName("note") val note: String? = null,
    @SerialName("direction") val direction: String,
    @SerialName("openedOn") val openedOn: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
    @SerialName("payments") val payments: List<LoanPaymentDto> = emptyList(),
) {
    fun toDomain(): Loan = Loan(
        id = LoanId(id),
        person = person,
        note = note.orEmpty(),
        direction = LoanDirection.fromWire(direction) ?: LoanDirection.LENT,
        openedOn = LocalDate.parse(openedOn),
        money = MoneyFields(amount, currency, base, baseCurrency).toDomain(),
        payments = payments.map { it.toDomain() },
    )
}

@Serializable
internal data class LoanListDto(@SerialName("items") val items: List<LoanDto> = emptyList())

@Serializable
internal data class CreateLoanDto(
    @SerialName("person") val person: String,
    @SerialName("direction") val direction: String,
    @SerialName("openedOn") val openedOn: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
    @SerialName("note") val note: String? = null,
)

@Serializable
internal data class CreateLoanPaymentDto(
    @SerialName("paidOn") val paidOn: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("amount") val amount: BigDecimal,
    @SerialName("currency") val currency: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    @SerialName("base") val base: BigDecimal? = null,
    @SerialName("baseCurrency") val baseCurrency: String? = null,
    @SerialName("label") val label: String? = null,
)
