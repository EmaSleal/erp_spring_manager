## 🎨 Formato de Mensajes

### ✅ **Buenos ejemplos:**

```java
// ✅ Descriptivo con contexto
log.info("Usuario {} creó factura {} para cliente {}", 
         usuario.getNombre(), factura.getId(), cliente.getNombre());

// ✅ Con valores importantes
log.info("Factura {} actualizada - Total: {} → {}", 
         id, totalAnterior, totalNuevo);

// ✅ Con operación y resultado
log.info("✅ Reporte PDF generado exitosamente - {} bytes", pdfBytes.length);

// ✅ Con detalles de error
log.error("Error al exportar Excel: {} - Facturas procesadas: {}/{}", 
          e.getMessage(), procesadas, total, e);
```

### ❌ **Malos ejemplos:**

```java
// ❌ No descriptivo
log.info("Operación completada");

// ❌ Sin contexto
log.error("Error");

// ❌ Demasiado verboso
log.info("El usuario con nombre " + usuario.getNombre() + " y rol " + 
         usuario.getRol() + " ha creado una nueva factura...");

// ❌ Información sensible
log.info("Password del usuario: {}", password); // ⛔ NUNCA
```

---

