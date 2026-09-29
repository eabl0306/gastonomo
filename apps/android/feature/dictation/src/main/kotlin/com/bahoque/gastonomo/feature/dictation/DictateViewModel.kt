package com.bahoque.gastonomo.feature.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.application.port.SpeechEvent
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.application.port.SpeechSession
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El dictado: escuchar, mandar el texto a la API y ensenar el borrador para revisar.
 *
 * Empieza escuchando si el dispositivo trae reconocimiento y en modo escrito si no. **Ningun
 * fallo del microfono deja sin salida**: los cinco caen al modo escrito con su explicacion, y
 * desde ahi se puede escribir el registro y se interpreta igual.
 *
 * Lo que se guarda no lo decide esta clase: [DictationSaver] pasa por los mismos casos de uso
 * que los formularios, con su clave de idempotencia.
 */
class DictateViewModel(private val dependencies: DictationDependencies) : ViewModel() {

    private val saver = DictationSaver(dependencies)
    private var listening: Job? = null
    private var session: SpeechSession? = null
    private var language: AppLanguage = AppLanguage.DEFAULT

    private val supported = dependencies.speechToText.isAvailable()

    private val mutable = MutableStateFlow(
        DictateUiState(
            phase = if (supported) DictationPhase.LISTENING else DictationPhase.TYPING,
            micFailure = if (supported) null else SpeechFailure.UNAVAILABLE,
            micSupported = supported,
        ),
    )

    val state: StateFlow<DictateUiState> = mutable.asStateFlow()

    init {
        // El idioma se carga antes de abrir el microfono, no en paralelo: el sistema necesita
        // saber en que se le va a hablar, y pedirselo en castellano a quien tiene la cuenta en
        // ingles lo hace fallar en silencio.
        viewModelScope.launch {
            val context = DictationContextLoader(dependencies).load()
            language = context.language
            mutable.update {
                it.copy(displayCurrency = context.displayCurrency, rates = context.rates)
            }
            if (mutable.value.phase == DictationPhase.LISTENING) listen()
        }
    }

    /**
     * Arranca el microfono. Se llama al abrir la hoja y cada vez que el usuario lo pulsa, asi que
     * cancela lo anterior: dos reconocimientos a la vez se pelean por el microfono.
     */
    fun listen() {
        stopRecognizer()
        mutable.update {
            it.copy(
                phase = DictationPhase.LISTENING,
                heard = "",
                text = "",
                micFailure = null,
                problem = null,
                draft = null,
            )
        }
        val opened = dependencies.speechToText.start(language.localeTag)
        session = opened
        listening = viewModelScope.launch { opened.events.collect(::onSpeech) }
    }

    /**
     * Termina de escuchar.
     *
     * **No cancela**: le pide al sistema su resultado definitivo, que llega como un `Final` y es
     * mejor que el ultimo parcial. Si no entendio nada contestara con un fallo, y eso ya lleva al
     * modo escrito.
     */
    fun stopListening() {
        session?.finish()
    }

    /**
     * Pasa al modo escrito, con lo que se hubiera entendido ya puesto en el campo.
     *
     * @param reason por que se pasa, o null si lo pidio el usuario. **Negar el permiso no es lo
     *   mismo que preferir escribir**: ahi el motivo se ensena, porque el usuario lo puede cambiar,
     *   y un campo que aparece sin explicacion deja sin saber por que no se escucho.
     */
    fun typeInstead(reason: SpeechFailure? = null) {
        stopRecognizer()
        mutable.update {
            it.copy(phase = DictationPhase.TYPING, micFailure = reason, text = it.heard)
        }
    }

    fun onText(text: String) {
        mutable.update { it.copy(text = text, problem = null) }
    }

    /** Manda el texto a la API. Un texto en blanco no se manda. */
    fun interpret(text: String = mutable.value.text) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        stopRecognizer()
        mutable.update {
            it.copy(phase = DictationPhase.PROCESSING, text = clean, problem = null, draft = null)
        }
        viewModelScope.launch {
            when (val result = dependencies.parseDictation(clean)) {
                is Outcome.Success -> mutable.update {
                    it.copy(phase = DictationPhase.REVIEW, draft = result.value)
                }
                is Outcome.Failure -> mutable.update {
                    it.copy(
                        phase = DictationPhase.TYPING,
                        problem = DictationProblem.ofParse(result.error),
                    )
                }
            }
        }
    }

    /**
     * Guarda el borrador. Marca [DictateUiState.closing] al terminar: la hoja se ve bajar y quien
     * la pinta la saca de la pila cuando acaba de salir.
     */
    fun save(onSaved: (isLoan: Boolean) -> Unit) {
        val current = mutable.value
        val draft = current.draft
        val money = current.money
        if (!current.canSave || draft == null || money == null) return
        mutable.update { it.copy(saving = true, problem = null) }
        viewModelScope.launch {
            when (val result = saver.save(draft, money)) {
                is DictationSaveResult.Saved -> {
                    mutable.update { it.copy(saving = false, closing = true) }
                    if (result.announce) onSaved(result.isLoan)
                }
                is DictationSaveResult.Failed -> mutable.update {
                    it.copy(saving = false, problem = result.problem)
                }
            }
        }
    }

    /** Empieza a cerrar la hoja. */
    fun close() {
        stopRecognizer()
        mutable.update { it.copy(closing = true) }
    }

    override fun onCleared() {
        stopRecognizer()
    }

    /** Corta la escucha y suelta el microfono. Cancelar el flujo es lo que lo libera. */
    private fun stopRecognizer() {
        listening?.cancel()
        listening = null
        session = null
    }

    private fun onSpeech(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Partial -> mutable.update { it.copy(heard = event.text) }
            is SpeechEvent.Final -> {
                // El flujo ya se cerro solo: soltar la referencia aqui evita que interpret intente
                // cancelar la corrutina desde la que se le esta llamando.
                listening = null
                session = null
                val heard = event.text.trim().ifEmpty { mutable.value.heard.trim() }
                if (heard.isEmpty()) onSpeechFailure(SpeechFailure.NO_MATCH) else interpret(heard)
            }
            is SpeechEvent.Failed -> onSpeechFailure(event.reason)
        }
    }

    /**
     * Cualquier fallo del microfono cae al modo escrito, con lo que se hubiera entendido ya
     * puesto en el campo: que el usuario tenga que repetirlo entero seria el peor final.
     */
    private fun onSpeechFailure(reason: SpeechFailure) {
        listening = null
        session = null
        mutable.update {
            it.copy(phase = DictationPhase.TYPING, micFailure = reason, text = it.heard)
        }
    }

    companion object {
        fun factory(dependencies: DictationDependencies): ViewModelProvider.Factory =
            viewModelFactory { initializer { DictateViewModel(dependencies) } }
    }
}
