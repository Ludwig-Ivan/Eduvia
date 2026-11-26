# Verificación completa de la arquitectura EduvIA: validate → backend → app → stack → E2E.
# Es el equivalente a un `pytest` de todo el sistema; se ejecuta desde la raíz del repo:
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-all.ps1
# Cualquier paso que devuelva un código de salida distinto de cero aborta la suite.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Las variables de entorno permiten adaptar la suite sin editar el script
# (por ejemplo, otra ruta de app u otra tarea de pruebas KMP).
$appDir     = if ($env:APP_DIR)      { $env:APP_DIR }      else { 'Librerias_eduvia' }
$appTestTask = if ($env:APP_TEST_TASK) { $env:APP_TEST_TASK } else { ':shared:testAndroid' }
# Prefijo de proyecto aislado: evita mezclar los contenedores de pruebas con los de desarrollo.
$project    = if ($env:PROJECT)      { $env:PROJECT }      else { 'miapp-test' }
# Puertos propios para no chocar con un stack de desarrollo ya levantado en 8080/5432.
# Atención: no se debe detener un httpd ajeno que esté usando el puerto 8080.
$env:API_PORT = '18080'; $env:DB_PORT = '15432'; $env:ADMINER_PORT = '18081'

# Envuelve cada fase para que un fallo se identifique por su título y se propague.
function Invoke-Step([string]$title, [scriptblock]$action) {
    Write-Host "`n▶ $title" -ForegroundColor Cyan
    & $action
    if ($LASTEXITCODE -ne 0) { throw "Falló: $title (código $LASTEXITCODE)" }
}

try {
    # 1) Validar la sintaxis de Compose antes de gastar tiempo en compilaciones.
    Invoke-Step '1/5 Validando docker-compose.yml' { docker compose -p $project config -q }
    # 2) `check` incluye tests, verificación de contrato y JaCoCo (ver docs/pruebas.md).
    Invoke-Step '2/5 Backend: pruebas + contrato + cobertura' {
        Push-Location backend; try { .\gradlew.bat --no-daemon check } finally { Pop-Location }
    }
    # 3) Pruebas compartidas de la app KMP con MockEngine; no necesitan emulador.
    Invoke-Step '3/5 App KMP: pruebas' {
        Push-Location $appDir; try { .\gradlew.bat --no-daemon $appTestTask } finally { Pop-Location }
    }
    # 4) Stack aislado: `--wait` espera a los healthchecks antes de pasar a E2E.
    Invoke-Step '4/5 Levantando stack aislado' { docker compose -p $project up -d --build --wait }
    # 5) E2E contra la API en ejecución; E2E_BASE_URL apunta al puerto publicado arriba.
    Invoke-Step '5/5 Pruebas E2E' {
        $env:E2E_BASE_URL = "http://localhost:$($env:API_PORT)"
        Push-Location backend; try { .\gradlew.bat --no-daemon e2eTest } finally { Pop-Location }
    }
    Write-Host "`n✅ Toda la arquitectura pasó las pruebas" -ForegroundColor Green
}
finally {
    # Siempre se limpia, incluso si falló un paso: `-v` borra el volumen de datos
    # de pruebas para que la próxima ejecución empiece con la BD sembrada desde cero.
    docker compose -p $project down -v --remove-orphans | Out-Null
}
