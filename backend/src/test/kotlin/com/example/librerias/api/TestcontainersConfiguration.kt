package com.example.librerias.api

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Aporta un PostgreSQL 17 real a las pruebas mediante `@ServiceConnection`.
 *
 * Se usa PostgreSQL y no H2 porque los tests ejecutan SQL propio (`returning`, `date::text`,
 * `truncate ... restart identity cascade`) y migraciones Flyway que una base en memoria no
 * interpretaría igual: así se valida el esquema y el SQL que realmente corre en despliegue.
 * Lo importan [ApiApplicationTests] y [IntegrationTestBase].
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer() =
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
}
