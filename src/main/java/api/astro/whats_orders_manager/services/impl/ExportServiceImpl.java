package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Empresa;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.services.EmpresaService;
import api.astro.whats_orders_manager.services.ExportService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de exportación de reportes
 */
@Service
@Slf4j
public class ExportServiceImpl implements ExportService {

    @Autowired
    private EmpresaService empresaService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ============================================================================
    // EXPORTACIÓN A PDF
    // ============================================================================

    @Override
    public ByteArrayOutputStream exportarVentasPDF(List<Factura> facturas, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de ventas a PDF - {} facturas", facturas.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Agregar encabezado
            agregarEncabezadoPDF(document, "Reporte de Ventas");
            
            // Agregar estadísticas
            agregarEstadisticasVentasPDF(document, estadisticas);
            
            // Agregar tabla de facturas
            agregarTablaVentasPDF(document, facturas);
            
            document.close();
            log.info("Reporte de ventas PDF generado exitosamente");
            
        } catch (Exception e) {
            log.error("Error al generar PDF de ventas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarClientesPDF(List<Cliente> clientes, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de clientes a PDF - {} clientes", clientes.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            agregarEncabezadoPDF(document, "Reporte de Clientes");
            agregarEstadisticasClientesPDF(document, estadisticas);
            agregarTablaClientesPDF(document, clientes);
            
            document.close();
            log.info("Reporte de clientes PDF generado exitosamente");
            
        } catch (Exception e) {
            log.error("Error al generar PDF de clientes: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarProductosPDF(List<Producto> productos, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de productos a PDF - {} productos", productos.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            agregarEncabezadoPDF(document, "Reporte de Productos");
            agregarEstadisticasProductosPDF(document, estadisticas);
            agregarTablaProductosPDF(document, productos);
            
            document.close();
            log.info("Reporte de productos PDF generado exitosamente");
            
        } catch (Exception e) {
            log.error("Error al generar PDF de productos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF: " + e.getMessage());
        }
        
        return baos;
    }

    // ============================================================================
    // MÉTODOS AUXILIARES PARA PDF
    // ============================================================================

    private void agregarEncabezadoPDF(Document document, String titulo) {
        Empresa empresa = empresaService.getEmpresaPrincipal();
        
        // Título principal
        Paragraph tituloPrincipal = new Paragraph(titulo)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(tituloPrincipal);
        
        // Información de la empresa
        if (empresa != null && empresa.getNombreEmpresa() != null) {
            Paragraph infoEmpresa = new Paragraph(empresa.getNombreEmpresa())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            document.add(infoEmpresa);
            
            if (empresa.getRuc() != null) {
                Paragraph ruc = new Paragraph("RUC: " + empresa.getRuc())
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(2);
                document.add(ruc);
            }
        }
        
        // Fecha de generación
        Paragraph fecha = new Paragraph("Fecha de generación: " + DATETIME_FORMAT.format(new Date()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(fecha);
    }

    private void agregarEstadisticasVentasPDF(Document document, Map<String, Object> estadisticas) {
        Paragraph tituloEstadisticas = new Paragraph("Estadísticas Generales")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(tituloEstadisticas);
        
        Table table = new Table(new float[]{1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("Total Ventas", headerColor));
        table.addHeaderCell(crearCeldaHeader("Promedio", headerColor));
        table.addHeaderCell(crearCeldaHeader("Pagado", headerColor));
        table.addHeaderCell(crearCeldaHeader("Pendiente", headerColor));
        
        // Datos
        table.addCell(formatearMoneda((BigDecimal) estadisticas.get("totalVentas")));
        table.addCell(formatearMoneda((BigDecimal) estadisticas.get("promedioVenta")));
        table.addCell(formatearMoneda((BigDecimal) estadisticas.get("totalPagado")));
        table.addCell(formatearMoneda((BigDecimal) estadisticas.get("totalPendiente")));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void agregarEstadisticasClientesPDF(Document document, Map<String, Object> estadisticas) {
        Paragraph tituloEstadisticas = new Paragraph("Estadísticas Generales")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(tituloEstadisticas);
        
        Table table = new Table(new float[]{1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("Total", headerColor));
        table.addHeaderCell(crearCeldaHeader("Activos", headerColor));
        table.addHeaderCell(crearCeldaHeader("Institucionales", headerColor));
        table.addHeaderCell(crearCeldaHeader("Mayoristas", headerColor));
        
        table.addCell(String.valueOf(estadisticas.get("totalClientes")));
        table.addCell(String.valueOf(estadisticas.get("clientesActivos")));
        table.addCell(String.valueOf(estadisticas.get("clientesInstitucionales")));
        table.addCell(String.valueOf(estadisticas.get("clientesMayoristas")));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void agregarEstadisticasProductosPDF(Document document, Map<String, Object> estadisticas) {
        Paragraph tituloEstadisticas = new Paragraph("Estadísticas Generales")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(tituloEstadisticas);
        
        Table table = new Table(new float[]{1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("Total Productos", headerColor));
        table.addHeaderCell(crearCeldaHeader("Activos", headerColor));
        table.addHeaderCell(crearCeldaHeader("Inactivos", headerColor));
        
        table.addCell(String.valueOf(estadisticas.get("totalProductos")));
        table.addCell(String.valueOf(estadisticas.get("productosActivos")));
        table.addCell(String.valueOf(estadisticas.get("productosInactivos")));
        
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void agregarTablaVentasPDF(Document document, List<Factura> facturas) {
        Paragraph titulo = new Paragraph("Detalle de Ventas")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(titulo);
        
        Table table = new Table(new float[]{1, 2, 2, 1.5f, 1.5f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("Factura", headerColor));
        table.addHeaderCell(crearCeldaHeader("Cliente", headerColor));
        table.addHeaderCell(crearCeldaHeader("Fecha", headerColor));
        table.addHeaderCell(crearCeldaHeader("Subtotal", headerColor));
        table.addHeaderCell(crearCeldaHeader("IGV", headerColor));
        table.addHeaderCell(crearCeldaHeader("Total", headerColor));
        
        // Datos
        for (Factura factura : facturas) {
            table.addCell(factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "-");
            table.addCell(factura.getCliente() != null ? factura.getCliente().getNombre() : "-");
            table.addCell(factura.getCreateDate() != null ? DATE_FORMAT.format(factura.getCreateDate()) : "-");
            table.addCell(formatearMoneda(factura.getSubtotal()));
            table.addCell(formatearMoneda(factura.getIgv()));
            table.addCell(formatearMoneda(factura.getTotal()));
        }
        
        document.add(table);
    }

    private void agregarTablaClientesPDF(Document document, List<Cliente> clientes) {
        Paragraph titulo = new Paragraph("Listado de Clientes")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(titulo);
        
        Table table = new Table(new float[]{0.5f, 2, 2, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));
        
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("ID", headerColor));
        table.addHeaderCell(crearCeldaHeader("Nombre", headerColor));
        table.addHeaderCell(crearCeldaHeader("Tipo", headerColor));
        table.addHeaderCell(crearCeldaHeader("Fecha Registro", headerColor));
        
        for (Cliente cliente : clientes) {
            table.addCell(String.valueOf(cliente.getIdCliente()));
            table.addCell(cliente.getNombre() != null ? cliente.getNombre() : "-");
            table.addCell(cliente.getTipoCliente() != null ? cliente.getTipoCliente().getDisplayName() : "-");
            table.addCell(cliente.getFechaRegistro() != null ? DATE_FORMAT.format(cliente.getFechaRegistro()) : "-");
        }
        
        document.add(table);
    }

    private void agregarTablaProductosPDF(Document document, List<Producto> productos) {
        Paragraph titulo = new Paragraph("Listado de Productos")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(titulo);
        
        Table table = new Table(new float[]{0.5f, 1, 3, 1.5f, 1.5f, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        
        DeviceRgb headerColor = new DeviceRgb(52, 58, 64);
        table.addHeaderCell(crearCeldaHeader("ID", headerColor));
        table.addHeaderCell(crearCeldaHeader("Código", headerColor));
        table.addHeaderCell(crearCeldaHeader("Descripción", headerColor));
        table.addHeaderCell(crearCeldaHeader("P. Institucional", headerColor));
        table.addHeaderCell(crearCeldaHeader("P. Mayorista", headerColor));
        table.addHeaderCell(crearCeldaHeader("Estado", headerColor));
        
        for (Producto producto : productos) {
            table.addCell(String.valueOf(producto.getIdProducto()));
            table.addCell(producto.getCodigo() != null ? producto.getCodigo() : "-");
            table.addCell(producto.getDescripcion() != null ? producto.getDescripcion() : "-");
            table.addCell(formatearMoneda(producto.getPrecioInstitucional()));
            table.addCell(formatearMoneda(producto.getPrecioMayorista()));
            table.addCell(Boolean.TRUE.equals(producto.getActive()) ? "Activo" : "Inactivo");
        }
        
        document.add(table);
    }

    private com.itextpdf.layout.element.Cell crearCeldaHeader(String texto, DeviceRgb color) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) return "S/ 0.00";
        return String.format("S/ %.2f", valor);
    }

    // ============================================================================
    // EXPORTACIÓN A EXCEL
    // ============================================================================

    @Override
    public ByteArrayOutputStream exportarVentasExcel(List<Factura> facturas, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de ventas a Excel - {} facturas", facturas.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");
            
            int rowNum = 0;
            
            // Título
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VENTAS");
            titleCell.setCellStyle(crearEstiloTitulo(workbook));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
            
            rowNum++; // Línea en blanco
            
            // Estadísticas
            rowNum = agregarEstadisticasVentasExcel(sheet, rowNum, estadisticas, workbook);
            
            rowNum++; // Línea en blanco
            
            // Tabla de datos
            agregarTablaVentasExcel(sheet, rowNum, facturas, workbook);
            
            // Auto-ajustar columnas
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(baos);
            log.info("Reporte de ventas Excel generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar Excel de ventas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar Excel: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarClientesExcel(List<Cliente> clientes, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de clientes a Excel - {} clientes", clientes.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Clientes");
            
            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE CLIENTES");
            titleCell.setCellStyle(crearEstiloTitulo(workbook));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
            
            rowNum++;
            rowNum = agregarEstadisticasClientesExcel(sheet, rowNum, estadisticas, workbook);
            rowNum++;
            agregarTablaClientesExcel(sheet, rowNum, clientes, workbook);
            
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(baos);
            log.info("Reporte de clientes Excel generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar Excel de clientes: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar Excel: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarProductosExcel(List<Producto> productos, Map<String, Object> estadisticas) {
        log.info("Exportando reporte de productos a Excel - {} productos", productos.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Productos");
            
            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE PRODUCTOS");
            titleCell.setCellStyle(crearEstiloTitulo(workbook));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
            
            rowNum++;
            rowNum = agregarEstadisticasProductosExcel(sheet, rowNum, estadisticas, workbook);
            rowNum++;
            agregarTablaProductosExcel(sheet, rowNum, productos, workbook);
            
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(baos);
            log.info("Reporte de productos Excel generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar Excel de productos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar Excel: " + e.getMessage());
        }
        
        return baos;
    }

    // ============================================================================
    // MÉTODOS AUXILIARES PARA EXCEL
    // ============================================================================

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloHeader(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloMoneda(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("S/ #,##0.00"));
        return style;
    }

    private int agregarEstadisticasVentasExcel(Sheet sheet, int rowNum, Map<String, Object> estadisticas, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        CellStyle monedaStyle = crearEstiloMoneda(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Total Ventas", "Promedio", "Pagado", "Pendiente"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        Row dataRow = sheet.createRow(rowNum++);
        BigDecimal totalVentas = (BigDecimal) estadisticas.get("totalVentas");
        BigDecimal promedioVenta = (BigDecimal) estadisticas.get("promedioVenta");
        BigDecimal totalPagado = (BigDecimal) estadisticas.get("totalPagado");
        BigDecimal totalPendiente = (BigDecimal) estadisticas.get("totalPendiente");
        
        Cell cell0 = dataRow.createCell(0);
        cell0.setCellValue(totalVentas != null ? totalVentas.doubleValue() : 0);
        cell0.setCellStyle(monedaStyle);
        
        Cell cell1 = dataRow.createCell(1);
        cell1.setCellValue(promedioVenta != null ? promedioVenta.doubleValue() : 0);
        cell1.setCellStyle(monedaStyle);
        
        Cell cell2 = dataRow.createCell(2);
        cell2.setCellValue(totalPagado != null ? totalPagado.doubleValue() : 0);
        cell2.setCellStyle(monedaStyle);
        
        Cell cell3 = dataRow.createCell(3);
        cell3.setCellValue(totalPendiente != null ? totalPendiente.doubleValue() : 0);
        cell3.setCellStyle(monedaStyle);
        
        return rowNum;
    }

    private int agregarEstadisticasClientesExcel(Sheet sheet, int rowNum, Map<String, Object> estadisticas, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Total", "Activos", "Institucionales", "Mayoristas"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        Row dataRow = sheet.createRow(rowNum++);
        dataRow.createCell(0).setCellValue(getLongValue(estadisticas.get("totalClientes")));
        dataRow.createCell(1).setCellValue(getLongValue(estadisticas.get("clientesActivos")));
        dataRow.createCell(2).setCellValue(getLongValue(estadisticas.get("clientesInstitucionales")));
        dataRow.createCell(3).setCellValue(getLongValue(estadisticas.get("clientesMayoristas")));
        
        return rowNum;
    }

    private int agregarEstadisticasProductosExcel(Sheet sheet, int rowNum, Map<String, Object> estadisticas, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Total Productos", "Activos", "Inactivos"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        Row dataRow = sheet.createRow(rowNum++);
        dataRow.createCell(0).setCellValue(getLongValue(estadisticas.get("totalProductos")));
        dataRow.createCell(1).setCellValue(getLongValue(estadisticas.get("productosActivos")));
        dataRow.createCell(2).setCellValue(getLongValue(estadisticas.get("productosInactivos")));
        
        return rowNum;
    }

    private void agregarTablaVentasExcel(Sheet sheet, int rowNum, List<Factura> facturas, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        CellStyle monedaStyle = crearEstiloMoneda(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Factura", "Cliente", "Fecha", "Subtotal", "IGV", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (Factura factura : facturas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "-");
            row.createCell(1).setCellValue(factura.getCliente() != null ? factura.getCliente().getNombre() : "-");
            row.createCell(2).setCellValue(factura.getCreateDate() != null ? DATE_FORMAT.format(factura.getCreateDate()) : "-");
            
            Cell subtotalCell = row.createCell(3);
            subtotalCell.setCellValue(factura.getSubtotal() != null ? factura.getSubtotal().doubleValue() : 0);
            subtotalCell.setCellStyle(monedaStyle);
            
            Cell igvCell = row.createCell(4);
            igvCell.setCellValue(factura.getIgv() != null ? factura.getIgv().doubleValue() : 0);
            igvCell.setCellStyle(monedaStyle);
            
            Cell totalCell = row.createCell(5);
            totalCell.setCellValue(factura.getTotal() != null ? factura.getTotal().doubleValue() : 0);
            totalCell.setCellStyle(monedaStyle);
        }
    }

    private void agregarTablaClientesExcel(Sheet sheet, int rowNum, List<Cliente> clientes, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Nombre", "Tipo", "Fecha Registro"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (Cliente cliente : clientes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(cliente.getIdCliente());
            row.createCell(1).setCellValue(cliente.getNombre() != null ? cliente.getNombre() : "-");
            row.createCell(2).setCellValue(cliente.getTipoCliente() != null ? cliente.getTipoCliente().getDisplayName() : "-");
            row.createCell(3).setCellValue(cliente.getFechaRegistro() != null ? DATE_FORMAT.format(cliente.getFechaRegistro()) : "-");
        }
    }

    private void agregarTablaProductosExcel(Sheet sheet, int rowNum, List<Producto> productos, Workbook workbook) {
        CellStyle headerStyle = crearEstiloHeader(workbook);
        CellStyle monedaStyle = crearEstiloMoneda(workbook);
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Código", "Descripción", "P. Institucional", "P. Mayorista", "Estado"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (Producto producto : productos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(producto.getIdProducto());
            row.createCell(1).setCellValue(producto.getCodigo() != null ? producto.getCodigo() : "-");
            row.createCell(2).setCellValue(producto.getDescripcion() != null ? producto.getDescripcion() : "-");
            
            Cell precioInstCell = row.createCell(3);
            precioInstCell.setCellValue(producto.getPrecioInstitucional() != null ? producto.getPrecioInstitucional().doubleValue() : 0);
            precioInstCell.setCellStyle(monedaStyle);
            
            Cell precioMayCell = row.createCell(4);
            precioMayCell.setCellValue(producto.getPrecioMayorista() != null ? producto.getPrecioMayorista().doubleValue() : 0);
            precioMayCell.setCellStyle(monedaStyle);
            
            row.createCell(5).setCellValue(Boolean.TRUE.equals(producto.getActive()) ? "Activo" : "Inactivo");
        }
    }

    private long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return 0L;
    }

    // ============================================================================
    // EXPORTACIÓN A CSV
    // ============================================================================

    @Override
    public ByteArrayOutputStream exportarVentasCSV(List<Factura> facturas) {
        log.info("Exportando reporte de ventas a CSV - {} facturas", facturas.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            StringBuilder csv = new StringBuilder();
            
            // Headers
            csv.append("Factura,Cliente,Fecha,Subtotal,IGV,Total\n");
            
            // Datos
            for (Factura factura : facturas) {
                csv.append(escapeCSV(factura.getNumeroFactura())).append(",");
                csv.append(escapeCSV(factura.getCliente() != null ? factura.getCliente().getNombre() : "")).append(",");
                csv.append(factura.getCreateDate() != null ? DATE_FORMAT.format(factura.getCreateDate()) : "").append(",");
                csv.append(factura.getSubtotal() != null ? factura.getSubtotal().toString() : "0").append(",");
                csv.append(factura.getIgv() != null ? factura.getIgv().toString() : "0").append(",");
                csv.append(factura.getTotal() != null ? factura.getTotal().toString() : "0").append("\n");
            }
            
            baos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            log.info("Reporte de ventas CSV generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar CSV de ventas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar CSV: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarClientesCSV(List<Cliente> clientes) {
        log.info("Exportando reporte de clientes a CSV - {} clientes", clientes.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            StringBuilder csv = new StringBuilder();
            
            csv.append("ID,Nombre,Tipo,Fecha Registro\n");
            
            for (Cliente cliente : clientes) {
                csv.append(cliente.getIdCliente()).append(",");
                csv.append(escapeCSV(cliente.getNombre())).append(",");
                csv.append(cliente.getTipoCliente() != null ? cliente.getTipoCliente().getDisplayName() : "").append(",");
                csv.append(cliente.getFechaRegistro() != null ? DATE_FORMAT.format(cliente.getFechaRegistro()) : "").append("\n");
            }
            
            baos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            log.info("Reporte de clientes CSV generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar CSV de clientes: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar CSV: " + e.getMessage());
        }
        
        return baos;
    }

    @Override
    public ByteArrayOutputStream exportarProductosCSV(List<Producto> productos) {
        log.info("Exportando reporte de productos a CSV - {} productos", productos.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            StringBuilder csv = new StringBuilder();
            
            csv.append("ID,Código,Descripción,Precio Institucional,Precio Mayorista,Estado\n");
            
            for (Producto producto : productos) {
                csv.append(producto.getIdProducto()).append(",");
                csv.append(escapeCSV(producto.getCodigo())).append(",");
                csv.append(escapeCSV(producto.getDescripcion())).append(",");
                csv.append(producto.getPrecioInstitucional() != null ? producto.getPrecioInstitucional().toString() : "0").append(",");
                csv.append(producto.getPrecioMayorista() != null ? producto.getPrecioMayorista().toString() : "0").append(",");
                csv.append(Boolean.TRUE.equals(producto.getActive()) ? "Activo" : "Inactivo").append("\n");
            }
            
            baos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            log.info("Reporte de productos CSV generado exitosamente");
            
        } catch (IOException e) {
            log.error("Error al generar CSV de productos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar CSV: " + e.getMessage());
        }
        
        return baos;
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
