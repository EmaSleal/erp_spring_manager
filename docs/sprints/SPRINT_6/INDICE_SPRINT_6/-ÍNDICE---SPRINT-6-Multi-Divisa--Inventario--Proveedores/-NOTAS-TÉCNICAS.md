## 💡 NOTAS TÉCNICAS

### Integración con Hallazgos

**`formatearMoneda()` existente:**
```java
// Código actual (ejemplo):
String formatearMoneda(BigDecimal monto) {
    return "₡" + NumberFormat.getInstance().format(monto);
}

// Extensión Sprint 6:
String formatearMoneda(BigDecimal monto, Moneda moneda) {
    return moneda.getSimbolo() + " " + NumberFormat.getInstance().format(monto);
}
```

**Enum `PRODUCTO_AJUSTAR_INVENTARIO`:**
```java
// Ubicación: Producto.java o enum separado
// Valores posibles: SI, NO, AUTOMATICO (?)
// Sprint 6: Activar en formularios y validaciones
```

**Filtro `stockBajo`:**
```java
// TODO actual en ReporteController:
// @GetMapping("/stock-bajo")
// public String reporteStockBajo() { ... }

// Sprint 6: Implementar completamente
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO
