package com.example.librerias.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

/** Implementación `actual` del `expect` común: usa el motor Darwin (NSURLSession) en iOS. */
actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin, config)

/**
 * URL base en iOS: el simulador comparte la red de la máquina host, por lo que `localhost:8080`
 * alcanza el backend local. ATS trata `localhost` como red local y permite el HTTP plano en
 * desarrollo mediante la excepción correspondiente del proyecto.
 */
actual fun apiBaseUrl(): String = "http://localhost:8080/api/v1/"
