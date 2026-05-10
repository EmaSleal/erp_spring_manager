## 📝 Implementación Detallada

### **Exportación a PDF (iText 7)**

**Características:**
- ✅ Encabezado con información de empresa (nombre, RUC)
- ✅ Título del reporte
- ✅ Fecha de generación
- ✅ Tabla de estadísticas destacada
- ✅ Tabla de datos con formato profesional
- ✅ Colores corporativos (gris oscuro para headers)
- ✅ Texto centrado y formateado
- ✅ Formato de moneda (S/ #,##0.00)

**Ejemplo de código - Ventas PDF:**
```java
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
```

**Método auxiliar para encabezado:**
```java
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
```

**Creación de celdas con estilo:**
```java
private com.itextpdf.layout.element.Cell crearCeldaHeader(String texto, DeviceRgb color) {
    return new com.itextpdf.layout.element.Cell()
            .add(new Paragraph(texto).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(color)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
}
```

---

### **Exportación a Excel (Apache POI)**

**Características:**
- ✅ Archivo XLSX (Excel 2007+)
- ✅ Hoja con nombre del reporte
- ✅ Título centrado y en negrita (16pt)
- ✅ Sección de estadísticas con headers formateados
- ✅ Tabla de datos con headers grises y texto blanco
- ✅ Formato de moneda en columnas numéricas
- ✅ Auto-ajuste de columnas
- ✅ Bordes en todas las celdas

**Ejemplo de código - Clientes Excel:**
```java
@Override
public ByteArrayOutputStream exportarClientesExcel(List<Cliente> clientes, Map<String, Object> estadisticas) {
    log.info("Exportando reporte de clientes a Excel - {} clientes", clientes.size());
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Reporte de Clientes");
        
        int rowNum = 0;
        
        Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
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
```

**Estilos de celdas:**
```java
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
```

---

### **Exportación a CSV**

**Características:**
- ✅ Formato de texto plano
- ✅ Separador: coma (,)
- ✅ Codificación: UTF-8
- ✅ Compatible con Excel, Google Sheets, LibreOffice
- ✅ Escape de comillas y saltos de línea
- ✅ Headers en primera línea

**Ejemplo de código - Productos CSV:**
```java
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
```

---

