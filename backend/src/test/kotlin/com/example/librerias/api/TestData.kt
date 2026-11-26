package com.example.librerias.api

import org.springframework.jdbc.core.JdbcTemplate

/**
 * Constructores de datos mínimos para cada prueba, sin depender del orden ni del seed del perfil
 * `dev` (el perfil `test` solo aplica migraciones).
 *
 * [user], [group] y [student] insertan por JDBC lo justo para llegar al endpoint que se quiere
 * probar. El contador atómico [next] genera emails y teléfonos distintos para evitar colisiones con
 * la regla de unicidad del contrato (email y teléfono normalizados, 409 si se repiten); [reset] lo
 * devuelve a cero porque [IntegrationTestBase] trunca la base antes de cada prueba. Así cada test
 * puede usar `demo@eduvia.test` como primer usuario sin choques.
 */
class TestData(private val jdbc: JdbcTemplate) {
    companion object {
        private val seq = java.util.concurrent.atomic.AtomicLong(0)

        /** La base se trunca antes de cada test: el contador debe empezar de cero. */
        fun reset() = seq.set(0)

        private fun next(): Long = seq.getAndIncrement()
        private fun email(): String {
            val n = next()
            return if (n == 0L) "demo@eduvia.test" else "demo-$n@eduvia.test"
        }

        private fun phone(): String {
            val n = next()
            return if (n == 0L) "600000001" else "6000${String.format("%05d", n)}"
        }
    }

    fun user(
        fullName: String = "Demo Teacher",
        email: String? = null,
        phone: String? = null,
        role: String = "TEACHER",
        passwordHash: String = "\$2a\$10\$7wLQvFjfpVd2AoQyZPZ1Re8NqRrhLzLJ0HqS4HkO5jZq3b9Q0YyK",
    ): Long = jdbc.queryForObject(
        """insert into users(full_name,email,phone,role,password_hash)
           values (?,?,?,?,?) returning id""",
        Long::class.java,
        fullName, email ?: email(), phone ?: phone(), role, passwordHash,
    )!!

    fun group(
        teacherId: Long,
        name: String = "3º A",
        code: String = "AB-1234",
        subject: String = "Matemáticas",
        studentCount: Int = 0,
    ): Long = jdbc.queryForObject(
        """insert into school_groups(name,school,subject,code,teacher_id,student_count)
           values (?,?,?,?,?,?) returning id""",
        Long::class.java,
        name, "Colegio EduvIA", subject, code, teacherId, studentCount,
    )!!

    fun student(name: String, groupId: Long, parentId: Long? = null): Long = jdbc.queryForObject(
        "insert into students(name,group_id,parent_id) values (?,?,?) returning id",
        Long::class.java,
        name, groupId, parentId,
    )!!
}
