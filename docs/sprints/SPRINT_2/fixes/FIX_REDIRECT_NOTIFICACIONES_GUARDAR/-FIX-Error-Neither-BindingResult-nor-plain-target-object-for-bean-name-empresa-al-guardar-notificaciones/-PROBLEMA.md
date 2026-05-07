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

