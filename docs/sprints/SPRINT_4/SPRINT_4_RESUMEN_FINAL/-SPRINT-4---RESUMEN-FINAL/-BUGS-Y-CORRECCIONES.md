## 🐛 BUGS Y CORRECCIONES

### Bugs Encontrados Durante el Sprint

#### 1. Notificaciones sin email (RESUELTO)
```
Error: NullPointerException al intentar enviar notificación
       a usuario sin email configurado

Solución:
- Validación previa de email
- Manejo graceful del error
- Log de advertencia
- Continuar con otros canales

Commit: fix: manejo graceful de notificaciones sin email
```

#### 2. Bootstrap Icons duplicados (RESUELTO)
```
Warning: Múltiples importaciones de Bootstrap Icons
         causaban conflictos

Solución:
- Centralizar importación en layout.html
- Eliminar importaciones duplicadas
- Verificar en todos los templates

Commit: refactor: centralizar Bootstrap Icons
```

#### 3. Cache de permisos no invalidado (RESUELTO)
```
Error: Cambios de permisos no se reflejaban hasta
       reiniciar servidor

Solución:
- Agregar @CacheEvict en métodos de actualización
- Limpiar cache al modificar roles/permisos
- Agregar logs de invalidación

Commit: fix: invalidar cache al modificar permisos
```

**Total de bugs:** 3  
**Bugs resueltos:** 3  
**Bugs pendientes:** 0 ✅

---

