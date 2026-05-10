# ============================================================================
# Script: Generar Certificado de Prueba (.p12) 
# Autor: Sistema ERP - WhatsApp Orders Manager
# Fecha: 6 de febrero de 2026
# Propósito: Generar un certificado autofirmado SOLO para pruebas locales
# IMPORTANTE: NO USAR EN PRODUCCIÓN - Solo para testing sin conexión a Hacienda
# ============================================================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Generar Certificado de Prueba (.p12)        " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  ADVERTENCIA: Este certificado es SOLO para pruebas locales" -ForegroundColor Yellow
Write-Host "   NO es válido para enviar a Hacienda" -ForegroundColor Yellow
Write-Host ""

# Configuración
$certPath = "certificados"
$certName = "certificado_prueba.p12"
$certPassword = "test1234"
$certSubject = "CN=Empresa Prueba, O=Test Company, C=CR"

# Crear directorio si no existe
if (-not (Test-Path $certPath)) {
    Write-Host "[→] Creando directorio: $certPath" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $certPath | Out-Null
}

$fullPath = Join-Path $certPath $certName

Write-Host "[→] Generando certificado autofirmado..." -ForegroundColor Cyan

try {
    # Generar certificado autofirmado
    $cert = New-SelfSignedCertificate `
        -Subject $certSubject `
        -CertStoreLocation "Cert:\CurrentUser\My" `
        -KeyExportPolicy Exportable `
        -KeySpec Signature `
        -KeyLength 2048 `
        -KeyAlgorithm RSA `
        -HashAlgorithm SHA256 `
        -NotAfter (Get-Date).AddYears(1) `
        -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.2")
    
    Write-Host "    ✅ Certificado generado en almacén temporal" -ForegroundColor Green
    
    # Exportar a .p12
    $certBytes = $cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Pkcs12, $certPassword)
    [System.IO.File]::WriteAllBytes($fullPath, $certBytes)
    
    Write-Host "    ✅ Certificado exportado a: $fullPath" -ForegroundColor Green
    
    # Limpiar del almacén temporal
    Remove-Item "Cert:\CurrentUser\My\$($cert.Thumbprint)" -Force
    
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "  ✅ CERTIFICADO GENERADO EXITOSAMENTE        " -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📍 Ubicación: $fullPath" -ForegroundColor White
    Write-Host "🔑 PIN: $certPassword" -ForegroundColor White
    Write-Host ""
    Write-Host "Configurar en el sistema:" -ForegroundColor Cyan
    Write-Host "  Ruta del Certificado: certificados/certificado_prueba.p12" -ForegroundColor White
    Write-Host "  PIN del Certificado: $certPassword" -ForegroundColor White
    Write-Host ""
    Write-Host "⚠️  RECORDATORIO:" -ForegroundColor Yellow
    Write-Host "   - Este certificado es AUTOFIRMADO" -ForegroundColor Yellow
    Write-Host "   - Solo sirve para pruebas SIN conexión a Hacienda" -ForegroundColor Yellow
    Write-Host "   - Para sandbox de Hacienda, usa el certificado oficial de pruebas" -ForegroundColor Yellow
    Write-Host "   - Para producción, necesitas certificado de autoridad certificadora" -ForegroundColor Yellow
    
} catch {
    Write-Host ""
    Write-Host "❌ Error generando certificado: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Write-Host "Presiona cualquier tecla para salir..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
