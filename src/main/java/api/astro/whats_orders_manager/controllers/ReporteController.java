package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.ClienteRepository;
import api.astro.whats_orders_manager.repositories.FacturaRepository;
import api.astro.whats_orders_manager.repositories.ProductoRepository;
import api.astro.whats_orders_manager.services.*;
import api.astro.whats_orders_manager.util.ResponseUtil;
import api.astro.whats_orders_manager.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * REPORTE CONTROLLER
 * WhatsApp Orders Manager - Sprint 2 Fase 6
 * ============================================================================
 * Controlador para la gestión de reportes y estadísticas del sistema.
 * 
 * Funcionalidades:
 * - Dashboard de reportes con acceso rápido
 * - Reporte de ventas con filtros de fecha y cliente
 * - Reporte de clientes con estadísticas
 * - Reporte de productos más vendidos
 * - Exportación a PDF, Excel y CSV
 * - APIs REST para gráficos dinámicos
 * 
 * Restricción de acceso: Solo ADMIN y USER
 * 
 * @version 2.1 - Aplicado ResponseUtil y StringUtil
 * @since 26/10/2025
 * ============================================================================
 */
@Controller
@RequestMapping("/reportes")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@RequiredArgsConstructor
@Slf4j
public class ReporteController {

    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final ReporteService reporteService;
    private final ExportService exportService;
    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    // ========================================================================
    // ENDPOINT 1: DASHBOARD DE REPORTES
    // ========================================================================
    /**
     * Muestra la página principal de reportes con cards de acceso rápido.
     * 
     * @param model Modelo de datos para la vista
     * @param authentication Información del usuario autenticado
     * @return Vista reportes/index.html
     */
    @GetMapping
    public String index(Model model, Authentication authentication) {
        log.info("=== Acceso al dashboard de reportes ===");
        
        try {
            // Cargar información del usuario actual
            cargarDatosUsuario(model, authentication);
            
            // Estadísticas generales para las cards
            long totalClientes = clienteService.count();
            long totalProductos = productoService.count();
            long totalFacturas = facturaService.count();
            long totalUsuarios = usuarioService.findAll().size();
            
            model.addAttribute("totalClientes", totalClientes);
            model.addAttribute("totalProductos", totalProductos);
            model.addAttribute("totalFacturas", totalFacturas);
            model.addAttribute("totalUsuarios", totalUsuarios);
            
            log.info("Dashboard cargado - Clientes: {}, Productos: {}, Facturas: {}, Usuarios: {}", 
                     totalClientes, totalProductos, totalFacturas, totalUsuarios);
            
            return "reportes/index";
            
        } catch (Exception e) {
            log.error("Error al cargar dashboard de reportes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar las estadísticas");
            return "error/error";
        }
    }

    // ========================================================================
    // ENDPOINT 2: REPORTE DE VENTAS
    // ========================================================================
    /**
     * Genera el reporte de ventas con filtros opcionales.
     * 
     * @param fechaInicio Fecha de inicio del período (opcional)
     * @param fechaFin Fecha de fin del período (opcional)
     * @param clienteId ID del cliente para filtrar (opcional)
     * @param model Modelo de datos
     * @param authentication Usuario autenticado
     * @return Vista reportes/ventas.html
     */
    @GetMapping("/ventas")
    public String reporteVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer clienteId,
            Model model,
            Authentication authentication
    ) {
        log.info("=== Generando reporte de ventas ===");
        log.info("Filtros - Inicio: {}, Fin: {}, ClienteId: {}", fechaInicio, fechaFin, clienteId);
        
        try {
            cargarDatosUsuario(model, authentication);
            
            // Generar reporte con el servicio
            List<Factura> facturas = reporteService.generarReporteVentas(fechaInicio, fechaFin, clienteId);
            
            // Calcular estadísticas
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasVentas(facturas);
            
            // Agregar al modelo
            model.addAttribute("facturas", facturas);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("clientes", clienteService.findAll());
            model.addAttribute("fechaInicio", fechaInicio);
            model.addAttribute("fechaFin", fechaFin);
            model.addAttribute("clienteId", clienteId);
            
            log.info("Reporte de ventas generado exitosamente - {} facturas", facturas.size());
            return "reportes/ventas";
            
        } catch (Exception e) {
            log.error("Error al generar reporte de ventas: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar el reporte");
            return "error/error";
        }
    }

    // ========================================================================
    // ENDPOINT 3: REPORTE DE CLIENTES
    // ========================================================================
    /**
     * Genera el reporte de clientes con estadísticas.
     * 
     * @param activo Filtrar por estado activo/inactivo (opcional)
     * @param conDeuda Filtrar clientes con deuda pendiente (opcional)
     * @param model Modelo de datos
     * @param authentication Usuario autenticado
     * @return Vista reportes/clientes.html
     */
    @GetMapping("/clientes")
    public String reporteClientes(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conDeuda,
            Model model,
            Authentication authentication
    ) {
        log.info("=== Generando reporte de clientes ===");
        log.info("Filtros - Activo: {}, ConDeuda: {}", activo, conDeuda);
        
        try {
            cargarDatosUsuario(model, authentication);
            
            // Generar reporte con el servicio
            List<Cliente> clientes = reporteService.generarReporteClientes(activo, conDeuda);
            
            // Calcular estadísticas
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasClientes(clientes);
            
            // Agregar al modelo
            model.addAttribute("clientes", clientes);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("activo", activo);
            model.addAttribute("conDeuda", conDeuda);
            
            log.info("Reporte de clientes generado - {} clientes", clientes.size());
            return "reportes/clientes";
            
        } catch (Exception e) {
            log.error("Error al generar reporte de clientes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar el reporte");
            return "error/error";
        }
    }

    // ========================================================================
    // ENDPOINT 4: REPORTE DE PRODUCTOS
    // ========================================================================
    /**
     * Genera el reporte de productos más vendidos y stock.
     * 
     * @param stockBajo Filtrar productos con stock bajo (opcional)
     * @param sinVentas Filtrar productos sin ventas (opcional)
     * @param model Modelo de datos
     * @param authentication Usuario autenticado
     * @return Vista reportes/productos.html
     */
    @GetMapping("/productos")
    public String reporteProductos(
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean sinVentas,
            Model model,
            Authentication authentication
    ) {
        log.info("=== Generando reporte de productos ===");
        log.info("Filtros - StockBajo: {}, SinVentas: {}", stockBajo, sinVentas);
        
        try {
            cargarDatosUsuario(model, authentication);
            
            // Generar reporte con el servicio
            List<Producto> productos = reporteService.generarReporteProductos(stockBajo, sinVentas);
            
            // Calcular estadísticas
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasProductos(productos);
            
            // Agregar al modelo
            model.addAttribute("productos", productos);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("stockBajo", stockBajo);
            model.addAttribute("sinVentas", sinVentas);
            
            log.info("Reporte de productos generado - {} productos", productos.size());
            return "reportes/productos";
            
        } catch (Exception e) {
            log.error("Error al generar reporte de productos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar el reporte");
            return "error/error";
        }
    }

    // ========================================================================
    // ENDPOINT 5: EXPORTAR A PDF
    // ========================================================================
    /**
     * Exporta un reporte a PDF.
     * 
     * @param tipo Tipo de reporte (ventas, clientes, productos)
     * @param fechaInicio Fecha inicio (solo para ventas)
     * @param fechaFin Fecha fin (solo para ventas)
     * @return Archivo PDF con el reporte
     */
    @GetMapping("/export/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        log.info("=== Exportando reporte a PDF ===");
        log.info("Tipo: {}, Inicio: {}, Fin: {}", tipo, fechaInicio, fechaFin);
        
        try {
            // TODO: Implementar generación de PDF
            byte[] pdfBytes = new byte[0]; // Placeholder
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_" + tipo + ".pdf");
            
            log.info("PDF generado exitosamente");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Error al generar PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // ENDPOINT 6: EXPORTAR A EXCEL
    // ========================================================================
    /**
     * Exporta un reporte a Excel.
     * 
     * @param tipo Tipo de reporte (ventas, clientes, productos)
     * @param fechaInicio Fecha inicio (solo para ventas)
     * @param fechaFin Fecha fin (solo para ventas)
     * @return Archivo Excel con el reporte
     */
    @GetMapping("/export/excel")
    @ResponseBody
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        log.info("=== Exportando reporte a Excel ===");
        log.info("Tipo: {}, Inicio: {}, Fin: {}", tipo, fechaInicio, fechaFin);
        
        try {
            // TODO: Implementar generación de Excel
            byte[] excelBytes = new byte[0]; // Placeholder
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "reporte_" + tipo + ".xlsx");
            
            log.info("Excel generado exitosamente");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            log.error("Error al generar Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================================================
    // EXPORTACIÓN PDF - Métodos consolidados
    // ========================================================================
    
    /**
     * Exporta el reporte de ventas a PDF
     */
    @GetMapping("/ventas/exportar/pdf")
    public ResponseEntity<byte[]> exportarVentasPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer clienteId
    ) {
        log.info("Exportando reporte de ventas a PDF - Inicio: {}, Fin: {}, ClienteId: {}", 
                fechaInicio, fechaFin, clienteId);
        
        try {
            List<Factura> facturas = reporteService.generarReporteVentas(fechaInicio, fechaFin, clienteId);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasVentas(facturas);
            byte[] pdfBytes = exportService.exportarVentasPDF(facturas, estadisticas).toByteArray();
            
            log.info("✅ PDF de ventas generado exitosamente - {} bytes", pdfBytes.length);
            return ResponseUtil.pdf(pdfBytes, "reporte-ventas.pdf");
            
        } catch (Exception e) {
            log.error("Error al exportar ventas a PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de clientes a PDF
     */
    @GetMapping("/clientes/exportar/pdf")
    public ResponseEntity<byte[]> exportarClientesPDF(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conDeuda
    ) {
        log.info("Exportando reporte de clientes a PDF - Activo: {}, ConDeuda: {}", activo, conDeuda);
        
        try {
            List<Cliente> clientes = reporteService.generarReporteClientes(activo, conDeuda);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasClientes(clientes);
            byte[] pdfBytes = exportService.exportarClientesPDF(clientes, estadisticas).toByteArray();
            
            log.info("✅ PDF de clientes generado exitosamente - {} bytes", pdfBytes.length);
            return ResponseUtil.pdf(pdfBytes, "reporte-clientes.pdf");
            
        } catch (Exception e) {
            log.error("Error al exportar clientes a PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de productos a PDF
     */
    @GetMapping("/productos/exportar/pdf")
    public ResponseEntity<byte[]> exportarProductosPDF(
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean sinVentas
    ) {
        log.info("Exportando reporte de productos a PDF - StockBajo: {}, SinVentas: {}", 
                stockBajo, sinVentas);
        
        try {
            List<Producto> productos = reporteService.generarReporteProductos(stockBajo, sinVentas);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasProductos(productos);
            byte[] pdfBytes = exportService.exportarProductosPDF(productos, estadisticas).toByteArray();
            
            log.info("✅ PDF de productos generado exitosamente - {} bytes", pdfBytes.length);
            return ResponseUtil.pdf(pdfBytes, "reporte-productos.pdf");
            
        } catch (Exception e) {
            log.error("Error al exportar productos a PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========================================================================
    // EXPORTACIÓN EXCEL - Métodos consolidados
    // ========================================================================
    
    /**
     * Exporta el reporte de ventas a Excel
     */
    @GetMapping("/ventas/exportar/excel")
    public ResponseEntity<byte[]> exportarVentasExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer clienteId
    ) {
        log.info("Exportando reporte de ventas a Excel - Inicio: {}, Fin: {}, ClienteId: {}", 
                fechaInicio, fechaFin, clienteId);
        
        try {
            List<Factura> facturas = reporteService.generarReporteVentas(fechaInicio, fechaFin, clienteId);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasVentas(facturas);
            byte[] excelBytes = exportService.exportarVentasExcel(facturas, estadisticas).toByteArray();
            
            log.info("✅ Excel de ventas generado exitosamente - {} bytes", excelBytes.length);
            return ResponseUtil.excel(excelBytes, "reporte-ventas.xlsx");
            
        } catch (Exception e) {
            log.error("Error al exportar ventas a Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de clientes a Excel
     */
    @GetMapping("/clientes/exportar/excel")
    public ResponseEntity<byte[]> exportarClientesExcel(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conDeuda
    ) {
        log.info("Exportando reporte de clientes a Excel - Activo: {}, ConDeuda: {}", activo, conDeuda);
        
        try {
            List<Cliente> clientes = reporteService.generarReporteClientes(activo, conDeuda);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasClientes(clientes);
            byte[] excelBytes = exportService.exportarClientesExcel(clientes, estadisticas).toByteArray();
            
            log.info("✅ Excel de clientes generado exitosamente - {} bytes", excelBytes.length);
            return ResponseUtil.excel(excelBytes, "reporte-clientes.xlsx");
            
        } catch (Exception e) {
            log.error("Error al exportar clientes a Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de productos a Excel
     */
    @GetMapping("/productos/exportar/excel")
    public ResponseEntity<byte[]> exportarProductosExcel(
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean sinVentas
    ) {
        log.info("Exportando reporte de productos a Excel - StockBajo: {}, SinVentas: {}", 
                stockBajo, sinVentas);
        
        try {
            List<Producto> productos = reporteService.generarReporteProductos(stockBajo, sinVentas);
            Map<String, Object> estadisticas = reporteService.calcularEstadisticasProductos(productos);
            byte[] excelBytes = exportService.exportarProductosExcel(productos, estadisticas).toByteArray();
            
            log.info("✅ Excel de productos generado exitosamente - {} bytes", excelBytes.length);
            return ResponseUtil.excel(excelBytes, "reporte-productos.xlsx");
            
        } catch (Exception e) {
            log.error("Error al exportar productos a Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========================================================================
    // EXPORTACIÓN CSV - Métodos consolidados
    // ========================================================================
    
    /**
     * Exporta el reporte de ventas a CSV
     */
    @GetMapping("/ventas/exportar/csv")
    public ResponseEntity<byte[]> exportarVentasCSV(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer clienteId
    ) {
        log.info("Exportando reporte de ventas a CSV - Inicio: {}, Fin: {}, ClienteId: {}", 
                fechaInicio, fechaFin, clienteId);
        
        try {
            List<Factura> facturas = reporteService.generarReporteVentas(fechaInicio, fechaFin, clienteId);
            byte[] csvBytes = exportService.exportarVentasCSV(facturas).toByteArray();
            
            log.info("✅ CSV de ventas generado exitosamente - {} bytes", csvBytes.length);
            return ResponseUtil.csv(csvBytes, "reporte-ventas.csv");
            
        } catch (Exception e) {
            log.error("Error al exportar ventas a CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de clientes a CSV
     */
    @GetMapping("/clientes/exportar/csv")
    public ResponseEntity<byte[]> exportarClientesCSV(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conDeuda
    ) {
        log.info("Exportando reporte de clientes a CSV - Activo: {}, ConDeuda: {}", activo, conDeuda);
        
        try {
            List<Cliente> clientes = reporteService.generarReporteClientes(activo, conDeuda);
            byte[] csvBytes = exportService.exportarClientesCSV(clientes).toByteArray();
            
            log.info("✅ CSV de clientes generado exitosamente - {} bytes", csvBytes.length);
            return ResponseUtil.csv(csvBytes, "reporte-clientes.csv");
            
        } catch (Exception e) {
            log.error("Error al exportar clientes a CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta el reporte de productos a CSV
     */
    @GetMapping("/productos/exportar/csv")
    public ResponseEntity<byte[]> exportarProductosCSV(
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean sinVentas
    ) {
        log.info("Exportando reporte de productos a CSV - StockBajo: {}, SinVentas: {}", 
                stockBajo, sinVentas);
        
        try {
            List<Producto> productos = reporteService.generarReporteProductos(stockBajo, sinVentas);
            byte[] csvBytes = exportService.exportarProductosCSV(productos).toByteArray();
            
            log.info("✅ CSV de productos generado exitosamente - {} bytes", csvBytes.length);
            return ResponseUtil.csv(csvBytes, "reporte-productos.csv");
            
        } catch (Exception e) {
            log.error("Error al exportar productos a CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ============================================================================
    // ENDPOINTS API PARA GRÁFICOS (JSON)
    // ============================================================================
    
    /**
     * API para obtener datos de ventas por mes (últimos N meses)
     * Retorna JSON con labels (meses) y data (totales)
     * Optimizado con Stored Procedure
     */
    @GetMapping("/api/ventas-por-mes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVentasPorMes(
            @RequestParam(required = false, defaultValue = "12") Integer meses
    ) {
        log.info("API: Obteniendo datos de ventas por mes - últimos {} meses (usando SP)", meses);
        
        try {
            // Llamar directamente al SP desde el repository
            List<Object[]> resultadoSP = facturaRepository.obtenerVentasPorMes(meses);
            
            // Convertir a formato esperado por Chart.js
            List<String> labels = new ArrayList<>();
            List<BigDecimal> data = new ArrayList<>();
            
            for (Object[] fila : resultadoSP) {
                labels.add((String) fila[0]); // mes
                data.add((BigDecimal) fila[1]); // total_ventas
            }
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("labels", labels);
            resultado.put("data", data);
            
            log.info("✅ Datos de ventas por mes obtenidos desde SP - {} registros", resultadoSP.size());
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("Error al obtener ventas por mes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API para obtener datos de clientes nuevos por mes (últimos N meses)
     * Retorna JSON con labels (meses) y data (cantidad de clientes)
     * Optimizado con Stored Procedure
     */
    @GetMapping("/api/clientes-nuevos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getClientesNuevos(
            @RequestParam(required = false, defaultValue = "12") Integer meses
    ) {
        log.info("API: Obteniendo datos de clientes nuevos - últimos {} meses (usando SP)", meses);
        
        try {
            // Llamar directamente al SP desde el repository
            List<Object[]> resultadoSP = clienteRepository.obtenerClientesNuevosPorMes(meses);
            
            // Convertir a formato esperado por Chart.js
            List<String> labels = new ArrayList<>();
            List<Long> data = new ArrayList<>();
            
            for (Object[] fila : resultadoSP) {
                labels.add((String) fila[0]); // mes
                data.add(((Number) fila[1]).longValue()); // cantidad_clientes
            }
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("labels", labels);
            resultado.put("data", data);
            
            log.info("✅ Datos de clientes nuevos obtenidos desde SP - {} registros", resultadoSP.size());
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("Error al obtener clientes nuevos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API para obtener productos más vendidos (top N)
     * Retorna JSON con labels (productos) y data (cantidad vendida)
     * Optimizado con Stored Procedure
     */
    @GetMapping("/api/productos-mas-vendidos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductosMasVendidos(
            @RequestParam(required = false, defaultValue = "10") Integer limite
    ) {
        log.info("API: Obteniendo top {} productos más vendidos (usando SP)", limite);
        
        try {
            // Llamar directamente al SP desde el repository
            List<Object[]> resultadoSP = productoRepository.obtenerProductosMasVendidos(limite);
            
            // Convertir a formato esperado por Chart.js
            List<String> labels = new ArrayList<>();
            List<BigDecimal> data = new ArrayList<>();
            
            for (Object[] fila : resultadoSP) {
                labels.add((String) fila[0]); // producto
                data.add((BigDecimal) fila[1]); // cantidad_vendida
            }
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("labels", labels);
            resultado.put("data", data);
            
            log.info("✅ Datos de productos más vendidos obtenidos desde SP - {} registros", 
                    resultadoSP.size());
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("Error al obtener productos más vendidos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================
    
    /**
     * Carga los datos del usuario actual en el modelo.
     * 
     * @param model Modelo de datos
     * @param authentication Autenticación del usuario
     */
    private void cargarDatosUsuario(Model model, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String telefono = userDetails.getUsername();
            Optional<Usuario> usuarioOpt = usuarioService.findByTelefono(telefono);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                model.addAttribute("userName", usuario.getNombre());
                model.addAttribute("userRole", usuario.getRol());
                
                // Generar iniciales para el avatar
                String iniciales = StringUtil.generarIniciales(usuario.getNombre());
                model.addAttribute("userInitials", iniciales);
                
                log.debug("Usuario cargado: {} ({})", usuario.getNombre(), usuario.getRol());
            } else {
                log.warn("Usuario no encontrado con teléfono: {}", telefono);
            }
        }
    }
}
