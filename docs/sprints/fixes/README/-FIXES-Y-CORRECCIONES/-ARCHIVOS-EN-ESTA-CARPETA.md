## 📁 ARCHIVOS EN ESTA CARPETA

### **Sprint 1 - Completado**

#### Responsive (3 archivos)

**1. FIX_RESPONSIVE_TABLES.md**
**Problema:** Tablas con 6-7 columnas causaban overflow horizontal en móvil  
**Solución:** 3 capas (CSS media queries + Bootstrap classes + JavaScript dinámico)  
**Impacto:** Reducción 50% en ancho mínimo de tablas  
**Módulos:** Productos, Facturas

**2. FIX_PAGINACION_RESPONSIVE.md**
**Problema:** 17 botones de paginación ocupaban 850px en una sola línea  
**Solución:** Algoritmo sliding window (muestra 3-10 páginas según tamaño pantalla)  
**Impacto:** Reducción 76% en overflow horizontal  
**Módulos:** Productos (con extensión a otros módulos)

**3. RESUMEN_RESPONSIVE_COMPLETO.md**
**Contenido:** Consolidación de las dos mejoras responsive anteriores  
**Uso:** Documento ejecutivo para entender ambas mejoras rápidamente

#### Seguridad (1 archivo)

**4. FIX_LOGOUT_403.md**
**Problema:** Error 403 "Forbidden" al hacer logout  
**Causa:** Falta de CSRF token en petición AJAX  
**Solución:** Agregar CSRF token desde meta tag en `navbar.js`  
**Impacto:** Logout funcional sin errores

#### UX/UI (2 archivos)

**5. FIX_PALETA_COLORES_AUTH.md**
**Problema:** Login y registro usaban colores púrpura (#667eea) diferentes al resto de la app (azul #1976D2)  
**Solución:** Actualizar login.html y register.html a Material Design azul  
**Impacto:** Consistencia visual 100% (9/9 páginas con misma paleta)

**6. FIX_ACTUALIZACION_ESTADO_FACTURA.md**
**Problema:** Actualización de estado de facturas  
**Solución:** (Ver archivo para detalles)  
**Impacto:** (Ver archivo para detalles)

#### Formularios (2 archivos)

**7. FIX_FORMULARIO_USUARIOS.md**
**Problema:** Correcciones en formulario de usuarios  
**Solución:** (Ver archivo para detalles)  

**8. FIX_FORMULARIO_USUARIOS_FINAL.md**
**Problema:** Ajustes finales del formulario  
**Solución:** (Ver archivo para detalles)  

---

### **Sprint 2 - Fase 5 (Notificaciones)**

#### Templates y Errores (1 archivo)

**9. FIX_PLANTILLAS_ERROR.md**
**Problema:** Spring Boot intentaba renderizar `error/404.html` y `error/500.html` que no existían  
**Causa:** Solo existía `error/403.html`, faltaban plantillas para otros códigos HTTP  
**Solución:** Crear plantillas personalizadas para errores 404 y 500 con diseño consistente  
**Impacto:** Errores ahora se muestran correctamente sin cascada de excepciones  
**Archivos creados:**
- `templates/error/404.html` (150 líneas)
- `templates/error/500.html` (135 líneas)

#### JavaScript y Dependencias (1 archivo)

**10. FIX_JQUERY_ORDEN_CARGA.md**
**Problema:** Botones no respondían, error `$ is not defined` en consola  
**Causa:** `usuarios.js` se cargaba ANTES de jQuery, causando ReferenceError  
**Solución:** Mover script `usuarios.js` después del fragmento de scripts comunes  
**Impacto:** Restaurada toda la funcionalidad JavaScript (event listeners, AJAX, SweetAlert2)  
**Archivos modificados:**
- `templates/usuarios/usuarios.html` (orden de scripts)

---

### **Sprint 2 - Fase 7 (Integración)**

#### Autenticación (1 archivo)

**11. FIX_LOGIN_FLEXIBLE_NOMBRE_TELEFONO.md**
**Problema:** Login dejó de funcionar después de implementar Punto 7.3 (Último Acceso)  
**Causa:** Se cambió `loadUserByUsername()` para buscar SOLO por teléfono, pero el formulario envía un campo genérico que puede ser nombre O teléfono  
**Solución:** Búsqueda flexible con `Optional.or()` - primero intenta por teléfono, luego por nombre  
**Impacto:** ✅ Login funciona con nombre o teléfono (más robusto que antes)  
**Severidad:** 🔴 CRÍTICA (Sistema bloqueado - 0% usuarios podían entrar)  
**Tiempo de resolución:** 15 minutos  
**Archivos modificados:**
- `services/impl/UserDetailsServiceImpl.java` (líneas 28-46)

**12. FIX_TIMESTAMP_FORMAT_THYMELEAF.md**
**Problema:** Vista de usuarios (`/usuarios`) generaba error al intentar formatear `ultimoAcceso`  
**Causa:** Thymeleaf `#temporals.format()` no puede formatear `java.sql.Timestamp` directamente, solo tipos `java.time.*`  
**Solución:** Convertir Timestamp a LocalDateTime con `.toLocalDateTime()` antes de formatear  
**Impacto:** ✅ Vista de usuarios carga correctamente con fechas formateadas  
**Severidad:** 🟡 MEDIA (Vista no se cargaba - Error 500)  
**Tiempo de resolución:** 5 minutos  
**Archivos modificados:**
- `templates/usuarios/usuarios.html` (línea 262)

**13. FIX_LINEAS_FACTURA_PRODUCTO_NULL.md**
**Problema:** Error al guardar factura cuando se agregaba línea nueva sin seleccionar producto  
**Causa:** Sistema intentaba insertar línea con `id_producto = null`, violando constraint de BD  
**Solución:** Filtrar líneas vacías automáticamente antes de enviar al backend + validación de IDs temporales (timestamps)  
**Impacto:** ✅ Facturas se guardan correctamente omitiendo líneas vacías automáticamente  
**Severidad:** 🔴 CRÍTICA (Guardado de facturas bloqueado)  
**Tiempo de resolución:** 20 minutos  
**Archivos modificados:**
- `static/js/editar-factura.js` (funciones `guardarLineas()` y `createLineaRow()`)

---

