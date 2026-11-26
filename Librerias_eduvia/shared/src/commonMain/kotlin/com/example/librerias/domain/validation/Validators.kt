package com.example.librerias.domain.validation

/**
 * Validaciones de formato centralizadas para que todos los casos de uso muestren los mismos
 * mensajes y no consulten repositorios con datos evidentemente inválidos: correo, celular de
 * 10 dígitos, contraseña fuerte, hora en formato am/pm, URL http(s) y nombre de dos palabras.
 * Las reglas de negocio (duplicados, fechas pasadas, códigos de clase) viven en los casos de uso.
 */
object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val timeRegex = Regex("^(1[0-2]|0?[1-9]):[0-5][0-9]\\s?(am|pm)$", RegexOption.IGNORE_CASE)

    /**
     * Comprueba el formato de correo.
     * @param value texto a validar (se recorta antes de aplicar la expresión regular).
     * @return `true` si parece un correo con dominio y extensión.
     */
    fun isValidEmail(value: String) = emailRegex.matches(value.trim())

    /**
     * Comprueba que el celular tenga exactamente 10 dígitos.
     * @param value texto a validar.
     * @return `true` si son 10 caracteres numéricos.
     */
    fun isValidPhone(value: String) = value.length == 10 && value.all { it.isDigit() }

    /**
     * Comprueba la fuerza de la contraseña.
     * @param value texto a validar.
     * @return `true` si tiene al menos 8 caracteres, una letra y un número.
     */
    fun isStrongPassword(value: String) =
        value.length >= 8 && value.any { it.isLetter() } && value.any { it.isDigit() }

    /**
     * Comprueba el formato de hora de 12 horas con sufijo am/pm, p. ej. "8:00 am".
     * @param value texto a validar (se recorta).
     * @return `true` si coincide con el formato esperado.
     */
    fun isValidTime(value: String) = timeRegex.matches(value.trim())   // "8:00 am"

    /**
     * Comprueba que el enlace use http o https.
     * @param value texto a validar.
     * @return `true` si empieza por `http://` o `https://`.
     */
    fun isValidUrl(value: String) = value.startsWith("http://") || value.startsWith("https://")

    /**
     * Comprueba que haya nombre y apellidos (al menos dos palabras de dos letras cada una).
     * @param value texto a validar (se recorta y se separa por espacios).
     * @return `true` si tiene al menos dos palabras válidas.
     */
    fun isValidName(value: String): Boolean {
        val words = value.trim().split(Regex("\\s+"))
        return words.size >= 2 && words.all { it.length >= 2 }
    }
    /**
     * "8:00 am" -> 480, "12:30 pm" -> 750, "12:00 am" -> 0. Devuelve null si no es válida.
     *
     * @param value hora a convertir, p. ej. "8:00 am".
     * @return minutos desde la medianoche (480, 750, 0...), o null si [value] no es válida.
     */
    fun timeToMinutes(value: String): Int? {
        if (!isValidTime(value)) return null
        val clean = value.trim().lowercase().replace(" ", "")
        val isPm = clean.endsWith("pm")
        val parts = clean.dropLast(2).split(":")
        var hour = parts[0].toInt() % 12      // 12 am y 12 pm quedan en 0 antes de sumar
        if (isPm) hour += 12
        return hour * 60 + parts[1].toInt()
    }
}
