# ═══════════════════════════════════════════════════════════
# Script de inicio de la aplicación con variables de entorno
# ═══════════════════════════════════════════════════════════

param(
    [switch]$Optimized,  # Usar perfil optimizado de memoria
    [switch]$Monitor     # Activar monitoreo de memoria
)

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   WhatsApp Orders Manager - Inicio de Aplicación      ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Configurar opciones de memoria JVM optimizadas
Write-Host "⚙️  Configurando JVM...`n" -ForegroundColor Cyan

$env:JAVA_OPTS = @"
-Xms256m
-Xmx512m
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-Djava.awt.headless=true
"@ -replace "`r`n", " " -replace "`n", " "

Write-Host "  ✓ Heap máximo: 512 MB" -ForegroundColor Green
Write-Host "  ✓ Metaspace máximo: 256 MB" -ForegroundColor Green
Write-Host "  ✓ GC: G1 (optimizado)" -ForegroundColor Green
Write-Host ""

# Verificar si existe .env.local
if (!(Test-Path ".\.env.local")) {
    Write-Host "❌ Error: No se encontró .env.local" -ForegroundColor Red
    Write-Host "`nPor favor crea el archivo .env.local basándote en .env.local.template`n" -ForegroundColor Yellow
    Write-Host "Comando: Copy-Item .env.local.template .env.local`n" -ForegroundColor Gray
    exit 1
}

# Cargar variables de entorno
Write-Host "🔐 Cargando variables de entorno...`n" -ForegroundColor Cyan

Get-Content ".\.env.local" | ForEach-Object {
    if ($_ -and !$_.StartsWith("#")) {
        $parts = $_ -split "=", 2
        if ($parts.Length -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            
            if ($name -like "*TOKEN*" -or $name -like "*PASSWORD*") {
                Write-Host "  ✅ $name = ********" -ForegroundColor Green
            } else {
                Write-Host "  ✅ $name = $value" -ForegroundColor Green
            }
        }
    }
}

Write-Host "`n✨ Variables cargadas correctamente!`n" -ForegroundColor Green

# Determinar perfil a usar
$profile = "default"
if ($Optimized) {
    $profile = "optimized"
    Write-Host "🎯 Usando perfil OPTIMIZADO (menor consumo de memoria)`n" -ForegroundColor Yellow
}

# Iniciar aplicación
Write-Host "🚀 Iniciando Spring Boot...`n" -ForegroundColor Yellow

# Iniciar monitoreo en segundo plano si se solicitó
if ($Monitor) {
    Write-Host "📊 Iniciando monitor de memoria en 10 segundos...`n" -ForegroundColor Cyan
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 10
        & ".\monitor-memory.ps1" -Intervalo 10 -Duracion 300
    } | Out-Null
}

if ($Optimized) {
    mvn spring-boot:run "-Dspring-boot.run.profiles=$profile"
} else {
    mvn spring-boot:run
}
