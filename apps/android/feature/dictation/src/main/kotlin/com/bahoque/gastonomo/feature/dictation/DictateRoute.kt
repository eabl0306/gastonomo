package com.bahoque.gastonomo.feature.dictation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import kotlinx.coroutines.delay

/**
 * El dictado montado: el permiso del microfono, la hoja y la salida.
 *
 * El permiso se pide aqui y no en el ViewModel porque necesita la actividad. Si el usuario lo
 * niega, el dictado **no se queda sin salida**: cae al modo escrito con su explicacion, igual que
 * cuando no hay microfono.
 *
 * Cerrar no saca la pantalla de la pila de golpe: primero se marca el cierre, la hoja se ve bajar
 * y solo cuando termina se vuelve atras. Sin eso la hoja desaparece de golpe.
 */
@Composable
fun DictateRoute(
    dependencies: DictationDependencies,
    onClose: () -> Unit,
    onSaved: (isLoan: Boolean) -> Unit,
    onEdit: (DictationDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DictateViewModel = viewModel { DictateViewModel(dependencies) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exitMillis = GastonomoTheme.motion.exitMillis.toLong()
    // La hoja entra en el segundo fotograma para que se vea subir. En el primero la pantalla esta
    // vacia, y como la navegacion no anima un modal, no se ve nada raro.
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { opened = true }
    RequestMicrophone(needed = state.micSupported, onDenied = { viewModel.typeInstead(SpeechFailure.PERMISSION) })
    LaunchedEffect(state.closing) {
        if (state.closing) {
            delay(exitMillis)
            onClose()
        }
    }
    DictateSheet(
        state = state,
        opened = opened,
        actions = remember(viewModel) {
            DictateActions(
                onListen = viewModel::listen,
                onStopListening = viewModel::stopListening,
                onTypeInstead = viewModel::typeInstead,
                onText = viewModel::onText,
                onInterpret = viewModel::interpret,
                onSave = { viewModel.save(onSaved) },
                onEdit = { viewModel.state.value.draft?.let(onEdit) },
                onRedo = viewModel::listen,
                onClose = viewModel::close,
            )
        },
        modifier = modifier,
    )
}

/**
 * Pide el microfono la primera vez que se abre el dictado.
 *
 * Solo se pide si no esta concedido: lanzar la peticion con el permiso ya dado hace parpadear el
 * dialogo del sistema cada vez que se abre la hoja. Y solo si el dispositivo trae reconocimiento,
 * porque pedir un permiso para algo que no se va a poder usar es molestar por nada.
 */
@Composable
private fun RequestMicrophone(needed: Boolean, onDenied: () -> Unit) {
    val context = LocalContext.current
    val granted = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed -> if (!allowed) onDenied() }
    LaunchedEffect(needed, granted) {
        if (needed && !granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
