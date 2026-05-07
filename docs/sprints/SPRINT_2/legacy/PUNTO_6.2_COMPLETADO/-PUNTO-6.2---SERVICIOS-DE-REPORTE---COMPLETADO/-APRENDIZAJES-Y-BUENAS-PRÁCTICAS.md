## 🎓 APRENDIZAJES Y BUENAS PRÁCTICAS

### 1. Separación de Responsabilidades

**Controlador → Servicio → Repositorio**
```
ReporteController (Web)
    ↓
ReporteService (Lógica de negocio)
    ↓
Repository (Acceso a datos)
```

- ✅ Controlador: Recibe peticiones HTTP, valida entrada, retorna vistas
- ✅ Servicio: Procesa datos, aplica filtros, calcula estadísticas
- ✅ Repositorio: Acceso directo a base de datos

---

### 2. Reutilización de Código

**Separación de generación y cálculo:**
```java
// Generar datos filtrados
List<Factura> facturas = generarReporteVentas(fechaInicio, fechaFin, clienteId);

// Calcular estadísticas sobre esos datos
Map<String, Object> estadisticas = calcularEstadisticasVentas(facturas);
```

**Ventajas:**
- ✅ Métodos pequeños y enfocados (Single Responsibility)
- ✅ Fácil de testear unitariamente
- ✅ Reutilizable desde diferentes puntos
- ✅ Estadísticas se pueden calcular sobre cualquier lista de facturas

---

### 3. Programación Declarativa vs Imperativa

**Imperativa (antes):**
```java
List<Factura> resultado = new ArrayList<>();
for (Factura factura : facturas) {
    if (factura.getTotal() != null && factura.getTotal().compareTo(BigDecimal.ZERO) > 0) {
        resultado.add(factura);
    }
}
```

**Declarativa (ahora):**
```java
List<Factura> resultado = facturas.stream()
    .filter(f -> f.getTotal() != null)
    .filter(f -> f.getTotal().compareTo(BigDecimal.ZERO) > 0)
    .collect(Collectors.toList());
```

**Ventajas:**
- ✅ Más legible
- ✅ Menos propensa a errores
- ✅ Facilita paralelización (parallel streams)
- ✅ Expresiva y concisa

---

### 4. Inmutabilidad y Seguridad

**Uso de LocalDate (inmutable) en lugar de Date (mutable):**
```java
LocalDate fechaInicio = LocalDate.now().minusMonths(3);  // Inmutable
LocalDate inicioMes = LocalDate.now().withDayOfMonth(1); // Nueva instancia
```

**BigDecimal para precisión:**
```java
BigDecimal total = BigDecimal.ZERO;  // Valor exacto, no aproximación
total = total.add(precio);           // Crea nueva instancia
```

---

### 5. Documentación y Mantenibilidad

**JavaDoc completo:**
```java
/**
 * Genera un reporte de ventas filtrado por fechas y cliente.
 * 
 * @param fechaInicio Fecha inicial del período (opcional, null = sin límite)
 * @param fechaFin Fecha final del período (opcional, null = sin límite)
 * @param clienteId ID del cliente para filtrar (opcional, null = todos)
 * @return Lista de facturas que cumplen los criterios ordenadas por fecha desc
 */
public List<Factura> generarReporteVentas(LocalDate fechaInicio, LocalDate fechaFin, Integer clienteId) {
    // ...
}
```

**Comentarios de sección:**
```java
// ========================================================================
// REPORTES DE VENTAS
// ========================================================================
```

---

