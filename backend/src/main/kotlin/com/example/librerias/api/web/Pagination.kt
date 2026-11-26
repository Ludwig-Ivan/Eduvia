package com.example.librerias.api.web

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/** Límite de tamaño de página acordado con la app KMP, aplicado por los controllers. */
internal const val MAX_PAGE_SIZE = 100

/** Construye la paginación de los listados, conservando el orden solicitado por cada endpoint. */
internal fun pageRequest(page: Int, size: Int, sort: Sort? = null): PageRequest =
    sort?.let { PageRequest.of(page, size.coerceAtMost(MAX_PAGE_SIZE), it) }
        ?: PageRequest.of(page, size.coerceAtMost(MAX_PAGE_SIZE))
