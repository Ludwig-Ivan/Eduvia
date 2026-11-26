package com.example.librerias.domain.util

import com.example.librerias.model.CalendarDate

/**
 * Abstrae "la fecha y hora de ahora" para no depender de la plataforma (y poder probar).
 *
 * `DemoTodayProvider` es la implementación inyectada en la demo, cuyos datos de muestra y
 * previews toman como referencia el 12 de octubre de 2026; en los tests se sustituye por un
 * proveedor que fija "hoy" para que las reglas de fechas (juntas y tareas) sean deterministas.
 */
interface TodayProvider {
    /**
     * Fecha actual del escenario en curso.
     * @return la fecha usada para validar vencimientos y calcular etiquetas relativas.
     */
    fun today(): CalendarDate

    /**
     * Minutos transcurridos desde la medianoche, en hora local. Ej. 10:45 am = 645.
     *
     * @return minutos en el rango 0..1439; permite comprobar si una junta de hoy ya pasó.
     */
    fun nowMinutes(): Int
}
