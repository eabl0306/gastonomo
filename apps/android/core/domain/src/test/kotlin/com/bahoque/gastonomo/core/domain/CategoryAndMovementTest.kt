package com.bahoque.gastonomo.core.domain

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryAndMovementTest {

    @Test
    fun `a both category is offered for income and for expense`() {
        assertTrue(CategoryKind.BOTH.accepts(MovementKind.IN))
        assertTrue(CategoryKind.BOTH.accepts(MovementKind.OUT))
    }

    @Test
    fun `an income category is only offered for income`() {
        assertTrue(CategoryKind.IN.accepts(MovementKind.IN))
        assertFalse(CategoryKind.IN.accepts(MovementKind.OUT))
        assertTrue(CategoryKind.OUT.accepts(MovementKind.OUT))
        assertFalse(CategoryKind.OUT.accepts(MovementKind.IN))
    }

    @Test
    fun `the user can only create income and expense categories`() {
        assertTrue(CategoryKind.IN.isSelectableByUsers())
        assertTrue(CategoryKind.OUT.isSelectableByUsers())
        assertFalse(CategoryKind.BOTH.isSelectableByUsers())
    }

    @Test
    fun `the wire values of the category kind match the api`() {
        assertEquals(CategoryKind.BOTH, CategoryKind.fromWire("both"))
        assertEquals(CategoryKind.IN, CategoryKind.fromWire("in"))
        assertNull(CategoryKind.fromWire("neither"))
        assertEquals(CategoryKind.IN, CategoryKind.of(MovementKind.IN))
        assertEquals(CategoryKind.OUT, CategoryKind.of(MovementKind.OUT))
    }

    @Test
    fun `the sheet offers sixteen icons and eight colours`() {
        assertEquals(16, Category.ICON_CHOICES.size)
        assertEquals(8, Category.COLOR_CHOICES.size)
        assertEquals("shopping_cart", Category.ICON_CHOICES.first())
        assertEquals("#5b6573", Category.COLOR_CHOICES.last())
    }

    @Test
    fun `identifiers print as their value`() {
        assertEquals("abc", CategoryId("abc").toString())
        assertEquals("def", MovementId("def").toString())
    }

    @Test
    fun `the movement kind knows whether it is income`() {
        assertTrue(MovementKind.IN.isIncome)
        assertFalse(MovementKind.OUT.isIncome)
        assertEquals(MovementKind.OUT, MovementKind.fromWire("out"))
        assertNull(MovementKind.fromWire("sideways"))
    }

    @Test
    fun `the settings default to pesos and spanish`() {
        val settings = UserSettings()
        assertEquals(CurrencyCode.COP, settings.displayCurrency)
        assertEquals(AppLanguage.SPANISH, settings.language)
        assertEquals("es-CO", AppLanguage.SPANISH.localeTag)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromWire("en"))
        assertNull(AppLanguage.fromWire("fr"))
    }

    @Test
    fun `the device settings start with the lock off and the amounts visible`() {
        val device = DeviceSettings()
        assertFalse(device.lockEnabled)
        assertFalse(device.hideAmounts)
    }

    @Test
    fun `a currency of the catalog carries its name and symbol`() {
        val info = CurrencyInfo(CurrencyCode.EUR, "Euro", "€")
        assertEquals("Euro", info.name)
        assertEquals("€", info.symbol)
    }
}
