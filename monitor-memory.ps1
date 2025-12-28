# monitor-memory.ps1 - Monitor de uso de memoria para procesos Java
# Autor: Sistema de Optimización
# Uso: .\monitor-memory.ps1 -Intervalo 5 -Duracion 300

param(
    [int]$Intervalo = 5,  # Segundos entre mediciones
    [int]$Duracion = 60   # Duración total en segundos
)

Clear-Host
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         MONITOR DE MEMORIA - SPRING BOOT APP              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "Configuración:" -ForegroundColor Yellow
Write-Host "  • Intervalo entre mediciones: $Intervalo segundos" -ForegroundColor White
Write-Host "  • Duración total del monitoreo: $Duracion segundos" -ForegroundColor White
Write-Host "  • Hora de inicio: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor White
Write-Host ""
Write-Host "Monitoreando..." -ForegroundColor Green
Write-Host ("─" * 80) -ForegroundColor DarkGray
Write-Host ""

$inicio = Get-Date
$fin = $inicio.AddSeconds($Duracion)
$resultados = @()
$contador = 0

while ((Get-Date) -lt $fin) {
    $contador++
    $procesos = Get-Process -Name java -ErrorAction SilentlyContinue
    
    if ($procesos) {
        $total = 0
        $timestamp = Get-Date -Format 'HH:mm:ss'
        
        Write-Host "[$timestamp] " -NoNewline -ForegroundColor Green
        Write-Host "Medición #$contador " -NoNewline -ForegroundColor Yellow
        Write-Host "| " -NoNewline
        
        $detallesProcesos = @()
        foreach ($proceso in $procesos) {
            $memoriaMB = [math]::Round($proceso.WorkingSet64 / 1MB, 2)
            $cpuPercent = [math]::Round($proceso.CPU, 2)
            $total += $memoriaMB
            
            $detallesProcesos += "PID $($proceso.Id): $memoriaMB MB (CPU: $cpuPercent%)"
        }
        
        Write-Host ($detallesProcesos -join " | ") -NoNewline -ForegroundColor Cyan
        Write-Host " | " -NoNewline
        Write-Host "TOTAL: $([math]::Round($total, 2)) MB" -ForegroundColor Magenta
        
        $resultados += [PSCustomObject]@{
            Medicion = $contador
            Timestamp = Get-Date
            Hora = $timestamp
            NumProcesos = $procesos.Count
            TotalMB = [math]::Round($total, 2)
        }
    } else {
        Write-Host "[$timestamp] " -NoNewline -ForegroundColor Red
        Write-Host "No se encontraron procesos Java ejecutándose" -ForegroundColor Yellow
    }
    
    Start-Sleep -Seconds $Intervalo
}

# Estadísticas finales
Write-Host ""
Write-Host ("─" * 80) -ForegroundColor DarkGray
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                    ESTADÍSTICAS FINALES                    ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

if ($resultados.Count -gt 0) {
    $promedio = ($resultados | Measure-Object -Property TotalMB -Average).Average
    $maximo = ($resultados | Measure-Object -Property TotalMB -Maximum).Maximum
    $minimo = ($resultados | Measure-Object -Property TotalMB -Minimum).Minimum
    $mediana = ($resultados | Sort-Object TotalMB)[[math]::Floor($resultados.Count / 2)].TotalMB
    
    Write-Host "Resumen del monitoreo:" -ForegroundColor Yellow
    Write-Host "  • Total de mediciones: $($resultados.Count)" -ForegroundColor White
    Write-Host "  • Duración real: $([math]::Round((New-TimeSpan -Start $inicio -End (Get-Date)).TotalSeconds, 2)) segundos" -ForegroundColor White
    Write-Host ""
    Write-Host "Uso de memoria:" -ForegroundColor Yellow
    Write-Host "  • Promedio: $([math]::Round($promedio, 2)) MB" -ForegroundColor Cyan
    Write-Host "  • Mediana: $([math]::Round($mediana, 2)) MB" -ForegroundColor Cyan
    Write-Host "  • Máximo: $([math]::Round($maximo, 2)) MB" -ForegroundColor Red
    Write-Host "  • Mínimo: $([math]::Round($minimo, 2)) MB" -ForegroundColor Green
    Write-Host "  • Variación: $([math]::Round($maximo - $minimo, 2)) MB" -ForegroundColor Yellow
    Write-Host ""
    
    # Evaluación del consumo
    Write-Host "Evaluación:" -ForegroundColor Yellow
    if ($promedio -lt 300) {
        Write-Host "  ✓ EXCELENTE - Consumo muy bajo" -ForegroundColor Green
    } elseif ($promedio -lt 512) {
        Write-Host "  ✓ BUENO - Consumo optimizado" -ForegroundColor Cyan
    } elseif ($promedio -lt 768) {
        Write-Host "  ⚠ MODERADO - Se puede optimizar" -ForegroundColor Yellow
    } else {
        Write-Host "  ✗ ALTO - Requiere optimización urgente" -ForegroundColor Red
    }
    
    # Guardar reporte
    $reportFile = "memory-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').csv"
    $resultados | Export-Csv -Path $reportFile -NoTypeInformation -Encoding UTF8
    
    Write-Host ""
    Write-Host "Reporte guardado en: $reportFile" -ForegroundColor Green
    
    # Mostrar gráfico ASCII simple
    Write-Host ""
    Write-Host "Tendencia de uso (últimas 10 mediciones):" -ForegroundColor Yellow
    $ultimas10 = $resultados | Select-Object -Last 10
    $maxGrafico = ($ultimas10 | Measure-Object -Property TotalMB -Maximum).Maximum
    
    foreach ($medicion in $ultimas10) {
        $barras = [math]::Floor(($medicion.TotalMB / $maxGrafico) * 50)
        $barra = "█" * $barras
        $espacios = " " * (50 - $barras)
        
        Write-Host "  $($medicion.Hora) | " -NoNewline -ForegroundColor DarkGray
        Write-Host $barra -NoNewline -ForegroundColor $(
            if ($medicion.TotalMB -lt 400) { "Green" }
            elseif ($medicion.TotalMB -lt 600) { "Yellow" }
            else { "Red" }
        )
        Write-Host "$espacios | $($medicion.TotalMB) MB" -ForegroundColor White
    }
} else {
    Write-Host "No se pudieron recopilar datos de memoria" -ForegroundColor Red
}

Write-Host ""
Write-Host "Monitoreo completado." -ForegroundColor Green
Write-Host ""
