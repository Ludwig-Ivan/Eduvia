package com.example.librerias.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Punto de entrada de la API REST de Eduvia y raíz de la configuración de Spring.
 *
 * Responsabilidad: agrupar en un único paquete base (`com.example.librerias.api`) el escaneo de
 * componentes, la autoconfiguración y los beans de configuración, seguridad y servicios que
 * sostienen el contrato consumido por la app KMP. No contiene lógica de negocio: colabora con el
 * contenedor de Spring, que inyecta en los servicios los repositorios y el codificador JWT.
 */
@SpringBootApplication
class ApiApplication

fun main(args: Array<String>) {
	runApplication<ApiApplication>(*args)
}
