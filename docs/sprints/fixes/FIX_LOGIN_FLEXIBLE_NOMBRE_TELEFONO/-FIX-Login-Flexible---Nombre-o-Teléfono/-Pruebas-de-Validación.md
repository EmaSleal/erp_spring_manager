## 🧪 Pruebas de Validación

### Casos de Prueba

| Caso | Input | Resultado Esperado | Estado |
|------|-------|-------------------|--------|
| Login con nombre | "Admin" | ✅ Login exitoso | ✅ PASS |
| Login con teléfono | "9999999999" | ✅ Login exitoso | ✅ PASS |
| Usuario inactivo | (nombre o teléfono) | ❌ "Usuario inactivo" | ✅ PASS |
| Usuario inexistente | "noexiste" | ❌ "Usuario no encontrado" | ✅ PASS |

### Logs de Hibernate (Evidencia de Búsqueda)

```sql
-- Primera búsqueda: por teléfono
select u1_0.* from usuario u1_0 where u1_0.telefono=?

-- Si no encuentra: segunda búsqueda por nombre
select u1_0.* from usuario u1_0 where u1_0.nombre=?
```

**Observación:** El sistema primero intenta por teléfono (más específico), luego por nombre (fallback).

---

