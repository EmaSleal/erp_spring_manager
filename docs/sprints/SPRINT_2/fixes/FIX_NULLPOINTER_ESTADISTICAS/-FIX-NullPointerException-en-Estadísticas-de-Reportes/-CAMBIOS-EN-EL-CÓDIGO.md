## 📝 CAMBIOS EN EL CÓDIGO

### Archivo Modificado
```
src/main/java/api/astro/whats_orders_manager/services/impl/ReporteServiceImpl.java
```

### Líneas Modificadas

**Línea ~141-147** (calcularEstadisticasVentas):
```diff
- long facturasEntregadas = facturas.stream()
-     .filter(Factura::getEntregado)
-     .count();
+ long facturasEntregadas = facturas.stream()
+     .filter(f -> f.getEntregado() != null && f.getEntregado())
+     .count();
```

**Línea ~223-231** (calcularEstadisticasClientes):
```diff
  long clientesNuevosEsteMes = clientes.stream()
      .filter(c -> {
+         if (c.getCreateDate() == null) return false;
          LocalDate fechaCreacion = convertirTimestampALocalDate(c.getCreateDate());
          return fechaCreacion != null && !fechaCreacion.isBefore(inicioMes);
      })
      .count();
```

**Línea ~295-301** (calcularEstadisticasProductos):
```diff
  Map<String, Long> productosPorPresentacion = productos.stream()
      .collect(Collectors.groupingBy(
-         p -> p.getPresentacion() != null ? p.getPresentacion().getNombre() : "SIN_PRESENTACION",
+         p -> {
+             if (p.getPresentacion() != null && p.getPresentacion().getNombre() != null) {
+                 return p.getPresentacion().getNombre();
+             }
+             return "SIN_PRESENTACION";
+         },
          Collectors.counting()
      ));
```

---

