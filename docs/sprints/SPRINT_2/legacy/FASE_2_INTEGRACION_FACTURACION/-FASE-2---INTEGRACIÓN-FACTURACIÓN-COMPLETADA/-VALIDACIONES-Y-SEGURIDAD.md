## 🛡️ VALIDACIONES Y SEGURIDAD

### Thread-Safety

- Método `save()` es `@Transactional`
- El incremento del número es atómico
- Si falla el guardado, no se incrementa el número

### Validación de unicidad

```java
@Column(name = "numeroFactura", unique = true)
```
- La BD rechaza números duplicados
- Si hay error de concurrencia, lanza exception

### Manejo de errores

```java
try {
    configuracionFacturacionService.incrementarNumeroFactura();
} catch (Exception e) {
    log.error("Error al incrementar número: {}", e.getMessage());
    // No revierte la transacción de la factura
    // El número puede ajustarse manualmente
}
```

---

