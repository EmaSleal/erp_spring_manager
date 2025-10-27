package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * REPORTE SERVICE
 * WhatsApp Orders Manager - Sprint 2 Fase 6
 * ============================================================================
 * Servicio para la generación de reportes y estadísticas del sistema.
 * 
 * Funcionalidades:
 * - Generar reportes de ventas con filtros
 * - Generar reportes de clientes
 * - Generar reportes de productos
 * - Calcular estadísticas generales
 * - Filtrado y agregación de datos
 * ============================================================================
 * @author GitHub Copilot
 * @version 1.0
 * @since 18/10/2025
 */
@Service
public interface ReporteService {
    
    // ========================================================================
    // REPORTES DE VENTAS
    // ========================================================================
    
    /**
     * Genera el reporte de ventas según los filtros especificados.
     * 
     * @param fechaInicio Fecha de inicio del período (opcional)
     * @param fechaFin Fecha de fin del período (opcional)
     * @param clienteId ID del cliente para filtrar (opcional)
     * @return Lista de facturas filtradas
     */
    List<Factura> generarReporteVentas(LocalDate fechaInicio, LocalDate fechaFin, Integer clienteId);
    
    /**
     * Calcula las estadísticas del reporte de ventas.
     * 
     * @param facturas Lista de facturas del reporte
     * @return Map con las estadísticas calculadas
     */
    Map<String, Object> calcularEstadisticasVentas(List<Factura> facturas);
    
    // ========================================================================
    // REPORTES DE CLIENTES
    // ========================================================================
    
    /**
     * Genera el reporte de clientes según los filtros especificados.
     * 
     * @param activo Filtrar por estado activo/inactivo (opcional)
     * @param conDeuda Filtrar clientes con deuda pendiente (opcional)
     * @return Lista de clientes filtrados
     */
    List<Cliente> generarReporteClientes(Boolean activo, Boolean conDeuda);
    
    /**
     * Calcula las estadísticas del reporte de clientes.
     * 
     * @param clientes Lista de clientes del reporte
     * @return Map con las estadísticas calculadas
     */
    Map<String, Object> calcularEstadisticasClientes(List<Cliente> clientes);
    
    // ========================================================================
    // REPORTES DE PRODUCTOS
    // ========================================================================
    
    /**
     * Genera el reporte de productos según los filtros especificados.
     * 
     * @param stockBajo Filtrar productos con stock bajo (opcional)
     * @param sinVentas Filtrar productos sin ventas (opcional)
     * @return Lista de productos filtrados
     */
    List<Producto> generarReporteProductos(Boolean stockBajo, Boolean sinVentas);
    
    /**
     * Calcula las estadísticas del reporte de productos.
     * 
     * @param productos Lista de productos del reporte
     * @return Map con las estadísticas calculadas
     */
    Map<String, Object> calcularEstadisticasProductos(List<Producto> productos);
    
    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================
    
    /**
     * Obtiene los productos más vendidos.
     * 
     * @param limite Cantidad de productos a retornar
     * @return Lista de productos más vendidos
     */
    List<Map<String, Object>> obtenerProductosMasVendidos(int limite);
    
    /**
     * Obtiene las ventas agrupadas por mes.
     * 
     * @param meses Cantidad de meses hacia atrás
     * @return Map con ventas por mes
     */
    Map<String, BigDecimal> obtenerVentasPorMes(int meses);
    
    /**
     * Obtiene los clientes con mayor volumen de compras.
     * 
     * @param limite Cantidad de clientes a retornar
     * @return Lista de clientes top
     */
    List<Map<String, Object>> obtenerClientesTop(int limite);
}
