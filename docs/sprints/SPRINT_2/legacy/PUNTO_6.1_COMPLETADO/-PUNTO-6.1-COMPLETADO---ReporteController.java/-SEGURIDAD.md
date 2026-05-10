## 🔐 SEGURIDAD

### Restricción de Acceso

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
```

**Roles permitidos:**
- ✅ **ADMIN**: Acceso completo a todos los reportes
- ✅ **USER**: Acceso completo a todos los reportes
- ❌ **VENDEDOR**: Sin acceso (no necesita reportes avanzados)
- ❌ **VISUALIZADOR**: Sin acceso (solo consulta datos básicos)

**Comportamiento:**
- Si un usuario con rol VENDEDOR o VISUALIZADOR intenta acceder → **403 Forbidden**
- Redirige a página de error `/error/403.html`

---

