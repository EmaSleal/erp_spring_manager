## 🔍 CAUSA RAÍZ

El método `guardarNotificaciones()` hacía redirect a:
```java
return "redirect:/configuracion/notificaciones";
```

Pero ese endpoint (`/configuracion/notificaciones`) es un método **separado** que solo carga datos de notificaciones y retorna `"configuracion/index"`.

Sin embargo, `configuracion/index.html` carga **TODOS los fragments** (empresa, facturacion, notificaciones), y cada fragment necesita su objeto en el modelo.

El redirect debería ir a `/configuracion?tab=notificaciones` que es el método `index()` principal que carga **TODOS** los objetos necesarios.

---

