package com.example.librerias.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caché en memoria de listados remotos, compartida por todos los observadores de una misma clave.
 * La primera suscripción descarga los datos; las mutaciones actualizan la caché para que los
 * Flow de la UI reaccionen sin esperar otro viaje de red.
 *
 * La caché existe para que la UI pueda observar flujos estables mientras la red responde: el primer
 * suscriptor dispara el `loader`, `filterNotNull` evita emitir una lista vacía antes de la respuesta
 * y los demás suscriptores comparten el mismo estado. Los fallos de carga conservan el último valor
 * conocido, pero se informan a [reporter] para que la UI pueda mostrar un aviso de conexión.
 * Todo el estado está en memoria y se protege con [mutex].
 *
 * @param reporter canal compartido donde se publican los fallos de carga.
 */
internal class RemoteListStore<K, T : Any>(
    private val reporter: RemoteErrorReporter? = null,
) {

    private val mutex = Mutex()
    private val states = mutableMapOf<K, MutableStateFlow<List<T>?>>()
    private val loaders = mutableMapOf<K, suspend () -> List<T>>()
    private val subscribers = mutableMapOf<K, Int>()

    /**
     * Devuelve el flujo cacheado para [key], registrando [loader] la primera vez. Solo el primer
     * suscriptor lanza la descarga; al cancelarse el último suscriptor se olvida el contador, pero
     * el estado cargado se conserva.
     */
    fun flow(key: K, loader: suspend () -> List<T>): Flow<List<T>> = flow {
        val state = mutex.withLock {
            if (!loaders.containsKey(key)) loaders[key] = loader
            states.getOrPut(key) { MutableStateFlow(null) }
        }
        val firstSubscriber = mutex.withLock {
            val count = subscribers[key] ?: 0
            subscribers[key] = count + 1
            count == 0
        }
        try {
            if (firstSubscriber) {
                runCatching { state.value = loader() }
                    .onSuccess { reporter?.clear() }
                    .onFailure { reporter?.report(it) }
            }
            emitAll(state.filterNotNull().distinctUntilChanged())
        } finally {
            mutex.withLock {
                val count = (subscribers[key] ?: 1) - 1
                if (count <= 0) subscribers.remove(key) else subscribers[key] = count
            }
        }
    }

    /** Vuelve a ejecutar el `loader` de [key] y sustituye la caché si la carga tiene éxito. */
    suspend fun refresh(key: K) {
        val loader = mutex.withLock { loaders[key] } ?: return
        runCatching { loader() }
            .onSuccess { fresh ->
                mutex.withLock { states[key]?.value = fresh }
                reporter?.clear()
            }
            .onFailure { reporter?.report(it) }
    }

    /** Recarga todas las claves con `loader` registrado (por ejemplo, al cambiar un grupo). */
    suspend fun refreshAll() {
        val keys = mutex.withLock { loaders.keys.toList() }
        keys.forEach { refresh(it) }
    }

    /** Sustituye la lista cacheada de [key] por [items] (no hace nada si la clave no existe). */
    suspend fun replace(key: K, items: List<T>) {
        mutex.withLock { states[key]?.value = items }
    }

    /** Aplica [transform] a la lista cacheada de [key] para responder a mutaciones locales. */
    suspend fun update(key: K, transform: (List<T>) -> List<T>) {
        mutex.withLock {
            val state = states[key]
            state?.value?.let { current -> state.value = transform(current) }
        }
    }

    /** Elimina de la caché de [key] los elementos que cumplen [predicate]. */
    suspend fun removeWhere(key: K, predicate: (T) -> Boolean) {
        update(key) { list -> list.filterNot(predicate) }
    }

    /** Aplica [transform] a todas las listas ya cargadas. */
    suspend fun updateAll(transform: (List<T>) -> List<T>) {
        mutex.withLock {
            states.values.forEach { state -> state.value?.let { state.value = transform(it) } }
        }
    }

    /** Elimina de todas las listas cargadas los elementos que cumplen [predicate]. */
    suspend fun removeWhereAll(predicate: (T) -> Boolean) {
        mutex.withLock {
            states.values.forEach { state -> state.value?.let { state.value = it.filterNot(predicate) } }
        }
    }

    /** Vacía todas las listas cacheadas (por ejemplo, al cerrar sesión). */
    suspend fun clear() {
        mutex.withLock { states.values.forEach { it.value = emptyList() } }
    }
}
