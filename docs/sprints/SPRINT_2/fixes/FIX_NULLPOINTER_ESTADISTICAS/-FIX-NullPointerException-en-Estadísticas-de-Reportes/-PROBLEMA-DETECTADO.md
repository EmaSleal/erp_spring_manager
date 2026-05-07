## 🐛 PROBLEMA DETECTADO

### Error Principal: NullPointerException en calcularEstadisticasVentas

**Stacktrace:**
```
java.lang.NullPointerException: null
    at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:178)
    at api.astro.whats_orders_manager.services.impl.ReporteServiceImpl.calcularEstadisticasVentas(ReporteServiceImpl.java:141)
    at api.astro.whats_orders_manager.controllers.ReporteController.reporteVentas(ReporteController.java:132)
```

**Línea problemática (141):**
```java
long facturasEntregadas = facturas.stream()
    .filter(Factura::getEntregado)  // ❌ getEntregado() puede ser null
    .count();
```

**Causa raíz:**
El método `Factura.getEntregado()` puede retornar `null`, y al usar el reference method `Factura::getEntregado` directamente en el filter, se produce un `NullPointerException` cuando intenta auto-unboxear un `Boolean` null a `boolean`.

**Contexto:**
- El usuario estaba probando el reporte de ventas (/reportes/ventas)
- El servicio encontró 6 facturas exitosamente
- El error ocurrió al calcular las estadísticas
- Aparentemente, al menos una de las 6 facturas tiene `entregado = null` en la base de datos

---

