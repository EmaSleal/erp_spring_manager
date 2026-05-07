## 📊 FLUJO DE ACCESO DENEGADO

```
Usuario intenta acceder a /configuracion
         ↓
Spring Security valida permisos
         ↓
Usuario tiene rol USER (no ADMIN)
         ↓
AccessDeniedException lanzada
         ↓
ExceptionHandler de Spring Security
         ↓
Redirige a /error/403
         ↓
CustomErrorController.error403()
         ↓
Renderiza error/403.html
         ↓
Usuario ve página personalizada con:
  - Mensaje de acceso denegado
  - Su rol actual
  - Información de roles
  - Botones de acción
```

---

