## 📝 LOGGING

Todos los endpoints tienen logging completo:

```java
log.info("=== Acceso al dashboard de reportes ===");
log.info("Dashboard cargado - Clientes: {}, Productos: {}, Facturas: {}, Usuarios: {}", ...);
log.info("=== Generando reporte de ventas ===");
log.info("Filtros - Inicio: {}, Fin: {}, ClienteId: {}", ...);
log.error("Error al generar PDF: {}", e.getMessage(), e);
```

**Niveles de log:**
- `INFO`: Accesos, generación de reportes
- `DEBUG`: Carga de datos de usuario
- `ERROR`: Errores en generación de archivos

---

