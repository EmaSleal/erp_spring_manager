# 🚀 GUÍA RÁPIDA - JavaScript de Configuración

**Sprint 3 - Módulos de Gestión Avanzada**

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
src/main/resources/static/js/
├── configuracion.js                  # 474 líneas - Utilidades base
├── configuracion-empresa.js          # 164 líneas - Gestión empresa
├── configuracion-facturacion.js      # 152 líneas - Gestión facturación
├── configuracion-email.js            # 310 líneas - Gestión SMTP + pruebas
└── configuracion-parametros.js       # 464 líneas - CRUD parámetros
```

---

## 🔑 NAMESPACES

```javascript
Configuracion               // Base utilities
ConfiguracionEmpresa        // Módulo empresa
ConfiguracionFacturacion    // Módulo facturación
ConfiguracionEmail          // Módulo email
ConfiguracionParametros     // Módulo parámetros
```

---

## 📞 APIS DISPONIBLES

### Configuracion (Base)

```javascript
// ALERTAS
Configuracion.mostrarExito('Mensaje')
Configuracion.mostrarError('Mensaje')
Configuracion.mostrarAdvertencia('Mensaje')
Configuracion.mostrarInfo('Mensaje')
Configuracion.mostrarAlertaEn('alert-container-id', 'success', 'Mensaje')
Configuracion.limpiarAlertas('alert-container-id')

// FORMULARIOS
Configuracion.validarFormulario('form-id')              // Returns boolean
Configuracion.obtenerDatosFormulario('form-id')         // Returns object
Configuracion.cargarDatosEnFormulario('form-id', datos) // Void
Configuracion.limpiarFormulario('form-id')              // Void

// REST API
await Configuracion.get('/api/endpoint')
await Configuracion.post('/api/endpoint', datos)
await Configuracion.put('/api/endpoint', datos)
await Configuracion.patch('/api/endpoint', datos)
await Configuracion.delete('/api/endpoint')

// UI
Configuracion.mostrarSpinner('btn-id')
Configuracion.ocultarSpinner('btn-id')

// UTILIDADES
await Configuracion.confirmar('Título', 'Texto', 'Texto Botón')
Configuracion.formatearFecha(new Date())               // dd/mm/yyyy
Configuracion.formatearFechaHora(new Date())           // dd/mm/yyyy hh:mm
Configuracion.sincronizarColorPicker('color-id', 'text-id')
```

---

## 🏢 ConfiguracionEmpresa

### Inicialización
```javascript
ConfiguracionEmpresa.init()  // Se ejecuta automáticamente al entrar al tab
```

### Métodos Públicos
```javascript
await ConfiguracionEmpresa.cargarConfiguracion()
await ConfiguracionEmpresa.guardarConfiguracion()
```

### Campos del Formulario
```html
<!-- Legal/Fiscal -->
<input id="razonSocial">
<input id="nombreComercial">
<input id="rfc">
<input id="regimenFiscal">

<!-- Dirección -->
<input id="direccionCalle">
<input id="direccionCiudad">
<input id="direccionEstado">
<input id="direccionCodigoPostal">
<input id="direccionPais">

<!-- Contacto -->
<input id="telefono">
<input id="email">
<input id="sitioWeb">

<!-- Branding -->
<input id="logoUrl">
<input id="faviconUrl">
<input type="color" id="colorPrimario">
<input type="text" id="colorPrimarioText">
<input type="color" id="colorSecundario">
<input type="text" id="colorSecundarioText">
```

### Endpoints Usados
```
GET    /api/configuracion/empresa
POST   /api/configuracion/empresa
PUT    /api/configuracion/empresa
```

---

## 💰 ConfiguracionFacturacion

### Inicialización
```javascript
ConfiguracionFacturacion.init()
```

### Métodos Públicos
```javascript
await ConfiguracionFacturacion.cargarConfiguracion()
await ConfiguracionFacturacion.guardarConfiguracion()
```

### Campos del Formulario
```html
<!-- Numeración -->
<input id="serieFactura">
<input id="prefijoFactura">
<input type="number" id="numeroInicial">
<input type="number" id="numeroActual">
<input id="formatoNumero">

<!-- Impuestos -->
<input type="number" step="0.01" id="igv">
<input type="checkbox" id="incluirIgvEnPrecio">

<!-- Moneda -->
<input id="moneda">
<input id="simboloMoneda">
<input type="number" id="decimales">

<!-- Adicional -->
<textarea id="terminosCondiciones"></textarea>
<textarea id="notaPiePagina"></textarea>
<input type="checkbox" id="activo">
```

### Endpoints Usados
```
GET    /api/configuracion/facturacion
POST   /api/configuracion/facturacion
PUT    /api/configuracion/facturacion
```

---

## 📧 ConfiguracionEmail

### Inicialización
```javascript
ConfiguracionEmail.init()
```

### Métodos Públicos
```javascript
await ConfiguracionEmail.cargarConfiguracion()
await ConfiguracionEmail.guardarConfiguracion()
await ConfiguracionEmail.validarConfiguracion()
ConfiguracionEmail.mostrarModalPrueba()
await ConfiguracionEmail.enviarEmailPrueba()
```

### Campos del Formulario
```html
<!-- Servidor SMTP -->
<input id="host">
<input type="number" id="puerto">
<select id="protocolo">
  <option value="smtp">SMTP</option>
  <option value="smtps">SMTPS</option>
</select>
<input id="username">
<input type="password" id="password">
<button id="toggle-password">👁️</button>

<!-- Seguridad -->
<input type="checkbox" id="usarTls">
<input type="checkbox" id="usarSsl">
<input type="checkbox" id="autenticacionRequerida">

<!-- Remitente -->
<input type="email" id="emailRemitente">
<input id="nombreRemitente">
<input type="email" id="emailRespuesta">

<!-- Timeouts -->
<input type="number" id="timeout">
<input type="number" id="connectionTimeout">

<!-- Codificación -->
<input id="codificacion">

<!-- Plantillas -->
<textarea id="plantillaEncabezado"></textarea>
<textarea id="plantillaPiePagina"></textarea>

<!-- Estado -->
<input type="checkbox" id="activo">
<span id="ultima-prueba-exitosa"></span>
<span id="ultima-prueba-error"></span>
```

### Modal de Prueba
```html
<div id="modal-prueba-email">
  <input type="email" id="email-prueba-destino">
  <div id="alert-prueba-email"></div>
  <button id="btn-enviar-prueba">Enviar Prueba</button>
</div>
```

### Endpoints Usados
```
GET    /api/configuracion/email
POST   /api/configuracion/email
PUT    /api/configuracion/email
GET    /api/configuracion/email/validar
POST   /api/configuracion/email/probar
```

### Ejemplo de Uso
```javascript
// Enviar email de prueba
ConfiguracionEmail.mostrarModalPrueba();
document.getElementById('email-prueba-destino').value = 'test@example.com';
await ConfiguracionEmail.enviarEmailPrueba();
```

---

## ⚙️ ConfiguracionParametros

### Inicialización
```javascript
ConfiguracionParametros.init()
```

### Métodos Públicos (CRUD)
```javascript
// READ
await ConfiguracionParametros.cargarParametros()

// CREATE
ConfiguracionParametros.mostrarModalNuevo()
await ConfiguracionParametros.guardarParametro()

// UPDATE
ConfiguracionParametros.mostrarModalEditar('clave')
await ConfiguracionParametros.actualizarValor()

// DELETE
await ConfiguracionParametros.eliminarParametro('clave')

// INICIALIZAR 17 PARÁMETROS DEFAULT
await ConfiguracionParametros.inicializarParametros()
```

### Métodos de Filtrado
```javascript
ConfiguracionParametros.buscarParametros('query')
await ConfiguracionParametros.filtrarPorCategoria('WHATSAPP')
ConfiguracionParametros.filtrarPorEditable('editables')  // 'todos', 'editables', 'sistema'
```

### Métodos de UI
```javascript
ConfiguracionParametros.renderizarTabla()
ConfiguracionParametros.actualizarEstadisticas()
ConfiguracionParametros.formatearValor(valor, tipo)
ConfiguracionParametros.getBadgeTipo(tipo)
```

### Estructura de Parámetro
```javascript
{
  clave: 'NOMBRE_PARAMETRO',
  valor: 'valor_como_string',
  tipoDato: 'TEXT' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN',
  descripcion: 'Descripción del parámetro',
  categoria: 'GENERAL' | 'WHATSAPP' | 'FACTURACION' | 'NOTIFICACIONES',
  editable: true | false
}
```

### Campos del Formulario (Modal Crear)
```html
<input id="parametro-clave">
<input id="parametro-valor">
<select id="parametro-tipo">
  <option value="TEXT">Texto</option>
  <option value="INTEGER">Número Entero</option>
  <option value="DECIMAL">Número Decimal</option>
  <option value="BOOLEAN">Booleano</option>
</select>
<textarea id="parametro-descripcion"></textarea>
<select id="parametro-categoria">
  <option value="GENERAL">General</option>
  <option value="WHATSAPP">WhatsApp</option>
  <option value="FACTURACION">Facturación</option>
  <option value="NOTIFICACIONES">Notificaciones</option>
</select>
<input type="checkbox" id="parametro-editable">
```

### Modal de Edición Rápida
```html
<div id="modal-editar-valor">
  <input id="editar-clave" readonly>
  <span id="editar-valor-actual"></span>
  <span id="editar-tipo-dato"></span>
  <!-- Input dinámico según tipo -->
  <input id="editar-valor-nuevo">
</div>
```

### Filtros
```html
<!-- Búsqueda en tiempo real -->
<input id="buscar-parametro" placeholder="Buscar...">

<!-- Categoría -->
<select id="filtro-categoria">
  <option value="">Todas</option>
  <option value="GENERAL">General</option>
  <option value="WHATSAPP">WhatsApp</option>
  <option value="FACTURACION">Facturación</option>
  <option value="NOTIFICACIONES">Notificaciones</option>
</select>

<!-- Tipo (Radio buttons) -->
<input type="radio" name="filtro-editable" value="todos" checked>
<input type="radio" name="filtro-editable" value="editables">
<input type="radio" name="filtro-editable" value="sistema">
```

### Estadísticas
```html
<span id="stat-total">0</span>
<span id="stat-editables">0</span>
<span id="stat-sistema">0</span>
```

### Tabla de Parámetros
```html
<table>
  <tbody id="tabla-parametros">
    <!-- Generado dinámicamente -->
  </tbody>
</table>
```

### Endpoints Usados
```
GET    /api/configuracion/parametros
GET    /api/configuracion/parametros/categoria/{categoria}
POST   /api/configuracion/parametros
POST   /api/configuracion/parametros/inicializar
PATCH  /api/configuracion/parametros/{clave}
DELETE /api/configuracion/parametros/{clave}
```

### Ejemplo de Uso
```javascript
// Inicializar parámetros por defecto
await ConfiguracionParametros.inicializarParametros();

// Buscar parámetros
ConfiguracionParametros.buscarParametros('whatsapp');

// Filtrar por categoría
await ConfiguracionParametros.filtrarPorCategoria('FACTURACION');

// Crear nuevo parámetro
ConfiguracionParametros.mostrarModalNuevo();
document.getElementById('parametro-clave').value = 'MI_PARAMETRO';
document.getElementById('parametro-valor').value = 'valor';
document.getElementById('parametro-tipo').value = 'TEXT';
await ConfiguracionParametros.guardarParametro();

// Editar valor
ConfiguracionParametros.mostrarModalEditar('MI_PARAMETRO');
document.getElementById('editar-valor-nuevo').value = 'nuevo_valor';
await ConfiguracionParametros.actualizarValor();

// Eliminar
await ConfiguracionParametros.eliminarParametro('MI_PARAMETRO');
```

---

## 🎯 EVENTOS BOOTSTRAP TABS

Todos los módulos escuchan el evento `shown.bs.tab`:

```javascript
document.addEventListener('shown.bs.tab', function (event) {
    switch(event.target.id) {
        case 'empresa-tab':
            ConfiguracionEmpresa.init();
            break;
        case 'facturacion-tab':
            ConfiguracionFacturacion.init();
            break;
        case 'email-tab':
            ConfiguracionEmail.init();
            break;
        case 'parametros-tab':
            ConfiguracionParametros.init();
            break;
    }
});
```

**Ventaja**: Solo se cargan datos cuando el usuario entra al tab (lazy loading).

---

## 🔄 FLUJO TÍPICO DE OPERACIÓN

### Cargar Datos
```javascript
1. Usuario entra al tab
2. Bootstrap dispara evento 'shown.bs.tab'
3. Módulo.init() se ejecuta
4. Módulo.cargarConfiguracion() se ejecuta
5. GET request al backend
6. Datos cargan en el formulario
```

### Guardar Datos
```javascript
1. Usuario completa formulario
2. Click en "Guardar"
3. Form submit event
4. Configuracion.validarFormulario() → HTML5 validation
5. Módulo.obtenerDatosFormulario() → Extrae datos
6. POST/PUT request al backend
7. Backend responde con success/error
8. Mostrar alert
9. Recargar datos
```

### Filtrar Parámetros
```javascript
1. Usuario escribe en búsqueda
2. Input event se dispara
3. ConfiguracionParametros.buscarParametros()
4. Filtra array local this.parametros
5. Actualiza this.parametrosFiltrados
6. Renderiza tabla con datos filtrados
```

---

## 🎨 BADGES Y COLORES

### Tipos de Dato
```css
TEXT    → bg-info (azul claro)
INTEGER → bg-primary (azul)
DECIMAL → bg-success (verde)
BOOLEAN → bg-warning text-dark (amarillo)
```

### Categorías
```css
GENERAL         → bg-secondary (gris)
WHATSAPP        → bg-secondary (gris)
FACTURACION     → bg-secondary (gris)
NOTIFICACIONES  → bg-secondary (gris)
```

### Editable
```html
Editable → <i class="fas fa-check text-success"></i>
Sistema  → <i class="fas fa-lock text-warning"></i>
```

---

## 🛠️ DEBUGGING

### Console Logs
Todos los módulos usan console logging:

```javascript
console.log('✅ Módulo X inicializado');
console.log('✅ N parámetros cargados');
console.error('❌ Error:', error);
```

### Browser DevTools
```javascript
// En la consola del navegador puedes acceder a:
Configuracion
ConfiguracionEmpresa
ConfiguracionFacturacion
ConfiguracionEmail
ConfiguracionParametros

// Ejemplo:
await ConfiguracionParametros.cargarParametros();
ConfiguracionParametros.parametros  // Ver array de parámetros
```

---

## ⚠️ ERRORES COMUNES

### 1. "Configuracion is not defined"
**Causa**: configuracion.js no se cargó  
**Solución**: Verificar que index.html tiene:
```html
<script src="/js/configuracion.js"></script>
```

### 2. "Cannot read property 'value' of null"
**Causa**: ID de elemento HTML no coincide  
**Solución**: Verificar IDs en HTML vs JavaScript

### 3. "Bootstrap Modal no se muestra"
**Causa**: Bootstrap JS no cargado o versión incorrecta  
**Solución**: Verificar:
```html
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
```

### 4. "Fetch error 404"
**Causa**: Endpoint no existe o URL incorrecta  
**Solución**: Verificar backend con Postman/curl

### 5. "Validation failed"
**Causa**: Campos requeridos vacíos  
**Solución**: Verificar atributo `required` en inputs

---

## 📋 CHECKLIST DE INTEGRACIÓN

Al agregar un nuevo módulo de configuración:

- [ ] Crear archivo JS en `/static/js/`
- [ ] Definir namespace único
- [ ] Implementar `init()` method
- [ ] Implementar `configurarEventos()`
- [ ] Implementar `cargar()` method → GET
- [ ] Implementar `guardar()` method → POST/PUT
- [ ] Agregar listener de `shown.bs.tab`
- [ ] Agregar script tag en HTML
- [ ] Usar `Configuracion.*` para alerts/forms/API
- [ ] Probar validación de formularios
- [ ] Probar guardar/actualizar/eliminar
- [ ] Verificar console logs

---

## 🚀 EJEMPLO COMPLETO

```javascript
// 1. Usuario entra al tab de Email
// → shown.bs.tab event → ConfiguracionEmail.init()

// 2. Cargar configuración
await ConfiguracionEmail.cargarConfiguracion()
// → GET /api/configuracion/email
// → Rellena formulario con datos

// 3. Usuario modifica campos y guarda
await ConfiguracionEmail.guardarConfiguracion()
// → Validación HTML5
// → POST/PUT /api/configuracion/email
// → Alert de éxito/error
// → Recarga datos

// 4. Usuario prueba email
ConfiguracionEmail.mostrarModalPrueba()
// → Abre modal
document.getElementById('email-prueba-destino').value = 'test@example.com'
await ConfiguracionEmail.enviarEmailPrueba()
// → POST /api/configuracion/email/probar
// → Muestra resultado
// → Actualiza última prueba
// → Cierra modal
```

---

**Documentación actualizada**: 1 de diciembre de 2025  
**Versión**: 1.0  
**Sprint**: 3 - Fase 1
