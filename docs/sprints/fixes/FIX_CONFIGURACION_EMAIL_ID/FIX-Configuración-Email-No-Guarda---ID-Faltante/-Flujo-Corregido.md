## 📊 Flujo Corregido

### Crear Nueva Configuración

```
1. Usuario abre /configuracion (primera vez)
2. cargarConfiguracion() → sin datos
3. this.idConfiguracion = null
4. Usuario llena formulario
5. guardarConfiguracion()
   ├─ datos.idConfiguracion NO se agrega (null)
   ├─ Usa POST (crear)
   └─ Backend: guardarConfiguracion() [CREATE]
6. Éxito → recarga datos
7. this.idConfiguracion = 1 (guardado)
```

### Actualizar Configuración Existente

```
1. Usuario abre /configuracion (con datos)
2. cargarConfiguracion()
   └─ this.idConfiguracion = 1 ✅
3. Usuario modifica campos
4. guardarConfiguracion()
   ├─ datos.idConfiguracion = 1 ✅
   ├─ Usa PUT (actualizar)
   └─ Backend: actualizarConfiguracion() [UPDATE]
5. Éxito → cambios persisten ✅
```

---

