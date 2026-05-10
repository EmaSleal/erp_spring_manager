## 🏗️ ARQUITECTURA

### Componentes Principales

```
┌──────────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN                        │
├──────────────────────────────────────────────────────────────┤
│  /admin/reportes/dashboard.html                              │
│  - 5 gráficas Chart.js (Line, Bar, Doughnut, Radar, Polar)  │
│  - Filtros: rango fechas, categoría, estado                 │
│  - Botones de exportación (PDF/Excel)                       │
│  - Tablas resumen con paginación                            │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                   CAPA DE CONTROLADOR                         │
├──────────────────────────────────────────────────────────────┤
│  ReporteController.java                                      │
│  - GET  /admin/reportes/dashboard                           │
│  - POST /admin/reportes/datos (AJAX)                        │
│  - GET  /admin/reportes/exportar/pdf                        │
│  - GET  /admin/reportes/exportar/excel                      │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                   CAPA DE SERVICIO                            │
├──────────────────────────────────────────────────────────────┤
│  ReporteService.java                                         │
│  - obtenerVentasPorMes(filtros)                             │
│  - obtenerProductosMasVendidos(filtros)                     │
│  - obtenerEstadisticasClientes(filtros)                     │
│  - obtenerComparativaAnual(filtros)                         │
│  - obtenerDistribucionCategorias(filtros)                   │
│                                                              │
│  ExportacionService.java                                     │
│  - exportarPDF(datos, tipo)                                 │
│  - exportarExcel(datos, tipo)                               │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                   CAPA DE PERSISTENCIA                        │
├──────────────────────────────────────────────────────────────┤
│  ReporteRepository.java (JPA + Native Queries)              │
│  - Llamadas a Stored Procedures                             │
│  - Consultas nativas optimizadas                            │
│  - @Query con proyecciones DTO                              │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                   BASE DE DATOS (MySQL 8.0)                   │
├──────────────────────────────────────────────────────────────┤
│  Stored Procedures:                                          │
│  - sp_reporte_ventas_mes(fechaInicio, fechaFin)             │
│  - sp_productos_mas_vendidos(limite, categoriaId)           │
│  - sp_estadisticas_clientes(fechaInicio, fechaFin)          │
│  - sp_comparativa_anual(anio1, anio2)                       │
│  - sp_distribucion_categorias()                             │
│  - sp_top_clientes(limite)                                  │
│  - sp_margen_beneficio(fechaInicio, fechaFin)               │
│  - sp_estado_facturas()                                     │
└──────────────────────────────────────────────────────────────┘
```

---

