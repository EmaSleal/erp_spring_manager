## ✅ Solución Implementada

### 1. Agregar variable para almacenar ID

**Archivo:** `configuracion-email.js`

```javascript
const ConfiguracionEmail = {
    API_URL: '/api/configuracion/email',
    idConfiguracion: null,  // ✅ NUEVO: Variable para almacenar ID
    
    init: function() {
        this.cargarConfiguracion();
        this.configurarEventos();
    },
```

### 2. Guardar ID al cargar configuración

```javascript
cargarConfiguracion: async function() {
    try {
        const response = await Configuracion.get(this.API_URL);
        
        if (response.success && response.data) {
            const datos = response.data;
            
            // ✅ NUEVO: Guardar ID para actualizaciones
            this.idConfiguracion = datos.idConfiguracion || null;
            
            // Cargar campos en el formulario
            document.getElementById('smtp-host').value = datos.smtpHost || '';
            document.getElementById('smtp-port').value = datos.smtpPort || 587;
            // ... resto de campos
        }
    }
}
```

### 3. Incluir ID al guardar y usar método HTTP correcto

```javascript
guardarConfiguracion: async function() {
    try {
        // Obtener datos del formulario
        const datos = {
            smtpHost: document.getElementById('smtp-host').value,
            smtpPort: parseInt(document.getElementById('smtp-port').value),
            smtpUsuario: document.getElementById('smtp-usuario').value,
            smtpPassword: document.getElementById('smtp-password').value,
            emailRemitente: document.getElementById('email-remitente').value,
            nombreRemitente: document.getElementById('nombre-remitente').value || null,
            smtpTls: document.getElementById('smtp-tls').checked,
            smtpSsl: document.getElementById('smtp-ssl').checked,
            smtpAuth: document.getElementById('smtp-auth').checked,
            timeout: parseInt(document.getElementById('timeout')?.value) || 5000,
            charset: document.getElementById('charset')?.value || 'UTF-8',
            activo: document.getElementById('activoEmail').checked
        };
        
        // ✅ NUEVO: Agregar ID si existe (para actualizaciones)
        if (this.idConfiguracion) {
            datos.idConfiguracion = this.idConfiguracion;
        }
        
        // ✅ NUEVO: Decidir método HTTP según si hay ID
        let response;
        if (this.idConfiguracion) {
            // Actualizar con PUT
            response = await Configuracion.put(this.API_URL, datos);
        } else {
            // Crear con POST
            response = await Configuracion.post(this.API_URL, datos);
        }
        
        if (response.success) {
            Configuracion.mostrarAlertaEn('alert-email-container', 'success', 
                response.message || 'Configuración guardada exitosamente');
            
            // Recargar datos para actualizar el ID
            setTimeout(() => this.cargarConfiguracion(), 1000);
        }
    }
}
```

---

