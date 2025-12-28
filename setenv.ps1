# setenv.ps1 - Configuración de memoria JVM optimizada
# Ejecutar este script ANTES de iniciar la aplicación Spring Boot

Write-Host "`n=== CONFIGURACIÓN DE MEMORIA JVM ===" -ForegroundColor Cyan

# Configuración optimizada de memoria
$env:JAVA_OPTS = @"
-Xms256m
-Xmx512m
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-Djava.awt.headless=true
"@ -replace "`r`n", " " -replace "`n", " "

Write-Host "✓ Variables JAVA_OPTS configuradas correctamente" -ForegroundColor Green
Write-Host ""
Write-Host "Configuración aplicada:" -ForegroundColor Yellow
Write-Host "  • Heap inicial: 256 MB" -ForegroundColor Cyan
Write-Host "  • Heap máximo: 512 MB" -ForegroundColor Cyan
Write-Host "  • Metaspace inicial: 128 MB" -ForegroundColor Cyan
Write-Host "  • Metaspace máximo: 256 MB" -ForegroundColor Cyan
Write-Host "  • GC: G1 (optimizado para baja latencia)" -ForegroundColor Cyan
Write-Host "  • String deduplication: Habilitado" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ahora puedes iniciar la aplicación:" -ForegroundColor Green
Write-Host "  .\start.ps1" -ForegroundColor White
Write-Host "  o" -ForegroundColor White
Write-Host "  .\mvnw spring-boot:run" -ForegroundColor White
Write-Host ""
