## ✅ Solución Implementada

### Cambios en Frontend

#### 1. Configuración de Facturación

**Archivo:** `src/main/resources/static/js/configuracion-facturacion.js`

**Línea 51 - ANTES:**
```javascript
document.getElementById('idConfiguracionFacturacion').value = datos.idConfiguracion || '';  // ❌
```

**Línea 51 - DESPUÉS:**
```javascript
document.getElementById('idConfiguracionFacturacion').value = datos.id || '';  // ✅
```

**Línea 111 - ANTES:**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Línea 111 - DESPUÉS:**
```javascript
if (idConfiguracion) {
    // Actualizar - usar 'id' en lugar de 'idConfiguracion'
    datos.id = parseInt(idConfiguracion);  // ✅
    response = await Configuracion.put(this.API_URL, datos);
}
```

---

#### 2. Configuración de Empresa

**Archivo:** `src/main/resources/static/js/configuracion-empresa.js`

**Línea 51 - ANTES:**
```javascript
document.getElementById('idConfiguracionEmpresa').value = datos.idConfiguracion || '';  // ❌
```

**Línea 51 - DESPUÉS:**
```javascript
document.getElementById('idConfiguracionEmpresa').value = datos.id || '';  // ✅
```

**Línea 125 - ANTES:**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Línea 125 - DESPUÉS:**
```javascript
if (idConfiguracion) {
    // Actualizar - usar 'id' en lugar de 'idConfiguracion'
    datos.id = parseInt(idConfiguracion);  // ✅
    response = await Configuracion.put(this.API_URL, datos);
}
```

---

