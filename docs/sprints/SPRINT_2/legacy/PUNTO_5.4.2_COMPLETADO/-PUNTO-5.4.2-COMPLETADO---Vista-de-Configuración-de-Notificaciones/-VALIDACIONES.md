## ✅ VALIDACIONES

### Frontend (HTML5)
- `type="email"` para campos de email
- `min` y `max` para números
- `required` implícito en switches

### Frontend (JavaScript)
- Validación de email con `.includes('@')`
- Frecuencia >= 1 día
- Confirmación al desactivar sistema

### Backend (Spring)
- `@Valid` en método guardarNotificaciones()
- `BindingResult` para capturar errores
- Validaciones del modelo (`@NotNull`, `@Min`)
- Try-catch para IllegalArgumentException

---

