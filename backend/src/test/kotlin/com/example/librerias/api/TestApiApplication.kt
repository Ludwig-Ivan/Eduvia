package com.example.librerias.api

import org.springframework.boot.fromApplication
import org.springframework.boot.with


/**
 * Punto de entrada manual para arrancar la API con el contenedor de Testcontainers y la
 * configuración de pruebas (`application-test.yml`, perfil `test`).
 *
 * No es una prueba JUnit: sirve para depurar desde el IDE o la línea de comandos el mismo escenario
 * que [IntegrationTestBase], sin el stack Docker del perfil `dev` que usa [E2eIntegrationTest].
 */
fun main(args: Array<String>) {
	fromApplication<ApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
