---
name: springboot-jpa-postgres
description: Implementa la capa de persistencia de Spring Boot sobre PostgreSQL con Spring Data JPA/Hibernate y Flyway - entidades alineadas con las migraciones, repositorios, consultas, relaciones, auditoría y transacciones, evitando N+1. Úsala al crear o cambiar entidades y repositorios.
license: MIT
compatibility: opencode
---

# Persistencia: Spring Data JPA + PostgreSQL

Entrada: migraciones de `postgres-schema-design` y `docs/backend/data-model.md`. El esquema lo manda Flyway; las entidades se ajustan a él (`ddl-auto: validate` lo comprueba al arrancar).

## 1. Entidades (Kotlin)

```kotlin
@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    var customer: CustomerEntity,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItemEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    var version: Long = 0,
)
```

Reglas:
- **No usar `data class`** para entidades (equals/hashCode/toString rompen con proxies y relaciones). Clase normal; `equals/hashCode` basados en `id` si hace falta.
- `@Enumerated(EnumType.STRING)` siempre, nunca ORDINAL.
- `@ManyToOne(fetch = LAZY)` siempre (por defecto es EAGER).
- Dinero: `BigDecimal` con `@Column(precision = 19, scale = 4)`.
- `jsonb`: `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6+).
- Arrays `text[]`: `@JdbcTypeCode(SqlTypes.ARRAY)`.
- Auditoría: `@PrePersist/@PreUpdate` o `@EnableJpaAuditing` + `@CreatedDate/@LastModifiedDate`.

En Java: misma idea; constructor protegido sin args, getters/setters, sin Lombok `@Data` en entidades.

## 2. Repositorios

```kotlin
interface OrderRepository : JpaRepository<OrderEntity, UUID> {
    fun findAllByCustomerId(customerId: UUID, pageable: Pageable): Page<OrderEntity>

    @EntityGraph(attributePaths = ["items", "customer"])
    fun findWithItemsById(id: UUID): OrderEntity?

    fun existsByCustomerIdAndStatus(customerId: UUID, status: OrderStatus): Boolean
}
```

- Métodos derivados para consultas simples; `@Query` JPQL para las complejas; `nativeQuery = true` solo para funciones específicas de PG (full-text, `jsonb` ops).
- Filtros dinámicos (muchos query params opcionales): `JpaSpecificationExecutor` + `Specification`.
- Proyecciones (interfaces o DTO constructors) para listados grandes.

## 3. Evitar N+1

- Listados que muestran relaciones → `@EntityGraph` o `join fetch`.
- No paginar con `join fetch` sobre colecciones (Hibernate pagina en memoria); en su lugar: paginar IDs y luego cargar con fetch, o `@BatchSize`.
- Activa en `dev`: `spring.jpa.properties.hibernate.generate_statistics: true` o logs SQL para revisar.

## 4. Transacciones

- `@Transactional` en **servicios**, no en controllers ni repositorios.
- Lecturas: `@Transactional(readOnly = true)`.
- Mapear a DTO **dentro** de la transacción (con `open-in-view: false`, fuera daría `LazyInitializationException`).
- `OptimisticLockingFailureException` → 409 en el handler global.

## 5. Verificación

- La app arranca con `ddl-auto: validate` sin errores (las entidades coinciden con Flyway).
- Tests de repositorio con `@DataJpaTest` + Testcontainers (ver `springboot-testing`), **no H2** (tipos de PG como `jsonb`, `uuid`, `timestamptz` se comportan distinto).
