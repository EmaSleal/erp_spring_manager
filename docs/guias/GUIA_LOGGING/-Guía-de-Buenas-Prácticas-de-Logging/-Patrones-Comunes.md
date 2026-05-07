## 📝 Patrones Comunes

### **1. Inicio y fin de operaciones importantes**

```java
@GetMapping("/reporte/ventas")
public String reporteVentas(...) {
    log.info("=== Generando reporte de ventas ===");
    log.info("Filtros - Inicio: {}, Fin: {}, ClienteId: {}", fechaInicio, fechaFin, clienteId);
    
    try {
        // ... lógica ...
        
        log.info("✅ Reporte generado - {} facturas encontradas", facturas.size());
        return "reportes/ventas";
    } catch (Exception e) {
        log.error("Error al generar reporte de ventas: {}", e.getMessage(), e);
        return "error/error";
    }
}
```

### **2. Operaciones CRUD**

```java
// CREATE
log.info("Creando nuevo cliente: {}", cliente.getNombre());
Cliente guardado = clienteService.save(cliente);
log.info("✅ Cliente creado exitosamente: ID {}", guardado.getIdCliente());

// UPDATE
log.info("Actualizando cliente ID: {} - Cambios: nombre, email", id);
clienteService.update(cliente);
log.info("✅ Cliente actualizado exitosamente");

// DELETE
log.info("Eliminando cliente ID: {}", id);
clienteService.delete(id);
log.info("✅ Cliente eliminado");

// READ
log.debug("Consultando cliente ID: {}", id);
Optional<Cliente> cliente = clienteService.findById(id);
log.debug("Cliente encontrado: {}", cliente.isPresent());
```

### **3. Validaciones y errores de negocio**

```java
if (!validarDatos(cliente)) {
    log.warn("Validación fallida - Cliente con datos incompletos: {}", cliente.getNombre());
    return ResponseUtil.error("Datos incompletos");
}

if (facturaRepository.existeNumero(numero)) {
    log.warn("Número de factura duplicado: {}", numero);
    throw new IllegalArgumentException("Número de factura ya existe");
}
```

### **4. Paginación y búsquedas**

```java
log.info("Listando clientes - Página: {}, Tamaño: {}, Orden: {} {}", 
         page, size, sortBy, sortDir);

Page<Cliente> resultado = clienteService.findAll(pageable);

log.info("Clientes cargados: {} de {} total", 
         resultado.getContent().size(), resultado.getTotalElements());
```

### **5. Exportaciones y generación de archivos**

```java
log.info("=== Exportando reporte a PDF ===");
log.info("Tipo: {}, Registros: {}", tipo, datos.size());

byte[] pdfBytes = exportService.generarPDF(datos);

log.info("✅ PDF generado exitosamente - {} bytes", pdfBytes.length);
```

---

