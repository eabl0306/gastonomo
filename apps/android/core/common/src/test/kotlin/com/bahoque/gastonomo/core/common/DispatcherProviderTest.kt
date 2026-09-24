package com.bahoque.gastonomo.core.common

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertSame

class DispatcherProviderTest {

    @Test
    fun `default provider exposes the standard dispatchers`() {
        assertSame(Dispatchers.IO, DefaultDispatcherProvider.io)
        assertSame(Dispatchers.Default, DefaultDispatcherProvider.default)
    }
}
