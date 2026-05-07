## 📋 TAREA 3: PERMISOS PERSONALIZADOS (UsuarioPermiso) ⏳

### Análisis
- [ ] Revisar modelo UsuarioPermiso.java existente
- [ ] Validar estructura de tabla en BD
- [ ] Definir casos de uso principales
- [ ] Diseñar flujos de UI

### Backend
- [ ] Crear UsuarioPermisoRepository
- [ ] Crear UsuarioPermisoService interface
- [ ] Implementar UsuarioPermisoServiceImpl
  - [ ] concederPermiso()
  - [ ] denegarPermiso()
  - [ ] removerPermisoPersonalizado()
  - [ ] obtenerPermisosEfectivos()
  - [ ] obtenerHistorialPermisos()

### Controller
- [ ] Extender UsuarioAdminController
  - [ ] POST /admin/usuarios/{id}/permisos/conceder
  - [ ] POST /admin/usuarios/{id}/permisos/denegar
  - [ ] DELETE /admin/usuarios/{id}/permisos/{permisoId}
  - [ ] GET /admin/usuarios/{id}/permisos/efectivos

### Frontend
- [ ] Sección en formulario de usuarios
- [ ] Tabla de permisos del rol (base)
- [ ] Interfaz para agregar permisos custom
- [ ] Indicadores visuales (verde=concedido, rojo=denegado)
- [ ] Tabla de historial con auditoría
- [ ] Modal de confirmación

### Testing
- [ ] Tests unitarios de servicio
- [ ] Tests de endpoints
- [ ] Validación de lógica de prioridad
- [ ] Tests de UI

### Documentación
- [ ] Actualizar JavaDoc
- [ ] Documentar casos de uso
- [ ] Actualizar dashboard de progreso

---

