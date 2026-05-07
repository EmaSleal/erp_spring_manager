## 📋 Plan de Ejecución

### Fase 1: Preparación
1. ✅ Documentar estructura actual
2. ✅ Definir estructura objetivo
3. ⬜ Crear script de migración
4. ⬜ Hacer backup/commit de estado actual

### Fase 2: Reorganización de Static
1. ⬜ Crear estructura de directorios en `static/`
2. ⬜ Mover archivos CSS compartidos a `static/shared/css/`
3. ⬜ Mover archivos JS compartidos a `static/shared/js/`
4. ⬜ Mover archivos CSS de módulos a `static/modules/{modulo}/css/`
5. ⬜ Mover archivos JS de módulos a `static/modules/{modulo}/js/`
6. ⬜ Actualizar referencias en templates HTML

### Fase 3: Reorganización de Templates
1. ⬜ Crear estructura de directorios en `templates/`
2. ⬜ Mover templates compartidos a `templates/shared/`
3. ⬜ Mover templates de módulos a `templates/modules/{modulo}/`
4. ⬜ Actualizar referencias en controladores Java
5. ⬜ Actualizar referencias th:fragment y th:replace

### Fase 4: Verificación
1. ⬜ Compilar proyecto
2. ⬜ Probar rutas de recursos estáticos
3. ⬜ Probar rutas de templates
4. ⬜ Verificar que todas las vistas se renderizan correctamente
5. ⬜ Revisar console del navegador para errores 404

### Fase 5: Documentación
1. ⬜ Actualizar ESTRUCTURA_ARCHIVOS.md
2. ⬜ Documentar nuevas convenciones
3. ⬜ Crear guía para nuevos desarrolladores

---

