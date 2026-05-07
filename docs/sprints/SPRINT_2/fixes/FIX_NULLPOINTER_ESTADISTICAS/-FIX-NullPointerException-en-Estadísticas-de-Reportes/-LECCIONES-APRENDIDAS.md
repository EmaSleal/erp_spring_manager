## 💡 LECCIONES APRENDIDAS

### Problema de Auto-unboxing
**❌ Incorrecto:**
```java
.filter(Factura::getEntregado)  // Falla si getEntregado() retorna null
```

**✅ Correcto:**
```java
.filter(f -> f.getEntregado() != null && f.getEntregado())
```

**Explicación:**
- El method reference `Factura::getEntregado` intenta auto-unboxear `Boolean` a `boolean`
- Si el valor es `null`, lanza `NullPointerException`
- Siempre verificar null explícitamente cuando se trabaja con Wrappers

### Protección en Cadenas de Llamadas
**❌ Incorrecto:**
```java
p.getPresentacion() != null ? p.getPresentacion().getNombre() : "DEFAULT"
// Falla si getPresentacion() retorna objeto con getNombre() == null
```

**✅ Correcto:**
```java
if (p.getPresentacion() != null && p.getPresentacion().getNombre() != null) {
    return p.getPresentacion().getNombre();
}
return "DEFAULT";
```

### Early Return Pattern
**✅ Recomendado:**
```java
.filter(c -> {
    if (c.getCreateDate() == null) return false;  // Early return
    // Resto de la lógica...
})
```

**Ventajas:**
- Código más legible
- Evita niveles de anidación profundos
- Fácil de detectar casos edge

---

