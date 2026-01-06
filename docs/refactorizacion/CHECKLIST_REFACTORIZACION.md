# ✅ CHECKLIST RÁPIDO - REFACTORIZACIÓN MODULAR

**Proyecto:** WhatsApp Orders Manager  
**Fecha inicio:** ___________  
**Fecha fin:** ___________  
**Estado:** 🔄 EN PROGRESO

---

## 📋 PREPARACIÓN

- [ ] 1. Crear backup del proyecto (Git branch + copia física)
- [ ] 2. Verificar que todo compila: `mvn clean compile`
- [ ] 3. Verificar que tests pasan: `mvn test`
- [ ] 4. Crear branch: `git checkout -b feature/modular-refactoring`
- [ ] 5. Leer guía completa: `docs/GUIA_REFACTORIZACION_MODULAR.md`

---

## 🏗️ FASE 1: ESTRUCTURA BASE (30 min)

- [ ] 1. Ejecutar script PowerShell: `.\refactoring-helper.ps1`
- [ ] 2. Opción 2: Crear estructura de módulos
- [ ] 3. Verificar carpetas creadas: `tree modules -L 2`
- [ ] 4. Commit: `git commit -m "feat: Crear estructura base para refactorización modular"`

---

## 📦 FASE 2: MÓDULO PRODUCTO (2h)

- [ ] 1. Mover archivos (script o manual)
- [ ] 2. Actualizar package en ProductoController
- [ ] 3. Actualizar package en ProductoService
- [ ] 4. Actualizar package en ProductoRepository
- [ ] 5. Actualizar package en Producto (model)
- [ ] 6. Actualizar package en ProductoDTO
- [ ] 7. Find & Replace imports en todo el proyecto
- [ ] 8. Compilar: `mvn clean compile` ✅
- [ ] 9. Tests: `mvn test` ✅
- [ ] 10. Commit: `git commit -m "refactor: Migrar módulo Producto"`

---

## 📦 FASE 3: MÓDULO CLIENTE (2h)

- [ ] 1. Mover ClienteController → `modules/cliente/controller/`
- [ ] 2. Mover ClienteService → `modules/cliente/service/`
- [ ] 3. Mover ClienteRepository → `modules/cliente/repository/`
- [ ] 4. Mover Cliente → `modules/cliente/model/`
- [ ] 5. Mover ClienteDTO → `modules/cliente/dto/`
- [ ] 6. Actualizar todos los packages
- [ ] 7. Find & Replace imports
- [ ] 8. Compilar ✅
- [ ] 9. Tests ✅
- [ ] 10. Commit

---

## 📦 FASE 4: MÓDULO REPORTES (2h)

- [ ] 1. Mover ReporteController
- [ ] 2. Mover DashboardController
- [ ] 3. Mover ReporteService
- [ ] 4. Mover ExportService
- [ ] 5. Mover DTOs relacionados
- [ ] 6. Actualizar packages
- [ ] 7. Find & Replace imports
- [ ] 8. Compilar ✅
- [ ] 9. Tests ✅
- [ ] 10. Commit

---

## 📦 FASE 5: MÓDULO CONFIGURACIÓN (3h)

- [ ] 1. Mover 5 Controllers de configuración
- [ ] 2. Mover 5 Services
- [ ] 3. Mover 5 Repositories
- [ ] 4. Mover 5 Models
- [ ] 5. Mover DTOs
- [ ] 6. Actualizar packages (20 archivos)
- [ ] 7. Find & Replace imports
- [ ] 8. Compilar ✅
- [ ] 9. Tests ✅
- [ ] 10. Commit

---

## 📦 FASE 6: MÓDULO FACTURACIÓN (4h) ⚠️ CRÍTICO

- [ ] 1. **PRIMERO:** Mover Factura.java (model)
- [ ] 2. **PRIMERO:** Mover LineaFactura.java (model)
- [ ] 3. Find & Replace imports de Factura (MUCHOS archivos)
- [ ] 4. Compilar para verificar
- [ ] 5. Mover FacturaRepository, LineaFacturaRepository
- [ ] 6. Mover FacturaService, LineaFacturaService
- [ ] 7. Mover FacturaController, LineaFacturaController
- [ ] 8. Mover DTOs
- [ ] 9. Mover Enums (EstadoFactura)
- [ ] 10. Actualizar packages
- [ ] 11. Find & Replace todos los imports
- [ ] 12. Compilar ✅
- [ ] 13. Tests ✅
- [ ] 14. Commit

---

## 📦 FASE 7: MÓDULO WHATSAPP (4h)

- [ ] 1. Mover 5 Controllers WhatsApp
- [ ] 2. Mover 5 Services WhatsApp
- [ ] 3. Mover 3 Repositories
- [ ] 4. Mover 3 Models (MensajeWhatsApp, PlantillaWhatsApp, WebhookLog)
- [ ] 5. Mover 2 DTOs
- [ ] 6. Mover Enums (DireccionMensaje, EstadoMensaje)
- [ ] 7. Actualizar packages
- [ ] 8. Find & Replace imports
- [ ] 9. Compilar ✅
- [ ] 10. Tests ✅
- [ ] 11. Commit

---

## 📦 FASE 8: MÓDULO NOTIFICACIÓN (4h)

- [ ] 1. Mover 3 Controllers Notificacion
- [ ] 2. Mover 4 Services
- [ ] 3. Mover 4 Repositories
- [ ] 4. Mover 4 Models
- [ ] 5. Mover DTOs
- [ ] 6. Mover 3 Enums (CanalNotificacion, TipoNotificacion, EstadoNotificacion)
- [ ] 7. Mover Events (si existen)
- [ ] 8. Actualizar packages
- [ ] 9. Find & Replace imports
- [ ] 10. Compilar ✅
- [ ] 11. Tests ✅
- [ ] 12. Commit

---

## 📦 FASE 9: MÓDULO SEGURIDAD (5h) ⚠️ MUY CRÍTICO

### Grupo 1: Models Básicos
- [ ] 1. Mover Usuario.java
- [ ] 2. Mover Rol.java
- [ ] 3. Mover Permiso.java
- [ ] 4. Actualizar packages
- [ ] 5. Find & Replace imports
- [ ] 6. Compilar ✅

### Grupo 2: Models de Relación
- [ ] 7. Mover UsuarioPermiso.java
- [ ] 8. Mover UsuarioActividad.java
- [ ] 9. Mover UsuarioSesion.java
- [ ] 10. Actualizar packages
- [ ] 11. Find & Replace imports
- [ ] 12. Compilar ✅

### Grupo 3: Repositories
- [ ] 13. Mover 5 Repositories
- [ ] 14. Actualizar packages
- [ ] 15. Compilar ✅

### Grupo 4: Services
- [ ] 16. Mover 5 Services
- [ ] 17. Actualizar packages
- [ ] 18. Compilar ✅

### Grupo 5: Controllers
- [ ] 19. Mover 8 Controllers
- [ ] 20. Actualizar packages
- [ ] 21. Find & Replace imports finales
- [ ] 22. Compilar ✅

### Testing Crítico
- [ ] 23. Tests de autenticación ✅
- [ ] 24. Tests de permisos ✅
- [ ] 25. Arrancar app y probar login ✅
- [ ] 26. Commit

---

## 📦 FASE 10: CÓDIGO COMPARTIDO (2h)

- [ ] 1. Mover Utils → `shared/util/`
- [ ] 2. Mover Exception handlers → `shared/exception/`
- [ ] 3. Mover DTOs compartidos → `shared/dto/`
- [ ] 4. Mover Configs → `shared/config/`
- [ ] 5. Actualizar packages
- [ ] 6. Find & Replace imports
- [ ] 7. Compilar ✅
- [ ] 8. Tests ✅
- [ ] 9. Commit

---

## 📦 FASE 11: CORE (1h)

- [ ] 1. Mover Listeners → `core/listeners/`
- [ ] 2. Mover Schedulers → `core/schedulers/`
- [ ] 3. Mover Events base → `core/events/`
- [ ] 4. Actualizar packages
- [ ] 5. Find & Replace imports
- [ ] 6. Compilar ✅
- [ ] 7. Tests ✅
- [ ] 8. Commit

---

## 📦 FASE 12: LIMPIEZA FINAL (1h)

- [ ] 1. Verificar carpetas antiguas vacías
- [ ] 2. Eliminar `controllers/` (si está vacía)
- [ ] 3. Eliminar `services/` (si está vacía)
- [ ] 4. Eliminar `repositories/` (si está vacía)
- [ ] 5. Eliminar `models/` (si está vacía)
- [ ] 6. Optimize imports: `Ctrl + Alt + O` en IntelliJ
- [ ] 7. Reformat code: `Ctrl + Alt + L` en IntelliJ
- [ ] 8. `mvn clean compile` ✅
- [ ] 9. `mvn test` ✅
- [ ] 10. `mvn package` ✅
- [ ] 11. Commit final

---

## ✅ VALIDACIÓN FINAL

### Compilación
- [ ] `mvn clean` → Sin errores
- [ ] `mvn compile` → Sin errores
- [ ] Sin warnings críticos

### Tests
- [ ] `mvn test` → Todos pasan
- [ ] Coverage > 70% (opcional)

### Aplicación
- [ ] `mvn spring-boot:run` → Arranca sin errores
- [ ] Login funciona: `http://localhost:8080/login`
- [ ] Productos: `http://localhost:8080/productos`
- [ ] Clientes: `http://localhost:8080/clientes`
- [ ] Facturas: `http://localhost:8080/facturas`
- [ ] WhatsApp: `http://localhost:8080/whatsapp/mensajes`
- [ ] Reportes: `http://localhost:8080/reportes/dashboard`

### Funcionalidades
- [ ] Crear producto funciona
- [ ] Crear cliente funciona
- [ ] Crear factura funciona
- [ ] Login funciona
- [ ] Permisos funcionan
- [ ] WhatsApp funciona
- [ ] Notificaciones funcionan

### Estructura
- [ ] 9 módulos creados
- [ ] shared/ con código compartido
- [ ] core/ con infraestructura
- [ ] Carpetas antiguas eliminadas

### Git
- [ ] Todos los cambios commiteados
- [ ] Branch actualizado
- [ ] No hay conflictos

### Documentación
- [ ] README actualizado
- [ ] Guía de refactorización completada
- [ ] Este checklist completado

---

## 📊 ESTADÍSTICAS FINALES

**Total de módulos:** _____ / 9  
**Archivos migrados:** _____ / ~106  
**Tiempo total:** _____ horas  
**Commits realizados:** _____  

**Estado final:** 
- [ ] ✅ COMPLETADO
- [ ] ⚠️ COMPLETADO CON OBSERVACIONES
- [ ] ❌ PENDIENTE

---

## 📝 NOTAS Y OBSERVACIONES

```
Escribe aquí cualquier problema encontrado, 
solución aplicada, o nota importante:

___________________________________________________________
___________________________________________________________
___________________________________________________________
___________________________________________________________
___________________________________________________________
```

---

## 🎉 SIGUIENTE PASO

- [ ] Merge a master (después de QA)
- [ ] Actualizar documentación del proyecto
- [ ] Notificar al equipo
- [ ] Celebrar 🎉

---

**Última actualización:** 27 de diciembre de 2025  
**Responsable:** ___________
