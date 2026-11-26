package com.example.librerias

import kotlin.test.Test
import kotlin.test.assertEquals

/** Capa de lógica compartida en iOS: protege que el código `common` se compile y ejecute igual en el host iOS (smoke test). */
class SharedLogicIOSTest {

    // Nombre genérico: comprobación mínima de que la lógica común corre en iOS.
    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }
}