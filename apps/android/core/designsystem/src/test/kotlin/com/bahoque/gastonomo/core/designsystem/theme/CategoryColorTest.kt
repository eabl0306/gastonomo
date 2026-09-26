package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryColorTest {

    @Test
    fun `a six digit hex becomes an opaque color`() {
        assertEquals(Color(0xFFCF5F35), categoryColorOf("#cf5f35"))
    }

    @Test
    fun `the hash is optional`() {
        assertEquals(Color(0xFF2F8A55), categoryColorOf("2f8a55"))
    }

    @Test
    fun `an eight digit hex keeps its alpha`() {
        assertEquals(Color(0x802F8A55), categoryColorOf("#802f8a55"))
    }

    @Test
    fun `something that is not a color falls back instead of breaking the list`() {
        assertEquals(FALLBACK_CATEGORY_COLOR, categoryColorOf("verde"))
        assertEquals(FALLBACK_CATEGORY_COLOR, categoryColorOf("#abc"))
        assertEquals(FALLBACK_CATEGORY_COLOR, categoryColorOf(""))
    }
}
