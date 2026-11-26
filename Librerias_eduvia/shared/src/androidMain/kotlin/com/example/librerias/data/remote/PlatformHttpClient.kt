package com.example.librerias.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

/** Implementación `actual` del `expect` común: usa OkHttp como motor en Android. */
actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp, config)

/**
 * 10.0.2.2 es el host que representa al equipo desde el emulador Android.
 * El emulador tiene su propia pila de red, así que `localhost` apuntaría a él mismo; esta IP alias
 * permite alcanzar el backend que escucha en el puerto 8080 de la máquina host.
 */
actual fun apiBaseUrl(): String = "http://10.0.2.2:8080/api/v1/"
