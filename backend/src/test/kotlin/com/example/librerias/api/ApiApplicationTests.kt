package com.example.librerias.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * Prueba de humo del arranque completo de la aplicación (capa de composición e infraestructura).
 *
 * Protege contra un fallo de configuración del contexto Spring: beans de seguridad JWT, JPA,
 * Flyway sobre PostgreSQL real y los controladores bajo el prefijo `/api/v1`. Sin este arranque,
 * el resto de pruebas no podría verificar el contrato de `docs/backend/api-contract.md`
 * (convenciones globales y operaciones A1 a C3).
 */
@Import(TestcontainersConfiguration::class)
@SpringBootTest
class ApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
