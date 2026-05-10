## 🔧 FIX #2: NullPointerException en Estadísticas

### Problema
```
java.lang.NullPointerException: null
at ReporteServiceImpl.calcularEstadisticasVentas(ReporteServiceImpl.java:141)
```

### Causa
- Método `Factura.getEntregado()` puede retornar `null`
- Uso de method reference `Factura::getEntregado` intenta auto-unboxear `Boolean` null → `boolean`
- Falta de protección contra nulls en otros métodos de estadísticas

### Solución
1. ✅ **calcularEstadisticasVentas** (CRÍTICO)
   ```java
   // ANTES
   .filter(Factura::getEntregado)
   
   // DESPUÉS
   .filter(f -> f.getEntregado() != null && f.getEntregado())
   ```

2. ✅ **calcularEstadisticasClientes** (PREVENTIVO)
   ```java
   if (c.getCreateDate() == null) return false;
   ```

3. ✅ **calcularEstadisticasProductos** (PREVENTIVO)
   ```java
   if (p.getPresentacion() != null && p.getPresentacion().getNombre() != null) {
       return p.getPresentacion().getNombre();
   }
   ```

### Archivos Modificados
- services/impl/ReporteServiceImpl.java (3 métodos, ~15 líneas)

### Impacto
- **Severidad:** Crítica (impedía usar reporte de ventas)
- **Usuarios afectados:** ADMIN, USER
- **Tiempo de fix:** 10 minutos

### Documentación
📄 `docs/sprints/SPRINT_2/fixes/FIX_NULLPOINTER_ESTADISTICAS.md`

---

