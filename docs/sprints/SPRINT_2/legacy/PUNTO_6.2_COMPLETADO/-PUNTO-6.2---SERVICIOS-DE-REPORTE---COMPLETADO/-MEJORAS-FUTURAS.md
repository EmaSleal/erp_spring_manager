## 🔮 MEJORAS FUTURAS

### 1. Optimización de Consultas

**Actual:** Obtiene todos los registros y filtra en Java
```java
List<Factura> facturas = facturaRepository.findAll();
// Filtrado en memoria con Stream API
```

**Futuro:** Consultas específicas en base de datos
```java
// Agregar al FacturaRepository:
List<Factura> findByCreateDateBetween(Timestamp inicio, Timestamp fin);
List<Factura> findByCreateDateBetweenAndClienteIdCliente(Timestamp inicio, Timestamp fin, Integer clienteId);

// Mejor rendimiento con JPQL/Native SQL
@Query("SELECT f FROM Factura f WHERE f.createDate BETWEEN :inicio AND :fin")
List<Factura> buscarPorRangoFechas(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);
```

---

### 2. Caché de Estadísticas

**Implementar caché para reportes frecuentes:**
```java
@Cacheable(value = "estadisticasVentas", key = "#fechaInicio + '-' + #fechaFin")
public Map<String, Object> calcularEstadisticasVentas(LocalDate fechaInicio, LocalDate fechaFin) {
    // ...
}
```

**Configuración de Spring Cache:**
```java
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("estadisticasVentas", "estadisticasClientes");
    }
}
```

---

### 3. Paginación de Resultados

**Para reportes con muchos registros:**
```java
public Page<Factura> generarReporteVentasPaginado(
    LocalDate fechaInicio, 
    LocalDate fechaFin, 
    Integer clienteId,
    Pageable pageable
) {
    // Retornar datos paginados
}
```

---

### 4. Procesamiento Asíncrono

**Para reportes pesados:**
```java
@Async
public CompletableFuture<List<Factura>> generarReporteVentasAsync(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Integer clienteId
) {
    List<Factura> resultado = generarReporteVentas(fechaInicio, fechaFin, clienteId);
    return CompletableFuture.completedFuture(resultado);
}
```

---

### 5. Implementar LineaFactura

**Análisis detallado de productos:**
```java
// Una vez se tenga el modelo LineaFactura
public List<Map<String, Object>> obtenerProductosMasVendidos(int limite) {
    return lineaFacturaRepository.findAll().stream()
        .collect(Collectors.groupingBy(
            LineaFactura::getProducto,
            Collectors.summingInt(LineaFactura::getCantidad)
        ))
        .entrySet().stream()
        .map(entry -> Map.of(
            "producto", entry.getKey(),
            "cantidadVendida", entry.getValue()
        ))
        .sorted(/* por cantidad desc */)
        .limit(limite)
        .collect(Collectors.toList());
}
```

---

