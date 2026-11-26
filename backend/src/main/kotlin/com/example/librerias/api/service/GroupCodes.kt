package com.example.librerias.api.service

import java.security.SecureRandom

private const val GROUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** Código legible `XX-YYYY`, con el mismo alfabeto y formato que usaba cada servicio por separado. */
internal fun generateGroupCode(random: SecureRandom): String =
    (1..2).map { GROUP_CODE_ALPHABET[random.nextInt(GROUP_CODE_ALPHABET.length)] }.joinToString("") + "-" +
        (1..4).map { GROUP_CODE_ALPHABET[random.nextInt(GROUP_CODE_ALPHABET.length)] }.joinToString("")

/** Devuelve `null` si tras 20 intentos no aparece un código libre; cada llamador decide su error. */
internal fun uniqueGroupCode(random: SecureRandom, exists: (String) -> Boolean): String? {
    repeat(20) {
        val code = generateGroupCode(random)
        if (!exists(code)) return code
    }
    return null
}
