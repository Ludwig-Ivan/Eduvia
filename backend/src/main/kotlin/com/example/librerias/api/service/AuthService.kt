package com.example.librerias.api.service

import com.example.librerias.api.domain.PasswordResetCode
import com.example.librerias.api.domain.RefreshToken
import com.example.librerias.api.domain.SchoolGroup
import com.example.librerias.api.domain.Student
import com.example.librerias.api.domain.User
import com.example.librerias.api.domain.UserRole
import com.example.librerias.api.repository.PasswordResetCodeRepository
import com.example.librerias.api.repository.RefreshTokenRepository
import com.example.librerias.api.repository.SchoolGroupRepository
import com.example.librerias.api.repository.StudentRepository
import com.example.librerias.api.repository.UserRepository
import com.example.librerias.api.web.badRequest
import com.example.librerias.api.web.conflict
import com.example.librerias.api.web.dto.ChildInfoDto
import com.example.librerias.api.web.dto.FieldErrorDto
import com.example.librerias.api.web.dto.GroupInputDto
import com.example.librerias.api.web.dto.LoginRequest
import com.example.librerias.api.web.dto.RefreshRequest
import com.example.librerias.api.web.dto.RegisterRequest
import com.example.librerias.api.web.dto.SchoolGroupDto
import com.example.librerias.api.web.dto.TokenResponse
import com.example.librerias.api.web.dto.UserDto
import com.example.librerias.api.web.notFound
import com.example.librerias.api.web.unauthorized
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

/**
 * Servicio de identidad, sesión y recuperación de cuenta de la API de Eduvia.
 *
 * Responsabilidad: normalizar los datos de registro (email en minúsculas y teléfono solo con
 * dígitos), guardar contraseñas con hash, emitir el JWT de acceso y el refresh token que consume
 * la app KMP, y gestionar códigos de restablecimiento de un solo uso.
 *
 * Colaboradores: UserRepository, RefreshTokenRepository, PasswordResetCodeRepository,
 * SchoolGroupRepository y StudentRepository permiten completar el alta en una sola transacción;
 * PasswordEncoder aplica bcrypt (incluidos los hashes legacy) y JwtEncoder firma el HS256 cuyos
 * claims (`sub`, `roles`, `email`) son los que lee después el cliente móvil.
 */
@Service
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val resetCodes: PasswordResetCodeRepository,
    private val groups: SchoolGroupRepository,
    private val students: StudentRepository,
    private val encoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    @Value("\${jwt.access-token-seconds}") private val accessTokenSeconds: Long,
    @Value("\${jwt.refresh-token-days}") private val refreshTokenDays: Long,
    @Value("\${app.reset-code-in-response:false}") private val exposeResetCode: Boolean,
) {
    private val random = SecureRandom()

    /**
     * Registra al usuario y, en la misma transacción, crea sus grupos (docente) o vincula a sus
     * hijos (familia), devolviendo una sesión lista para usar. El flujo actual de la app envía
     * `groups` y `children` vacíos y los vincula después, pero el contrato admite ambos para no
     * romper integraciones futuras.
     *
     * @param req datos de alta, con confirmación de contraseña y aceptación de términos.
     * @return TokenResponse con el usuario, los grupos creados, el access token y el refresh token.
     * @throws ApiException 400 si las contraseñas no coinciden, faltan los términos, el teléfono no
     * contiene dígitos o el código de clase no existe; los errores señalan el campo para que la app
     * KMP pueda marcar el formulario.
     * @throws ApiException 409 si el email o el teléfono ya están registrados, si el código de
     * grupo generado se repite o si no se logra uno único: la unicidad evita cuentas duplicadas y
     * códigos de clase ambiguos cuando se comparten con las familias.
     */
    @Transactional
    fun register(req: RegisterRequest): TokenResponse {
        val (email, phone) = validateRegistration(req)
        val user = users.save(
            User(
                fullName = req.fullName.trim(),
                email = email,
                phone = phone,
                role = req.role,
                passwordHash = encoder.encode(req.password)!!,
            ),
        )

        val createdGroups = mutableListOf<SchoolGroup>()
        if (req.role == UserRole.TEACHER) {
            createdGroups += createGroups(req.groups, user.id)
        } else {
            linkChildren(req.children, user.id)
        }
        return tokens(user, createdGroups)
    }

    /**
     * Autentica por email y contraseña, aceptando tanto hashes bcrypt con prefijo como los antiguos
     * sin él para no invalidar cuentas ya migradas. El mensaje es el mismo para correo desconocido
     * y contraseña incorrecta, de modo que el endpoint no revela qué emails están dados de alta.
     *
     * @param req correo y contraseña enviados por la pantalla de login de la app.
     * @return TokenResponse con un access token nuevo y un refresh token recién emitido.
     * @throws ApiException 401 si el usuario no existe o la contraseña no coincide; es el código
     * que dispara el refresco automático del plugin Bearer de Ktor.
     */
    @Transactional
    fun login(req: LoginRequest): TokenResponse {
        val user = users.findByEmailIgnoreCase(normalizeEmail(req.email))
            ?: throw unauthorized("Credenciales inválidas")
        val storedHash = if (user.passwordHash.startsWith("{")) {
            user.passwordHash
        } else {
            // Compatibilidad con hashes bcrypt legacy sin prefijo.
            "{bcrypt}${user.passwordHash}"
        }
        if (!encoder.matches(req.password, storedHash)) {
            throw unauthorized("Credenciales inválidas")
        }
        return tokens(user, emptyList())
    }

    /**
     * Rota el refresh token: revoca el presentado y emite un par nuevo en la misma transacción.
     * La rotación reduce el tiempo de uso de un token robado y permite a la app KMP reintentar la
     * petición original tras un 401, tal como hace su cliente HTTP.
     *
     * @param req refresh token opaco en texto plano; en la BD solo se conserva su SHA-256.
     * @return TokenResponse con un access token y un refresh token nuevos.
     * @throws ApiException 401 si el hash no existe, ya estaba revocado, expiró o el usuario ya no
     * está disponible; un token reutilizado se detecta justamente porque quedó revocado.
     */
    @Transactional
    fun refresh(req: RefreshRequest): TokenResponse {
        val token = refreshTokens.findByTokenHash(sha256(req.refreshToken))
            ?: throw unauthorized("Refresh token inválido")
        val now = Instant.now()
        if (token.revokedAt != null || !token.expiresAt.isAfter(now)) {
            throw unauthorized("Refresh token inválido o expirado")
        }
        token.revokedAt = now
        refreshTokens.save(token)
        val user = users.findById(token.userId).orElseThrow { unauthorized("Usuario no encontrado") }
        return tokens(user, emptyList())
    }

    /**
     * Cierra la sesión revocando el refresh token en lugar de borrarlo: conservar la marca permite
     * distinguir un cierre legítimo de un intento de reutilizar un token ya gastado.
     *
     * @param req refresh token que se quiere invalidar.
     * @return Unit; el controlador responde 204 sin cuerpo, que es lo que acepta `postNoContent`
     * en la app.
     * @throws ApiException 401 si el token no existe o ya estaba revocado.
     */
    @Transactional
    fun logout(req: RefreshRequest) {
        val token = refreshTokens.findByTokenHash(sha256(req.refreshToken))
            ?: throw unauthorized("Refresh token inválido")
        val now = Instant.now()
        if (token.revokedAt != null) throw unauthorized("Refresh token ya revocado")
        token.revokedAt = now
        refreshTokens.save(token)
    }

    /**
     * Consulta de existencia usada por la validación de la app antes de enviar el formulario.
     * Normaliza el correo para que distintas mayúsculas den el mismo resultado, coherente con la
     * unicidad de la base de datos.
     *
     * @param email correo a comprobar, con o sin espacios y en cualquier casilla.
     * @return `true` si ya hay una cuenta con ese correo; `false` en caso contrario.
     */
    fun emailExists(email: String): Boolean = users.existsByEmailIgnoreCase(normalizeEmail(email))

    /**
     * Consulta de existencia equivalente para el teléfono; compara solo dígitos porque así se
     * almacena en el registro y porque la app puede enviarlo con espacios o guiones.
     *
     * @param phone teléfono a comprobar, con formato arbitrario.
     * @return `true` si ya hay una cuenta con ese teléfono normalizado.
     */
    fun phoneExists(phone: String): Boolean = users.existsByPhone(phone.filter { it.isDigit() })

    /**
     * Genera un código de recuperación de 6 dígitos, vigente 10 minutos y con 5 intentos; al emitir
     * uno nuevo se marcan como usados los anteriores del usuario para que solo quede uno vigente.
     * No lanza ApiException: si el correo no existe devuelve `null` y tampoco expone el código salvo
     * que se active `app.reset-code-in-response` para pruebas locales, evitando así enumerar
     * cuentas desde un endpoint público.
     *
     * @param email correo de la cuenta que olvidó su contraseña.
     * @return el código de 6 dígitos solo si está habilitada la exposición de depuración; `null`
     * en caso contrario y también cuando el correo no existe.
     */
    @Transactional
    fun requestResetCode(email: String): String? {
        val user = users.findByEmailIgnoreCase(normalizeEmail(email)) ?: return null
        val code = String.format("%06d", random.nextInt(1_000_000))
        val now = Instant.now()
        resetCodes.findAllByUserId(user.id).forEach { it.usedAt = now }
        resetCodes.save(
            PasswordResetCode(
                userId = user.id,
                code = code,
                expiresAt = now.plus(10, ChronoUnit.MINUTES),
                attemptsLeft = 5,
            ),
        )
        return if (exposeResetCode) code else null
    }

    /**
     * Cambia la contraseña validando el código vigente. Los límites de 6 dígitos, 10 minutos y
     * 5 intentos frenan el adivinado por fuerza bruta y los códigos antiguos: cada fallo consume
     * un intento y el acierto se marca como usado para impedir su reutilización. Se responde con
     * 400 y no con 404 para no confirmar qué correos existen.
     *
     * @param email correo de la cuenta.
     * @param code código de 6 dígitos recibido por el usuario.
     * @param newPassword nueva contraseña, de al menos 6 caracteres según el contrato.
     * @param confirmPassword repetición de la nueva contraseña.
     * @return Unit; el controlador responde 200 sin cuerpo.
     * @throws ApiException 400 si las contraseñas no coinciden, si no hay código vigente, si está
     * vencido o agotado, o si el código introducido es incorrecto (y agota un intento).
     */
    @Transactional
    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            throw badRequest("Las contraseñas no coinciden", listOf(FieldErrorDto("confirmPassword", "No coincide")))
        }
        val user = users.findByEmailIgnoreCase(normalizeEmail(email)) ?: throw badRequest("Código incorrecto")
        val reset = resetCodes.findTopByUserIdAndUsedAtIsNullOrderByIdDesc(user.id)
            ?: throw badRequest("Código incorrecto o agotado")
        val now = Instant.now()
        if (reset.usedAt != null || reset.expiresAt.isBefore(now) || reset.attemptsLeft <= 0) {
            throw badRequest("Código incorrecto o vencido")
        }
        if (reset.code != code) {
            reset.attemptsLeft--
            resetCodes.save(reset)
            throw badRequest("Código incorrecto")
        }
        reset.usedAt = now
        resetCodes.save(reset)
        user.passwordHash = encoder.encode(newPassword)!!
        user.updatedAt = now
        users.save(user)
    }

    /**
     * Resuelve el usuario asociado al `sub` del JWT para el endpoint `/me`, que la app usa al
     * restaurar su sesión en memoria al arrancar.
     *
     * @param userId identificador numérico tomado del subject del token.
     * @return User persistido; el controlador expone solo los campos públicos del DTO.
     * @throws ApiException 404 si el id del token ya no corresponde a ningún usuario.
     */
    fun currentUser(userId: Long) =
        users.findById(userId).orElseThrow { notFound("Usuario no encontrado") }

    private fun tokens(user: User, createdGroups: List<SchoolGroup>): TokenResponse {
        val now = Instant.now()
        val access = accessToken(user, now)
        val rawRefresh = issueRefreshToken(user, now)
        return TokenResponse(
            user = UserDto(
                user.id, user.fullName, user.email, user.phone, user.role,
            ),
            createdGroups = createdGroups.map { SchoolGroupDto(
                it.id, it.name, it.school, it.subject, it.code, it.teacherId, it.studentCount,
            ) },
            accessToken = access,
            refreshToken = rawRefresh,
            expiresIn = accessTokenSeconds,
        )
    }

    private fun accessToken(user: User, now: Instant): String {
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                header,
                JwtClaimsSet.builder()
                    .issuer("eduvia-api")
                    .subject(user.id.toString())
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(accessTokenSeconds))
                    .claim("roles", listOf(user.role.name))
                    .claim("email", user.email)
                    .build(),
            ),
        ).tokenValue
    }

    private fun issueRefreshToken(user: User, now: Instant): String {
        val rawRefresh = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also { random.nextBytes(it) })
        refreshTokens.save(
            RefreshToken(
                userId = user.id,
                tokenHash = sha256(rawRefresh),
                expiresAt = now.plus(refreshTokenDays, ChronoUnit.DAYS),
            ),
        )
        return rawRefresh
    }

    private fun validateRegistration(req: RegisterRequest): Pair<String, String> {
        if (req.password != req.confirmPassword) {
            throw badRequest("Las contraseñas no coinciden", listOf(FieldErrorDto("confirmPassword", "No coincide")))
        }
        if (!req.acceptedTerms) {
            throw badRequest("Debe aceptar los términos", listOf(FieldErrorDto("acceptedTerms", "Requerido")))
        }
        val email = normalizeEmail(req.email)
        val phone = req.phone.filter { it.isDigit() }
        if (phone.isEmpty()) {
            throw badRequest("Teléfono inválido", listOf(FieldErrorDto("phone", "Debe contener dígitos")))
        }
        if (users.existsByEmailIgnoreCase(email)) throw conflict("El email ya está registrado")
        if (users.existsByPhone(phone)) throw conflict("El teléfono ya está registrado")
        return email to phone
    }

    private fun createGroups(inputs: List<GroupInputDto>, teacherId: Long): List<SchoolGroup> =
        inputs.map { input ->
            if (groups.existsByCodeIgnoreCase(generateGroupCode(random))) {
                throw conflict("Código de grupo duplicado")
            }
            groups.save(
                SchoolGroup(
                    name = input.groupName.trim(),
                    school = input.schoolName.trim(),
                    subject = input.subject.trim(),
                    code = uniqueGroupCode(random, groups::existsByCodeIgnoreCase)
                        ?: throw conflict("No se pudo generar un código de grupo único"),
                    teacherId = teacherId,
                ),
            )
        }

    private fun linkChildren(children: List<ChildInfoDto>, parentId: Long) {
        children.forEach { child ->
            val group = groups.findByCodeIgnoreCase(child.classCode.trim())
                ?: throw badRequest("Código de clase inexistente: ${child.classCode}")
            students.save(Student(name = child.studentName.trim(), groupId = group.id, parentId = parentId))
            group.studentCount++
            groups.save(group)
        }
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    private fun sha256(value: String) =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)),
        )

}
