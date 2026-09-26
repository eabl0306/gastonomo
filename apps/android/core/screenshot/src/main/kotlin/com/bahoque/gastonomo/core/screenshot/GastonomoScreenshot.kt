package com.bahoque.gastonomo.core.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File

/**
 * Captura un composable a PNG desde la JVM, sin emulador ni dispositivo.
 *
 * Es lo que permite auditar la fidelidad del diseno: se renderiza con Robolectric en el
 * tamano del prototipo, 390x844, y se escribe la imagen en `build/outputs/roborazzi`.
 *
 * Una pantalla se captura asi:
 * ```
 * @RunWith(AndroidJUnit4::class)
 * @Config(qualifiers = GASTONOMO_QUALIFIERS)
 * class HomeScreenScreenshotTest {
 *
 *     @get:Rule val compose = createComposeRule()
 *
 *     @Test
 *     fun `home screen`() {
 *         compose.captureGastonomoScreen("home") { HomeScreen(state = HomeUiState.sample) }
 *     }
 * }
 * ```
 *
 * Hacen falta las dos anotaciones: `@Config(qualifiers = GASTONOMO_QUALIFIERS)` fija el
 * tamano de pantalla y la densidad, y el fichero `robolectric.properties` del modulo tiene
 * que llevar `graphicsMode=NATIVE` o la captura sale en blanco.
 */
fun ComposeContentTestRule.captureGastonomoScreen(
    name: String,
    language: AppLanguage = AppLanguage.SPANISH,
    displayCurrency: CurrencyCode = CurrencyCode.COP,
    content: @Composable () -> Unit,
) {
    setContent {
        GastonomoTheme(language = language, displayCurrency = displayCurrency) {
            Box(
                modifier = Modifier
                    .size(width = SCREEN_WIDTH.dp, height = SCREEN_HEIGHT.dp)
                    .background(GastonomoTheme.colors.bgApp),
            ) {
                content()
            }
        }
    }
    onRoot().captureRoboImage(screenshotFile(name).path)
}

/**
 * Los qualifiers de Robolectric que reproducen el telefono del prototipo: 390x844 a
 * densidad 3x, que es un iPhone 14 o un Pixel moderno.
 *
 * **No fija el idioma**, asi que los textos salen del `values` que corresponda al locale de
 * la JVM, que en la CI es en-US. Para una pantalla con textos en castellano hay que usar
 * [GASTONOMO_QUALIFIERS_ES], y para comprobar la traduccion, [GASTONOMO_QUALIFIERS_EN].
 */
const val GASTONOMO_QUALIFIERS = "w390dp-h844dp-xxhdpi"

/**
 * El telefono del prototipo en es-CO, que es el idioma por defecto de la aplicacion. Es el
 * que hay que usar para comparar una captura con el diseno entregado.
 *
 * El orden de los qualifiers de Android no es libre: el locale va antes del ancho.
 */
const val GASTONOMO_QUALIFIERS_ES = "es-rCO-w390dp-h844dp-xxhdpi"

/** El mismo telefono en en-US, para comprobar la traduccion. */
const val GASTONOMO_QUALIFIERS_EN = "en-rUS-w390dp-h844dp-xxhdpi"

/** Ancho del prototipo, en dp. */
const val SCREEN_WIDTH = 390

/** Alto del prototipo, en dp. */
const val SCREEN_HEIGHT = 844

/**
 * Donde acaban las imagenes. El directorio esta en `.gitignore`: son artefactos de una
 * ejecucion, no fuentes.
 */
fun screenshotFile(name: String): File =
    File(SCREENSHOT_DIRECTORY).apply { mkdirs() }.resolve("$name.png")

private const val SCREENSHOT_DIRECTORY = "build/outputs/roborazzi"
