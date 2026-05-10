## 🔧 MÉTODOS AUXILIARES

### cargarDatosUsuario()

```java
private void cargarDatosUsuario(Model model, Authentication authentication)
```

**Descripción:** Carga información del usuario autenticado en el modelo  
**Datos cargados:**
- `userName`: Nombre completo del usuario
- `userRole`: Rol del usuario (ADMIN, USER, etc.)
- `userInitials`: Iniciales para el avatar

**Uso:** Llamado en todos los endpoints para mantener consistencia

---

