package com.example.librerias.api.config

import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * Configuración de seguridad HTTP y de acceso a métodos de la API.
 *
 * Responsabilidad: exigir un JWT Bearer en todas las rutas salvo las públicas, mantener la
 * comunicación sin estado y convertir los fallos de seguridad en `ProblemDetail` (RFC 9457) con el
 * formato que ya interpreta la app KMP. Colabora con los beans de JwtConfig para validar la firma,
 * con el convertidor de autoridades para traducir los roles del token y con el codificador de
 * contraseñas usado por el servicio de autenticación.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper,
) {
    /**
     * Codificador que admite tanto los hashes bcrypt antiguos como los nuevos con prefijo: así no
     * se obliga a cambiar de contraseña a las cuentas ya migradas.
     *
     * @return codificador delegado para el servicio de autenticación.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    /**
     * Traduce los roles del JWT a autoridades de Spring Security.
     *
     * @return convertidor que lee el claim `roles` emitido por AuthService y le antepone `ROLE_`,
     * que es el prefijo que esperan `hasRole` y `@PreAuthorize`; sin él, un token válido no daría
     * ningún permiso a la app.
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        // El token guarda ["TEACHER"] en "roles", pero Spring solo reconoce "ROLE_TEACHER".
        val authorities = JwtGrantedAuthoritiesConverter().apply {
            setAuthorityPrefix("ROLE_")
            setAuthoritiesClaimName("roles")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authorities)
        }
    }

    /**
     * Escribe el error de seguridad como `ProblemDetail`, con `application/problem+json`.
     *
     * @param response respuesta HTTP donde se serializa el problema.
     * @param status código que debe ver la app (401 o 403).
     * @param detail mensaje legible para el usuario.
     * @param authenticate si se añade el desafío Bearer: solo en el 401, porque en el 403 el token
     * es válido y reintentar con otro no cambiaría el resultado.
     */
    private fun writeProblem(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        detail: String,
        authenticate: Boolean,
    ) {
        val pd = ProblemDetail.forStatusAndDetail(status, detail)
        pd.title = status.reasonPhrase
        if (authenticate) {
            // RFC 6750: el 401 debe decir al cliente (incluido el plugin Bearer de Ktor) que
            // puede refrescar el token y repetir la petición original.
            response.setHeader("WWW-Authenticate", "Bearer")
        }
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(pd))
    }

    /**
     * Define la cadena de filtros: sin sesión ni CSRF, rutas públicas restringidas, errores de
     * seguridad como ProblemDetail y validación OAuth2 del JWT entrante.
     *
     * @param http configuración HTTP facilitada por Spring Security.
     * @return cadena de seguridad resultante.
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        // API stateless con Bearer: no hay cookie de sesión que proteger, y exigir token CSRF
        // añadiría fricción a los clientes móviles sin aportar seguridad.
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            // Públicas: auth (login, registro, refresh y recuperación), health de Actuator para
            // los healthchecks, Swagger/OpenAPI para consultar el contrato y /error para poder
            // renderizar los fallos como ProblemDetail. Todo lo demás exige JWT.
            it.requestMatchers(
                "/api/v1/auth/**",
                "/actuator/health/**",
                "/actuator/health",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/error",
            ).permitAll().anyRequest().authenticated()
        }
        .exceptionHandling {
            it.authenticationEntryPoint(AuthenticationEntryPoint { _, res, _: AuthenticationException ->
                writeProblem(res, HttpStatus.UNAUTHORIZED, "Se requiere un token Bearer válido", true)
            })
            it.accessDeniedHandler(AccessDeniedHandler { _, res, _ ->
                writeProblem(res, HttpStatus.FORBIDDEN, "No tiene permisos para este recurso", false)
            })
        }
        .oauth2ResourceServer { oauth2 ->
            oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
        }
        .build()
}
