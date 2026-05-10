## 📋 Resumen Ejecutivo

**Problema:** La vista de usuarios (`/usuarios`) generaba un error al intentar formatear el campo `ultimoAcceso` con Thymeleaf.

**Causa Raíz:** Thymeleaf `#temporals.format()` no puede formatear directamente un `java.sql.Timestamp`. Solo acepta tipos `java.time.*` como `LocalDateTime`, `LocalDate`, etc.

**Solución:** Convertir el `Timestamp` a `LocalDateTime` antes de formatear usando `.toLocalDateTime()`.

**Impacto:** 🟡 MEDIA - Vista de usuarios no se cargaba, pero no afecta funcionalidad del sistema.

**Tiempo de Detección:** Inmediato (error al cargar vista)

**Tiempo de Resolución:** 5 minutos

---

