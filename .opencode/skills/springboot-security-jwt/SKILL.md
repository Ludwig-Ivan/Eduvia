---
name: springboot-security-jwt
description: Configura autenticación y autorización en una API Spring Boot para apps móviles KMP - login, emisión y validación de JWT (access + refresh), hash de contraseñas, rutas públicas/protegidas y compatibilidad con el plugin Auth bearer de Ktor. Úsala cuando la app envíe tokens o necesite usuarios.
license: MIT
compatibility: opencode
---

# Seguridad JWT para clientes móviles

Usar solo si el contrato (`kmp-api-contract`) muestra autenticación. Replica lo que la app ya hace: rutas de login/refresh, nombres de campos (`accessToken` vs `access_token`), y cómo se envía el token.

## 1. Dependencias

`security` y `oauth2-resource-server` (Initializr). Esto da validación de JWT nativa con Nimbus; no añadas librerías JWT de terceros salvo necesidad.

## 2. Modelo

Migración Flyway:

```sql
create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null,
    password_hash text not null,
    role varchar(32) not null default 'USER',
    created_at timestamptz not null default now(),
    constraint uk_users_email unique (email)
);
create table refresh_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_refresh_tokens_user on refresh_tokens(user_id);
```

- Contraseñas: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (bcrypt por defecto).
- Refresh tokens: guardar **hash** (SHA-256), nunca el valor en claro. Rotación: al refrescar, revocar el anterior y emitir uno nuevo.

## 3. Tokens

- Access token: JWT firmado, vida corta (10–15 min), claims `sub` (user id), `roles`, `iat`, `exp`, `iss`.
- Refresh token: valor opaco aleatorio (32+ bytes, Base64URL), vida larga (7–30 días).
- Firma: HS256 con secreto de ≥ 256 bits desde env (`JWT_SECRET`) para empezar; RS256 con par de claves si habrá varios servicios.
- Emitir con `JwtEncoder` (`NimbusJwtEncoder`), validar con `JwtDecoder` (`NimbusJwtDecoder`).

## 4. Configuración

```kotlin
@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }                       // API stateless con Bearer
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/v1/auth/**", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
              .anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt { } }
        .build()
}
```

- Mapear claim `roles` a authorities con un `JwtAuthenticationConverter` (prefijo `ROLE_`).
- 401 (sin token / token inválido) y 403 (sin permiso) deben responder en el **formato de error del contrato**: configurar `authenticationEntryPoint` y `accessDeniedHandler`.

## 5. Compatibilidad con Ktor `Auth { bearer { } }`

- Ktor solo dispara `refreshTokens` cuando recibe **401** con header `WWW-Authenticate: Bearer`. Asegúrate de que el entry point lo envíe (el de resource server lo hace por defecto; si lo personalizas, mantenlo).
- El endpoint de refresh debe aceptar el refresh token tal como la app lo envía (body JSON o header) y devolver la misma forma que el login.
- Nunca devolver 403 para token expirado (la app no refrescaría).

## 6. Endpoints típicos

| Endpoint | Body | Respuesta |
|---|---|---|
| `POST /api/v1/auth/register` | email, password | 201 + tokens (o 201 sin tokens, según app) |
| `POST /api/v1/auth/login` | email, password | 200 `{accessToken, refreshToken, expiresIn}` |
| `POST /api/v1/auth/refresh` | refreshToken | 200 nuevos tokens / 401 |
| `POST /api/v1/auth/logout` | refreshToken | 204 (revoca) |
| `GET /api/v1/me` | — | 200 usuario actual |

## 7. Buenas prácticas

- Mensaje de error de login genérico ("credenciales inválidas"); no revelar si el email existe.
- Rate limiting en login (p. ej. Bucket4j) si se expone públicamente.
- Validar longitud mínima de contraseña.
- Obtener usuario actual con `@AuthenticationPrincipal jwt: Jwt` → `jwt.subject`.
- Comprobar propiedad de recursos en el servicio (un usuario no accede a pedidos de otro) → 404 o 403 según contrato.

## 8. Verificación (tests)

Login ok/fallido, acceso sin token (401 + `WWW-Authenticate`), token expirado (401), refresh con rotación, refresh revocado (401), rol insuficiente (403).
