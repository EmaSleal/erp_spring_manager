## 🔍 IMPACTO ESTIMADO

### Archivos a Modificar
- **Backend:** 15 archivos Java
- **Frontend:** 5 archivos HTML + 1 archivo JS
- **Base de Datos:** 4 tablas nuevas + 1 script de migración
- **Tests:** 10+ archivos de test nuevos

### Esfuerzo Estimado
- **Desarrollo:** 40-60 horas
- **Testing:** 20-30 horas
- **Documentación:** 10-15 horas
- **Total:** ~70-105 horas (2-3 semanas)

### Riesgos
- 🔴 **Alto:** Perder permisos críticos de ADMIN
- 🟡 **Medio:** Inconsistencia entre roles y permisos
- 🟢 **Bajo:** Performance degradada

### Mitigación
- ✅ Mantener enum deprecado como fallback
- ✅ Migración gradual por módulo
- ✅ Feature flag para activar/desactivar
- ✅ Rollback plan documentado

---

