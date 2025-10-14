# 🔧 FIX: Error "Neither BindingResult nor plain target object for bean name 'empresa'" al guardar notificaciones

**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5 (Notificaciones)  
**Punto:** 5.4.2 - Vista de Configuración de Notificaciones

---

## 📋 PROBLEMA

Al intentar **guardar la configuración de notificaciones**, se producía el siguiente error:

```
java.lang.IllegalStateException: Neither BindingResult nor plain target object 
for bean name 'empresa' available as request attribute
```

**Error en:** `configuracion/empresa` - line 15

**Flujo del Error:**
1. Usuario guarda configuración en tab "Notificaciones"
2. POST a `/configuracion/notificaciones/guardar`
3. Redirect a `/configuracion/notificaciones` ❌ **MAL**
4. GET a `/configuracion/notificaciones` (método separado)
5. Intenta cargar vista pero fragment "empresa" también se carga
6. Fragment "empresa" necesita objeto `empresa` en el modelo
7. **ERROR:** Objeto `empresa` no está disponible

---

## 🔍 CAUSA RAÍZ

El método `guardarNotificaciones()` hacía redirect a:
```java
return "redirect:/configuracion/notificaciones";
```

Pero ese endpoint (`/configuracion/notificaciones`) es un método **separado** que solo carga datos de notificaciones y retorna `"configuracion/index"`.

Sin embargo, `configuracion/index.html` carga **TODOS los fragments** (empresa, facturacion, notificaciones), y cada fragment necesita su objeto en el modelo.

El redirect debería ir a `/configuracion?tab=notificaciones` que es el método `index()` principal que carga **TODOS** los objetos necesarios.

---

## ✅ SOLUCIÓN APLICADA

### Corrección en ConfiguracionController.java

**Cambiados 2 redirects:**

1. **Al guardar exitosamente:**
   ```java
   // ANTES ❌
   return "redirect:/configuracion/notificaciones";
   
   // DESPUÉS ✅
   return "redirect:/configuracion?tab=notificaciones";
   ```

2. **Al tener errores de validación:**
   ```java
   // ANTES ❌
   if (result.hasErrors()) {
       redirectAttributes.addFlashAttribute("error", "...");
       return "redirect:/configuracion/notificaciones";
   }
   
   // DESPUÉS ✅
   if (result.hasErrors()) {
       redirectAttributes.addFlashAttribute("error", "...");
       return "redirect:/configuracion?tab=notificaciones";
   }
   ```

---

## 🔄 FLUJO CORREGIDO

### Antes (❌ Error):
```
1. POST /configuracion/notificaciones/guardar
2. Guardar datos ✓
3. Redirect → /configuracion/notificaciones
4. GET /configuracion/notificaciones (método notificaciones())
   - Carga solo configuracionNotif
   - Retorna "configuracion/index"
5. Vista carga fragment empresa
6. ❌ ERROR: empresa no está en modelo
```

### Después (✅ Funciona):
```
1. POST /configuracion/notificaciones/guardar
2. Guardar datos ✓
3. Redirect → /configuracion?tab=notificaciones
4. GET /configuracion (método index(tab="notificaciones"))
   - Carga empresa ✓
   - Carga configuracion (facturacion) ✓
   - Carga configuracionNotif ✓
   - Carga previewNumero ✓
   - Tab activo = notificaciones ✓
   - Retorna "configuracion/index"
5. Vista carga todos los fragments
6. ✅ SUCCESS: Todos los objetos disponibles
```

---

## 📝 CÓDIGO MODIFICADO

### ConfiguracionController.java - Línea ~480

**Antes:**
```java
if (result.hasErrors()) {
    log.warn("Errores de validación en configuración de notificaciones");
    redirectAttributes.addFlashAttribute("error", "Por favor, corrija los errores en el formulario");
    redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
    return "redirect:/configuracion/notificaciones"; // ❌ MAL
}
```

**Después:**
```java
if (result.hasErrors()) {
    log.warn("Errores de validación en configuración de notificaciones");
    redirectAttributes.addFlashAttribute("error", "Por favor, corrija los errores en el formulario");
    redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
    return "redirect:/configuracion?tab=notificaciones"; // ✅ CORRECTO
}
```

### ConfiguracionController.java - Línea ~507

**Antes:**
```java
log.info("✅ Configuración de notificaciones guardada exitosamente: {}", guardada);
redirectAttributes.addFlashAttribute("success", "Configuración guardada correctamente");
redirectAttributes.addFlashAttribute("activeTab", "notificaciones");

return "redirect:/configuracion/notificaciones"; // ❌ MAL
```

**Después:**
```java
log.info("✅ Configuración de notificaciones guardada exitosamente: {}", guardada);
redirectAttributes.addFlashAttribute("success", "Configuración guardada correctamente");
redirectAttributes.addFlashAttribute("activeTab", "notificaciones");

return "redirect:/configuracion?tab=notificaciones"; // ✅ CORRECTO
```

---

## 🎯 LECCIÓN APRENDIDA

### Problema Común: POST-REDIRECT-GET Pattern

Cuando usas **fragments de Thymeleaf** que dependen de **múltiples objetos en el modelo**, el redirect después de un POST debe ir a un endpoint que:

1. ✅ **Cargue TODOS los objetos** necesarios para la vista completa
2. ✅ **Use parámetros GET** para indicar qué fragment activar
3. ❌ **NO redirija a un endpoint específico** que solo carga un subset de datos

### Regla General:

```java
// ❌ EVITAR: Redirect a endpoint específico
return "redirect:/configuracion/notificaciones";

// ✅ PREFERIR: Redirect a endpoint principal con parámetro
return "redirect:/configuracion?tab=notificaciones";
```

---

## 🧪 TESTING

### Para verificar el fix:

1. **Acceder a configuración:**
   ```
   http://localhost:8080/configuracion?tab=notificaciones
   ```

2. **Modificar valores:**
   - Cambiar "Días de recordatorio preventivo" a 5
   - Activar "Notificar nuevo cliente"
   - Ingresar email de administrador

3. **Guardar configuración:**
   - Click en "Guardar Configuración"
   - **Debería:**
     * ✅ Mostrar mensaje "Configuración guardada correctamente"
     * ✅ Permanecer en tab "Notificaciones"
     * ✅ Mostrar los valores guardados
     * ✅ NO mostrar error de "bean name 'empresa'"

4. **Verificar otros tabs:**
   - Click en tab "Empresa" → Debe cargar sin errores
   - Click en tab "Facturación" → Debe cargar sin errores
   - Volver a "Notificaciones" → Debe mantener valores guardados

---

## 📊 ARCHIVOS AFECTADOS

- ✅ **ConfiguracionController.java** - 2 líneas modificadas
  - Línea ~480: Redirect al tener errores de validación
  - Línea ~507: Redirect después de guardar exitosamente

---

## ✅ RESULTADO

**Estado:** ✅ CORREGIDO

La aplicación ahora:
- ✅ Guarda configuración correctamente
- ✅ Redirect funciona sin errores
- ✅ Todos los fragments cargan sus datos
- ✅ Tab "Notificaciones" mantiene el foco
- ✅ Mensajes flash se muestran correctamente

---

## 🔗 FIXES RELACIONADOS

Este fix complementa:
- **FIX_CONFIGURACION_NOTIFICACIONES_BEAN.md** - Agregó `configuracionNotif` al método `index()`
- **FIX_QUERY_FACTURAS_VENCIDAS.md** - Corrigió query de recordatorios

Todos juntos permiten que el módulo de notificaciones funcione correctamente.

---

**Documento generado:** 13 de octubre de 2025  
**Versión:** 1.0
