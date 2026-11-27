package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.AuthRepository
import com.example.librerias.domain.repository.GroupRepository
import com.example.librerias.domain.util.EmailSender
import com.example.librerias.domain.validation.Validators
import com.example.librerias.model.AppResult
import com.example.librerias.model.RegisterInput
import com.example.librerias.model.RegistrationResult
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.User
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.StateFlow

/**
 * Expone la sesión como `StateFlow` para que la UI reaccione a login y logout.
 * La sesión se guarda solo en memoria (`SessionStore`, con el token de la API): al reiniciar
 * el proceso se pierde, así que siempre hay que leerla de aquí y no de un estado local.
 *
 * @param auth repositorio del que se lee el usuario activo.
 * @return flujo con el usuario con sesión, o `null` si no hay sesión.
 */
class ObserveSessionUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<User?> = auth.currentUser
}

/**
 * Cierra la sesión en memoria de forma inmediata; en la implementación HTTP también revoca
 * el refresh token. No pide confirmación porque no destruye datos del usuario.
 *
 * @param auth repositorio que limpia la sesión.
 * @return sin valor: `currentUser` pasa a `null`.
 */
class LogoutUseCase(private val auth: AuthRepository) {
    operator fun invoke() = auth.logout()
}

/**
 * Valida el formato antes de pedir el login: correo válido y contraseña no vacía.
 * Así se evita una llamada a la API con datos claramente incorrectos y se muestra el error
 * de formato antes que el de credenciales.
 *
 * @param auth repositorio que comprueba las credenciales.
 * @return el usuario autenticado, o `AppResult.Error` con el primer error de formato o de login.
 */
class LoginUseCase(private val auth: AuthRepository) {
    /**
     * @param email correo del usuario; se recorta antes de delegar en el repositorio.
     * @param password contraseña tal como la escribió el usuario (sin recortar).
     * @return el usuario con la sesión iniciada, o `AppResult.Error` si falla la validación o el login.
     */
    suspend operator fun invoke(email: String, password: String): AppResult<User> {
        if (!Validators.isValidEmail(email)) return AppResult.Error("Escribe un correo válido.")
        if (password.isBlank()) return AppResult.Error("Escribe tu contraseña.")
        return auth.login(email.trim(), password)
    }
}

/**
 * Paso 1 del registro: valida todo y que correo y celular no existan.
 *
 * Validación por pasos: primero el formato (nombre, correo, celular de 10 dígitos,
 * contraseña fuerte y confirmación) y solo después consulta duplicados. Así se muestra primero
 * el error que el usuario puede corregir y se evitan consultas innecesarias.
 *
 * @param auth repositorio donde se consulta si ya existen el correo y el celular.
 * @return `AppResult.Success(Unit)` si el paso es válido, o el primer `AppResult.Error` encontrado.
 */
class ValidateAccountStepUseCase(private val auth: AuthRepository) {
    /**
     * @param fullName nombre y apellidos (al menos dos palabras de dos letras).
     * @param email correo con formato válido.
     * @param phone celular de exactamente 10 dígitos.
     * @param password contraseña de al menos 8 caracteres, con letra y número.
     * @param confirmPassword debe coincidir con [password].
     * @return `Unit` si pasa la validación; si no, el mensaje de error correspondiente.
     */
    suspend operator fun invoke(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): AppResult<Unit> {
        val formatError = when {
            !Validators.isValidName(fullName) -> "Escribe tu nombre y apellidos."
            !Validators.isValidEmail(email) -> "Escribe un correo válido. Ej. nombre@correo.com"
            !Validators.isValidPhone(phone) -> "El celular debe tener 10 dígitos."
            !Validators.isStrongPassword(password) ->
                "La contraseña debe tener mínimo 8 caracteres, una letra y un número."
            password != confirmPassword -> "Las contraseñas no coinciden."
            else -> null
        }
        if (formatError != null) return AppResult.Error(formatError)

        if (auth.emailExists(email))
            return AppResult.Error("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña.")
        if (auth.phoneExists(phone))
            return AppResult.Error("Ese número de celular ya está registrado en otra cuenta.")
        return AppResult.Success(Unit)
    }
}

/**
 * Registro completo, tratado como operación atómica desde la app: se descartan filas vacías,
 * se valida todo y, para los padres, se comprueban los códigos de clase ANTES de crear la cuenta;
 * cualquier error se devuelve sin continuar con los pasos siguientes.
 *
 * La petición HTTP de registro envía `children` y `groups` vacíos por limitación de la app
 * (el DTO de `auth/register` solo acepta los datos de la cuenta). Por eso, después de crear el usuario,
 * los grupos se crean con `createGroup` y los hijos se vinculan con `linkStudent` desde aquí.
 *
 * @param auth repositorio que crea la cuenta (y vuelve a revisar correo y celular).
 * @param groups repositorio usado para validar códigos, crear grupos y vincular hijos.
 * @return el usuario y los grupos creados, o `AppResult.Error` en el primer paso que falle.
 */
class RegisterUseCase(
    private val auth: AuthRepository,
    private val groups: GroupRepository
) {
    /**
     * @param rawInput datos del formulario; las filas totalmente vacías se ignoran.
     * @return `RegistrationResult` con el usuario y los grupos creados (vacío para padres), o el error.
     */
    suspend operator fun invoke(rawInput: RegisterInput): AppResult<RegistrationResult> {
        // Ignora filas totalmente vacías (hijos o grupos que no se llenaron)
        val input = rawInput.copy(
            children = rawInput.children.filterNot { it.classCode.isBlank() && it.studentName.isBlank() },
            groups = rawInput.groups.filterNot {
                it.groupName.isBlank() && it.schoolName.isBlank() && it.subject.isBlank()
            }
        )

        validate(input)?.let { return AppResult.Error(it) }

        // Padres: verifica los códigos ANTES de crear la cuenta
        if (input.role == UserRole.PARENT) {
            for (child in input.children) {
                if (groups.findByCode(child.classCode) == null) {
                    return AppResult.Error(
                        "El código ${child.classCode.trim()} no existe. Pídele el correcto al maestro."
                    )
                }
            }
        }

        // auth.register vuelve a revisar correo y celular repetidos (última barrera)
        val user = when (
            val r = auth.register(input.role, input.fullName, input.email, input.phone, input.password)
        ) {
            is AppResult.Success -> r.value
            is AppResult.Error -> return r
        }

        val createdGroups = mutableListOf<SchoolGroup>()
        when (input.role) {
            UserRole.TEACHER -> for (g in input.groups) {
                when (val r = groups.createGroup(user.id, g)) {
                    is AppResult.Success -> createdGroups.add(r.value)
                    is AppResult.Error -> return r
                }
            }
            UserRole.PARENT -> for (child in input.children) {
                val r = groups.linkStudent(user.id, child)
                if (r is AppResult.Error) return r
            }
        }
        return AppResult.Success(RegistrationResult(user, createdGroups))
    }

    private fun validate(i: RegisterInput): String? = when {
        !Validators.isValidName(i.fullName) -> "Escribe tu nombre y apellidos."
        !Validators.isValidEmail(i.email) -> "Escribe un correo válido."
        !Validators.isValidPhone(i.phone) -> "El celular debe tener 10 dígitos."
        !Validators.isStrongPassword(i.password) ->
            "La contraseña debe tener mínimo 8 caracteres, una letra y un número."
        i.password != i.confirmPassword -> "Las contraseñas no coinciden."
        i.role == UserRole.PARENT && i.children.isEmpty() ->
            "Agrega al menos un hijo con su código de clase."
        i.role == UserRole.PARENT &&
                i.children.any { it.classCode.isBlank() || it.studentName.isBlank() } ->
            "Completa el código y el nombre de cada hijo."
        i.role == UserRole.TEACHER && i.groups.isEmpty() -> "Crea al menos un grupo."
        i.role == UserRole.TEACHER &&
                i.groups.any { it.groupName.isBlank() || it.schoolName.isBlank() } ->
            "Completa el nombre del grupo y la escuela."
        !i.acceptedTerms -> "Debes aceptar los Términos de Servicio y la Política de Privacidad."
        else -> null
    }
}

/**
 * Envía el código de recuperación de 6 dígitos mediante [EmailSender].
 * Siempre devuelve éxito, exista o no la cuenta, para no revelar quién tiene usuario registrado;
 * en la demo el correo se deja en la bandeja de `FakeEmailSender` en vez de enviarse.
 *
 * @param auth repositorio que genera y guarda el código (vence a los 10 minutos).
 * @param sender abstracción del envío de correo.
 * @return `AppResult.Success(Unit)` incluso si [email] no existe; error solo si el formato es inválido.
 */
class SendResetCodeUseCase(
    private val auth: AuthRepository,
    private val sender: EmailSender
) {
    /**
     * @param email correo al que se envía el código; se recorta antes de usarlo.
     * @return `Unit` en éxito, o `AppResult.Error` si el correo no tiene formato válido.
     */
    suspend operator fun invoke(email: String): AppResult<Unit> {
        if (!Validators.isValidEmail(email)) return AppResult.Error("Escribe un correo válido.")
        val code = auth.createResetCode(email.trim())
        if (code != null) sender.sendResetCode(email.trim(), code)
        // Misma respuesta exista o no el correo, para no revelar quién tiene cuenta.
        return AppResult.Success(Unit)
    }
}

/**
 * Restablece la contraseña validando por pasos: correo, código de exactamente 6 dígitos,
 * contraseña fuerte y confirmación. El repositorio es quien comprueba caducidad, intentos y
 * que el código sea de un solo uso, para que esas reglas no se puedan saltar desde la UI.
 *
 * @param auth repositorio que valida el código y aplica la contraseña nueva.
 * @return `AppResult.Success(Unit)` si se cambió, o `AppResult.Error` con el primer fallo.
 */
class ResetPasswordUseCase(private val auth: AuthRepository) {
    /**
     * @param email correo de la cuenta; se recorta antes de delegar.
     * @param code código de 6 dígitos enviado por correo.
     * @param newPassword contraseña nueva, fuerte y confirmada.
     * @param confirmPassword debe coincidir con [newPassword].
     * @return `Unit` en éxito, o el error de validación o del repositorio.
     */
    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): AppResult<Unit> {
        if (!Validators.isValidEmail(email)) return AppResult.Error("Escribe un correo válido.")
        if (code.length != 6 || !code.all { it.isDigit() })
            return AppResult.Error("El código debe tener 6 dígitos.")
        if (!Validators.isStrongPassword(newPassword))
            return AppResult.Error("La contraseña debe tener mínimo 8 caracteres, una letra y un número.")
        if (newPassword != confirmPassword) return AppResult.Error("Las contraseñas no coinciden.")
        return auth.resetPassword(email.trim(), code, newPassword)
    }
}
