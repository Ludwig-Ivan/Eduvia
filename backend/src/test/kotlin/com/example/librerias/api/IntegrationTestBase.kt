package com.example.librerias.api

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

/**
 * Base de las pruebas de integración HTTP: arranca el contexto Spring en puerto aleatorio con
 * PostgreSQL real vía [TestcontainersConfiguration] y el perfil `test` (`application-test.yml`,
 * solo migraciones Flyway, sin los datos semilla del perfil `dev`).
 *
 * [rest] llama a la API como un cliente HTTP y [jdbc] comprueba lo persistido, cubriendo en un mismo
 * test la capa web, de servicio y SQL. [cleanDatabase] trunca las tablas y reinicia las identidades
 * antes de cada prueba para aislarlas: sin ello, los IDs y la unicidad de email/teléfono dependerían
 * del orden de ejecución. El reinicio de la secuencia de eventos respeta que la app usa id = 1 de
 * forma explícita para el primer evento de calendario.
 *
 * [tokenFor] y [authHeaders] centralizan el Bearer que el contrato exige en todos los recursos
 * excepto auth, actuator y swagger.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var rest: TestRestTemplate

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        TestData.reset()
        jdbc.execute(
            """
            truncate table
                meeting_responses, task_deliveries, calendar_events,
                password_reset_codes, refresh_tokens, meetings,
                school_tasks, students, school_groups, users
            restart identity cascade
            """.trimIndent(),
        )
        // La app usa id = 1 de forma explícita para el primer evento de calendario.
        jdbc.execute("alter sequence calendar_events_id_seq restart with 100000")
    }

    protected fun tokenFor(email: String = "demo@eduvia.test", password: String = "Demo1234!"): String {
        val body = mapOf("email" to email, "password" to password)
        val response = rest.postForEntity("/api/v1/auth/login", body, Map::class.java)
        check(response.statusCode.value() == 200) { "Login de test falló: ${response.statusCode} ${response.body}" }
        @Suppress("UNCHECKED_CAST")
        return (response.body as Map<String, Any>)["accessToken"] as String
    }

    protected fun authHeaders(token: String) =
        org.springframework.http.HttpHeaders().apply { set("Authorization", "Bearer $token") }
}
