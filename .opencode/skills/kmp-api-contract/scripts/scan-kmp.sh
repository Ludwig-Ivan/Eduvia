#!/usr/bin/env bash
# Escanea una app Kotlin Multiplatform y lista archivos/lineas relevantes
# para reconstruir el contrato de API. Uso: bash scan-kmp.sh [raiz_proyecto]
set -euo pipefail
ROOT="${1:-.}"

EXCLUDES=(--exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git \
          --exclude-dir=node_modules --exclude-dir=backend --exclude-dir=.opencode)

section() { printf '\n==================== %s ====================\n' "$1"; }
search() { grep -rnE "${EXCLUDES[@]}" --include="$2" "$1" "$ROOT" 2>/dev/null || true; }

section "Modulos y targets KMP"
find "$ROOT" -name "build.gradle.kts" -not -path "*/build/*" -not -path "*/backend/*" \
  -exec grep -lE "kotlin\(\"multiplatform\"\)|kotlinMultiplatform" {} \; 2>/dev/null || true

section "Dependencias de red/serializacion/persistencia"
search "ktor-client|ktorfit|kotlinx-serialization|sqldelight|androidx.room|kotlinx-datetime" "*.kts"
search "ktor|ktorfit|serialization|sqldelight|room|datetime" "*.toml"

section "Configuracion del HttpClient / base URL"
search "HttpClient\(|defaultRequest|BASE_URL|baseUrl|base_url|URLProtocol|install\((Auth|ContentNegotiation|HttpTimeout)" "*.kt"

section "Llamadas HTTP (Ktor)"
search "\.(get|post|put|patch|delete|head)\(|\.request\(|HttpMethod\." "*.kt"

section "Interfaces Ktorfit / estilo Retrofit"
search "@(GET|POST|PUT|PATCH|DELETE|HTTP)\(" "*.kt"

section "Clases @Serializable (DTOs candidatos)"
search "@Serializable" "*.kt" | cut -d: -f1 | sort | uniq -c | sort -rn

section "@SerialName y configuracion Json"
search "@SerialName|Json \{|ignoreUnknownKeys|explicitNulls|namingStrategy|encodeDefaults" "*.kt"

section "Autenticacion"
search "bearer|Bearer|Authorization|refreshTokens|loadTokens|BearerTokens|apiKey|X-API-Key" "*.kt"

section "Manejo de errores"
search "HttpResponseValidator|ClientRequestException|ServerResponseException|ResponseException|status\.value|HttpStatusCode\." "*.kt"

section "Paginacion"
search "page|pageSize|cursor|offset|limit|nextPage|hasMore|totalElements" "*.kt" | grep -iE "param|query|class|val " | head -n 80 || true

section "Persistencia local (pistas de modelo)"
find "$ROOT" -name "*.sq" -not -path "*/build/*" 2>/dev/null || true
search "@Entity|@Dao|@PrimaryKey|@ForeignKey" "*.kt"

printf '\nListo. Lee los archivos listados para extraer el contrato completo.\n'
