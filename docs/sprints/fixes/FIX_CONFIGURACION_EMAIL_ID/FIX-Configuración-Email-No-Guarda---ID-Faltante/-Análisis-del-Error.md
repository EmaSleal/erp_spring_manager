## 🔍 Análisis del Error

### Flujo Problemático

```
Usuario modifica configuración
    ↓
JavaScript recolecta datos del formulario
    ↓
❌ NO incluye campo 'idConfiguracion'
    ↓
Envía POST a /api/configuracion/email
    ↓
Backend recibe DTO sin ID
    ↓
configuracion.getIdConfiguracion() → null
    ↓
Backend llama a guardarConfiguracion() (CREATE)
    ↓
Intenta crear nueva configuración
    ↓
⚠️ Puede fallar o crear duplicado
```

### Causa Raíz

El **JavaScript no guardaba ni enviaba el ID** de la configuración al actualizar:

**ANTES - configuracion-email.js:**
```javascript
cargarConfiguracion: async function() {
    const datos = response.data;
    
    // ❌ No guardaba el ID
    document.getElementById('smtp-host').value = datos.smtpHost || '';
    // ... otros campos
}

guardarConfiguracion: async function() {
    const datos = {
        smtpHost: document.getElementById('smtp-host').value,
        // ... otros campos
        // ❌ No incluía idConfiguracion
    };
    
    // ❌ Siempre POST (crear)
    const response = await Configuracion.post(this.API_URL, datos);
}
```

### Backend Esperado

**ConfiguracionEmailRestController.java:**
```java
@PostMapping
public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionEmailDTO dto) {
    ConfiguracionEmail configuracion = convertirAEntidad(dto);
    
    if (configuracion.getIdConfiguracion() != null && configuracion.getIdConfiguracion() > 0) {
        guardada = actualizarConfiguracion(configuracion);  // UPDATE
    } else {
        guardada = guardarConfiguracion(configuracion);     // CREATE
    }
}
```

**Problema:** `dto.getIdConfiguracion()` siempre era `null` porque el frontend no lo enviaba.

---

