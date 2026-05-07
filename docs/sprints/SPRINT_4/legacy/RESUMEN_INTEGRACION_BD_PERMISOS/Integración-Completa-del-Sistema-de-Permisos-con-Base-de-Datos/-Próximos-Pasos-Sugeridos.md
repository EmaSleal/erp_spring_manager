## 🚀 Próximos Pasos Sugeridos

### Fase 4.7: Gestión de Permisos Individuales
- [ ] Crear `PermisoAdminController` para CRUD de permisos
- [ ] Vistas: `permisos.html` (lista) y `permiso-formulario.html`
- [ ] Permitir crear/editar/desactivar permisos individuales

### Fase 4.8: Permisos Personalizados por Usuario
- [ ] Implementar `UsuarioPermisoService`
- [ ] Crear formulario de asignación de permisos custom
- [ ] Métodos: `concederPermiso()`, `denegarPermiso()`, `removerPermiso()`
- [ ] UI en formulario de usuario

### Fase 4.9: Migración de @PreAuthorize
- [ ] Actualizar controladores para usar BD en lugar de enum
- [ ] Migrar de `T(Permiso).PERMISO_NAME` a `@permisoService.tienePermisoPorCodigo()`
- [ ] Actualizar templates `sec:authorize`
- [ ] Testing completo

### Fase 4.10: Deprecación del Sistema Antiguo
- [ ] Marcar `Permiso.java` enum como `@Deprecated`
- [ ] Marcar `MatrizPermisos.java` como `@Deprecated`
- [ ] Documentar guía de migración
- [ ] Crear tests de regresión

---

