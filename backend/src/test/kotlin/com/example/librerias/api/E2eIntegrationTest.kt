package com.example.librerias.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pruebas de extremo a extremo contra el stack levantado con `docker compose up -d --build --wait`.
 *
 * A diferencia del resto, no arranca el contexto Spring ni Testcontainers: habla por HTTP con la API
 * desplegada (perfil `dev`, con el usuario semilla `laura@eduvia.mx`) y su PostgreSQL. Protege el
 * despliegue real: health de Actuator, login JWT, `/api/v1/me`, persistencia del CRUD de grupo y
 * tarea en PostgreSQL y el formato `ProblemDetail` de los errores. Verifica así A2, A9, G4, T1, T3,
 * T5 y la convención global de errores de `docs/backend/api-contract.md`.
 *
 * Se aísla con `@Tag("e2e")` porque la tarea `test` la excluye: requiere el stack Docker levantado;
 * la tarea `e2eTest` la ejecuta apuntando a `e2e.baseUrl`.
 */
@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class E2eIntegrationTest {

    private var token: String = ""

    @BeforeAll
    fun configure() {
        val baseUrl = System.getProperty("e2e.baseUrl", "http://localhost:8080")
        RestAssured.baseURI = baseUrl
        RestAssured.requestSpecification = RestAssured.given().contentType(ContentType.JSON)
        token = RestAssured.given()
            .body(mapOf("email" to "laura@eduvia.mx", "password" to "Maestra123"))
            .post("/api/v1/auth/login")
            .then().statusCode(200).extract().path<String>("accessToken")
    }

    @Test
    fun `health esta UP`() {
        val body = RestAssured.given().get("/actuator/health").then().statusCode(200).extract().path<String>("status")
        assertEquals("UP", body)
    }

    @Test
    fun `login devuelve JWT y me devuelve el usuario`() {
        assertTrue(token.count { it == '.' } == 2)
        val email = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .get("/api/v1/me")
            .then().statusCode(200).extract().path<String>("email")
        assertEquals("laura@eduvia.mx", email)
    }

    @Test
    fun `crud completo de grupo y tarea persiste en postgres`() {
        val group = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .body(
                mapOf(
                    "teacherId" to 1,
                    "groupName" to "E2E-${System.currentTimeMillis()}",
                    "schoolName" to "Colegio E2E",
                    "subject" to "Matemáticas",
                ),
            )
            .post("/api/v1/groups")
            .then().statusCode(201).extract().path<Int>("id")

        val task = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .body(
                mapOf(
                    "groupId" to group,
                    "title" to "Tarea E2E",
                    "subject" to "MATEMATICAS",
                    "description" to "Descripción E2E",
                    "dateText" to "2026-12-20",
                ),
            )
            .post("/api/v1/tasks")
            .then().statusCode(201).extract().path<Int>("id")

        val listed = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .get("/api/v1/tasks?groupId=$group")
            .then().statusCode(200).extract().path<List<Int>>("items.id")
        assertTrue(listed.contains(task))

        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .delete("/api/v1/tasks/$task")
            .then().statusCode(204)
        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .get("/api/v1/tasks/$task")
            .then().statusCode(404)
    }

    @Test
    fun `errores usan problem detail`() {
        val response: Response = RestAssured.given()
            .header("Authorization", "Bearer $token")
            .get("/api/v1/groups/999999")
        assertEquals(404, response.statusCode())
        assertEquals("application/problem+json", response.contentType().split(";").first())
        assertEquals(404, response.jsonPath().getInt("status"))
    }
}
