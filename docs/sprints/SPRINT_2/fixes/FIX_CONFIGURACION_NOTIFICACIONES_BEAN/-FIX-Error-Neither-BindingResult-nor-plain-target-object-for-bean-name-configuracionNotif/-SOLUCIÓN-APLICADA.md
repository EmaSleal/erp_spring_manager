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

