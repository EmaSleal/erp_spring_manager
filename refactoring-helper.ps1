# Script de Ayuda para Refactorización Modular
# WhatsApp Orders Manager
# Fecha: 27 de diciembre de 2025

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  SCRIPT DE REFACTORIZACIÓN MODULAR" -ForegroundColor Cyan
Write-Host "  WhatsApp Orders Manager" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Variables de configuración
$baseDir = "D:\programacion\java\spring-boot\whats_orders_manager\src\main\java\api\astro\whats_orders_manager"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# Función para crear backup
function Create-Backup {
    Write-Host "Creando backup del proyecto..." -ForegroundColor Yellow
    
    $backupDir = "D:\programacion\java\spring-boot\whats_orders_manager_backup_$timestamp"
    Copy-Item -Path "D:\programacion\java\spring-boot\whats_orders_manager" -Destination $backupDir -Recurse
    
    Write-Host "✅ Backup creado en: $backupDir" -ForegroundColor Green
    Write-Host ""
}

# Función para crear estructura de módulos
function Create-ModuleStructure {
    Write-Host "Creando estructura de módulos..." -ForegroundColor Yellow
    
    cd $baseDir
    
    # Crear módulos
    $modules = @(
        "producto",
        "cliente",
        "facturacion",
        "whatsapp",
        "notificacion",
        "seguridad",
        "configuracion",
        "reportes",
        "presentacion"
    )
    
    $subfolders = @("controller", "service", "repository", "model", "dto")
    
    foreach ($module in $modules) {
        Write-Host "  - Creando módulo: $module" -ForegroundColor Cyan
        
        foreach ($subfolder in $subfolders) {
            $path = "modules\$module\$subfolder"
            if (-not (Test-Path $path)) {
                New-Item -ItemType Directory -Path $path -Force | Out-Null
            }
        }
        
        # Enums y events para módulos específicos
        if ($module -eq "facturacion" -or $module -eq "whatsapp" -or $module -eq "notificacion" -or $module -eq "seguridad") {
            $enumPath = "modules\$module\enums"
            if (-not (Test-Path $enumPath)) {
                New-Item -ItemType Directory -Path $enumPath -Force | Out-Null
            }
        }
        
        if ($module -eq "notificacion") {
            $eventsPath = "modules\$module\events"
            if (-not (Test-Path $eventsPath)) {
                New-Item -ItemType Directory -Path $eventsPath -Force | Out-Null
            }
        }
    }
    
    # Crear shared y core
    Write-Host "  - Creando carpeta shared" -ForegroundColor Cyan
    New-Item -ItemType Directory -Path "shared\config" -Force | Out-Null
    New-Item -ItemType Directory -Path "shared\exception" -Force | Out-Null
    New-Item -ItemType Directory -Path "shared\util" -Force | Out-Null
    New-Item -ItemType Directory -Path "shared\dto" -Force | Out-Null
    
    Write-Host "  - Creando carpeta core" -ForegroundColor Cyan
    New-Item -ItemType Directory -Path "core\listeners" -Force | Out-Null
    New-Item -ItemType Directory -Path "core\schedulers" -Force | Out-Null
    New-Item -ItemType Directory -Path "core\events" -Force | Out-Null
    
    # Crear .gitkeep en carpetas vacías
    Get-ChildItem -Path "modules" -Recurse -Directory | Where-Object { (Get-ChildItem $_.FullName).Count -eq 0 } | ForEach-Object {
        New-Item -ItemType File -Path "$($_.FullName)\.gitkeep" -Force | Out-Null
    }
    
    Write-Host "✅ Estructura de módulos creada" -ForegroundColor Green
    Write-Host ""
}

# Función para migrar módulo Producto
function Migrate-ProductoModule {
    Write-Host "Migrando módulo Producto..." -ForegroundColor Yellow
    
    cd $baseDir
    
    # Verificar que los archivos existen
    if (Test-Path "controllers\ProductoController.java") {
        Move-Item "controllers\ProductoController.java" "modules\producto\controller\" -Force
        Write-Host "  ✅ ProductoController migrado" -ForegroundColor Green
    }
    
    if (Test-Path "services\ProductoService.java") {
        Move-Item "services\ProductoService.java" "modules\producto\service\" -Force
        Write-Host "  ✅ ProductoService migrado" -ForegroundColor Green
    }
    
    if (Test-Path "repositories\ProductoRepository.java") {
        Move-Item "repositories\ProductoRepository.java" "modules\producto\repository\" -Force
        Write-Host "  ✅ ProductoRepository migrado" -ForegroundColor Green
    }
    
    if (Test-Path "models\Producto.java") {
        Move-Item "models\Producto.java" "modules\producto\model\" -Force
        Write-Host "  ✅ Producto (model) migrado" -ForegroundColor Green
    }
    
    if (Test-Path "models\dto\ProductoDTO.java") {
        Move-Item "models\dto\ProductoDTO.java" "modules\producto\dto\" -Force
        Write-Host "  ✅ ProductoDTO migrado" -ForegroundColor Green
    }
    
    Write-Host "✅ Módulo Producto migrado" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE: Ahora debes actualizar los packages en los archivos Java" -ForegroundColor Yellow
    Write-Host "   Usa IntelliJ IDEA: Ctrl + Shift + R para Find & Replace" -ForegroundColor Yellow
    Write-Host ""
}

# Función para migrar módulo Cliente
function Migrate-ClienteModule {
    Write-Host "Migrando módulo Cliente..." -ForegroundColor Yellow
    
    cd $baseDir
    
    if (Test-Path "controllers\ClienteController.java") {
        Move-Item "controllers\ClienteController.java" "modules\cliente\controller\" -Force
        Write-Host "  ✅ ClienteController migrado" -ForegroundColor Green
    }
    
    if (Test-Path "services\ClienteService.java") {
        Move-Item "services\ClienteService.java" "modules\cliente\service\" -Force
        Write-Host "  ✅ ClienteService migrado" -ForegroundColor Green
    }
    
    if (Test-Path "repositories\ClienteRepository.java") {
        Move-Item "repositories\ClienteRepository.java" "modules\cliente\repository\" -Force
        Write-Host "  ✅ ClienteRepository migrado" -ForegroundColor Green
    }
    
    if (Test-Path "models\Cliente.java") {
        Move-Item "models\Cliente.java" "modules\cliente\model\" -Force
        Write-Host "  ✅ Cliente (model) migrado" -ForegroundColor Green
    }
    
    if (Test-Path "models\dto\ClienteDTO.java") {
        Move-Item "models\dto\ClienteDTO.java" "modules\cliente\dto\" -Force
        Write-Host "  ✅ ClienteDTO migrado" -ForegroundColor Green
    }
    
    Write-Host "✅ Módulo Cliente migrado" -ForegroundColor Green
    Write-Host ""
}

# Función para verificar compilación
function Test-Compilation {
    Write-Host "Verificando compilación..." -ForegroundColor Yellow
    
    cd "D:\programacion\java\spring-boot\whats_orders_manager"
    
    $compileResult = & mvn clean compile 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Compilación exitosa" -ForegroundColor Green
    } else {
        Write-Host "❌ Error en compilación" -ForegroundColor Red
        Write-Host "Revisa los errores y actualiza los packages/imports" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# Función para generar reporte
function Generate-Report {
    Write-Host "Generando reporte de migración..." -ForegroundColor Yellow
    
    cd $baseDir
    
    $report = @"
================================================
REPORTE DE MIGRACIÓN MODULAR
Fecha: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
================================================

ESTRUCTURA DE MÓDULOS:
"@
    
    if (Test-Path "modules") {
        $modules = Get-ChildItem "modules" -Directory
        $report += "`n`nMódulos creados: $($modules.Count)`n"
        
        foreach ($module in $modules) {
            $fileCount = (Get-ChildItem "$($module.FullName)" -Recurse -File | Where-Object { $_.Extension -eq ".java" }).Count
            $report += "  - $($module.Name): $fileCount archivos Java`n"
        }
    }
    
    $report += "`n`nCARPETAS ANTIGUAS:`n"
    
    if (Test-Path "controllers") {
        $controllerCount = (Get-ChildItem "controllers" -File -Filter "*.java").Count
        $report += "  - controllers/: $controllerCount archivos restantes`n"
    }
    
    if (Test-Path "services") {
        $serviceCount = (Get-ChildItem "services" -File -Filter "*.java").Count
        $report += "  - services/: $serviceCount archivos restantes`n"
    }
    
    if (Test-Path "models") {
        $modelCount = (Get-ChildItem "models" -File -Filter "*.java" -Recurse).Count
        $report += "  - models/: $modelCount archivos restantes`n"
    }
    
    $reportPath = "D:\programacion\java\spring-boot\whats_orders_manager\docs\REPORTE_MIGRACION_$timestamp.txt"
    $report | Out-File -FilePath $reportPath -Encoding UTF8
    
    Write-Host "✅ Reporte generado en: $reportPath" -ForegroundColor Green
    Write-Host ""
    Write-Host $report
}

# Menú principal
function Show-Menu {
    Write-Host "Selecciona una opción:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  1. Crear backup del proyecto" -ForegroundColor White
    Write-Host "  2. Crear estructura de módulos" -ForegroundColor White
    Write-Host "  3. Migrar módulo Producto" -ForegroundColor White
    Write-Host "  4. Migrar módulo Cliente" -ForegroundColor White
    Write-Host "  5. Verificar compilación (mvn clean compile)" -ForegroundColor White
    Write-Host "  6. Generar reporte de migración" -ForegroundColor White
    Write-Host "  7. Ejecutar TODO (backup + estructura + Producto + Cliente)" -ForegroundColor Yellow
    Write-Host "  0. Salir" -ForegroundColor Red
    Write-Host ""
}

# Función para ejecutar todo
function Run-All {
    Create-Backup
    Start-Sleep -Seconds 2
    
    Create-ModuleStructure
    Start-Sleep -Seconds 2
    
    Migrate-ProductoModule
    Start-Sleep -Seconds 2
    
    Migrate-ClienteModule
    Start-Sleep -Seconds 2
    
    Generate-Report
    
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  MIGRACIÓN AUTOMÁTICA COMPLETADA" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "⚠️  SIGUIENTE PASO:" -ForegroundColor Yellow
    Write-Host "   1. Abre IntelliJ IDEA" -ForegroundColor White
    Write-Host "   2. Actualiza packages en archivos Java" -ForegroundColor White
    Write-Host "   3. Usa Ctrl + Shift + R para Find & Replace imports" -ForegroundColor White
    Write-Host "   4. Ejecuta: mvn clean compile" -ForegroundColor White
    Write-Host "   5. Ejecuta: mvn test" -ForegroundColor White
    Write-Host ""
}

# Loop principal
do {
    Show-Menu
    $option = Read-Host "Opción"
    
    switch ($option) {
        "1" { Create-Backup }
        "2" { Create-ModuleStructure }
        "3" { Migrate-ProductoModule }
        "4" { Migrate-ClienteModule }
        "5" { Test-Compilation }
        "6" { Generate-Report }
        "7" { Run-All }
        "0" { 
            Write-Host "Saliendo..." -ForegroundColor Yellow
            break
        }
        default { Write-Host "Opción no válida" -ForegroundColor Red }
    }
    
    if ($option -ne "0") {
        Write-Host ""
        Read-Host "Presiona Enter para continuar"
        Clear-Host
    }
    
} while ($option -ne "0")

Write-Host ""
Write-Host "¡Hasta luego! 👋" -ForegroundColor Cyan
Write-Host ""
