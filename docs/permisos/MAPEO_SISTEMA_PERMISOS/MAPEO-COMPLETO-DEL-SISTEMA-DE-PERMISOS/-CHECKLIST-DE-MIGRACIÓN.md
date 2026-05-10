## 📝 CHECKLIST DE MIGRACIÓN

### Preparación
- [ ] Backup completo de base de datos
- [ ] Crear rama git: `feature/permisos-dinamicos`
- [ ] Documento de rollback preparado

### Ejecución
- [ ] Crear tablas nuevas (permiso, rol, rol_permiso, usuario_permiso)
- [ ] Ejecutar script de migración de datos
- [ ] Crear entidades JPA
- [ ] Crear repositorios
- [ ] Refactorizar PermisoService
- [ ] Actualizar controllers (backend)
- [ ] Actualizar templates (frontend)
- [ ] Eliminar enum y clase estática

### Validación
- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Pruebas manuales con cada rol
- [ ] Verificar logs de auditoría
- [ ] Performance acceptable

### Deployment
- [ ] Merge a develop
- [ ] Deploy a staging
- [ ] Validación en staging
- [ ] Deploy a producción
- [ ] Monitoreo post-deployment

---

