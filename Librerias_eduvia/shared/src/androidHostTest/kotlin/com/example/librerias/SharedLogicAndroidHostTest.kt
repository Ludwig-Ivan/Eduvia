package com.example.librerias

import kotlin.test.Test
import kotlin.test.assertEquals

/** Capa de lógica compartida en Android: protege que el código `common` se compile y ejecute igual en el host Android (smoke test). */
class SharedLogicAndroidHostTest {

    // Nombre genérico: comprobación mínima de que la lógica común corre en Android.
    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }
}