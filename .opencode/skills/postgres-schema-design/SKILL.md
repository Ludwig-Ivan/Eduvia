---
name: postgres-schema-design
description: Diseña el esquema PostgreSQL (tablas, tipos, claves, índices, restricciones) a partir del contrato de API y los DTOs de una app KMP, y lo escribe como migraciones Flyway versionadas. Úsala al crear o cambiar el modelo de datos del backend.
license: MIT
compatibility: opencode
---

# Diseño de esquema PostgreSQL + migraciones Flyway

Entrada: `docs/backend/api-contract.md` y `openapi.yaml`. Pistas adicionales: archivos `.sq` (SQLDelight) o `@Entity` (Room) de la app.

## 1. De DTOs a entidades

- Un DTO de API **no** es una tabla automáticamente. Identifica entidades reales (sustantivos con identidad y ciclo de vida) y separa:
  - Objetos anidados con identidad propia → tabla con FK.
  - Objetos anidados sin identidad (dirección, coordenadas) → columnas embebidas o `jsonb` si son variables.
  - Listas de primitivos (`List<String>` de tags) → tabla hija, o `text[]` si nunca se consultan por separado.
  - Campos calculados (totales, contadores) → no se guardan salvo que haya motivo de rendimiento; documentarlo.

## 2. Mapeo de tipos

| Kotlin (DTO) | PostgreSQL | Nota |
|---|---|---|
| `String` id | `uuid` | `default gen_random_uuid()` (PG 13+) |
| `Long` id | `bigint generated always as identity` | |
| `String` | `text` o `varchar(n)` | `varchar(n)` solo si hay límite de negocio real |
| `Int` / `Long` | `integer` / `bigint` | |
| `Double` | `double precision` | **Dinero → `numeric(19,4)`**, nunca double |
| `Boolean` | `boolean not null default false` | |
| `Instant` | `timestamptz` | Siempre con zona |
| `LocalDate` | `date` | |
| `LocalDateTime` | `timestamp` | Evitar si se puede; preferir `timestamptz` |
| `enum class` | `varchar(32)` + `check (col in (...))` | Más fácil de migrar que un `enum` de PG |
| Objeto libre / mapa | `jsonb` | Solo si la estructura es realmente variable |

Nulabilidad: si el campo es no nulo en el DTO de respuesta → `not null` en la columna.

## 3. Convenciones

- Nombres en `snake_case`, tablas en **plural** (`orders`, `order_items`).
- PK: `id`. FK: `<entidad_singular>_id` con `references ... on delete` explícito (`cascade` para composición, `restrict` para referencias).
- Toda tabla de negocio: `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`.
- Optimistic locking opcional: `version bigint not null default 0`.
- Soft delete solo si la app lo necesita (`deleted_at timestamptz`).
- Restricciones `unique` para claves naturales (email, sku). Nombrar constraints: `uk_users_email`, `fk_orders_customer`, `ck_orders_status`.

## 4. Índices

- Índice en **cada FK**.
- Índices para cada filtro/orden que aparezca como query param en el contrato (`?status=`, `?sort=createdAt`).
- Búsqueda de texto con `ILIKE '%x%'` → considerar `pg_trgm` + índice GIN (extensión en su propia migración).
- No indexar de más; justificar cada índice en `data-model.md`.

## 5. Migraciones Flyway

Ubicación: `backend/src/main/resources/db/migration/`

- `V1__init_schema.sql` – extensiones + tablas base.
- `V2__...sql`, `V3__...sql` – un cambio lógico por archivo.
- **Nunca editar una migración ya aplicada**; crear una nueva.
- Datos de ejemplo para desarrollo: `src/main/resources/db/dev-data/` y activarlos solo con perfil `dev` (`spring.flyway.locations` por perfil), nunca en producción.

Plantilla:

```sql
create extension if not exists pgcrypto;

create table customers (
    id          uuid primary key default gen_random_uuid(),
    email       varchar(320) not null,
    full_name   text not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint uk_customers_email unique (email)
);
```

## 6. Entregable `docs/backend/data-model.md`

- Diagrama ER en Mermaid (`erDiagram`).
- Tabla por entidad: columna, tipo, null, default, origen (campo DTO).
- Mapeo DTO ↔ tabla, señalando campos calculados o derivados.
- Lista de índices con justificación.

## 7. Verificación

Levanta un Postgres temporal y aplica las migraciones:

```bash
docker run --rm -d --name pg-check -e POSTGRES_PASSWORD=pg -p 55432:5432 postgres:17
# esperar a que acepte conexiones, luego:
for f in backend/src/main/resources/db/migration/V*.sql; do
  docker exec -i pg-check psql -U postgres -v ON_ERROR_STOP=1 < "$f"; done
docker stop pg-check
```

O bien, mejor, deja que los tests con Testcontainers (skill `springboot-testing`) apliquen Flyway.
