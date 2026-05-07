# ============================================================================
# Script: Descargar Esquemas XSD de Hacienda Costa Rica v4.4
# Autor: Sistema ERP - WhatsApp Orders Manager
# Fecha: 2 de febrero de 2026
# Propósito: Descargar automáticamente los esquemas XSD oficiales de Hacienda
# ============================================================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Descarga de Esquemas XSD - Hacienda CR v4.4  " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuración
$baseUrl = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/"
$destDir = "src/main/resources/xsd/"

# Crear directorio si no existe
if (-not (Test-Path $destDir)) {
    Write-Host "[INFO] Creando directorio: $destDir" -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
}

# Lista de archivos a descargar
$archivos = @(
    @{
        Nombre = "FacturaElectronica_V4.4.xsd"
        Descripcion = "Factura Electrónica"
    },
    @{
        Nombre = "TiqueteElectronico_V4.4.xsd"
        Descripcion = "Tiquete Electrónico"
    },
    @{
        Nombre = "NotaCreditoElectronica_V4.4.xsd"
        Descripcion = "Nota de Crédito"
    },
    @{
        Nombre = "NotaDebitoElectronica_V4.4.xsd"
        Descripcion = "Nota de Débito"
    }
)

# Contadores
$exitosos = 0
$fallidos = 0

# Descargar cada archivo
foreach ($archivo in $archivos) {
    $nombre = $archivo.Nombre
    $descripcion = $archivo.Descripcion
    $url = $baseUrl + $nombre
    $destino = Join-Path $destDir $nombre
    
    Write-Host "[→] Descargando: $descripcion ($nombre)" -ForegroundColor Cyan
    
    try {
        # Intentar descargar
        Invoke-WebRequest -Uri $url -OutFile $destino -TimeoutSec 30 -ErrorAction Stop
        
        # Verificar que el archivo existe y tiene contenido
        if ((Test-Path $destino) -and ((Get-Item $destino).Length -gt 0)) {
            $tamano = (Get-Item $destino).Length
            $tamanoKB = [Math]::Round($tamano / 1KB, 2)
            Write-Host "    ✅ Descargado exitosamente ($tamanoKB KB)" -ForegroundColor Green
            $exitosos++
        } else {
            Write-Host "    ❌ Error: Archivo vacío o no creado" -ForegroundColor Red
            $fallidos++
        }
    }
    catch {
        Write-Host "    ❌ Error: $($_.Exception.Message)" -ForegroundColor Red
        $fallidos++
    }
    
    Write-Host ""
}

# Resumen
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "              RESUMEN DE DESCARGA              " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Archivos exitosos: $exitosos" -ForegroundColor Green
Write-Host "  Archivos fallidos:  $fallidos" -ForegroundColor $(if ($fallidos -gt 0) { "Red" } else { "Green" })
Write-Host ""

# Verificación final
Write-Host "Verificando archivos descargados..." -ForegroundColor Yellow
Write-Host ""

$todosPresentes = $true
foreach ($archivo in $archivos) {
    $ruta = Join-Path $destDir $archivo.Nombre
    $existe = Test-Path $ruta
    
    $icono = if ($existe) { "✅" } else { "❌"; $todosPresentes = $false }
    $color = if ($existe) { "Green" } else { "Red" }
    
    Write-Host "  $icono $($archivo.Nombre)" -ForegroundColor $color
}

Write-Host ""

# Mensaje final
if ($todosPresentes) {
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "  ✅ DESCARGA COMPLETADA CON ÉXITO             " -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Todos los esquemas XSD están listos para usar." -ForegroundColor Green
    Write-Host "El sistema ahora puede validar XMLs contra los esquemas oficiales de Hacienda." -ForegroundColor White
    Write-Host ""
    Write-Host "Siguiente paso:" -ForegroundColor Cyan
    Write-Host "  1. Configurar certificado digital (.p12)" -ForegroundColor White
    Write-Host "  2. Configurar credenciales de API de Hacienda" -ForegroundColor White
    Write-Host "  3. Ver: docs/sprints/SPRINT_5/CHECKLIST_FACTURACION_ELECTRONICA.md" -ForegroundColor White
} else {
    Write-Host "================================================" -ForegroundColor Red
    Write-Host "  ⚠️ DESCARGA INCOMPLETA                       " -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Algunos archivos no se descargaron correctamente." -ForegroundColor Yellow
    Write-Host "Por favor, verifica tu conexión a internet e intenta nuevamente." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Alternativamente, puedes descargar manualmente desde:" -ForegroundColor Cyan
    Write-Host "  $baseUrl" -ForegroundColor White
}

Write-Host ""
Write-Host "Presiona cualquier tecla para salir..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
