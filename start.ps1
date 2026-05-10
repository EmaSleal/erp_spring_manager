# ═══════════════════════════════════════════════════════════
# Script de inicio de la aplicación con variables de entorno
# ═══════════════════════════════════════════════════════════

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   ERP Orders Manager - Inicio de Aplicación      ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

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

# Iniciar aplicación
Write-Host "🚀 Iniciando Spring Boot...`n" -ForegroundColor Yellow

mvn spring-boot:run
