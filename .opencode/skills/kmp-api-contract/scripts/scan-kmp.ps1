# Escanea una app Kotlin Multiplatform y lista archivos/lineas relevantes
# para reconstruir el contrato de API (version Windows / PowerShell).
# Uso: powershell -ExecutionPolicy Bypass -File scan-kmp.ps1 [raiz_proyecto]
param([string]$Root = ".")

$exclude = '\\(build|\.gradle|\.git|node_modules|backend|\.opencode)\\'
function Get-Files($pattern) {
  Get-ChildItem -Path $Root -Recurse -File -Filter $pattern -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch $exclude }
}
function Section($t) { Write-Output ""; Write-Output "==================== $t ====================" }
function Search($regex, $pattern) {
  Get-Files $pattern | Select-String -Pattern $regex -CaseSensitive |
    ForEach-Object { "{0}:{1}: {2}" -f $_.Path, $_.LineNumber, $_.Line.Trim() }
}

$kt = "*.kt"

Section "Modulos y targets KMP"
Get-Files "build.gradle.kts" | Select-String -Pattern 'kotlin\("multiplatform"\)|kotlinMultiplatform' -List | ForEach-Object { $_.Path }

Section "Dependencias de red/serializacion/persistencia"
Search 'ktor-client|ktorfit|kotlinx-serialization|sqldelight|androidx.room|kotlinx-datetime' "*.kts"
Search 'ktor|ktorfit|serialization|sqldelight|room|datetime' "*.toml"

Section "Configuracion del HttpClient / base URL"
Search 'HttpClient\(|defaultRequest|BASE_URL|baseUrl|base_url|URLProtocol|install\((Auth|ContentNegotiation|HttpTimeout)' $kt

Section "Llamadas HTTP (Ktor)"
Search '\.(get|post|put|patch|delete|head)\(|\.request\(|HttpMethod\.' $kt

Section "Interfaces Ktorfit / estilo Retrofit"
Search '@(GET|POST|PUT|PATCH|DELETE|HTTP)\(' $kt

Section "Clases @Serializable (DTOs candidatos)"
Get-Files $kt | Select-String -Pattern '@Serializable' -CaseSensitive | Group-Object Path |
  Sort-Object Count -Descending | ForEach-Object { "{0,5} {1}" -f $_.Count, $_.Name }

Section "@SerialName y configuracion Json"
Search '@SerialName|Json \{|ignoreUnknownKeys|explicitNulls|namingStrategy|encodeDefaults' $kt

Section "Autenticacion"
Search 'bearer|Bearer|Authorization|refreshTokens|loadTokens|BearerTokens|apiKey|X-API-Key' $kt

Section "Manejo de errores"
Search 'HttpResponseValidator|ClientRequestException|ServerResponseException|ResponseException|status\.value|HttpStatusCode\.' $kt

Section "Paginacion"
Search 'page|pageSize|cursor|offset|limit|nextPage|hasMore|totalElements' $kt |
  Where-Object { $_ -match '(?i)param|query|class|val ' } | Select-Object -First 80

Section "Persistencia local (pistas de modelo)"
Get-Files "*.sq" | ForEach-Object { $_.FullName }
Search '@Entity|@Dao|@PrimaryKey|@ForeignKey' $kt

Write-Output ""
Write-Output "Listo. Lee los archivos listados para extraer el contrato completo."
