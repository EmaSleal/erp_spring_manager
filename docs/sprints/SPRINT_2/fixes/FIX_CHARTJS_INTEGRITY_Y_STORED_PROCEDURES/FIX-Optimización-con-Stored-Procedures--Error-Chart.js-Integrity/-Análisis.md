## 🔍 Análisis

### **Causa del Error 1:**
El atributo `integrity` del CDN de Chart.js tiene un hash SHA-384 incorrecto o desactualizado que no coincide con el contenido real del archivo descargado. Esto causa que el navegador bloquee el recurso por razones de seguridad (Subresource Integrity - SRI).

### **Causa del Problema 2:**
Los métodos actuales procesaban datos en Java:

**Antes (Ineficiente):**
```java
// En ReporteServiceImpl
public Map<String, BigDecimal> obtenerVentasPorMes(int meses) {
    List<Factura> todasLasFacturas = facturaRepository.findAll(); // ❌ Cargar todo
    
    Map<String, BigDecimal> ventasPorMes = new LinkedHashMap<>();
    LocalDate hoy = LocalDate.now();
    
    for (int i = meses - 1; i >= 0; i--) {
        LocalDate inicioMes = hoy.minusMonths(i).withDayOfMonth(1);
        LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
        
        // ❌ Filtrar en Java (costoso en memoria)
        BigDecimal totalMes = todasLasFacturas.stream()
                .filter(f -> f.getFechaEmision() != null)
                .filter(f -> {
                    LocalDate fecha = convertirTimestampALocalDate(f.getFechaEmision());
                    return !fecha.isBefore(inicioMes) && !fecha.isAfter(finMes);
                })
                .map(Factura::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        ventasPorMes.put(formatearMes(inicioMes), totalMes);
    }
    
    return ventasPorMes;
}
```

**Problemas:**
- ❌ `findAll()` carga TODAS las facturas en memoria
- ❌ Stream API procesa cada factura (N iteraciones)
- ❌ Conversión de Timestamp a LocalDate en cada iteración
- ❌ Filtrado y agregación en Java (lento)
- ❌ Sobrecarga de CPU y memoria del servidor

---

