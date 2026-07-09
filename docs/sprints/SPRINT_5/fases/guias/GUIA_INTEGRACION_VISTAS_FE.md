# 📊 GUÍA: Integración de Facturación Electrónica en Vistas

**Sprint:** 5  
**Fase:** 3  
**Fecha:** 24 de enero de 2026  
**Autor:** Sistema ERP - Módulo de Facturación Electrónica

---

## 📋 INTRODUCCIÓN

Esta guía documenta las mejoras en la interfaz de usuario para integrar la facturación electrónica en el flujo normal de gestión de facturas.

### Objetivos

1. **Visibilidad:** Mostrar el estado de FE directamente en la lista de facturas
2. **Accesibilidad:** Facilitar el envío a Hacienda desde la vista de facturas
3. **Reportería:** Agregar gráficos y exportaciones de comprobantes
4. **UX Mejorada:** Integración fluida con el sistema existente

---

## 🎨 MEJORA 1: Columna "Estado FE" en Tabla de Facturas

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html`

### Implementación

#### 1.1 Agregar Columna en Tabla

```html
<!-- En la sección <thead> -->
<thead>
    <tr>
        <th>ID</th>
        <th>Cliente</th>
        <th>Número</th>
        <th>Fecha</th>
        <th>Total</th>
        <th>Estado FE</th> <!-- NUEVA COLUMNA -->
        <th>Acciones</th>
    </tr>
</thead>

<!-- En la sección <tbody> -->
<tbody>
    <tr th:each="factura : ${facturas}">
        <td th:text="${factura.idFactura}"></td>
        <td th:text="${factura.cliente.nombre}"></td>
        <td th:text="${factura.numeroFactura}"></td>
        <td th:text="${#temporals.format(factura.createDate, 'dd/MM/yyyy')}"></td>
        <td th:text="${'S/ ' + #numbers.formatDecimal(factura.total, 1, 2)}"></td>
        
        <!-- NUEVA COLUMNA: Estado FE con Badge -->
        <td>
            <span th:if="${factura.comprobanteElectronico}" 
                  th:class="${'badge ' + #strings.concat('bg-', factura.comprobanteElectronico.estado.color)}"
                  th:title="${factura.comprobanteElectronico.mensajeRespuesta ?: 'Sin mensaje'}"
                  data-bs-toggle="tooltip">
                
                <i th:classappend="${factura.comprobanteElectronico.estado.icono}"></i>
                <span th:text="${factura.comprobanteElectronico.estado.descripcion}"></span>
            </span>
            
            <span th:unless="${factura.comprobanteElectronico}" 
                  class="badge bg-secondary"
                  title="No enviado a Hacienda"
                  data-bs-toggle="tooltip">
                <i class="bi bi-dash-circle"></i> Sin FE
            </span>
            
            <!-- Link al comprobante (si existe) -->
            <a th:if="${factura.comprobanteElectronico}"
               th:href="@{/facturas/comprobantes/{id}(id=${factura.comprobanteElectronico.id})}"
               class="ms-2"
               title="Ver comprobante electrónico">
                <i class="bi bi-box-arrow-up-right"></i>
            </a>
        </td>
        
        <td>
            <!-- Acciones existentes -->
        </td>
    </tr>
</tbody>
```

#### 1.2 Agregar Colores y Íconos en Enum

**Archivo:** `src/main/java/.../electronica/enums/EstadoComprobante.java`

```java
public enum EstadoComprobante {
    GENERADO("Generado", "warning", "bi-file-earmark-text"),
    FIRMADO("Firmado", "info", "bi-shield-check"),
    ENVIADO("Enviado", "primary", "bi-send"),
    ACEPTADO("Aceptado", "success", "bi-check-circle-fill"),
    RECHAZADO("Rechazado", "danger", "bi-x-circle-fill"),
    ERROR("Error", "danger", "bi-exclamation-triangle-fill");
    
    private final String descripcion;
    private final String color;
    private final String icono;
    
    EstadoComprobante(String descripcion, String color, String icono) {
        this.descripcion = descripcion;
        this.color = color;
        this.icono = icono;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getIcono() {
        return icono;
    }
}
```

#### 1.3 CSS Personalizado

```css
/* En facturas.html o archivo CSS separado */
.badge {
    font-size: 0.875rem;
    padding: 0.35em 0.65em;
    font-weight: 500;
}

.badge i {
    margin-right: 0.25rem;
}

/* Animación para estados en proceso */
.badge.bg-primary {
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.7; }
}
```

---

## 🔘 MEJORA 2: Botón "Enviar a Hacienda" en Detalle de Factura

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html` (Modal de detalle)

### Implementación

#### 2.1 Agregar Botón en Card de Acciones

```html
<!-- En el modal de detalle de factura -->
<div class="modal-footer">
    <div class="btn-group" role="group">
        <!-- Botones existentes -->
        <button type="button" class="btn btn-primary" onclick="imprimirFactura(${factura.id})">
            <i class="bi bi-printer"></i> Imprimir
        </button>
        
        <button type="button" class="btn btn-info" onclick="enviarEmail(${factura.id})">
            <i class="bi bi-envelope"></i> Email
        </button>
        
        <!-- NUEVO BOTÓN: Enviar a Hacienda -->
        <button th:if="${!factura.comprobanteElectronico}" 
                type="button" 
                class="btn btn-success"
                onclick="enviarAHacienda([[${factura.id}]])"
                title="Enviar factura a Hacienda de Costa Rica">
            <i class="bi bi-cloud-upload"></i> Enviar a Hacienda
        </button>
        
        <!-- Si ya tiene comprobante, mostrar botón de estado -->
        <button th:if="${factura.comprobanteElectronico}" 
                type="button" 
                class="btn btn-outline-info"
                onclick="verComprobante([[${factura.comprobanteElectronico.id}]])"
                title="Ver comprobante electrónico">
            <i class="bi bi-file-earmark-check"></i> Ver Comprobante
        </button>
    </div>
    
    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
</div>
```

#### 2.2 JavaScript para Envío a Hacienda

```javascript
/**
 * Envía una factura a Hacienda de Costa Rica.
 */
async function enviarAHacienda(facturaId) {
    // Mostrar modal de confirmación
    const confirmed = await Swal.fire({
        title: '¿Enviar a Hacienda?',
        html: `
            <p>Se generará y enviará el comprobante electrónico a Hacienda de Costa Rica.</p>
            <div class="alert alert-info mt-3">
                <strong>Información:</strong>
                <ul class="text-start mb-0">
                    <li>Se generará el XML según especificación v4.4</li>
                    <li>Se firmará digitalmente con certificado</li>
                    <li>Se enviará a la API de Hacienda</li>
                    <li>Recibirá notificación del resultado</li>
                </ul>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-cloud-upload"></i> Enviar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#198754',
        showLoaderOnConfirm: true,
        preConfirm: async () => {
            try {
                const response = await fetch(`/api/facturas/electronica/comprobantes`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        facturaId: facturaId
                    })
                });
                
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || 'Error al enviar comprobante');
                }
                
                return await response.json();
                
            } catch (error) {
                Swal.showValidationMessage(`Error: ${error.message}`);
            }
        },
        allowOutsideClick: () => !Swal.isLoading()
    });
    
    if (confirmed.isConfirmed) {
        const comprobante = confirmed.value;
        
        // Mostrar resultado
        await Swal.fire({
            title: '¡Enviado!',
            html: `
                <div class="alert alert-success">
                    <h6>Comprobante enviado exitosamente</h6>
                    <p class="mb-1"><strong>Clave:</strong> ${comprobante.claveNumerica}</p>
                    <p class="mb-1"><strong>Estado:</strong> ${comprobante.estado}</p>
                </div>
                <p>Será redirigido a la vista de comprobantes electrónicos...</p>
            `,
            icon: 'success',
            timer: 3000,
            timerProgressBar: true
        });
        
        // Redirigir a comprobantes
        window.location.href = `/facturas/comprobantes?id=${comprobante.id}`;
    }
}

/**
 * Ver detalle de comprobante electrónico.
 */
function verComprobante(comprobanteId) {
    window.location.href = `/facturas/comprobantes?id=${comprobanteId}`;
}
```

---

## 🔍 MEJORA 3: Filtros de Facturación Electrónica

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html`

### Implementación

#### 3.1 Agregar Filtro de Estado FE

```html
<!-- En la sección de filtros -->
<div class="row mb-3">
    <div class="col-md-3">
        <label for="filtroEstadoFE" class="form-label">Estado FE</label>
        <select id="filtroEstadoFE" class="form-select" onchange="aplicarFiltros()">
            <option value="">Todos</option>
            <option value="CON_FE">Con FE</option>
            <option value="SIN_FE">Sin FE</option>
            <option value="ACEPTADO">Aceptados</option>
            <option value="RECHAZADO">Rechazados</option>
            <option value="ENVIADO">Enviados (pendientes)</option>
            <option value="ERROR">Con errores</option>
        </select>
    </div>
    
    <!-- Filtros existentes (fecha, cliente, etc.) -->
</div>
```

#### 3.2 JavaScript para Filtrado

```javascript
/**
 * Aplica filtros combinados (incluyendo estado FE).
 */
function aplicarFiltros() {
    const estadoFE = document.getElementById('filtroEstadoFE').value;
    const fechaDesde = document.getElementById('fechaDesde').value;
    const fechaHasta = document.getElementById('fechaHasta').value;
    const cliente = document.getElementById('filtroCliente').value;
    
    // Construir URL con parámetros
    const params = new URLSearchParams();
    if (estadoFE) params.append('estadoFE', estadoFE);
    if (fechaDesde) params.append('fechaDesde', fechaDesde);
    if (fechaHasta) params.append('fechaHasta', fechaHasta);
    if (cliente) params.append('clienteId', cliente);
    
    // Guardar en localStorage
    localStorage.setItem('filtrosFacturas', JSON.stringify({
        estadoFE, fechaDesde, fechaHasta, cliente
    }));
    
    // Recargar página con filtros
    window.location.href = `/facturas?${params.toString()}`;
}

/**
 * Restaurar filtros desde localStorage al cargar página.
 */
document.addEventListener('DOMContentLoaded', () => {
    const filtrosGuardados = localStorage.getItem('filtrosFacturas');
    if (filtrosGuardados) {
        const filtros = JSON.parse(filtrosGuardados);
        if (filtros.estadoFE) document.getElementById('filtroEstadoFE').value = filtros.estadoFE;
        if (filtros.fechaDesde) document.getElementById('fechaDesde').value = filtros.fechaDesde;
        if (filtros.fechaHasta) document.getElementById('fechaHasta').value = filtros.fechaHasta;
        if (filtros.cliente) document.getElementById('filtroCliente').value = filtros.cliente;
    }
});
```

#### 3.3 Controlador Backend (Filtrado)

**Archivo:** `src/main/java/.../controller/FacturaController.java`

```java
@GetMapping
public String listarFacturas(
        @RequestParam(required = false) String estadoFE,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long clienteId,
        @PageableDefault(size = 20, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
        Model model
) {
    // Aplicar filtros
    Specification<Factura> spec = Specification.where(null);
    
    // Filtro de estado FE
    if (estadoFE != null && !estadoFE.isEmpty()) {
        spec = spec.and(FacturaSpecifications.withEstadoFE(estadoFE));
    }
    
    // Filtros existentes (fecha, cliente)...
    
    Page<Factura> facturas = facturaService.findAll(spec, pageable);
    model.addAttribute("facturas", facturas);
    
    return "modules/facturacion/facturas";
}
```

**Specifications para Estado FE:**

```java
public class FacturaSpecifications {
    
    public static Specification<Factura> withEstadoFE(String estadoFE) {
        return (root, query, cb) -> {
            Join<Factura, ComprobanteElectronico> comprobanteJoin = 
                root.join("comprobanteElectronico", JoinType.LEFT);
            
            return switch (estadoFE) {
                case "CON_FE" -> comprobanteJoin.isNotNull();
                case "SIN_FE" -> comprobanteJoin.isNull();
                case "ACEPTADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ACEPTADO);
                case "RECHAZADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.RECHAZADO);
                case "ENVIADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ENVIADO);
                case "ERROR" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ERROR);
                default -> cb.conjunction();
            };
        };
    }
}
```

---

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

## 📤 MEJORA 5: Exportación a Excel/PDF

### Implementación

#### 5.1 Botones de Exportación

```html
<!-- En comprobantes.html -->
<div class="card-header d-flex justify-content-between align-items-center">
    <h5 class="card-title mb-0">Comprobantes Electrónicos</h5>
    
    <div class="btn-group">
        <button class="btn btn-success btn-sm" onclick="exportarExcel()">
            <i class="bi bi-file-earmark-excel"></i> Excel
        </button>
        <button class="btn btn-danger btn-sm" onclick="exportarPDF()">
            <i class="bi bi-file-earmark-pdf"></i> PDF
        </button>
    </div>
</div>
```

#### 5.2 JavaScript para Exportación

```javascript
function exportarExcel() {
    const filtros = obtenerFiltrosActuales();
    window.location.href = `/api/facturas/electronica/comprobantes/exportar/excel?${filtros}`;
}

function exportarPDF() {
    const filtros = obtenerFiltrosActuales();
    window.open(`/api/facturas/electronica/comprobantes/exportar/pdf?${filtros}`, '_blank');
}

function obtenerFiltrosActuales() {
    const params = new URLSearchParams();
    const estado = document.getElementById('filtroEstado').value;
    const fechaDesde = document.getElementById('fechaDesde').value;
    const fechaHasta = document.getElementById('fechaHasta').value;
    
    if (estado) params.append('estado', estado);
    if (fechaDesde) params.append('fechaDesde', fechaDesde);
    if (fechaHasta) params.append('fechaHasta', fechaHasta);
    
    return params.toString();
}
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] **8.1.1** Agregar columna "Estado FE" en tabla de facturas
- [ ] **8.1.2** Configurar colores e íconos en enum EstadoComprobante
- [ ] **8.1.3** Implementar tooltips con información detallada
- [ ] **8.2.1** Agregar botón "Enviar a Hacienda" en detalle de factura
- [ ] **8.2.2** Implementar JavaScript para envío a Hacienda
- [ ] **8.2.3** Agregar modal de confirmación con SweetAlert2
- [ ] **8.3.1** Implementar filtro de estado FE
- [ ] **8.3.2** Crear Specifications para filtrado por estado FE
- [ ] **8.3.3** Guardar preferencias de filtros en localStorage
- [ ] **8.4.1** Agregar gráfico de tendencia con Chart.js
- [ ] **8.4.2** Crear endpoint de estadísticas en backend
- [ ] **8.4.3** Implementar exportación a Excel (Apache POI)
- [ ] **8.4.4** Implementar exportación a PDF (iText)

---

**Documentación creada:** 24 de enero de 2026  
**Autor:** Sistema ERP - Equipo de Desarrollo
