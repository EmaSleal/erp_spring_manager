## ⏳ TAREAS PENDIENTES

### 1. Deprecar Enum Permiso.java ⏳
**Prioridad:** 🟡 BAJA  
**Estimación:** 10 minutos  
**Dependencias:** Tareas 4 y 5 completadas ✅

#### Descripción:
Marcar el enum `Permiso.java` como `@Deprecated` para indicar que ya no debe usarse. Eventualmente se eliminará del código una vez validado todo en producción.

#### Acción Requerida:
Agregar anotación `@Deprecated` y comentario al inicio de la clase:

```java
/**
 * @deprecated Este enum está deprecado desde Sprint 4.
 * Usar permisos desde base de datos mediante PermisoService.tienePermisoPorCodigo()
 * en lugar de referencias directas al enum.
 * Se mantendrá temporalmente para referencia, pero será eliminado en futuras versiones.
 */
@Deprecated
public enum Permiso {
    // ... contenido existente
}
```

#### Criterios de Aceptación:
- [ ] Enum marcado con @Deprecated
- [ ] Javadoc indica alternativa (usar BD)
- [ ] Compilación genera warnings en usos del enum
- [ ] No rompe código existente

---

### 2. Documentar Cambios en Manual de Usuario ⏳
**Prioridad:** 🟠 MEDIA  
**Estimación:** 1-2 horas  
**Dependencias:** Todas las tareas técnicas completadas ✅

#### Descripción:
Actualizar la documentación de usuario explicando el nuevo sistema dinámico de permisos y cómo gestionarlos.

#### Documentos a Actualizar:
1. `MANUAL_USUARIO_PERMISOS.md` - Guía de gestión de permisos
2. `MAPEO_SISTEMA_PERMISOS.md` - Arquitectura técnica
3. `README.md` - Sección de permisos

#### Contenido Nuevo a Agregar:
- Cómo crear/editar roles
- Cómo gestionar permisos individuales
- Cómo asignar permisos personalizados a usuarios
- Explicación de la lógica de prioridades (DENEGADO > CONCEDIDO > ROL)
- Screenshots de las nuevas interfaces

#### Criterios de Aceptación:
- [ ] Documentos actualizados con nueva funcionalidad
- [ ] Ejemplos prácticos incluidos
- [ ] Screenshots de UI agregados
- [ ] Validado por usuario final

---

### 3. Testing Exhaustivo en Desarrollo ✅
**Prioridad:** 🔴 ALTA  
**Estimación:** 2-3 horas  
**Dependencias:** Todas las tareas técnicas completadas ✅  
**Fecha de Completado:** 27 de diciembre de 2025

#### Descripción:
Realizar pruebas exhaustivas del sistema de permisos en ambiente de desarrollo antes de pasar a producción.

#### Casos de Prueba:
1. **Gestión de Roles:**
   - ✅ Crear nuevo rol con permisos
   - ✅ Editar rol existente
   - ✅ Activar/desactivar rol
   - ✅ Asignar rol a usuario

2. **Gestión de Permisos:**
   - ✅ Editar nombre/descripción de permiso
   - ✅ Cambiar categoría de permiso
   - ✅ Marcar permiso como crítico
   - ✅ Activar/desactivar permiso

3. **Permisos Personalizados:**
   - ✅ Conceder permiso adicional a usuario
   - ✅ Denegar permiso del rol
   - ✅ Verificar lógica de prioridades
   - ✅ Remover personalización

4. **Validación en Controllers:**
   - ✅ Verificar @PreAuthorize funciona correctamente
   - ✅ Usuario sin permiso es bloqueado (403)
   - ✅ Usuario con permiso accede sin problemas

5. **Validación en Templates:**
   - ✅ Botones se ocultan sin permiso
   - ✅ Enlaces se muestran con permiso correcto
   - ✅ Sidebar muestra solo módulos permitidos

6. **Pruebas de Integración:**
   - ✅ Creación de facturas con notificaciones
   - ✅ Manejo de errores (usuarios sin email)
   - ✅ Templates responsive en diferentes dispositivos
   - ✅ Navegación completa entre módulos

#### Herramientas:
- Usar roles de prueba (VENDEDOR, GERENTE)
- Crear usuarios de prueba con diferentes combinaciones
- Validar logs de seguridad
- Verificar auditoría en BD

#### Criterios de Aceptación:
- ✅ Todos los casos de prueba pasan
- ✅ No hay errores 403/500 inesperados
- ✅ Logs de seguridad son coherentes
- ✅ Auditoría registra cambios correctamente

#### Resultado:
✅ **TODAS LAS PRUEBAS PASARON EXITOSAMENTE**  
No se encontraron errores durante las pruebas exhaustivas en desarrollo.

---

