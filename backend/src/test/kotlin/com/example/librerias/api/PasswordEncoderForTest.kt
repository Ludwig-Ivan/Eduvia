package com.example.librerias.api

import org.springframework.security.crypto.factory.PasswordEncoderFactories

/**
 * Codificador de contraseñas de prueba: usa el mismo `DelegatingPasswordEncoder` que producción.
 *
 * Los datos creados por [TestData] necesitan un hash válido para que el login por HTTP
 * (`/api/v1/auth/login`) funcione; centralizarlo aquí evita pegar hashes literales en cada test.
 */
object PasswordEncoderForTest {
    private val delegate = PasswordEncoderFactories.createDelegatingPasswordEncoder()
    fun encodeForTest(raw: String): String = delegate.encode(raw)!!
}
