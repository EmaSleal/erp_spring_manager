# 📊 FASE 3: Reportes Financieros Avanzados

**Sprint:** 8  
**Fase:** 3 de 5  
**Duración estimada:** 5-7 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/32 tareas)

---

## 📋 OBJETIVO DE LA FASE

Implementar reportes financieros avanzados para análisis ejecutivo:
- **Estado de Resultados** (P&L - Profit & Loss)
- **Balance General** (Balance Sheet)
- **Flujo de Caja** (Cash Flow Statement)
- **Análisis de Ratios Financieros**
- **Dashboard Ejecutivo** con gráficos interactivos
- **Exportación** a Excel y PDF

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/32] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Estado de Resultados (P&L)       [0/8]  ░░░░░░░░░░ 0%
├─ 2. Balance General                  [0/8]  ░░░░░░░░░░ 0%
├─ 3. Flujo de Caja                    [0/6]  ░░░░░░░░░░ 0%
├─ 4. Ratios Financieros               [0/4]  ░░░░░░░░░░ 0%
├─ 5. Dashboard Ejecutivo              [0/4]  ░░░░░░░░░░ 0%
└─ 6. Exportación y Gráficos           [0/2]  ░░░░░░░░░░ 0%
```

---

## 📦 1. ESTADO DE RESULTADOS (P&L) (8 tareas)

### 1.1. Descripción

El **Estado de Resultados** muestra:
- **Ingresos** (ventas del período)
- **Costo de Ventas** (costo de productos vendidos)
- **Utilidad Bruta** (ingresos - costo ventas)
- **Gastos Operativos** (salarios, alquileres, servicios)
- **Utilidad Operativa** (utilidad bruta - gastos operativos)
- **Utilidad Neta** (utilidad operativa - impuestos)

#### Tareas:

- [ ] **1.1.1** Crear DTO para Estado de Resultados

```java
package com.erp.whatsorders.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para Estado de Resultados (P&L - Profit & Loss).
 */
@Data
public class EstadoResultadosDTO {
    
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private String periodo; // "Enero 2026", "2026"
    
    // ==================== INGRESOS ====================
    
    /**
     * Ingresos por ventas
     */
    private BigDecimal ingresosVentas = BigDecimal.ZERO;
    
    /**
     * Otros ingresos
     */
    private BigDecimal otrosIngresos = BigDecimal.ZERO;
    
    /**
     * TOTAL INGRESOS
     */
    private BigDecimal totalIngresos = BigDecimal.ZERO;
    
    // ==================== COSTO DE VENTAS ====================
    
    /**
     * Costo de productos vendidos
     */
    private BigDecimal costoVentas = BigDecimal.ZERO;
    
    /**
     * UTILIDAD BRUTA (Ingresos - Costo Ventas)
     */
    private BigDecimal utilidadBruta = BigDecimal.ZERO;
    
    /**
     * Margen bruto (%)
     */
    private BigDecimal margenBruto = BigDecimal.ZERO;
    
    // ==================== GASTOS OPERATIVOS ====================
    
    /**
     * Gastos de nómina (salarios + cargas sociales)
     */
    private BigDecimal gastosNomina = BigDecimal.ZERO;
    
    /**
     * Gastos de administración
     */
    private BigDecimal gastosAdministracion = BigDecimal.ZERO;
    
    /**
     * Gastos de ventas y marketing
     */
    private BigDecimal gastosVentas = BigDecimal.ZERO;
    
    /**
     * Gastos financieros (intereses)
     */
    private BigDecimal gastosFinancieros = BigDecimal.ZERO;
    
    /**
     * Depreciaciones
     */
    private BigDecimal depreciaciones = BigDecimal.ZERO;
    
    /**
     * Otros gastos operativos
     */
    private BigDecimal otrosGastos = BigDecimal.ZERO;
    
    /**
     * TOTAL GASTOS OPERATIVOS
     */
    private BigDecimal totalGastosOperativos = BigDecimal.ZERO;
    
    // ==================== UTILIDADES ====================
    
    /**
     * UTILIDAD OPERATIVA (Utilidad Bruta - Gastos Operativos)
     */
    private BigDecimal utilidadOperativa = BigDecimal.ZERO;
    
    /**
     * Margen operativo (%)
     */
    private BigDecimal margenOperativo = BigDecimal.ZERO;
    
    /**
     * Impuestos sobre la renta
     */
    private BigDecimal impuestos = BigDecimal.ZERO;
    
    /**
     * UTILIDAD NETA (Utilidad Operativa - Impuestos)
     */
    private BigDecimal utilidadNeta = BigDecimal.ZERO;
    
    /**
     * Margen neto (%)
     */
    private BigDecimal margenNeto = BigDecimal.ZERO;
    
    // ==================== COMPARATIVOS ====================
    
    /**
     * Período anterior para comparación
     */
    private EstadoResultadosDTO periodoAnterior;
    
    /**
     * Variación porcentual vs período anterior
     */
    private BigDecimal variacionPorcentual = BigDecimal.ZERO;
    
    /**
     * Desglose detallado de gastos
     */
    private List<LineaGasto> detalleGastos = new ArrayList<>();
    
    @Data
    public static class LineaGasto {
        private String categoria;
        private String concepto;
        private BigDecimal monto;
        private BigDecimal porcentaje; // % del total ingresos
    }
}
```

- [ ] **1.1.2** Crear servicio de cálculo de Estado de Resultados

```java
package com.erp.whatsorders.service;

import com.erp.whatsorders.dto.EstadoResultadosDTO;
import com.erp.whatsorders.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstadoResultadosService {
    
    private final FacturaRepository facturaRepository;
    private final DetalleFacturaRepository detalleFacturaRepository;
    private final NominaRepository nominaRepository;
    private final AsientoContableRepository asientoContableRepository;
    
    /**
     * Genera Estado de Resultados para un período.
     */
    @Transactional(readOnly = true)
    public EstadoResultadosDTO generar(LocalDate fechaDesde, LocalDate fechaHasta) {
        EstadoResultadosDTO estado = new EstadoResultadosDTO();
        estado.setFechaDesde(fechaDesde);
        estado.setFechaHasta(fechaHasta);
        estado.setPeriodo(formatearPeriodo(fechaDesde, fechaHasta));
        
        // 1. INGRESOS
        calcularIngresos(estado, fechaDesde, fechaHasta);
        
        // 2. COSTO DE VENTAS
        calcularCostoVentas(estado, fechaDesde, fechaHasta);
        
        // 3. UTILIDAD BRUTA
        calcularUtilidadBruta(estado);
        
        // 4. GASTOS OPERATIVOS
        calcularGastosOperativos(estado, fechaDesde, fechaHasta);
        
        // 5. UTILIDAD OPERATIVA Y NETA
        calcularUtilidades(estado);
        
        log.info("Estado de Resultados generado - Período: {} - Utilidad Neta: ₡{}", 
            estado.getPeriodo(), estado.getUtilidadNeta());
        
        return estado;
    }
    
    /**
     * Calcula ingresos del período.
     */
    private void calcularIngresos(EstadoResultadosDTO estado, LocalDate desde, LocalDate hasta) {
        // Ingresos por ventas (facturas pagadas)
        BigDecimal ingresosVentas = facturaRepository.sumTotalByFechaBetweenAndEstado(
            desde, hasta, "PAGADA"
        );
        estado.setIngresosVentas(ingresosVentas != null ? ingresosVentas : BigDecimal.ZERO);
        
        // Otros ingresos (consultar cuentas contables de ingresos)
        BigDecimal otrosIngresos = asientoContableRepository.sumByTipoCuentaAndFecha(
            "INGRESO", desde, hasta
        );
        estado.setOtrosIngresos(otrosIngresos != null ? otrosIngresos : BigDecimal.ZERO);
        
        // Total ingresos
        estado.setTotalIngresos(
            estado.getIngresosVentas().add(estado.getOtrosIngresos())
        );
    }
    
    /**
     * Calcula costo de ventas.
     */
    private void calcularCostoVentas(EstadoResultadosDTO estado, LocalDate desde, LocalDate hasta) {
        // Sumar costo de productos vendidos
        BigDecimal costoVentas = detalleFacturaRepository.sumCostoProductosByFecha(desde, hasta);
        estado.setCostoVentas(costoVentas != null ? costoVentas : BigDecimal.ZERO);
    }
    
    /**
     * Calcula utilidad bruta y margen.
     */
    private void calcularUtilidadBruta(EstadoResultadosDTO estado) {
        BigDecimal utilidadBruta = estado.getTotalIngresos().subtract(estado.getCostoVentas());
        estado.setUtilidadBruta(utilidadBruta);
        
        // Margen bruto (%)
        if (estado.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margenBruto = utilidadBruta
                .multiply(new BigDecimal("100"))
                .divide(estado.getTotalIngresos(), 2, RoundingMode.HALF_UP);
            estado.setMargenBruto(margenBruto);
        }
    }
    
    /**
     * Calcula gastos operativos.
     */
    private void calcularGastosOperativos(EstadoResultadosDTO estado, LocalDate desde, LocalDate hasta) {
        // Gastos de nómina (salarios + cargas patronales)
        BigDecimal gastosNomina = nominaRepository.sumTotalSalariosNetosYCargasByFecha(desde, hasta);
        estado.setGastosNomina(gastosNomina != null ? gastosNomina : BigDecimal.ZERO);
        
        // Otros gastos desde cuentas contables
        BigDecimal gastosAdmin = asientoContableRepository.sumByCuentaAndFecha(
            "GASTOS_ADMINISTRACION", desde, hasta
        );
        estado.setGastosAdministracion(gastosAdmin != null ? gastosAdmin : BigDecimal.ZERO);
        
        BigDecimal gastosVentas = asientoContableRepository.sumByCuentaAndFecha(
            "GASTOS_VENTAS", desde, hasta
        );
        estado.setGastosVentas(gastosVentas != null ? gastosVentas : BigDecimal.ZERO);
        
        BigDecimal gastosFinancieros = asientoContableRepository.sumByCuentaAndFecha(
            "GASTOS_FINANCIEROS", desde, hasta
        );
        estado.setGastosFinancieros(gastosFinancieros != null ? gastosFinancieros : BigDecimal.ZERO);
        
        // Total gastos operativos
        estado.setTotalGastosOperativos(
            estado.getGastosNomina()
                .add(estado.getGastosAdministracion())
                .add(estado.getGastosVentas())
                .add(estado.getGastosFinancieros())
                .add(estado.getOtrosGastos())
        );
    }
    
    /**
     * Calcula utilidad operativa y neta.
     */
    private void calcularUtilidades(EstadoResultadosDTO estado) {
        // Utilidad operativa
        BigDecimal utilidadOperativa = estado.getUtilidadBruta()
            .subtract(estado.getTotalGastosOperativos());
        estado.setUtilidadOperativa(utilidadOperativa);
        
        // Margen operativo (%)
        if (estado.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margenOperativo = utilidadOperativa
                .multiply(new BigDecimal("100"))
                .divide(estado.getTotalIngresos(), 2, RoundingMode.HALF_UP);
            estado.setMargenOperativo(margenOperativo);
        }
        
        // Utilidad neta (después de impuestos)
        BigDecimal utilidadNeta = utilidadOperativa.subtract(estado.getImpuestos());
        estado.setUtilidadNeta(utilidadNeta);
        
        // Margen neto (%)
        if (estado.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margenNeto = utilidadNeta
                .multiply(new BigDecimal("100"))
                .divide(estado.getTotalIngresos(), 2, RoundingMode.HALF_UP);
            estado.setMargenNeto(margenNeto);
        }
    }
    
    private String formatearPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.getYear() == hasta.getYear() && desde.getMonth() == hasta.getMonth()) {
            return desde.getMonth().name() + " " + desde.getYear();
        }
        return desde.getYear() + "";
    }
}
```

- [ ] **1.1.3** Crear controlador de reportes

```java
@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS')")
public class ReportesFinancierosController {
    
    private final EstadoResultadosService estadoResultadosService;
    
    @GetMapping("/estado-resultados")
    public String estadoResultados(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Model model
    ) {
        // Default: mes actual
        if (fechaDesde == null) {
            fechaDesde = LocalDate.now().withDayOfMonth(1);
        }
        if (fechaHasta == null) {
            fechaHasta = LocalDate.now();
        }
        
        EstadoResultadosDTO estado = estadoResultadosService.generar(fechaDesde, fechaHasta);
        
        model.addAttribute("estado", estado);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        
        return "reportes/estado-resultados";
    }
}
```

- [ ] **1.1.4** Crear vista de Estado de Resultados

```html
<!-- src/main/resources/templates/reportes/estado-resultados.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Estado de Resultados - Reportes Financieros</title>
</head>
<body>
<div class="container-fluid mt-4">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h3>📊 Estado de Resultados (P&L)</h3>
                    <div class="btn-group">
                        <button class="btn btn-success" onclick="exportarExcel()">
                            <i class="bi bi-file-excel"></i> Excel
                        </button>
                        <button class="btn btn-danger" onclick="exportarPDF()">
                            <i class="bi bi-file-pdf"></i> PDF
                        </button>
                    </div>
                </div>
                <div class="card-body">
                    
                    <!-- Filtros de período -->
                    <form method="get" class="row mb-4">
                        <div class="col-md-3">
                            <label>Desde:</label>
                            <input type="date" 
                                   name="fechaDesde" 
                                   class="form-control" 
                                   th:value="${fechaDesde}">
                        </div>
                        <div class="col-md-3">
                            <label>Hasta:</label>
                            <input type="date" 
                                   name="fechaHasta" 
                                   class="form-control" 
                                   th:value="${fechaHasta}">
                        </div>
                        <div class="col-md-2">
                            <label>&nbsp;</label>
                            <button type="submit" class="btn btn-primary w-100">
                                Generar
                            </button>
                        </div>
                    </form>
                    
                    <!-- Reporte -->
                    <div class="table-responsive">
                        <table class="table table-bordered">
                            <thead class="table-light">
                                <tr>
                                    <th colspan="2" class="text-center">
                                        <h4>Estado de Resultados</h4>
                                        <p th:text="${estado.periodo}"></p>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                <!-- INGRESOS -->
                                <tr class="table-info">
                                    <td><strong>INGRESOS</strong></td>
                                    <td></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Ventas</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.ingresosVentas, 'CRC')}"></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Otros Ingresos</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.otrosIngresos, 'CRC')}"></td>
                                </tr>
                                <tr class="table-success">
                                    <td><strong>TOTAL INGRESOS</strong></td>
                                    <td class="text-end">
                                        <strong th:text="${#numbers.formatCurrency(estado.totalIngresos, 'CRC')}"></strong>
                                    </td>
                                </tr>
                                
                                <!-- COSTO DE VENTAS -->
                                <tr>
                                    <td colspan="2">&nbsp;</td>
                                </tr>
                                <tr class="table-warning">
                                    <td><strong>COSTO DE VENTAS</strong></td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.costoVentas, 'CRC')}"></td>
                                </tr>
                                
                                <!-- UTILIDAD BRUTA -->
                                <tr class="table-success">
                                    <td><strong>UTILIDAD BRUTA</strong></td>
                                    <td class="text-end">
                                        <strong th:text="${#numbers.formatCurrency(estado.utilidadBruta, 'CRC')}"></strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="ps-4 text-muted">Margen Bruto</td>
                                    <td class="text-end text-muted">
                                        <span th:text="${#numbers.formatDecimal(estado.margenBruto, 1, 2) + '%'}"></span>
                                    </td>
                                </tr>
                                
                                <!-- GASTOS OPERATIVOS -->
                                <tr>
                                    <td colspan="2">&nbsp;</td>
                                </tr>
                                <tr class="table-info">
                                    <td><strong>GASTOS OPERATIVOS</strong></td>
                                    <td></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Gastos de Nómina</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.gastosNomina, 'CRC')}"></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Gastos de Administración</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.gastosAdministracion, 'CRC')}"></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Gastos de Ventas</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.gastosVentas, 'CRC')}"></td>
                                </tr>
                                <tr>
                                    <td class="ps-4">Gastos Financieros</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.gastosFinancieros, 'CRC')}"></td>
                                </tr>
                                <tr class="table-warning">
                                    <td><strong>TOTAL GASTOS OPERATIVOS</strong></td>
                                    <td class="text-end">
                                        <strong th:text="${#numbers.formatCurrency(estado.totalGastosOperativos, 'CRC')}"></strong>
                                    </td>
                                </tr>
                                
                                <!-- UTILIDAD OPERATIVA -->
                                <tr class="table-success">
                                    <td><strong>UTILIDAD OPERATIVA</strong></td>
                                    <td class="text-end">
                                        <strong th:text="${#numbers.formatCurrency(estado.utilidadOperativa, 'CRC')}"></strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="ps-4 text-muted">Margen Operativo</td>
                                    <td class="text-end text-muted">
                                        <span th:text="${#numbers.formatDecimal(estado.margenOperativo, 1, 2) + '%'}"></span>
                                    </td>
                                </tr>
                                
                                <!-- IMPUESTOS -->
                                <tr>
                                    <td colspan="2">&nbsp;</td>
                                </tr>
                                <tr>
                                    <td>Impuestos sobre la Renta</td>
                                    <td class="text-end" th:text="${#numbers.formatCurrency(estado.impuestos, 'CRC')}"></td>
                                </tr>
                                
                                <!-- UTILIDAD NETA -->
                                <tr class="table-primary">
                                    <td><strong>UTILIDAD NETA</strong></td>
                                    <td class="text-end">
                                        <strong th:text="${#numbers.formatCurrency(estado.utilidadNeta, 'CRC')}"></strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="ps-4 text-muted">Margen Neto</td>
                                    <td class="text-end text-muted">
                                        <strong th:text="${#numbers.formatDecimal(estado.margenNeto, 1, 2) + '%'}"></strong>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

- [ ] **1.1.5** Crear queries en repositorios
- [ ] **1.1.6** Implementar comparativo con período anterior
- [ ] **1.1.7** Agregar gráfico de barras con Chart.js
- [ ] **1.1.8** Tests de cálculo de Estado de Resultados

---

## 📦 2. BALANCE GENERAL (8 tareas)

**Similar estructura a Estado de Resultados...**

---

## 📦 3. FLUJO DE CAJA (6 tareas)

**Similar estructura...**

---

## 📦 4. RATIOS FINANCIEROS (4 tareas)

- [ ] **4.1** Implementar cálculo de ratios de liquidez
- [ ] **4.2** Implementar ratios de endeudamiento
- [ ] **4.3** Implementar ratios de rentabilidad
- [ ] **4.4** Crear vista comparativa de ratios

---

## 📦 5. DASHBOARD EJECUTIVO (4 tareas)

- [ ] **5.1** Crear dashboard con métricas clave (KPIs)
- [ ] **5.2** Integrar gráficos Chart.js
- [ ] **5.3** Implementar filtros por período
- [ ] **5.4** Agregar widgets de resumen

---

## 📦 6. EXPORTACIÓN Y GRÁFICOS (2 tareas)

- [ ] **6.1** Implementar exportación a Excel (Apache POI)
- [ ] **6.2** Implementar exportación a PDF (iText)

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Estado de Resultados completo y preciso  
✅ Balance General funcionando  
✅ Flujo de Caja generado  
✅ Ratios financieros calculados  
✅ Dashboard ejecutivo con KPIs  
✅ Gráficos interactivos funcionando  
✅ Exportación a Excel y PDF  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 5 Fase 2: Contabilidad
- ✅ Sprint 8 Fase 2: Nómina

**Habilita:**
- 🚀 Análisis financiero ejecutivo
- 🚀 Toma de decisiones basada en datos

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Completar reportes financieros
2. 🚀 Continuar con **FASE 4: Testing**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo  
**Prioridad:** ALTA - Análisis ejecutivo
