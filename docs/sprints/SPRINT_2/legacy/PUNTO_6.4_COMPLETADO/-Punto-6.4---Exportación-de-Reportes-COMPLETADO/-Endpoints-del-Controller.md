## 🌐 Endpoints del Controller

### **ReporteController.java** (9 nuevos endpoints - 190 líneas agregadas)

**Endpoints PDF:**
```java
@GetMapping("/ventas/exportar/pdf")
public ResponseEntity<byte[]> exportarVentasPDF(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
        @RequestParam(required = false) Integer clienteId
) {
    List<Factura> facturas = reporteService.generarReporteVentas(fechaInicio, fechaFin, clienteId);
    Map<String, Object> estadisticas = reporteService.calcularEstadisticasVentas(facturas);
    
    byte[] pdfBytes = exportService.exportarVentasPDF(facturas, estadisticas).toByteArray();
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "reporte-ventas.pdf");
    
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
}
```

**Endpoints Excel:**
```java
@GetMapping("/clientes/exportar/excel")
public ResponseEntity<byte[]> exportarClientesExcel(
        @RequestParam(required = false) Boolean activo,
        @RequestParam(required = false) Boolean conDeuda
) {
    List<Cliente> clientes = reporteService.generarReporteClientes(activo, conDeuda);
    Map<String, Object> estadisticas = reporteService.calcularEstadisticasClientes(clientes);
    
    byte[] excelBytes = exportService.exportarClientesExcel(clientes, estadisticas).toByteArray();
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("attachment", "reporte-clientes.xlsx");
    
    return ResponseEntity.ok().headers(headers).body(excelBytes);
}
```

**Endpoints CSV:**
```java
@GetMapping("/productos/exportar/csv")
public ResponseEntity<byte[]> exportarProductosCSV(
        @RequestParam(required = false) Boolean stockBajo,
        @RequestParam(required = false) Boolean sinVentas
) {
    List<Producto> productos = reporteService.generarReporteProductos(stockBajo, sinVentas);
    byte[] csvBytes = exportService.exportarProductosCSV(productos).toByteArray();
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDispositionFormData("attachment", "reporte-productos.csv");
    
    return ResponseEntity.ok().headers(headers).body(csvBytes);
}
```

---

