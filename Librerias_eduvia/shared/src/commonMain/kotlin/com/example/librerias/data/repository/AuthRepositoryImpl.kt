@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.librerias.data.repository

import com.example.librerias.data.SchoolSampleData
import com.example.librerias.domain.repository.AuthRepository
import com.example.librerias.model.AppResult
import com.example.librerias.model.User
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Autenticación EN MEMORIA, solo para la demo.
 * (Las contraseñas están en texto plano: en una app real las guarda un servidor con hash.)
 */
class AuthRepositoryImpl(
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() }
) : AuthRepository {

    private class Account(val user: User, var password: String)
    private class ResetEntry(val code: String, val expiresAt: Long, var attemptsLeft: Int)

    private val accounts = SchoolSampleData.accounts
        .map { (user, password) -> Account(user, password) }
        .toMutableList()

    private val resetCodes = mutableMapOf<String, ResetEntry>()
    private var nextId = (SchoolSampleData.users.maxOfOrNull { it.id } ?: 0L) + 1

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private fun normEmail(e: String) = e.trim().lowercase()
    private fun normPhone(p: String) = p.filter { it.isDigit() }
    private fun findByEmail(email: String) =
        accounts.firstOrNull { normEmail(it.user.email) == normEmail(email) }

    override suspend fun emailExists(email: String): Boolean = findByEmail(email) != null

    override suspend fun phoneExists(phone: String): Boolean {
        val p = normPhone(phone)
        return p.isNotEmpty() && accounts.any { normPhone(it.user.phone) == p }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        val account = findByEmail(email)
        if (account == null || account.password != password) {
            return AppResult.Error("Correo o contraseña incorrectos.")
        }
        _currentUser.value = account.user
        return AppResult.Success(account.user)
    }

    override suspend fun register(
        role: UserRole,
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): AppResult<User> {
        // Última barrera: aunque la UI ya validó, aquí nunca entra un duplicado.
        if (emailExists(email)) return AppResult.Error("Ya existe una cuenta con ese correo.")
        if (phoneExists(phone)) return AppResult.Error("Ya existe una cuenta con ese número de celular.")

        val user = User(nextId++, fullName.trim(), email.trim(), normPhone(phone), role)
        accounts.add(Account(user, password))
        _currentUser.value = user
        return AppResult.Success(user)
    }

    override suspend fun createResetCode(email: String): String? {
        if (findByEmail(email) == null) return null
        val code = Random.nextInt(100_000, 1_000_000).toString()
        // Un código nuevo reemplaza al anterior. Vence en 10 min y permite 5 intentos.
        resetCodes[normEmail(email)] = ResetEntry(code, nowMillis() + 10 * 60 * 1000, 5)
        return code
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): AppResult<Unit> {
        val key = normEmail(email)
        val account = findByEmail(email)
        val entry = resetCodes[key]
        if (account == null || entry == null) {
            return AppResult.Error("El código es incorrecto o ya venció. Solicita uno nuevo.")
        }
        if (nowMillis() > entry.expiresAt) {
            resetCodes.remove(key)
            return AppResult.Error("El código venció. Solicita uno nuevo.")
        }
        if (entry.code != code) {
            entry.attemptsLeft--
            if (entry.attemptsLeft <= 0) {
                resetCodes.remove(key)
                return AppResult.Error("Demasiados intentos. Solicita un código nuevo.")
            }
            return AppResult.Error("Código incorrecto. Te quedan ${entry.attemptsLeft} intentos.")
        }
        account.password = newPassword
        resetCodes.remove(key)          // el código solo sirve una vez
        return AppResult.Success(Unit)
    }

    override fun logout() {
        _currentUser.value = null
    }
}