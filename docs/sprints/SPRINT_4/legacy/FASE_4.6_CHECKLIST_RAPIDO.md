# ✅ CHECKLIST RÁPIDO - FASE 4.6 GESTIÓN DE PERMISOS

**Instrucciones:** Marcar con `[x]` cuando se complete cada ítem.

---

## 📋 TAREA 1: CRUD DE ROLES ✅

### Backend
- [x] Crear RolService interface
- [x] Implementar RolServiceImpl
- [x] Crear RolAdminController
- [x] 7 endpoints REST funcionando
- [x] Validaciones de negocio

### Frontend
- [x] roles.html (listado)
- [x] formulario.html (crear/editar)
- [x] Filtros y búsqueda
- [x] Estadísticas
- [x] Asignación de permisos

### Testing
- [x] Compilación sin errores
- [x] Endpoints accesibles
- [x] CRUD end-to-end funcional

---

## 📋 TAREA 2: CRUD DE PERMISOS INDIVIDUALES ✅

### Backend
- [x] Crear PermisoAdminController
- [x] Implementar método guardar() en servicio
- [x] 6 endpoints + 1 auxiliar
- [x] Lógica de filtrado manual

### Frontend
- [x] gestionar.html (listado con filtros)
- [x] editar.html (formulario)
- [x] 4 tarjetas de estadísticas
- [x] Tabla responsive con scroll
- [x] Panel lateral con roles

### UI/UX
- [x] Código en monospace
- [x] Badges de categoría
- [x] Indicadores de crítico
- [x] Confirmaciones JavaScript
- [x] Breadcrumbs

### Integración
- [x] Sidebar actualizado (enlace agregado)
- [x] Compilación BUILD SUCCESS
- [x] Tests unitarios pasando (22/22)

---

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

## 📋 TAREA 4: MIGRACIÓN DE CONTROLLERS ⏳

### Preparación
- [ ] Crear método tienePermisoPorCodigo() en PermisoService
- [ ] Implementar en PermisoServiceImpl
- [ ] Agregar tests del nuevo método
- [ ] Documentar uso con ejemplos

### Controllers - Críticos (Migrar Primero)
- [ ] FacturaController (10 anotaciones)
- [ ] ClienteController (7 anotaciones)
- [ ] ProductoController (8 anotaciones)
- [ ] UsuarioAdminController (9 anotaciones)
- [ ] ReporteController (8 anotaciones)

### Controllers - Importantes
- [ ] ConfiguracionController (6 anotaciones)
- [ ] RolAdminController (7 anotaciones)
- [ ] PermisoAdminController (6 anotaciones)
- [ ] WhatsAppController (5 anotaciones)

### Controllers - Secundarios
- [ ] EmpresaController (5 anotaciones)
- [ ] NotificacionController (4 anotaciones)
- [ ] AjaxController (4 anotaciones)
- [ ] DashboardController (3 anotaciones)
- [ ] PermisosController (1 anotación)

### Testing
- [ ] Compilación sin errores
- [ ] Tests de integración pasando
- [ ] Validar comportamiento de permisos
- [ ] Smoke tests en cada módulo

### Limpieza
- [ ] Marcar enum Permiso.java como @Deprecated
- [ ] Agregar comentarios de migración
- [ ] Documentar cambios

---

## 📋 TAREA 5: MIGRACIÓN DE TEMPLATES ⏳

### Componentes Compartidos (Migrar Primero)
- [ ] components/sidebar.html (12 directivas)
- [ ] components/navbar.html (3 directivas)
- [ ] home.html (2 directivas)

### Módulo Facturas
- [ ] facturas/facturas.html (10 directivas)
- [ ] facturas/formulario.html (6 directivas)
- [ ] facturas/detalle.html (8 directivas)
- [ ] facturas/pago.html (4 directivas)

### Módulo Clientes
- [ ] clientes/clientes.html (8 directivas)
- [ ] clientes/formulario.html (4 directivas)
- [ ] clientes/detalle.html (6 directivas)

### Módulo Productos
- [ ] productos/productos.html (7 directivas)
- [ ] productos/formulario.html (3 directivas)
- [ ] productos/detalle.html (5 directivas)

### Módulo Reportes
- [ ] reportes/dashboard.html (5 directivas)
- [ ] reportes/graficos.html (4 directivas)

### Módulo Configuración
- [ ] configuracion/general.html (8 directivas)
- [ ] configuracion/empresa.html (7 directivas)

### Módulo WhatsApp
- [ ] whatsapp/mensajes.html (5 directivas)
- [ ] whatsapp/configuracion.html (4 directivas)

### Módulo Administración
- [ ] admin/usuarios/usuarios.html (6 directivas)
- [ ] admin/usuarios/formulario.html (5 directivas)

### Testing
- [ ] Validar renderizado de páginas
- [ ] Verificar permisos ocultan/muestran elementos
- [ ] Smoke test en cada módulo
- [ ] Tests de navegación

### Limpieza
- [ ] Verificar con grep que no quedan referencias a enums
- [ ] Eliminar código comentado
- [ ] Validar indentación

---

## 📋 FINALIZACIÓN Y ENTREGA

### Testing Final
- [ ] Tests unitarios: 100% pasando
- [ ] Tests de integración: 100% pasando
- [ ] Tests de UI: Smoke tests OK
- [ ] Performance: Sin degradación

### Documentación
- [ ] JavaDoc completo y actualizado
- [ ] README con instrucciones de uso
- [ ] Changelog actualizado
- [ ] Diagramas de flujo (opcional)

### Revisión de Código
- [ ] Sin warnings críticos
- [ ] Compilación limpia
- [ ] Seguimiento de estándares
- [ ] Refactoring aplicado

### Limpieza Final
- [ ] Eliminar enum Permiso.java
- [ ] Eliminar MatrizPermisos.java
- [ ] Remover código comentado
- [ ] Optimizar imports

### Despliegue
- [ ] Migración de BD ejecutada
- [ ] Backup de BD previo
- [ ] Tests en ambiente de pruebas
- [ ] Validación con usuarios

---

## 📊 RESUMEN DE PROGRESO

```
Total de Ítems: 135
Completados:    40
Pendientes:     95
Progreso:       29.6%

Por Tarea:
├─ Tarea 1: 13/13 ✅ 100%
├─ Tarea 2: 27/27 ✅ 100%
├─ Tarea 3:  0/26 ⏳   0%
├─ Tarea 4:  0/34 ⏳   0%
└─ Tarea 5:  0/35 ⏳   0%
```

---

## 🎯 PRÓXIMO ÍTEM A COMPLETAR

**Tarea 3 - Paso 1:**
```
[ ] Revisar modelo UsuarioPermiso.java existente
```

**Acción:** Abrir archivo y validar estructura de campos.

---

**Última Actualización:** 26 de diciembre de 2025  
**Próxima Revisión:** Al completar Tarea 3
