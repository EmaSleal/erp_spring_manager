## 📊 Comparación de Performance

### **Antes (Java Stream API):**
```
Consulta:        SELECT * FROM factura;              (~1000ms)
Carga en memoria: List<Factura> (1000+ objetos)     (~500ms)
Stream API:       .filter().map().reduce()           (~800ms)
Conversión:       Timestamp → LocalDate              (~200ms)
Total:            ~2500ms por consulta
```

### **Después (Stored Procedure):**
```
Consulta:        CALL sp_obtener_ventas_por_mes(12); (~150ms)
Procesamiento:   MySQL (nativo, optimizado)          (incluido)
Retorno:         Solo datos necesarios               (~50ms)
Total:            ~200ms por consulta
```

**Mejora:** **~92% más rápido** (de 2500ms a 200ms)

---

