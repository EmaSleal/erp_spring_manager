## 📝 Notas Técnicas

### Compatibilidad Dual (Actual)
El sistema actualmente mantiene **compatibilidad dual**:

**Sistema Antiguo (Enum):**
- Controllers usan `@PreAuthorize("@permisoService.tienePermisoByUsername(..., T(Permiso).PERMISO_VER)")`
- Templates usan `sec:authorize="@permisoService.tienePermisoByUsername(...)"`
- `PermisoServiceImpl` consulta `MatrizPermisos`

**Sistema Nuevo (BD):**
- `RolAdminController` usa solo base de datos
- `PermisosController` consulta desde BD
- `RolService` y repositorios gestionan permisos dinámicamente

**Migración gradual:**
Se puede migrar controller por controller sin romper funcionalidad existente.

### Performance
- Relaciones `@ManyToMany` con FetchType.LAZY
- Método `buscarPorCodigoConPermisos()` para carga eager cuando sea necesario
- Caché de segundo nivel recomendado para producción (Hibernate + Redis)

### Seguridad
- Todos los endpoints de administración requieren `hasRole('ADMIN')`
- Validación de permisos en capa de servicio
- Logging completo de operaciones críticas

---

