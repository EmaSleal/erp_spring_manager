## 📋 PROBLEMA

Al hacer clic en el tab "Notificaciones" en `/configuracion?tab=notificaciones`, se producía el siguiente error:

```
java.lang.IllegalStateException: Neither BindingResult nor plain target object 
for bean name 'configuracionNotif' available as request attribute
```

**Causa Raíz:**
El método `index()` del `ConfiguracionController` NO estaba pasando el objeto `configuracionNotif` al modelo cuando se cargaba la página principal de configuración.

---

