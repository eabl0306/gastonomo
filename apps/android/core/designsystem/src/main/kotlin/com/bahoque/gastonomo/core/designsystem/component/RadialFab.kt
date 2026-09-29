package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El boton de registro rapido: uno solo que abre un menu en arco con hasta tres opciones.
 *
 * Se maneja de dos formas y las dos tienen que funcionar sin pensar:
 *
 * - **Mantener y deslizar.** El menu se abre en cuanto el dedo toca, sin esperar a un
 *   temporizador, y la opcion se elige por el angulo hacia el que apunta el dedo. Soltar sobre
 *   una la ejecuta; soltar fuera cierra.
 * - **Tocar y tocar.** Un toque limpio deja el menu abierto para elegir con un segundo toque.
 *
 * Lo que hace que las dos convivan es el umbral de arrastre: por debajo de el, levantar el dedo
 * fue un toque y el menu se queda; por encima, fue un gesto y se cierra.
 *
 * Se pinta **dentro del Box de la pantalla y ocupandolo entero**, porque el velo que oscurece lo
 * de detras tiene que cubrirlo todo. El boton se coloca solo abajo a la derecha.
 *
 * Mientras el menu esta abierto cuenta como hoja abierta, asi que la tab bar se vela y deja de
 * responder igual que con un bottom sheet: sin eso se podria cambiar de pestana con el menu
 * puesto, y el velo de la pantalla dejaria de significar nada.
 */
@Composable
fun BoxScope.GastonomoRadialFab(
    options: List<RadialOption>,
    closeLabel: String,
    openLabel: String,
) {
    val state = remember { RadialFabState() }
    val density = LocalDensity.current
    val dimens = GastonomoTheme.dimens
    val reach = with(density) { dimens.radialReach.toPx() }
    // Las opciones se alinean por la esquina, igual que el boton, y son mas pequenas: esto las
    // recoloca para que salgan del centro del boton y no de su esquina.
    val centering = with(density) { ((dimens.fabRadial - dimens.radialOption) / 2).toPx() }
    val metrics = RadialMetrics(
        center = with(density) { (dimens.fabRadial / 2).toPx() },
        selection = with(density) { dimens.radialSelection.toPx() },
        count = options.size,
    )

    DimTabBarWhileOpen(state.open)
    RadialScrim(open = state.open, onDismiss = state::close)

    // La caja tiene que cubrir el arco entero, no solo el boton: **un hijo que se sale de su
    // padre se pinta pero no se puede tocar**. Con una caja del tamano del boton, las opciones se
    // veian salir y el toque se colaba al velo, asi que el menu de dos toques no elegia nada.
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = FAB_END, bottom = FAB_BOTTOM)
            .size(dimens.radialReach + dimens.fabRadial / 2 + dimens.radialOption / 2),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Options(state = state, options = options, reach = reach, centering = centering)
        RadialTrigger(
            state = state,
            options = options,
            metrics = metrics,
            closeLabel = closeLabel,
            openLabel = openLabel,
        )
    }
}

/** El velo que oscurece la pantalla mientras el menu esta abierto. Tocarlo lo cierra. */
@Composable
private fun BoxScope.RadialScrim(open: Boolean, onDismiss: () -> Unit) {
    val motion = GastonomoTheme.motion
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(motion.enterSpec()),
        exit = fadeOut(motion.exitSpec()),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GastonomoTheme.colors.scrim)
                .pressable(onClick = onDismiss),
        )
    }
}

/** Las opciones en su arco, escalonadas al abrir y juntas al cerrar. */
@Composable
private fun Options(
    state: RadialFabState,
    options: List<RadialOption>,
    reach: Float,
    centering: Float,
) {
    val stagger = GastonomoTheme.motion.radialStaggerMillis
    options.take(RadialFabGeometry.CAPACITY).forEachIndexed { index, option ->
        RadialOptionButton(
            option = option,
            direction = RadialFabGeometry.DIRECTIONS[index],
            reach = reach,
            centering = centering,
            open = state.open,
            highlighted = state.hovered == index,
            delayMillis = if (state.open) index * stagger else 0,
            onClick = {
                state.close()
                option.onSelect()
            },
        )
    }
}

/** El boton en si: el `+` que gira hasta leerse como una equis y que recoge el gesto. */
@Composable
private fun RadialTrigger(
    state: RadialFabState,
    options: List<RadialOption>,
    metrics: RadialMetrics,
    closeLabel: String,
    openLabel: String,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.fabRadial)
    val turn by animateFloatAsState(
        targetValue = if (state.open) OPEN_TURN else 0f,
        animationSpec = GastonomoTheme.motion.selectionSpec(),
        label = "radialTurn",
    )
    Box(
        modifier = Modifier
            .size(GastonomoTheme.dimens.fabRadial)
            .shadow(GastonomoTheme.shadows.fab, shape)
            .background(GastonomoTheme.colors.ink, shape)
            .pointerInput(options) {
                radialGestures(state, metrics) { options[it].onSelect() }
            }
            .semantics { contentDescription = if (state.open) closeLabel else openLabel },
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = GastonomoIcons.ADD,
            size = FAB_ICON,
            tint = GastonomoTheme.colors.lime,
            modifier = Modifier.graphicsLayer { rotationZ = turn },
        )
    }
}

@Composable
private fun RadialOptionButton(
    option: RadialOption,
    direction: Offset,
    reach: Float,
    centering: Float,
    open: Boolean,
    highlighted: Boolean,
    delayMillis: Int,
    onClick: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = GastonomoTheme.motion.radialSpec(delayMillis),
        label = "radialOption",
    )
    val scale = if (open) {
        progress * (if (highlighted) HIGHLIGHT_SCALE else 1f)
    } else {
        CLOSED_SCALE + progress * (1f - CLOSED_SCALE)
    }
    Box(
        modifier = Modifier
            .size(GastonomoTheme.dimens.radialOption)
            .graphicsLayer {
                translationX = direction.x * reach * progress - centering
                translationY = direction.y * reach * progress - centering
                scaleX = scale
                scaleY = scale
                alpha = progress
            }
            .shadow(GastonomoTheme.shadows.fab, CircleShape)
            .background(
                if (highlighted) GastonomoTheme.colors.lime else GastonomoTheme.colors.surface,
                CircleShape,
            )
            .pressable(onClick = onClick, enabled = open, role = Role.Button)
            .semantics { contentDescription = option.label },
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = option.icon,
            size = OPTION_ICON,
            tint = GastonomoTheme.colors.ink,
            filled = true,
        )
    }
}

/** Lo que gira el `+` al abrirse, hasta leerse como una equis. */
private const val OPEN_TURN = 45f

/** Lo que crece la opcion sobre la que esta el dedo. */
private const val HIGHLIGHT_SCALE = 1.14f

/** De que tamano salen las opciones del boton. */
private const val CLOSED_SCALE = 0.3f

private val FAB_ICON = 30.dp
private val OPTION_ICON = 26.dp

/**
 * Donde lo coloca el diseno: el mismo hueco por abajo que por el lado.
 *
 * El de abajo se mide desde el borde de ESTA caja, que en la aplicacion termina donde empieza
 * la tab bar: `GastonomoApp` es una `Column` con el NavHost en `weight(1f)` y la barra como
 * hermano siguiente. Valia 92, que es el alto de la tab bar mas 14, y eso solo tiene sentido en
 * una caja que cubra tambien la barra, como la del catalogo de componentes. Aqui se sumaba al
 * alto de la barra y el boton quedaba flotando a 92 dp de ella.
 */
private val FAB_END = 20.dp
private val FAB_BOTTOM = 20.dp
