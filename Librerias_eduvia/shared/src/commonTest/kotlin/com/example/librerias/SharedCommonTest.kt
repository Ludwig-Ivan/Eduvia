package com.example.librerias

import kotlin.test.Test
import kotlin.test.assertEquals

/** Capa compartida (smoke test): protege que la lógica y el entorno de pruebas `common` funcionen en todas las plataformas. */
class SharedCommonTest {

    // Nombre genérico: comprobación mínima de que las pruebas comunes se ejecutan.
    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }
}