package com.bahoque.gastonomo.core.designsystem

import androidx.annotation.StringRes
import com.bahoque.gastonomo.core.domain.settings.AppLanguage

/**
 * Los textos que comparten varias pantallas, por su identificador de recurso.
 *
 * Se usan asi, y no por `R.string.gastonomo_...`, para que una feature no tenga que saber
 * en que modulo viven:
 * ```
 * GastonomoText(stringResource(GastonomoStrings.actionRetry))
 * ```
 *
 * Los textos propios de una pantalla van en el `res/values` de su feature, no aqui: asi
 * dos personas que escriben pantallas distintas nunca editan el mismo fichero.
 */
object GastonomoStrings {

    /** La palabra "gastonomo" del logo. No se traduce. */
    @StringRes val wordmark = R.string.gastonomo_wordmark

    /** El pie de Ajustes. Lleva la version como argumento. */
    @StringRes val footer = R.string.gastonomo_footer

    /** Por que el chip de moneda esta apagado sin conexion. */
    @StringRes val offlineCurrency = R.string.gastonomo_offline_currency

    /** La marca de un registro que todavia no ha salido del telefono. */
    @StringRes val pendingUpload = R.string.gastonomo_pending_upload

    @StringRes val tabHome = R.string.gastonomo_tab_home

    @StringRes val tabMovements = R.string.gastonomo_tab_movements

    @StringRes val tabLoans = R.string.gastonomo_tab_loans

    @StringRes val tabSettings = R.string.gastonomo_tab_settings

    @StringRes val actionSave = R.string.gastonomo_action_save

    @StringRes val actionCancel = R.string.gastonomo_action_cancel

    @StringRes val actionBack = R.string.gastonomo_action_back

    @StringRes val actionClose = R.string.gastonomo_action_close

    @StringRes val actionRetry = R.string.gastonomo_action_retry

    @StringRes val actionApply = R.string.gastonomo_action_apply

    @StringRes val actionDelete = R.string.gastonomo_action_delete

    @StringRes val actionEdit = R.string.gastonomo_action_edit

    @StringRes val actionSeeAll = R.string.gastonomo_action_see_all

    /** "(opcional)", el sufijo en faint del label de un campo que se puede dejar vacio. */
    @StringRes val fieldOptional = R.string.gastonomo_field_optional

    @StringRes val kindIncome = R.string.gastonomo_kind_income

    @StringRes val kindExpense = R.string.gastonomo_kind_expense

    @StringRes val filterAll = R.string.gastonomo_filter_all

    @StringRes val directionOwedToMe = R.string.gastonomo_direction_owed_to_me

    @StringRes val directionIOwe = R.string.gastonomo_direction_i_owe

    @StringRes val loanStatusPaid = R.string.gastonomo_loan_status_paid

    @StringRes val loanStatusPartial = R.string.gastonomo_loan_status_partial

    @StringRes val loanStatusPending = R.string.gastonomo_loan_status_pending

    /** El encabezado del grupo de hoy en Movimientos. */
    @StringRes val dayToday = R.string.gastonomo_day_today

    /** El encabezado del grupo de ayer en Movimientos. */
    @StringRes val dayYesterday = R.string.gastonomo_day_yesterday

    @StringRes val errorNetwork = R.string.gastonomo_error_network

    @StringRes val errorUnexpected = R.string.gastonomo_error_unexpected

    @StringRes val errorSessionExpired = R.string.gastonomo_error_session_expired

    /** Lo que se ensena mientras `GET /v1/exchange-rates` responde 503. */
    @StringRes val errorRatesUnavailable = R.string.gastonomo_error_rates_unavailable

    /**
     * 409 `idempotency_key_in_progress`: el mismo intento esta en vuelo. La API pide esperar y
     * repetir, y el texto es el mismo en los tres formularios que crean dinero.
     */
    @StringRes val errorInProgress = R.string.gastonomo_error_in_progress

    /** 409 `idempotency_key_reused`: cambio el cuerpo, asi que hace falta una clave nueva. */
    @StringRes val errorBodyChanged = R.string.gastonomo_error_body_changed

    @StringRes val loading = R.string.gastonomo_loading

    @StringRes val biometricTitle = R.string.gastonomo_biometric_title

    @StringRes val biometricSubtitle = R.string.gastonomo_biometric_subtitle

    @StringRes val biometricUsePin = R.string.gastonomo_biometric_use_pin

    /**
     * El nombre de un idioma en su propia lengua. Lo ensenan el sheet de idioma y la fila
     * "Idioma" de Ajustes, y no se traduce: quien busca "English" no sabe leer "Ingles".
     */
    @StringRes
    fun nameOf(language: AppLanguage): Int = when (language) {
        AppLanguage.SPANISH -> R.string.gastonomo_language_spanish
        AppLanguage.ENGLISH -> R.string.gastonomo_language_english
    }
}
