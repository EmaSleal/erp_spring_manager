## 📋 Descripción del Problema

Al intentar guardar la configuración de **facturación** y **empresa** en `/configuracion`, se producía un error 500:

```
POST http://localhost:8080/api/configuracion/facturacion 500 (Internal Server Error)
POST http://localhost:8080/api/configuracion/empresa 500 (Internal Server Error)
```

**Console Error:**
```javascript
Error en POST: Error: HTTP error! status: 500
❌ Error guardando configuración: Error: HTTP error! status: 500
```

**Afecta a:**
- ❌ Configuración de Facturación (`configuracion-facturacion.js`)
- ❌ Configuración de Empresa (`configuracion-empresa.js`)

---

