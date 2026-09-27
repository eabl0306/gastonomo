package com.bahoque.gastonomo.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Los tokens de movimiento. Dos cosas que dejan de cumplirse sin darse cuenta: que ninguna
 * animacion pase de 300 ms, que es cuando la aplicacion empieza a parecer lenta, y que apagar las
 * animaciones en el sistema las apague de verdad.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class GastonomoMotionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `no animation goes past 300 ms`() {
        val motion = GastonomoMotion()

        listOf(motion.enterMillis, motion.exitMillis, motion.selectionMillis).forEach { duration ->
            assertTrue(duration in 1..MAX_MILLIS, "$duration ms se sale del limite")
        }
    }

    @Test
    fun `what leaves is quicker than what enters`() {
        val motion = GastonomoMotion()

        assertTrue(motion.exitMillis < motion.enterMillis)
    }

    @Test
    fun `with the animations off every duration is zero`() {
        val motion = GastonomoMotion(enabled = false)

        assertEquals(0, motion.enterMillis)
        assertEquals(0, motion.exitMillis)
        assertEquals(0, motion.selectionMillis)
    }

    @Test
    fun `the system preference is what decides`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertTrue(systemAnimationsEnabled(context))

        setTransitionScale(context, 0f)
        assertFalse(systemAnimationsEnabled(context))

        setTransitionScale(context, 1f)
        assertTrue(systemAnimationsEnabled(context))
    }

    @Test
    fun `the theme hands the tokens to whoever paints`() {
        var seen: GastonomoMotion? = null
        compose.setContent {
            GastonomoTheme { seen = GastonomoTheme.motion }
        }

        assertEquals(GastonomoMotion().enterMillis, seen?.enterMillis)
    }

    private fun setTransitionScale(context: Context, scale: Float) {
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            scale,
        )
    }

    private companion object {
        /** Por encima de esto la aplicacion parece lenta, que es lo contrario de lo que se busca. */
        const val MAX_MILLIS = 300
    }
}
