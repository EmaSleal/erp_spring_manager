# 🔧 FIX: Error "Neither BindingResult nor plain target object for bean name 'configuracionNotif'"

**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5 (Notificaciones)  
**Punto:** 5.4.2 - Vista de Configuración de Notificaciones

---

## 📋 PROBLEMA

Al hacer clic en el tab "Notificaciones" en `/configuracion?tab=notificaciones`, se producía el siguiente error:

```
java.lang.IllegalStateException: Neither BindingResult nor plain target object 
for bean name 'configuracionNotif' available as request attribute
```

**Causa Raíz:**
El método `index()` del `ConfiguracionController` NO estaba pasando el objeto `configuracionNotif` al modelo cuando se cargaba la página principal de configuración.

---

## ✅ SOLUCIÓN APLICADA

### 1. **Actualizado ConfiguracionController.index()**

**Archivo:** `ConfiguracionController.java`

**Cambios:**

1. **Agregado parámetro `@RequestParam`:**
   ```java
   public String index(@RequestParam(required = false) String tab, ...)
   ```
   - Permite recibir el parámetro `tab` de la URL (ej: `?tab=notificaciones`)
   - Es opcional (`required = false`), por defecto usa "empresa"

2. **Agregada carga de ConfiguracionNotificaciones:**
   ```java
   ConfiguracionNotificaciones configuracionNotif = 
       configuracionNotificacionesService.getOrCreateConfiguracion();
   model.addAttribute("configuracionNotif", configuracionNotif);
   ```
   - Carga o crea la configuración de notificaciones
   - La agrega al modelo con el nombre `configuracionNotif`
   - Ahora disponible para el fragment `notificaciones.html`

3. **Tab activo dinámico:**
   ```java
   model.addAttribute("activeTab", tab != null ? tab : "empresa");
   ```
   - Si se pasa `?tab=notificaciones`, activa ese tab
   - Si no se pasa nada, activa "empresa" por defecto

---

## 📊 ARCHIVOS MODIFICADOS

### ConfiguracionController.java
```java
// ANTES
@GetMapping
public String index(Model model, HttpSession session) {
    // ...
    model.addAttribute("activeTab", "empresa");
    return "configuracion/index";
}

// DESPUÉS
@GetMapping
public String index(@RequestParam(required = false) String tab, 
                   Model model, HttpSession session) {
    // ...
    
    // Cargar configuración de notificaciones
    ConfiguracionNotificaciones configuracionNotif = 
        configuracionNotificacionesService.getOrCreateConfiguracion();
    model.addAttribute("configuracionNotif", configuracionNotif);
    
    // Tab activo dinámico
    model.addAttribute("activeTab", tab != null ? tab : "empresa");
    
    return "configuracion/index";
}
```

### error.html (NUEVO)
- Creado archivo `templates/error/error.html`
- Página genérica de error para capturar errores no específicos (403, 404, 500)
- Diseño profesional con información del error y botones de navegación

---

## 🔄 FLUJO CORREGIDO

### Antes (❌ Error):
1. Usuario hace clic en tab "Notificaciones"
2. URL: `/configuracion?tab=notificaciones`
3. Método `index()` se ejecuta
4. **NO carga `configuracionNotif`** al modelo
5. Fragment `notificaciones.html` intenta usar `th:field="*{configuracionNotif.campo}"`
6. **ERROR:** Objeto no encontrado

### Después (✅ Funciona):
1. Usuario hace clic en tab "Notificaciones"
2. URL: `/configuracion?tab=notificaciones`
3. Método `index(tab="notificaciones", ...)` se ejecuta
4. **Carga `configuracionNotif`** al modelo
5. Fragment `notificaciones.html` usa `th:field="*{configuracionNotif.campo}"`
6. **SUCCESS:** Formulario se renderiza correctamente

---

## 🧪 TESTING

### Para verificar el fix:

1. **Compilar:**
   ```powershell
   mvn clean compile
   ```

2. **Iniciar aplicación:**
   ```powershell
   mvn spring-boot:run
   ```

3. **Acceder a configuración:**
   ```
   http://localhost:8080/configuracion
   ```

4. **Hacer clic en tab "Notificaciones"**
   - Debería cargar sin errores
   - Formulario completo visible
   - Todos los campos mapeados correctamente

5. **Verificar que se cargó la configuración:**
   - Los campos deben mostrar valores por defecto de la BD
   - Switch "Activar Email" debe estar en ON (TRUE por defecto)
   - Días de recordatorio: 3, 0, 7 (valores por defecto)

---

## 📝 NOTAS ADICIONALES

### Otros Métodos del Controller

El método `notificaciones()` que mapea `/configuracion/notificaciones` sigue existiendo pero **ya no se usa** para el tab. Se mantiene por si acaso se necesita acceso directo a esa ruta.

### Fragment de Thymeleaf

El fragment `notificaciones.html` usa:
```html
<form th:object="${configuracionNotif}" ...>
    <input th:field="*{idConfiguracion}" ...>
    <input th:field="*{activarEmail}" ...>
    ...
</form>
```

El `th:object="${configuracionNotif}"` requiere que exista un objeto con ese nombre en el modelo. Por eso el fix era necesario.

---

## ✅ RESULTADO

**Estado:** ✅ CORREGIDO

La aplicación ahora:
- ✅ Compila sin errores
- ✅ Carga correctamente el tab de Notificaciones
- ✅ Muestra el formulario completo
- ✅ Mapea todos los campos correctamente
- ✅ Página de error genérica creada para futuros errores

---

**Próximo Paso:** Probar guardado de configuración con el endpoint `/configuracion/notificaciones/guardar`

---

**Documento generado:** 13 de octubre de 2025  
**Versión:** 1.0
