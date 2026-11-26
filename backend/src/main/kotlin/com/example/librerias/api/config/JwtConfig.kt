package com.example.librerias.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

/**
 * Configuración de la criptografía simétrica de los tokens JWT de la API.
 *
 * Responsabilidad: derivar una única clave a partir de `jwt.secret` y exponer el codificador y el
 * decodificador HS256. Compartir la misma clave entre quien firma el token en el login y quien lo
 * valida en el filtro OAuth2 es lo que permite a la app KMP enviar el `Authorization: Bearer` en
 * las siguientes peticiones. El requisito de 32 bytes evita secretos débiles que puedan romper esa
 * firma. Colabora únicamente con el contenedor de beans y con el servicio de autenticación, que
 * es quien emite los tokens leyendo estos mismos beans.
 */
@Configuration
class JwtConfig(
    @Value("\${jwt.secret}") private val secret: String,
) {
    private val key: SecretKeySpec by lazy {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "JWT_SECRET debe tener al menos 32 caracteres"
        }
        SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
    }

    /**
     * Expone la firma de tokens para el servicio de autenticación.
     *
     * @return codificador HS256 basado en la clave derivada de `jwt.secret`.
     */
    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder.withSecretKey(key).build()

    /**
     * Expone la validación de tokens para el resource server de Spring Security.
     *
     * @return decodificador HS256 que usa la misma clave que [jwtEncoder]; si no coincidieran,
     * todos los tokens emitidos por el login serían rechazados como no autorizados.
     */
    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
}
