# ========================================
# Script para cargar variables de entorno desde .env.local
# ========================================
# Uso: .\load-env.ps1
# ========================================

$envFile = ".env.local"

if (-not (Test-Path $envFile)) {
    Write-Host "❌ Error: No se encontró el archivo $envFile" -ForegroundColor Red
    Write-Host "📝 Crea el archivo copiando .env.example:" -ForegroundColor Yellow
    Write-Host "   Copy-Item .env.example .env.local" -ForegroundColor Cyan
    exit 1
}

Write-Host "🔧 Cargando variables de entorno desde $envFile..." -ForegroundColor Cyan
Write-Host ""

$loaded = 0
$skipped = 0

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    
    # Ignorar líneas vacías y comentarios
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }
    
    # Ignorar líneas decorativas (═══)
    if ($line -match "^[═=]+$") {
        return
    }
    
    # Buscar patrón KEY=VALUE
    if ($line -match '^([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        # Solo cargar si tiene valor
        if ($value -ne "") {
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
            Write-Host "  ✅ $name" -ForegroundColor Green
            $loaded++
        } else {
            Write-Host "  ⚠️  $name (sin valor)" -ForegroundColor Yellow
            $skipped++
        }
    }
}

Write-Host ""
Write-Host "📊 Resumen:" -ForegroundColor Cyan
Write-Host "  ✅ Variables cargadas: $loaded" -ForegroundColor Green
if ($skipped -gt 0) {
    Write-Host "  ⚠️  Variables omitidas: $skipped" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "🎯 Variables cargadas en esta sesión de PowerShell" -ForegroundColor Green
Write-Host "   Para usarlas en otra terminal, ejecuta este script nuevamente" -ForegroundColor Gray
Write-Host ""

# Verificar algunas variables importantes
Write-Host "🔍 Verificación de variables críticas:" -ForegroundColor Cyan

$criticalVars = @("DB_URL", "DB_USERNAME", "EMAIL_HOST", "EMAIL_USERNAME")
$allPresent = $true

foreach ($var in $criticalVars) {
    $value = [Environment]::GetEnvironmentVariable($var, 'Process')
    if ($value) {
        # Mostrar solo inicio de la variable por seguridad
        $preview = if ($value.Length -gt 30) { $value.Substring(0, 30) + "..." } else { $value }
        Write-Host "  ✅ $var = $preview" -ForegroundColor Green
    } else {
        Write-Host "  ❌ $var (no definida)" -ForegroundColor Red
        $allPresent = $false
    }
}

Write-Host ""

if ($allPresent) {
    Write-Host "✅ ¡Todas las variables críticas están configuradas!" -ForegroundColor Green
    Write-Host "🚀 Puedes ejecutar la aplicación con:" -ForegroundColor Cyan
    Write-Host "   .\mvnw spring-boot:run" -ForegroundColor White
} else {
    Write-Host "⚠️  Algunas variables críticas faltan" -ForegroundColor Yellow
    Write-Host "📝 Verifica tu archivo .env.local" -ForegroundColor Yellow
}

Write-Host ""
