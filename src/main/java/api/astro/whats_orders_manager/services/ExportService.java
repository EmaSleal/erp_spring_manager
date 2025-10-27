package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.Producto;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Servicio para exportar reportes a diferentes formatos (PDF, Excel, CSV)
 */
public interface ExportService {
    
    /**
     * Exporta un reporte de ventas a PDF
     * @param facturas Lista de facturas
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el PDF
     */
    ByteArrayOutputStream exportarVentasPDF(List<Factura> facturas, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de clientes a PDF
     * @param clientes Lista de clientes
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el PDF
     */
    ByteArrayOutputStream exportarClientesPDF(List<Cliente> clientes, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de productos a PDF
     * @param productos Lista de productos
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el PDF
     */
    ByteArrayOutputStream exportarProductosPDF(List<Producto> productos, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de ventas a Excel
     * @param facturas Lista de facturas
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el Excel
     */
    ByteArrayOutputStream exportarVentasExcel(List<Factura> facturas, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de clientes a Excel
     * @param clientes Lista de clientes
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el Excel
     */
    ByteArrayOutputStream exportarClientesExcel(List<Cliente> clientes, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de productos a Excel
     * @param productos Lista de productos
     * @param estadisticas Mapa con estadísticas del reporte
     * @return ByteArrayOutputStream con el Excel
     */
    ByteArrayOutputStream exportarProductosExcel(List<Producto> productos, Map<String, Object> estadisticas);
    
    /**
     * Exporta un reporte de ventas a CSV
     * @param facturas Lista de facturas
     * @return ByteArrayOutputStream con el CSV
     */
    ByteArrayOutputStream exportarVentasCSV(List<Factura> facturas);
    
    /**
     * Exporta un reporte de clientes a CSV
     * @param clientes Lista de clientes
     * @return ByteArrayOutputStream con el CSV
     */
    ByteArrayOutputStream exportarClientesCSV(List<Cliente> clientes);
    
    /**
     * Exporta un reporte de productos a CSV
     * @param productos Lista de productos
     * @return ByteArrayOutputStream con el CSV
     */
    ByteArrayOutputStream exportarProductosCSV(List<Producto> productos);
}
