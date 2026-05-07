## 🚀 PRÓXIMOS PASOS

### Fase 4.4: Backend Controllers (Pendiente)

**Tareas:**
1. ⏸️ Crear `UsuarioAdminController.java`
   - Endpoints REST para gestión de usuarios
   - GET /api/usuarios (listar con paginación)
   - GET /api/usuarios/{id} (detalle)
   - POST /api/usuarios (crear)
   - PUT /api/usuarios/{id} (editar)
   - PUT /api/usuarios/{id}/bloquear (bloquear)
   - PUT /api/usuarios/{id}/desbloquear (desbloquear)
   - PUT /api/usuarios/{id}/rol (cambiar rol)
   - DELETE /api/usuarios/{id} (eliminar/desactivar)

2. ⏸️ Crear `UsuarioActividadController.java`
   - GET /api/actividades (listar)
   - GET /api/actividades/usuario/{id} (por usuario)
   - GET /api/actividades/criticas (críticas)
   - GET /api/actividades/sospechosas (sospechosas)

3. ⏸️ Agregar validaciones con `@Valid`
4. ⏸️ Implementar manejo de excepciones

---

