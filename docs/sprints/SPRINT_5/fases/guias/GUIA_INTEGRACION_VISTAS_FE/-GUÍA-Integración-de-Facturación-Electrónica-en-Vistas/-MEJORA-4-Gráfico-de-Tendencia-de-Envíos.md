## 📈 MEJORA 4: Gráfico de Tendencia de Envíos

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/electronica/comprobantes.html`

### Implementación

#### 4.1 Agregar Canvas para Chart.js

```html
<!-- En la sección de estadísticas -->
<div class="row mb-4">
    <!-- Cards de estadísticas existentes -->
    
    <!-- NUEVO: Gráfico de tendencia -->
    <div class="col-md-12 mt-4">
        <div class="card">
            <div class="card-header">
                <h5 class="card-title mb-0">
                    <i class="bi bi-graph-up"></i> Tendencia de Envíos (Últimos 30 días)
                </h5>
            </div>
            <div class="card-body">
                <canvas id="chartTendenciaEnvios" height="80"></canvas>
            </div>
        </div>
    </div>
</div>
```

#### 4.2 JavaScript para Chart.js

```javascript
// Cargar datos y crear gráfico
async function cargarGraficoTendencia() {
    try {
        const response = await fetch('/api/facturas/electronica/comprobantes/estadisticas/tendencia');
        const data = await response.json();
        
        const ctx = document.getElementById('chartTendenciaEnvios').getContext('2d');
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.fechas, // ['2026-01-01', '2026-01-02', ...]
                datasets: [
                    {
                        label: 'Aceptados',
                        data: data.aceptados,
                        borderColor: '#198754',
                        backgroundColor: 'rgba(25, 135, 84, 0.1)',
                        tension: 0.3,
                        fill: true
                    },
                    {
                        label: 'Rechazados',
                        data: data.rechazados,
                        borderColor: '#dc3545',
                        backgroundColor: 'rgba(220, 53, 69, 0.1)',
                        tension: 0.3,
                        fill: true
                    },
                    {
                        label: 'Pendientes',
                        data: data.pendientes,
                        borderColor: '#ffc107',
                        backgroundColor: 'rgba(255, 193, 7, 0.1)',
                        tension: 0.3,
                        fill: true
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    title: {
                        display: false
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
        
    } catch (error) {
        console.error('Error cargando gráfico:', error);
    }
}

// Cargar al iniciar página
document.addEventListener('DOMContentLoaded', cargarGraficoTendencia);
```

#### 4.3 Endpoint Backend para Estadísticas

```java
@GetMapping("/estadisticas/tendencia")
public ResponseEntity<TendenciaDTO> getTendenciaEnvios() {
    LocalDate fechaInicio = LocalDate.now().minusDays(30);
    LocalDate fechaFin = LocalDate.now();
    
    Map<LocalDate, EstadisticasDia> estadisticas = 
        comprobanteService.getEstadisticasPorDia(fechaInicio, fechaFin);
    
    TendenciaDTO tendencia = new TendenciaDTO();
    estadisticas.forEach((fecha, stats) -> {
        tendencia.agregarDia(fecha, stats);
    });
    
    return ResponseEntity.ok(tendencia);
}
```

---

