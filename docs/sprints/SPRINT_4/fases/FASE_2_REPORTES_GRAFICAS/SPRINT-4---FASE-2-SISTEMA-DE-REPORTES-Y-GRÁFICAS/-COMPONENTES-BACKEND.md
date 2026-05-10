## ⚙️ COMPONENTES BACKEND

### 1. ReporteController

**Ubicación:** `src/main/java/api/whats_orders_manager/controller/admin/ReporteController.java`

**Endpoints:**

| Método | Ruta                                  | Descripción                      | Permiso           |
|--------|--------------------------------------|----------------------------------|-------------------|
| GET    | `/admin/reportes/dashboard`         | Renderiza dashboard de reportes  | `REPORTES_VER`    |
| POST   | `/admin/reportes/datos`             | Obtiene datos para gráficas (AJAX) | `REPORTES_VER`  |
| GET    | `/admin/reportes/exportar/pdf`      | Exporta reporte a PDF           | `REPORTES_EXPORTAR` |
| GET    | `/admin/reportes/exportar/excel`    | Exporta reporte a Excel         | `REPORTES_EXPORTAR` |

**Código clave:**

```java
@Controller
@RequestMapping("/admin/reportes")
@PreAuthorize("hasAuthority('REPORTES_VER')")
public class ReporteController {

    private final ReporteService reporteService;
    private final ExportacionService exportacionService;

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        // Cargar datos iniciales (último mes)
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        
        ReporteFiltrosDTO filtros = new ReporteFiltrosDTO();
        filtros.setFechaInicio(inicioMes);
        filtros.setFechaFin(hoy);
        
        model.addAttribute("filtros", filtros);
        model.addAttribute("categorias", categoriaService.listarActivas());
        
        return "admin/reportes/dashboard";
    }

    @PostMapping("/datos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatos(
            @Valid @RequestBody ReporteFiltrosDTO filtros) {
        
        try {
            Map<String, Object> datos = new HashMap<>();
            
            // Obtener todos los reportes
            datos.put("ventasPorMes", reporteService.obtenerVentasPorMes(filtros));
            datos.put("productosTop", reporteService.obtenerProductosMasVendidos(filtros));
            datos.put("estadisticasClientes", reporteService.obtenerEstadisticasClientes(filtros));
            datos.put("comparativaAnual", reporteService.obtenerComparativaAnual(filtros));
            datos.put("distribucionCategorias", reporteService.obtenerDistribucionCategorias(filtros));
            
            return ResponseEntity.ok(datos);
            
        } catch (Exception e) {
            log.error("Error al obtener datos de reportes", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarPDF(
            @Valid ReporteFiltrosDTO filtros,
            @RequestParam String tipoReporte) {
        
        try {
            byte[] pdf = exportacionService.exportarPDF(filtros, tipoReporte);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename("reporte_" + tipoReporte + "_" + LocalDate.now() + ".pdf")
                    .build()
            );
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
                
        } catch (Exception e) {
            log.error("Error al exportar PDF", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    public ResponseEntity<byte[]> exportarExcel(
            @Valid ReporteFiltrosDTO filtros,
            @RequestParam String tipoReporte) {
        
        try {
            byte[] excel = exportacionService.exportarExcel(filtros, tipoReporte);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename("reporte_" + tipoReporte + "_" + LocalDate.now() + ".xlsx")
                    .build()
            );
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excel);
                
        } catch (Exception e) {
            log.error("Error al exportar Excel", e);
            return ResponseEntity.status(500).build();
        }
    }
}
```

---

### 2. ReporteService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/ReporteService.java`

**Métodos principales:**

```java
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final EntityManager entityManager;
    
    // 1. Ventas por mes
    public List<VentasPorMesDTO> obtenerVentasPorMes(ReporteFiltrosDTO filtros) {
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_reporte_ventas_mes")
            .registerStoredProcedureParameter("p_fecha_inicio", Date.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_fecha_fin", Date.class, ParameterMode.IN)
            .setParameter("p_fecha_inicio", Date.valueOf(filtros.getFechaInicio()))
            .setParameter("p_fecha_fin", Date.valueOf(filtros.getFechaFin()));
        
        query.execute();
        
        List<Object[]> resultados = query.getResultList();
        return resultados.stream()
            .map(row -> new VentasPorMesDTO(
                (String) row[0],           // mes
                (BigDecimal) row[1],       // totalVentas
                ((Number) row[2]).longValue(), // cantidadFacturas
                (BigDecimal) row[3]        // ticketPromedio
            ))
            .collect(Collectors.toList());
    }
    
    // 2. Productos más vendidos
    public List<ProductoVendidoDTO> obtenerProductosMasVendidos(ReporteFiltrosDTO filtros) {
        Integer limite = filtros.getLimite() != null ? filtros.getLimite() : 10;
        
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_productos_mas_vendidos")
            .registerStoredProcedureParameter("p_limite", Integer.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_categoria_id", Long.class, ParameterMode.IN)
            .setParameter("p_limite", limite)
            .setParameter("p_categoria_id", filtros.getCategoriaId());
        
        query.execute();
        
        List<Object[]> resultados = query.getResultList();
        return resultados.stream()
            .map(row -> {
                ProductoVendidoDTO dto = new ProductoVendidoDTO();
                dto.setProductoId(((Number) row[0]).longValue());
                dto.setNombreProducto((String) row[1]);
                dto.setCategoria((String) row[2]);
                dto.setCantidadVendida(((Number) row[3]).longValue());
                dto.setTotalVentas((BigDecimal) row[4]);
                dto.setPrecioPromedio((BigDecimal) row[5]);
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    // 3. Estadísticas de clientes
    public List<EstadisticasClienteDTO> obtenerEstadisticasClientes(ReporteFiltrosDTO filtros) {
        String sql = """
            SELECT 
                c.id,
                c.nombre,
                c.email,
                COUNT(f.id) as total_compras,
                COALESCE(SUM(f.total), 0) as total_gastado,
                COALESCE(AVG(f.total), 0) as ticket_promedio,
                MAX(f.fecha_emision) as ultima_compra,
                CASE 
                    WHEN SUM(f.total) > 5000 THEN 'VIP'
                    WHEN COUNT(f.id) >= 10 THEN 'Frecuente'
                    WHEN COUNT(f.id) >= 3 THEN 'Ocasional'
                    ELSE 'Nuevo'
                END as categoria
            FROM clientes c
            LEFT JOIN facturas f ON c.id = f.cliente_id 
                AND f.fecha_emision BETWEEN :fechaInicio AND :fechaFin
            GROUP BY c.id, c.nombre, c.email
            ORDER BY total_gastado DESC
            """;
        
        return entityManager.createNativeQuery(sql)
            .setParameter("fechaInicio", filtros.getFechaInicio())
            .setParameter("fechaFin", filtros.getFechaFin())
            .getResultList()
            .stream()
            .map(row -> {
                Object[] cols = (Object[]) row;
                EstadisticasClienteDTO dto = new EstadisticasClienteDTO();
                dto.setClienteId(((Number) cols[0]).longValue());
                dto.setNombreCliente((String) cols[1]);
                dto.setEmail((String) cols[2]);
                dto.setTotalCompras(((Number) cols[3]).longValue());
                dto.setTotalGastado((BigDecimal) cols[4]);
                dto.setTicketPromedio((BigDecimal) cols[5]);
                dto.setUltimaCompra(cols[6] != null ? ((Date) cols[6]).toLocalDate() : null);
                dto.setCategoria((String) cols[7]);
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    // 4. Comparativa anual
    public List<ComparativaAnualDTO> obtenerComparativaAnual(ReporteFiltrosDTO filtros) {
        int anioActual = LocalDate.now().getYear();
        int anioAnterior = anioActual - 1;
        
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_comparativa_anual")
            .registerStoredProcedureParameter("p_anio_actual", Integer.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_anio_anterior", Integer.class, ParameterMode.IN)
            .setParameter("p_anio_actual", anioActual)
            .setParameter("p_anio_anterior", anioAnterior);
        
        query.execute();
        
        List<Object[]> resultados = query.getResultList();
        return resultados.stream()
            .map(row -> {
                ComparativaAnualDTO dto = new ComparativaAnualDTO();
                dto.setMes((String) row[0]);
                dto.setVentasAnioActual((BigDecimal) row[1]);
                dto.setVentasAnioAnterior((BigDecimal) row[2]);
                
                BigDecimal variacion = (BigDecimal) row[3];
                dto.setVariacionPorcentaje(variacion);
                
                if (variacion.compareTo(BigDecimal.valueOf(5)) > 0) {
                    dto.setTendencia("CRECIMIENTO");
                } else if (variacion.compareTo(BigDecimal.valueOf(-5)) < 0) {
                    dto.setTendencia("DECRECIMIENTO");
                } else {
                    dto.setTendencia("ESTABLE");
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    // 5. Distribución de categorías
    public List<DistribucionCategoriaDTO> obtenerDistribucionCategorias(ReporteFiltrosDTO filtros) {
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_distribucion_categorias")
            .execute();
        
        List<Object[]> resultados = query.getResultList();
        
        // Colores predefinidos para Chart.js
        String[] colores = {
            "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF",
            "#FF9F40", "#FF6384", "#C9CBCF", "#4BC0C0", "#FF6384"
        };
        
        AtomicInteger index = new AtomicInteger(0);
        
        return resultados.stream()
            .map(row -> {
                DistribucionCategoriaDTO dto = new DistribucionCategoriaDTO();
                dto.setCategoria((String) row[0]);
                dto.setCantidadProductos(((Number) row[1]).longValue());
                dto.setTotalVentas((BigDecimal) row[2]);
                dto.setPorcentajeVentas((BigDecimal) row[3]);
                dto.setColor(colores[index.getAndIncrement() % colores.length]);
                return dto;
            })
            .collect(Collectors.toList());
    }
}
```

---

### 3. ExportacionService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/ExportacionService.java`

```java
@Service
public class ExportacionService {

    private final ReporteService reporteService;
    private final EmpresaService empresaService;

    // Exportar a PDF con iText
    public byte[] exportarPDF(ReporteFiltrosDTO filtros, String tipoReporte) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        
        // Header con logo de empresa
        EmpresaDTO empresa = empresaService.obtenerConfiguracion();
        
        Paragraph header = new Paragraph(empresa.getNombre())
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER);
        document.add(header);
        
        Paragraph subtitle = new Paragraph("Reporte: " + tipoReporte)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER);
        document.add(subtitle);
        
        Paragraph fecha = new Paragraph("Generado: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT);
        document.add(fecha);
        
        document.add(new Paragraph("\n"));
        
        // Contenido según tipo de reporte
        switch (tipoReporte) {
            case "ventas":
                agregarTablaVentas(document, filtros);
                break;
            case "productos":
                agregarTablaProductos(document, filtros);
                break;
            case "clientes":
                agregarTablaClientes(document, filtros);
                break;
            // ... otros tipos
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    private void agregarTablaVentas(Document document, ReporteFiltrosDTO filtros) {
        List<VentasPorMesDTO> ventas = reporteService.obtenerVentasPorMes(filtros);
        
        Table table = new Table(4);
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        table.addHeaderCell(new Cell().add(new Paragraph("Mes").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Total Ventas").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Facturas").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Ticket Promedio").setBold()));
        
        // Datos
        for (VentasPorMesDTO venta : ventas) {
            table.addCell(venta.getMes());
            table.addCell(venta.getTotalVentas().toString() + " €");
            table.addCell(venta.getCantidadFacturas().toString());
            table.addCell(venta.getTicketPromedio().toString() + " €");
        }
        
        document.add(table);
    }
    
    // Exportar a Excel con Apache POI
    public byte[] exportarExcel(ReporteFiltrosDTO filtros, String tipoReporte) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte");
        
        // Estilo para headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Contenido según tipo
        switch (tipoReporte) {
            case "ventas":
                crearHojaVentas(sheet, headerStyle, filtros);
                break;
            case "productos":
                crearHojaProductos(sheet, headerStyle, filtros);
                break;
            // ... otros tipos
        }
        
        // Auto-ajustar columnas
        for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        return baos.toByteArray();
    }
    
    private void crearHojaVentas(Sheet sheet, CellStyle headerStyle, ReporteFiltrosDTO filtros) {
        List<VentasPorMesDTO> ventas = reporteService.obtenerVentasPorMes(filtros);
        
        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mes", "Total Ventas", "Cantidad Facturas", "Ticket Promedio"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Datos
        int rowNum = 1;
        for (VentasPorMesDTO venta : ventas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(venta.getMes());
            row.createCell(1).setCellValue(venta.getTotalVentas().doubleValue());
            row.createCell(2).setCellValue(venta.getCantidadFacturas());
            row.createCell(3).setCellValue(venta.getTicketPromedio().doubleValue());
        }
        
        // Totales
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(0).setCellValue("TOTAL");
        
        Cell totalVentasCell = totalRow.createCell(1);
        totalVentasCell.setCellFormula("SUM(B2:B" + rowNum + ")");
        
        Cell totalFacturasCell = totalRow.createCell(2);
        totalFacturasCell.setCellFormula("SUM(C2:C" + rowNum + ")");
    }
}
```

---

