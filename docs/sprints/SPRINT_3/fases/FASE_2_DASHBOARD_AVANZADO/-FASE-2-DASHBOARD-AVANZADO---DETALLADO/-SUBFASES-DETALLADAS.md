## 📋 SUBFASES DETALLADAS

### 🔲 SUBFASE 2.1: Backend - Servicios de Estadísticas (6-8h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** CRÍTICA  
**Tiempo:** 6-8 horas  
**Dependencias:** Fase 1 completada

#### Tareas Específicas:

##### 2.1.1 - Crear DashboardService (2h)
- [ ] Crear interfaz `DashboardService.java`
- [ ] Crear implementación `DashboardServiceImpl.java`
- [ ] Métodos para KPIs principales
- [ ] Cache con Spring Cache

**Archivo:** `src/main/java/com/astro/erp/service/DashboardService.java`

```java
package com.astro.erp.service;

import com.astro.erp.dto.dashboard.*;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    
    /**
     * Obtener KPIs principales del dashboard
     * @return Objeto con todos los KPIs
     */
    DashboardKPIsDTO getKPIs();
    
    /**
     * Obtener ventas mensuales (últimos N meses)
     * @param meses Número de meses a obtener
     * @return Lista de ventas por mes
     */
    List<VentasMensualesDTO> getVentasMensuales(int meses);
    
    /**
     * Obtener estado de facturas (pagadas, pendientes, vencidas)
     * @return Objeto con conteo por estado
     */
    EstadoFacturasDTO getEstadoFacturas();
    
    /**
     * Obtener comparativa año actual vs anterior
     * @return Objeto con comparativa
     */
    ComparativaAnualDTO getComparativaAnual();
    
    /**
     * Obtener ingresos por divisa
     * @return Lista de ingresos por divisa
     */
    List<IngresosPorDivisaDTO> getIngresosPorDivisa();
    
    /**
     * Refrescar caché del dashboard
     */
    void refrescarCache();
}
```

**Implementación:** `src/main/java/com/astro/erp/service/impl/DashboardServiceImpl.java`

```java
package com.astro.erp.service.impl;

import com.astro.erp.dto.dashboard.*;
import com.astro.erp.repository.*;
import com.astro.erp.service.DashboardService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private ProductoRepository productoRepository;
    
    @Override
    @Cacheable(value = "dashboardKPIs", key = "'kpis'")
    public DashboardKPIsDTO getKPIs() {
        log.info("Calculando KPIs del dashboard");
        
        DashboardKPIsDTO kpis = new DashboardKPIsDTO();
        
        // 1. Total ventas del mes actual
        LocalDate primerDiaMes = LocalDate.now().withDayOfMonth(1);
        BigDecimal ventasMes = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_divisa_maestra), 0) " +
            "FROM factura " +
            "WHERE fecha_emision >= ? AND estado_factura != 'CANCELADA'",
            BigDecimal.class,
            primerDiaMes
        );
        kpis.setVentasMesActual(ventasMes);
        
        // 2. Total facturas pendientes
        Long facturasPendientes = facturaRepository.countByEstadoFactura("PENDIENTE");
        kpis.setFacturasPendientes(facturasPendientes);
        
        // 3. Total clientes activos
        Long clientesActivos = clienteRepository.countByActivoTrue();
        kpis.setClientesActivos(clientesActivos);
        
        // 4. Productos en inventario
        Long productosInventario = productoRepository.countByActivoTrue();
        kpis.setProductosInventario(productosInventario);
        
        // 5. Tasa de cobro (facturas pagadas vs emitidas)
        Double tasaCobro = jdbcTemplate.queryForObject(
            "SELECT ROUND(" +
            "  (SELECT COUNT(*) FROM factura WHERE estado_factura = 'PAGADA' AND YEAR(fecha_emision) = YEAR(CURDATE())) * 100.0 / " +
            "  NULLIF((SELECT COUNT(*) FROM factura WHERE estado_factura != 'CANCELADA' AND YEAR(fecha_emision) = YEAR(CURDATE())), 0)" +
            ", 2)",
            Double.class
        );
        kpis.setTasaCobro(tasaCobro != null ? tasaCobro : 0.0);
        
        // 6. Crecimiento vs mes anterior
        LocalDate primerDiaMesAnterior = primerDiaMes.minusMonths(1);
        LocalDate ultimoDiaMesAnterior = primerDiaMes.minusDays(1);
        BigDecimal ventasMesAnterior = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_divisa_maestra), 0) " +
            "FROM factura " +
            "WHERE fecha_emision BETWEEN ? AND ? AND estado_factura != 'CANCELADA'",
            BigDecimal.class,
            primerDiaMesAnterior,
            ultimoDiaMesAnterior
        );
        
        if (ventasMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal crecimiento = ventasMes.subtract(ventasMesAnterior)
                .divide(ventasMesAnterior, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
            kpis.setCrecimientoMensual(crecimiento.doubleValue());
        } else {
            kpis.setCrecimientoMensual(0.0);
        }
        
        log.info("KPIs calculados: ventas={}, pendientes={}, clientes={}", 
                 ventasMes, facturasPendientes, clientesActivos);
        
        return kpis;
    }
    
    @Override
    @Cacheable(value = "dashboardVentas", key = "'ventas-' + #meses")
    public List<VentasMensualesDTO> getVentasMensuales(int meses) {
        log.info("Obteniendo ventas mensuales: últimos {} meses", meses);
        
        String sql = "CALL SP_GetVentasMensuales(?)";
        
        List<VentasMensualesDTO> ventas = jdbcTemplate.query(
            sql,
            new Object[]{meses},
            (rs, rowNum) -> {
                VentasMensualesDTO dto = new VentasMensualesDTO();
                dto.setMes(rs.getString("mes"));
                dto.setAnio(rs.getInt("anio"));
                dto.setTotalVentas(rs.getBigDecimal("total_ventas"));
                dto.setCantidadFacturas(rs.getInt("cantidad_facturas"));
                dto.setTicketPromedio(rs.getBigDecimal("ticket_promedio"));
                return dto;
            }
        );
        
        return ventas;
    }
    
    @Override
    @Cacheable(value = "dashboardEstado", key = "'estado-facturas'")
    public EstadoFacturasDTO getEstadoFacturas() {
        log.info("Obteniendo estado de facturas");
        
        String sql = "CALL SP_GetEstadoFacturas()";
        
        EstadoFacturasDTO estado = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                EstadoFacturasDTO dto = new EstadoFacturasDTO();
                dto.setPagadas(rs.getLong("pagadas"));
                dto.setPendientes(rs.getLong("pendientes"));
                dto.setVencidas(rs.getLong("vencidas"));
                dto.setCanceladas(rs.getLong("canceladas"));
                dto.setTotalPagadas(rs.getBigDecimal("total_pagadas"));
                dto.setTotalPendientes(rs.getBigDecimal("total_pendientes"));
                dto.setTotalVencidas(rs.getBigDecimal("total_vencidas"));
                return dto;
            }
        );
        
        return estado;
    }
    
    @Override
    @Cacheable(value = "dashboardComparativa", key = "'comparativa-anual'")
    public ComparativaAnualDTO getComparativaAnual() {
        log.info("Obteniendo comparativa anual");
        
        String sql = "CALL SP_GetComparativaAnual()";
        
        ComparativaAnualDTO comparativa = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                ComparativaAnualDTO dto = new ComparativaAnualDTO();
                dto.setAnioActual(rs.getInt("anio_actual"));
                dto.setVentasActual(rs.getBigDecimal("ventas_actual"));
                dto.setFacturasActual(rs.getInt("facturas_actual"));
                dto.setAnioAnterior(rs.getInt("anio_anterior"));
                dto.setVentasAnterior(rs.getBigDecimal("ventas_anterior"));
                dto.setFacturasAnterior(rs.getInt("facturas_anterior"));
                dto.setCrecimientoVentas(rs.getDouble("crecimiento_ventas"));
                dto.setCrecimientoFacturas(rs.getDouble("crecimiento_facturas"));
                return dto;
            }
        );
        
        return comparativa;
    }
    
    @Override
    @Cacheable(value = "dashboardDivisas", key = "'ingresos-divisas'")
    public List<IngresosPorDivisaDTO> getIngresosPorDivisa() {
        log.info("Obteniendo ingresos por divisa");
        
        String sql = "CALL SP_GetIngresosPorDivisa()";
        
        List<IngresosPorDivisaDTO> ingresos = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                IngresosPorDivisaDTO dto = new IngresosPorDivisaDTO();
                dto.setCodigoDivisa(rs.getString("codigo_divisa"));
                dto.setNombreDivisa(rs.getString("nombre_divisa"));
                dto.setTotalOriginal(rs.getBigDecimal("total_original"));
                dto.setTotalConvertido(rs.getBigDecimal("total_convertido"));
                dto.setCantidadFacturas(rs.getInt("cantidad_facturas"));
                dto.setPorcentaje(rs.getDouble("porcentaje"));
                return dto;
            }
        );
        
        return ingresos;
    }
    
    @Override
    @CacheEvict(value = {"dashboardKPIs", "dashboardVentas", "dashboardEstado", 
                         "dashboardComparativa", "dashboardDivisas"}, allEntries = true)
    public void refrescarCache() {
        log.info("Cache del dashboard refrescado");
    }
}
```

##### 2.1.2 - Crear EstadisticasService (2h)
- [ ] Crear `EstadisticasService.java`
- [ ] Métodos para top productos
- [ ] Métodos para top clientes
- [ ] Métodos para crecimiento

**Archivo:** `src/main/java/com/astro/erp/service/EstadisticasService.java`

```java
package com.astro.erp.service;

import com.astro.erp.dto.dashboard.*;
import java.util.List;

public interface EstadisticasService {
    
    /**
     * Obtener top N productos más vendidos
     * @param limit Cantidad de productos
     * @return Lista de productos ordenados por ventas
     */
    List<TopProductoDTO> getTopProductos(int limit);
    
    /**
     * Obtener top N clientes con más compras
     * @param limit Cantidad de clientes
     * @return Lista de clientes ordenados por total
     */
    List<TopClienteDTO> getTopClientes(int limit);
    
    /**
     * Obtener crecimiento de clientes por mes
     * @param meses Número de meses
     * @return Lista con crecimiento mensual
     */
    List<CrecimientoClientesDTO> getCrecimientoClientes(int meses);
    
    /**
     * Obtener tasa de cobro mensual
     * @param meses Número de meses
     * @return Lista con tasa de cobro por mes
     */
    List<TasaCobroDTO> getTasaCobroMensual(int meses);
}
```

##### 2.1.3 - Crear DTOs del Dashboard (2h)
- [ ] Crear todos los DTOs necesarios
- [ ] Documentar campos
- [ ] Agregar validaciones

**DTOs a crear:**

```java
// src/main/java/com/astro/erp/dto/dashboard/DashboardKPIsDTO.java
@Data
@Builder
public class DashboardKPIsDTO {
    private BigDecimal ventasMesActual;
    private Long facturasPendientes;
    private Long clientesActivos;
    private Long productosInventario;
    private Double tasaCobro;
    private Double crecimientoMensual;
}

// src/main/java/com/astro/erp/dto/dashboard/VentasMensualesDTO.java
@Data
public class VentasMensualesDTO {
    private String mes;
    private Integer anio;
    private BigDecimal totalVentas;
    private Integer cantidadFacturas;
    private BigDecimal ticketPromedio;
}

// src/main/java/com/astro/erp/dto/dashboard/EstadoFacturasDTO.java
@Data
public class EstadoFacturasDTO {
    private Long pagadas;
    private Long pendientes;
    private Long vencidas;
    private Long canceladas;
    private BigDecimal totalPagadas;
    private BigDecimal totalPendientes;
    private BigDecimal totalVencidas;
}

// src/main/java/com/astro/erp/dto/dashboard/TopProductoDTO.java
@Data
public class TopProductoDTO {
    private Long idProducto;
    private String nombre;
    private Integer cantidadVendida;
    private BigDecimal totalVentas;
}

// src/main/java/com/astro/erp/dto/dashboard/TopClienteDTO.java
@Data
public class TopClienteDTO {
    private Long idCliente;
    private String nombre;
    private Integer cantidadCompras;
    private BigDecimal totalCompras;
}
```

**Entregables Subfase 2.1:**
- [ ] DashboardService completo
- [ ] EstadisticasService completo
- [ ] 8 DTOs creados
- [ ] Cache configurado

---

### 🔲 SUBFASE 2.2: Backend - Stored Procedures (4-5h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA  
**Tiempo:** 4-5 horas  
**Dependencias:** Base de datos con Fase 3 completada (multi-divisa)

#### Tareas Específicas:

##### 2.2.1 - SP_GetVentasMensuales (45min)
- [ ] Crear stored procedure
- [ ] Parametrizar meses
- [ ] Incluir ticket promedio
- [ ] Optimizar con índices

```sql
DELIMITER $$

CREATE PROCEDURE SP_GetVentasMensuales(IN p_meses INT)
BEGIN
    SELECT 
        DATE_FORMAT(fecha_emision, '%Y-%m') as mes,
        YEAR(fecha_emision) as anio,
        MONTHNAME(fecha_emision) as nombre_mes,
        SUM(total_divisa_maestra) as total_ventas,
        COUNT(*) as cantidad_facturas,
        AVG(total_divisa_maestra) as ticket_promedio
    FROM factura
    WHERE fecha_emision >= DATE_SUB(CURDATE(), INTERVAL p_meses MONTH)
      AND estado_factura != 'CANCELADA'
    GROUP BY DATE_FORMAT(fecha_emision, '%Y-%m'), YEAR(fecha_emision), MONTHNAME(fecha_emision)
    ORDER BY fecha_emision DESC;
END$$

DELIMITER ;
```

##### 2.2.2 - SP_GetTopProductos (45min)
- [ ] Crear stored procedure
- [ ] Unir con detalle_factura
- [ ] Ordenar por cantidad y monto

```sql
DELIMITER $$

CREATE PROCEDURE SP_GetTopProductos(IN p_limit INT)
BEGIN
    SELECT 
        p.id_producto,
        p.nombre,
        SUM(df.cantidad) as cantidad_vendida,
        SUM(df.subtotal) as total_ventas
    FROM producto p
    INNER JOIN detalle_factura df ON p.id_producto = df.id_producto
    INNER JOIN factura f ON df.id_factura = f.id_factura
    WHERE f.estado_factura != 'CANCELADA'
      AND f.fecha_emision >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
    GROUP BY p.id_producto, p.nombre
    ORDER BY total_ventas DESC
    LIMIT p_limit;
END$$

DELIMITER ;
```

##### 2.2.3 - SP_GetTopClientes (45min)

```sql
DELIMITER $$

CREATE PROCEDURE SP_GetTopClientes(IN p_limit INT)
BEGIN
    SELECT 
        c.id_cliente,
        c.nombre,
        COUNT(f.id_factura) as cantidad_compras,
        SUM(f.total_divisa_maestra) as total_compras
    FROM cliente c
    INNER JOIN factura f ON c.id_cliente = f.id_cliente
    WHERE f.estado_factura != 'CANCELADA'
      AND f.fecha_emision >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
    GROUP BY c.id_cliente, c.nombre
    ORDER BY total_compras DESC
    LIMIT p_limit;
END$$

DELIMITER ;
```

##### 2.2.4 - SP_GetEstadoFacturas (30min)

```sql
DELIMITER $$

CREATE PROCEDURE SP_GetEstadoFacturas()
BEGIN
    SELECT 
        SUM(CASE WHEN estado_factura = 'PAGADA' THEN 1 ELSE 0 END) as pagadas,
        SUM(CASE WHEN estado_factura = 'PENDIENTE' THEN 1 ELSE 0 END) as pendientes,
        SUM(CASE WHEN estado_factura = 'VENCIDA' THEN 1 ELSE 0 END) as vencidas,
        SUM(CASE WHEN estado_factura = 'CANCELADA' THEN 1 ELSE 0 END) as canceladas,
        
        SUM(CASE WHEN estado_factura = 'PAGADA' THEN total_divisa_maestra ELSE 0 END) as total_pagadas,
        SUM(CASE WHEN estado_factura = 'PENDIENTE' THEN total_divisa_maestra ELSE 0 END) as total_pendientes,
        SUM(CASE WHEN estado_factura = 'VENCIDA' THEN total_divisa_maestra ELSE 0 END) as total_vencidas
    FROM factura
    WHERE fecha_emision >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH);
END$$

DELIMITER ;
```

##### 2.2.5 - SP_GetComparativaAnual (45min)
##### 2.2.6 - SP_GetIngresosPorDivisa (45min)
##### 2.2.7 - SP_GetTasaCobro (30min)
##### 2.2.8 - SP_GetCrecimientoClientes (30min)

**Entregables Subfase 2.2:**
- [ ] 8 Stored Procedures creados
- [ ] Todos testeados y funcionando
- [ ] Optimizados con índices
- [ ] Documentados

---

### 🔲 SUBFASE 2.3: Backend - API Controllers (2-3h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA  
**Tiempo:** 2-3 horas

#### Tareas Específicas:

##### 2.3.1 - Crear DashboardApiController (2h)
- [ ] Crear controller REST
- [ ] 5 endpoints principales
- [ ] Documentación Swagger
- [ ] Manejo de errores

```java
package com.astro.erp.controller.api;

import com.astro.erp.dto.dashboard.*;
import com.astro.erp.service.DashboardService;
import com.astro.erp.service.EstadisticasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "API para dashboard y estadísticas")
@Slf4j
public class DashboardApiController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private EstadisticasService estadisticasService;
    
    @GetMapping("/kpis")
    @Operation(summary = "Obtener KPIs principales")
    public ResponseEntity<DashboardKPIsDTO> getKPIs() {
        log.info("API: Obteniendo KPIs del dashboard");
        DashboardKPIsDTO kpis = dashboardService.getKPIs();
        return ResponseEntity.ok(kpis);
    }
    
    @GetMapping("/ventas-mensuales")
    @Operation(summary = "Obtener ventas mensuales")
    public ResponseEntity<List<VentasMensualesDTO>> getVentasMensuales(
            @RequestParam(defaultValue = "12") int meses) {
        log.info("API: Obteniendo ventas mensuales: {} meses", meses);
        List<VentasMensualesDTO> ventas = dashboardService.getVentasMensuales(meses);
        return ResponseEntity.ok(ventas);
    }
    
    @GetMapping("/estado-facturas")
    @Operation(summary = "Obtener estado de facturas")
    public ResponseEntity<EstadoFacturasDTO> getEstadoFacturas() {
        log.info("API: Obteniendo estado de facturas");
        EstadoFacturasDTO estado = dashboardService.getEstadoFacturas();
        return ResponseEntity.ok(estado);
    }
    
    @GetMapping("/top-productos")
    @Operation(summary = "Obtener top productos")
    public ResponseEntity<List<TopProductoDTO>> getTopProductos(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("API: Obteniendo top {} productos", limit);
        List<TopProductoDTO> productos = estadisticasService.getTopProductos(limit);
        return ResponseEntity.ok(productos);
    }
    
    @GetMapping("/top-clientes")
    @Operation(summary = "Obtener top clientes")
    public ResponseEntity<List<TopClienteDTO>> getTopClientes(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("API: Obteniendo top {} clientes", limit);
        List<TopClienteDTO> clientes = estadisticasService.getTopClientes(limit);
        return ResponseEntity.ok(clientes);
    }
    
    @GetMapping("/comparativa-anual")
    @Operation(summary = "Obtener comparativa anual")
    public ResponseEntity<ComparativaAnualDTO> getComparativaAnual() {
        log.info("API: Obteniendo comparativa anual");
        ComparativaAnualDTO comparativa = dashboardService.getComparativaAnual();
        return ResponseEntity.ok(comparativa);
    }
    
    @PostMapping("/refrescar-cache")
    @Operation(summary = "Refrescar caché del dashboard")
    public ResponseEntity<Void> refrescarCache() {
        log.info("API: Refrescando caché del dashboard");
        dashboardService.refrescarCache();
        return ResponseEntity.ok().build();
    }
}
```

**Entregables Subfase 2.3:**
- [ ] Controller REST completo
- [ ] 7 endpoints funcionando
- [ ] Documentación Swagger
- [ ] Tests de endpoints

---

### 🔲 SUBFASE 2.4: Frontend - Vistas Dashboard (4-6h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA  
**Tiempo:** 4-6 horas

#### Tareas Específicas:

##### 2.4.1 - Actualizar templates/dashboard/dashboard.html (3h)
- [ ] Agregar sección de KPIs (4 cards)
- [ ] Crear contenedores para gráficas
- [ ] Diseño responsivo con Bootstrap
- [ ] Iconos con Font Awesome

##### 2.4.2 - Crear layouts para gráficas (1-2h)
- [ ] Grid para múltiples gráficas
- [ ] Cards con sombras
- [ ] Colores corporativos

**Entregables Subfase 2.4:**
- [ ] Vista dashboard actualizada
- [ ] 4 KPI cards
- [ ] 5 contenedores para gráficas
- [ ] Responsive design

---

### 🔲 SUBFASE 2.5: Frontend - Chart.js Integration (6-8h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** CRÍTICA  
**Tiempo:** 6-8 horas

#### Tareas Específicas:

##### 2.5.1 - Configurar Chart.js 4.x (1h)
- [ ] Agregar CDN o npm
- [ ] Configurar en layout principal
- [ ] Verificar carga correcta

```html
<!-- En layout.html o dashboard.html -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
```

##### 2.5.2 - Crear dashboard-charts.js (5-7h)
- [ ] Gráfica 1: Ventas mensuales (Line Chart)
- [ ] Gráfica 2: Estado facturas (Pie Chart)
- [ ] Gráfica 3: Top productos (Bar Chart)
- [ ] Gráfica 4: Top clientes (Doughnut Chart)
- [ ] Gráfica 5: Comparativa anual (Mixed Chart)

**Archivo:** `static/js/dashboard-charts.js`

```javascript
// Configuración global de Chart.js
Chart.defaults.font.family = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";
Chart.defaults.font.size = 12;

// Colores corporativos
const COLORS = {
    primary: '#4e73df',
    success: '#1cc88a',
    info: '#36b9cc',
    warning: '#f6c23e',
    danger: '#e74a3b',
    secondary: '#858796'
};

/**
 * GRÁFICA 1: Ventas Mensuales (Line Chart)
 */
function cargarVentasMensuales() {
    fetch('/api/dashboard/ventas-mensuales?meses=12')
        .then(response => response.json())
        .then(data => {
            const ctx = document.getElementById('chartVentasMensuales').getContext('2d');
            
            new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.map(v => `${v.mes}/${v.anio.toString().substr(2)}`),
                    datasets: [{
                        label: 'Ventas',
                        data: data.map(v => v.totalVentas),
                        borderColor: COLORS.primary,
                        backgroundColor: COLORS.primary + '20',
                        fill: true,
                        tension: 0.4
                    }, {
                        label: 'Ticket Promedio',
                        data: data.map(v => v.ticketPromedio),
                        borderColor: COLORS.info,
                        backgroundColor: COLORS.info + '20',
                        fill: false,
                        tension: 0.4,
                        yAxisID: 'y1'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        },
                        title: {
                            display: true,
                            text: 'Ventas Mensuales (Últimos 12 meses)'
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false,
                            callbacks: {
                                label: function(context) {
                                    let label = context.dataset.label || '';
                                    if (label) {
                                        label += ': ';
                                    }
                                    label += new Intl.NumberFormat('es-MX', {
                                        style: 'currency',
                                        currency: 'MXN'
                                    }).format(context.parsed.y);
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toLocaleString('es-MX');
                                }
                            }
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            grid: {
                                drawOnChartArea: false
                            }
                        }
                    }
                }
            });
        })
        .catch(error => console.error('Error cargando ventas mensuales:', error));
}

/**
 * GRÁFICA 2: Estado de Facturas (Pie Chart)
 */
function cargarEstadoFacturas() {
    fetch('/api/dashboard/estado-facturas')
        .then(response => response.json())
        .then(data => {
            const ctx = document.getElementById('chartEstadoFacturas').getContext('2d');
            
            new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: ['Pagadas', 'Pendientes', 'Vencidas', 'Canceladas'],
                    datasets: [{
                        data: [
                            data.pagadas,
                            data.pendientes,
                            data.vencidas,
                            data.canceladas
                        ],
                        backgroundColor: [
                            COLORS.success,
                            COLORS.warning,
                            COLORS.danger,
                            COLORS.secondary
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        },
                        title: {
                            display: true,
                            text: 'Estado de Facturas'
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label || '';
                                    const value = context.parsed;
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = ((value * 100) / total).toFixed(1);
                                    return `${label}: ${value} (${percentage}%)`;
                                }
                            }
                        }
                    }
                }
            });
        })
        .catch(error => console.error('Error cargando estado facturas:', error));
}

/**
 * GRÁFICA 3: Top Productos (Bar Chart)
 */
function cargarTopProductos() {
    fetch('/api/dashboard/top-productos?limit=5')
        .then(response => response.json())
        .then(data => {
            const ctx = document.getElementById('chartTopProductos').getContext('2d');
            
            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.map(p => p.nombre),
                    datasets: [{
                        label: 'Ventas',
                        data: data.map(p => p.totalVentas),
                        backgroundColor: COLORS.primary,
                        borderColor: COLORS.primary,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y', // Horizontal bars
                    plugins: {
                        legend: {
                            display: false
                        },
                        title: {
                            display: true,
                            text: 'Top 5 Productos Más Vendidos'
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return new Intl.NumberFormat('es-MX', {
                                        style: 'currency',
                                        currency: 'MXN'
                                    }).format(context.parsed.x);
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toLocaleString('es-MX');
                                }
                            }
                        }
                    }
                }
            });
        })
        .catch(error => console.error('Error cargando top productos:', error));
}

/**
 * Cargar todas las gráficas al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {
    cargarKPIs();
    cargarVentasMensuales();
    cargarEstadoFacturas();
    cargarTopProductos();
    cargarTopClientes();
    cargarComparativaAnual();
    
    // Refrescar cada 5 minutos
    setInterval(() => {
        location.reload();
    }, 300000);
});
```

**Entregables Subfase 2.5:**
- [ ] Chart.js configurado
- [ ] 5 gráficas implementadas
- [ ] Responsive y profesional
- [ ] Colores corporativos

---

