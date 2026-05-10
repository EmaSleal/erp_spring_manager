## ⚠️ IMPORTANTE - COMPATIBILIDAD

### Sistema Actual (NO TOCAR TODAVÍA)

Estos archivos **aún se están usando** y NO deben modificarse:

- ❌ **NO eliminar** `Permiso.java` (enum)
- ❌ **NO eliminar** `MatrizPermisos.java`
- ❌ **NO modificar** `PermisoService` todavía

**Razón:** El sistema actual sigue funcionando con el enum. La migración completa requiere actualizar:
- Controllers con @PreAuthorize
- Templates con sec:authorize
- PermisoService para consultar base de datos

### Campo Deprecado

- ⚠️ `usuario.rol` (String) → Se considera **deprecado**
- ✅ `usuario.id_rol` (FK a tabla rol) → **Nuevo campo** a usar

**Transición:** Ambos campos coexistirán hasta completar la migración del código.

---

