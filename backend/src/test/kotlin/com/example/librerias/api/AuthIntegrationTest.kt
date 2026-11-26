package com.example.librerias.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integración HTTP de autenticación y cuenta (operaciones A1 a A9 de `docs/backend/api-contract.md`).
 *
 * Cubre la capa web y de servicio con PostgreSQL real: el riesgo es que el registro no sea atómico
 * (regla 1 del contrato), que el JWT no se emita, rote o revoque, que email y teléfono no se
 * normalicen antes de comprobar su unicidad (regla 8) o que un código de reset se reutilice
 * (regla 9). También verifica la convención global de errores `ProblemDetail` y el Bearer exigido
 * en `/api/v1/me`.
 */
class AuthIntegrationTest : IntegrationTestBase() {

    private fun register(
        email: String = "nuevo@eduvia.test",
        role: String = "PARENT",
        children: List<Map<String, String>> = emptyList(),
        groups: List<Map<String, String>> = emptyList(),
    ): Map<String, Any> {
        val body = mapOf(
            "role" to role,
            "fullName" to "Nuevo Usuario",
            "email" to email,
            "phone" to "600000009",
            "password" to "Password1!",
            "confirmPassword" to "Password1!",
            "acceptedTerms" to true,
            "children" to children,
            "groups" to groups,
        )
        val response = rest.postForEntity("/api/v1/auth/register", body, Map::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        @Suppress("UNCHECKED_CAST")
        return response.body as Map<String, Any>
    }

    @Test
    fun `registro de padre vincula alumno y devuelve tokens`() {
        val teacher = TestData(jdbc).user()
        val groupId = TestData(jdbc).group(teacher, code = "PA-0001")
        val result = register(
            role = "PARENT",
            children = listOf(mapOf("classCode" to "PA-0001", "studentName" to "Ana")),
        )
        val user = result["user"] as Map<*, *>
        assertEquals("nuevo@eduvia.test", user["email"])
        assertEquals("PARENT", user["role"])
        assertTrue((result["accessToken"] as String).isNotBlank())
        assertTrue((result["refreshToken"] as String).isNotBlank())
        assertEquals(900L, (result["expiresIn"] as Number).toLong())

        val students = jdbc.queryForList("select name, parent_id from students where group_id = ?", groupId)
        assertEquals(1, students.size)
        assertEquals("Ana", students[0]["name"])
        assertEquals((user["id"] as Number).toLong(), (students[0]["parent_id"] as Number).toLong())
        val count = jdbc.queryForObject("select student_count from school_groups where id = ?", Int::class.java, groupId)
        assertEquals(1, count)
    }

    @Test
    fun `registro de docente crea grupos en la misma transaccion`() {
        val result = register(
            email = "docente@eduvia.test",
            role = "TEACHER",
            groups = listOf(mapOf("groupName" to "4º B", "schoolName" to "Colegio", "subject" to "Historia")),
        )
        val groups = result["createdGroups"] as List<*>
        assertEquals(1, groups.size)
        val group = groups[0] as Map<*, *>
        assertTrue((group["code"] as String).matches(Regex("[A-Z0-9]{2}-[A-Z0-9]{4}")))
        assertEquals(1, jdbc.queryForObject("select count(*) from school_groups", Int::class.java))
    }

    @Test
    fun `registro con codigo de clase inexistente no crea usuario`() {
        val body = mapOf(
            "role" to "PARENT", "fullName" to "Nuevo Usuario", "email" to "fallo@eduvia.test",
            "phone" to "600000019", "password" to "Password1!", "confirmPassword" to "Password1!",
            "acceptedTerms" to true,
            "children" to listOf(mapOf("classCode" to "ZZ-9999", "studentName" to "Ana")),
        )
        val response = rest.postForEntity("/api/v1/auth/register", body, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(0, jdbc.queryForObject("select count(*) from users", Int::class.java))
    }

    @Test
    fun `login correcto e incorrecto`() {
        TestData(jdbc).user(passwordHash = encoder.encodeForTest("Password1!"))
        val ok = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "demo@eduvia.test", "password" to "Password1!"),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, ok.statusCode)
        val bad = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "demo@eduvia.test", "password" to "wrong"),
            Map::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, bad.statusCode)
    }

    // El cuerpo del 401 también es ProblemDetail; el nombre solo menciona la cabecera.
    @Test
    fun `endpoint protegido sin token responde 401 con WWW-Authenticate`() {
        val response = rest.getForEntity("/api/v1/me", Map::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Bearer", response.headers.getFirst("WWW-Authenticate"))
        val body = response.body!!
        assertEquals(401, body["status"])
    }

    @Test
    fun `me devuelve el usuario autenticado`() {
        TestData(jdbc).user(passwordHash = encoder.encodeForTest("Password1!"))
        val token = tokenFor(password = "Password1!")
        val request = HttpEntity<Void>(authHeaders(token))
        val response = rest.exchange("/api/v1/me", HttpMethod.GET, request, Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("demo@eduvia.test", (response.body as Map<*, *>)["email"])
    }

    @Test
    fun `refresh rota el token y el anterior queda revocado`() {
        TestData(jdbc).user(passwordHash = encoder.encodeForTest("Password1!"))
        val login = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "demo@eduvia.test", "password" to "Password1!"),
            Map::class.java,
        ).body as Map<*, *>
        val first = login["refreshToken"] as String

        val refreshed = rest.postForEntity(
            "/api/v1/auth/refresh",
            mapOf("refreshToken" to first),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, refreshed.statusCode)
        val second = (refreshed.body as Map<*, *>)["refreshToken"] as String
        assertNotEquals(first, second)

        val reuse = rest.postForEntity(
            "/api/v1/auth/refresh",
            mapOf("refreshToken" to first),
            Map::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.statusCode)
        assertEquals(1, jdbc.queryForObject("select count(*) from refresh_tokens where revoked_at is null", Int::class.java))
    }

    @Test
    fun `logout revoca el refresh token`() {
        TestData(jdbc).user(passwordHash = encoder.encodeForTest("Password1!"))
        val token = (rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "demo@eduvia.test", "password" to "Password1!"),
            Map::class.java,
        ).body as Map<*, *>)["refreshToken"] as String
        val response = rest.postForEntity("/api/v1/auth/logout", mapOf("refreshToken" to token), Void::class.java)
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        val refresh = rest.postForEntity(
            "/api/v1/auth/refresh",
            mapOf("refreshToken" to token),
            Map::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, refresh.statusCode)
    }

    // Comprueba la normalización: email en mayúsculas y teléfono con guiones deben encontrar al usuario.
    @Test
    fun `exists email y telefono`() {
        TestData(jdbc).user()
        assertTrue((rest.getForObject("/api/v1/auth/exists/email?email=DEMO@eduvia.test", Map::class.java)!!["exists"] as Boolean))
        assertFalse((rest.getForObject("/api/v1/auth/exists/email?email=otro@eduvia.test", Map::class.java)!!["exists"] as Boolean))
        assertTrue((rest.getForObject("/api/v1/auth/exists/phone?phone=600-000-001", Map::class.java)!!["exists"] as Boolean))
    }

    // El código se lee de la BD porque solo el perfil `dev` lo devuelve en la respuesta (`resetCode`).
    @Test
    fun `reset password con codigo de un solo uso`() {
        TestData(jdbc).user(passwordHash = encoder.encodeForTest("OldPass1!"))
        val codeResponse = rest.postForEntity(
            "/api/v1/auth/reset-code",
            mapOf("email" to "demo@eduvia.test"),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, codeResponse.statusCode)
        val code = jdbc.queryForObject("select code from password_reset_codes order by id desc limit 1", String::class.java)

        val reset = rest.postForEntity(
            "/api/v1/auth/reset-password",
            mapOf(
                "email" to "demo@eduvia.test", "code" to code,
                "newPassword" to "NewPass1!", "confirmPassword" to "NewPass1!",
            ),
            Void::class.java,
        )
        assertEquals(HttpStatus.OK, reset.statusCode)
        val retry = rest.postForEntity(
            "/api/v1/auth/reset-password",
            mapOf(
                "email" to "demo@eduvia.test", "code" to code,
                "newPassword" to "NewPass1!", "confirmPassword" to "NewPass1!",
            ),
            Void::class.java,
        )
        assertEquals(HttpStatus.BAD_REQUEST, retry.statusCode)
        val login = rest.postForEntity(
            "/api/v1/auth/login",
            mapOf("email" to "demo@eduvia.test", "password" to "NewPass1!"),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode)
    }

    @Test
    fun `validacion de registro devuelve ProblemDetail con errores`() {
        val body = mapOf(
            "role" to "PARENT", "fullName" to "", "email" to "no-es-email",
            "phone" to "600000009", "password" to "123", "confirmPassword" to "123",
            "acceptedTerms" to false,
        )
        val response = rest.postForEntity("/api/v1/auth/register", body, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val problem = response.body!!
        assertEquals(400, problem["status"])
        assertEquals("Bad Request", problem["title"])
        @Suppress("UNCHECKED_CAST")
        val errors = problem["errors"] as List<Map<*, *>>
        assertTrue(errors.any { it["field"] == "email" })
        assertTrue(errors.any { it["field"] == "acceptedTerms" })
    }

    private val encoder get() = PasswordEncoderForTest
}
