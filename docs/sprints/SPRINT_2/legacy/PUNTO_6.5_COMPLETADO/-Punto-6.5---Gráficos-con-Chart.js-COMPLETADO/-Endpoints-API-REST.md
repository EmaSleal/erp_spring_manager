## 🌐 Endpoints API REST

### **1. API Ventas por Mes**

**Endpoint:** `GET /reportes/api/ventas-por-mes`

**Parámetros:**
- `meses` (opcional, default: 12) - Número de meses a consultar

**Respuesta JSON:**
```json
{
  "labels": ["Nov 2024", "Dic 2024", "Ene 2025", ...],
  "data": [1500.00, 2300.50, 1800.75, ...]
}
```

**Implementación:**
```java
@GetMapping("/api/ventas-por-mes")
@ResponseBody
public Map<String, Object> getVentasPorMes(
        @RequestParam(required = false, defaultValue = "12") Integer meses) {
    
    Map<String, java.math.BigDecimal> ventasPorMes = reporteService.obtenerVentasPorMes(meses);
    
    // Convertir a formato esperado por Chart.js
    Map<String, Object> resultado = new HashMap<>();
    resultado.put("labels", new java.util.ArrayList<>(ventasPorMes.keySet()));
    resultado.put("data", new java.util.ArrayList<>(ventasPorMes.values()));
    
    return resultado;
}
```

---

### **2. API Clientes Nuevos por Mes**

**Endpoint:** `GET /reportes/api/clientes-nuevos`

**Parámetros:**
- `meses` (opcional, default: 12) - Número de meses a consultar

**Respuesta JSON:**
```json
{
  "labels": ["Nov 2024", "Dic 2024", "Ene 2025", ...],
  "data": [5, 8, 12, 7, ...]
}
```

**Implementación:**
```java
@GetMapping("/api/clientes-nuevos")
@ResponseBody
public Map<String, Object> getClientesNuevos(
        @RequestParam(required = false, defaultValue = "12") Integer meses) {
    
    List<Cliente> clientes = clienteService.findAll();
    
    Map<String, Long> clientesNuevosPorMes = new java.util.LinkedHashMap<>();
    java.time.LocalDate hoy = java.time.LocalDate.now();
    
    for (int i = meses - 1; i >= 0; i--) {
        java.time.LocalDate inicioMes = hoy.minusMonths(i).withDayOfMonth(1);
        java.time.LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
        
        String nombreMes = inicioMes.format(
            java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", 
                java.util.Locale.forLanguageTag("es-ES")));
        
        long cantidad = clientes.stream()
                .filter(c -> c.getCreateDate() != null)
                .filter(c -> {
                    java.time.LocalDate fechaCreacion = 
                        new java.sql.Timestamp(c.getCreateDate().getTime())
                            .toLocalDateTime().toLocalDate();
                    return !fechaCreacion.isBefore(inicioMes) && 
                           !fechaCreacion.isAfter(finMes);
                })
                .count();
        
        clientesNuevosPorMes.put(nombreMes, cantidad);
    }
    
    Map<String, Object> resultado = new HashMap<>();
    resultado.put("labels", new java.util.ArrayList<>(clientesNuevosPorMes.keySet()));
    resultado.put("data", new java.util.ArrayList<>(clientesNuevosPorMes.values()));
    
    return resultado;
}
```

---

### **3. API Productos Más Vendidos**

**Endpoint:** `GET /reportes/api/productos-mas-vendidos`

**Parámetros:**
- `limite` (opcional, default: 10) - Número de productos a mostrar

**Respuesta JSON:**
```json
{
  "labels": ["Producto A", "Producto B", "Producto C", ...],
  "data": [150, 120, 95, 80, ...]
}
```

**Implementación:**
```java
@GetMapping("/api/productos-mas-vendidos")
@ResponseBody
public Map<String, Object> getProductosMasVendidos(
        @RequestParam(required = false, defaultValue = "10") Integer limite) {
    
    List<Map<String, Object>> productosMasVendidos = 
        reporteService.obtenerProductosMasVendidos(limite);
    
    // Convertir a formato esperado por Chart.js
    List<String> labels = new java.util.ArrayList<>();
    List<Object> data = new java.util.ArrayList<>();
    
    for (Map<String, Object> item : productosMasVendidos) {
        labels.add((String) item.get("producto"));
        data.add(item.get("cantidad"));
    }
    
    Map<String, Object> resultado = new HashMap<>();
    resultado.put("labels", labels);
    resultado.put("data", data);
    
    return resultado;
}
```

---

