## 💻 Cambios Técnicos

### Archivos Modificados

| Archivo | Cambios | Líneas |
|---------|---------|--------|
| `add-form.html` | Agregar campos serie, número, fecha pago, resumen | ~80 |
| `facturas.html` | Agregar columna en tabla + campos en modal | ~30 |
| `editar-factura.js` | Función calcular fecha pago, actualizar totales | ~100 |
| `facturas.js` | Mostrar nuevos campos en modal de detalle | ~15 |
| `FacturaServiceImpl.java` | Solo generar si no viene del formulario | ~20 |

**Total:** ~245 líneas modificadas/agregadas

---

### 1. HTML - Formulario (add-form.html)

**Cambio 1: Nueva estructura de filas**

```html
<!-- ANTES: 2 filas simples -->
<div class="row">
  <div class="col-md-6">Cliente</div>
  <div class="col-md-6">Fecha Entrega</div>
</div>

<!-- DESPUÉS: 4 filas organizadas -->
<!-- Fila 1: Cliente + Tipo Factura -->
<div class="row">
  <div class="col-md-6">Cliente</div>
  <div class="col-md-6">Tipo Factura</div>
</div>

<!-- Fila 2: Serie + Número ⭐ NUEVO -->
<div class="row">
  <div class="col-md-3">
    <input id="serie" placeholder="Ej: F001" maxlength="10">
    <small>Opcional</small>
  </div>
  <div class="col-md-9">
    <input id="numeroFactura" placeholder="Ej: 001-2025-00123" maxlength="50">
    <small>Se generará automáticamente si no se especifica</small>
  </div>
</div>

<!-- Fila 3: Fecha Entrega + Fecha Pago ⭐ NUEVO -->
<div class="row">
  <div class="col-md-6">
    <input type="date" id="fechaEntrega" onchange="calcularFechaPago()">
  </div>
  <div class="col-md-6">
    <input type="date" id="fechaPago">
    <small>Se calcula automáticamente (+7 días desde entrega)</small>
  </div>
</div>
```

**Cambio 2: Resumen de Totales (Paso 2)**

```html
<!-- ⭐ NUEVO componente -->
<div class="card mt-4 border-primary">
  <div class="card-body">
    <h6 class="card-title text-primary">
      <i class="fas fa-calculator me-2"></i>Resumen de Factura
    </h6>
    <div class="row">
      <div class="col-md-4">
        <span>Subtotal:</span>
        <strong id="resumen-subtotal">$0.00</strong>
      </div>
      <div class="col-md-4">
        <span>IGV (0%):</span>
        <strong id="resumen-igv">$0.00</strong>
      </div>
      <div class="col-md-4">
        <span class="h5">Total:</span>
        <strong class="h5 text-success" id="resumen-total">$0.00</strong>
      </div>
    </div>
  </div>
</div>
```

---

### 2. JavaScript - Cálculos Automáticos (editar-factura.js)

**Función 1: Calcular Fecha de Pago**

```javascript
function calcularFechaPago() {
    const fechaEntrega = document.getElementById('fechaEntrega');
    const fechaPago = document.getElementById('fechaPago');
    
    if (fechaEntrega && fechaEntrega.value && fechaPago) {
        // Convertir fecha de entrega a objeto Date
        const entrega = new Date(fechaEntrega.value + 'T00:00:00');
        
        // Agregar 7 días
        entrega.setDate(entrega.getDate() + 7);
        
        // Formatear a YYYY-MM-DD
        const year = entrega.getFullYear();
        const month = String(entrega.getMonth() + 1).padStart(2, '0');
        const day = String(entrega.getDate()).padStart(2, '0');
        
        fechaPago.value = `${year}-${month}-${day}`;
    }
}
```

**Función 2: Actualizar Resumen de Totales**

```javascript
function actualizarResumenTotales() {
    const rows = document.querySelectorAll("#lineas-body tr");
    let subtotal = 0;
    
    // Sumar todos los subtotales de las líneas
    rows.forEach(row => {
        const subtotalInput = row.querySelector('input[name="subtotal"]');
        if (subtotalInput && subtotalInput.value) {
            subtotal += parseFloat(subtotalInput.value) || 0;
        }
    });
    
    // IGV es 0% por ahora
    const igv = 0;
    const total = subtotal + igv;
    
    // Actualizar UI
    document.getElementById('resumen-subtotal').textContent = `$${subtotal.toFixed(2)}`;
    document.getElementById('resumen-igv').textContent = `$${igv.toFixed(2)}`;
    document.getElementById('resumen-total').textContent = `$${total.toFixed(2)}`;
}
```

**Integración: Llamar actualizarResumenTotales() en:**

```javascript
// Al actualizar producto seleccionado
function actualizarProductoSeleccionado(element) {
    // ... código existente ...
    actualizarResumenTotales(); // ⭐ AGREGAR
}

// Al eliminar línea
function removeLinea(button) {
    button.closest("tr").remove();
    actualizarResumenTotales(); // ⭐ AGREGAR
}

// Al resetear formulario
function resetForm() {
    // ... código existente ...
    actualizarResumenTotales(); // ⭐ AGREGAR
}
```

**Actualización: Enviar nuevos campos al backend**

```javascript
function mostrarPaso2() {
    // ⭐ NUEVO: Obtener campos adicionales
    const serie = document.getElementById("serie");
    const numeroFactura = document.getElementById("numeroFactura");
    const fechaPago = document.getElementById("fechaPago");

    // Construir objeto factura con nuevos campos
    const factura = {
        cliente: { idCliente: parseInt(selectCliente.value) },
        fechaEntrega: fechaEntrega.value,
        fechaPago: fechaPago.value || null,        // ⭐ NUEVO
        serie: serie.value || null,                // ⭐ NUEVO
        numeroFactura: numeroFactura.value || null,// ⭐ NUEVO
        descripcion: descripcion.value,
        tipoFactura: tipoFactura.value,
        entregado: entregado.checked
    };
    
    // Enviar al backend...
}
```

---

### 3. Java - Servicio (FacturaServiceImpl.java)

**Cambio: Generación condicional**

```java
@Override
@Transactional
public Factura save(Factura factura) {
    log.debug("Guardando nueva factura");
    
    ConfiguracionFacturacion config = configuracionFacturacionService.getOrCreateConfiguracion();
    
    // ✅ MODIFICADO: Solo generar si no viene del formulario
    if (factura.getNumeroFactura() == null || factura.getNumeroFactura().trim().isEmpty()) {
        String numeroFactura = config.generarNumeroFactura();
        factura.setNumeroFactura(numeroFactura);
        log.info("Número de factura generado automáticamente: {}", numeroFactura);
    } else {
        log.info("Número de factura proporcionado manualmente: {}", factura.getNumeroFactura());
    }
    
    // ✅ MODIFICADO: Solo generar serie si no viene del formulario
    if (factura.getSerie() == null || factura.getSerie().trim().isEmpty()) {
        factura.setSerie(config.getSerieFactura());
        log.info("Serie generada automáticamente: {}", config.getSerieFactura());
    } else {
        log.info("Serie proporcionada manualmente: {}", factura.getSerie());
    }
    
    // Continúa con el guardado...
}
```

**Comportamiento:**
- ✅ Si el usuario ingresa serie/número → Se respeta
- ✅ Si el usuario deja vacío → Se auto-genera
- ✅ Logs informativos para tracking

---

### 4. HTML - Vista de Listado (facturas.html)

**Cambio: Nueva columna en tabla**

```html
<thead>
  <tr>
    <th>ID</th>
    <th>N° Factura</th> <!-- ⭐ NUEVA COLUMNA -->
    <th>Cliente</th>
    <th>Total</th>
    <th>Estado</th>
    <th>Fecha Entrega</th>
    <th>Acciones</th>
  </tr>
</thead>
<tbody>
  <tr th:each="factura : ${facturas}">
    <td th:text="${factura.idFactura}">1</td>
    
    <!-- ⭐ NUEVA CELDA -->
    <td>
      <div class="fw-semibold text-primary" 
           th:text="${factura.numeroFactura}">FA01-00001</div>
      <small class="text-muted" th:if="${factura.serie != null}">
        Serie: <span th:text="${factura.serie}">FA01</span>
      </small>
    </td>
    
    <td th:text="${factura.cliente.nombre}">Cliente</td>
    <!-- ... resto de columnas ... -->
  </tr>
</tbody>
```

**Cambio: Modal de detalle**

```html
<!-- Información General -->
<div class="list-group-item">
  <strong><i class="fas fa-file-invoice text-primary me-1"></i>N° Factura:</strong>
  <span id="modal-numeroFactura" class="fw-bold">-</span>
</div>
<div class="list-group-item">
  <strong><i class="fas fa-hashtag text-primary me-1"></i>Serie:</strong>
  <span id="modal-serie">-</span>
</div>
<!-- ... -->
<div class="list-group-item">
  <strong><i class="fas fa-money-check-alt text-primary me-1"></i>Fecha Límite de Pago:</strong>
  <span id="modal-fechaPago">-</span>
</div>
```

---

### 5. JavaScript - Modal (facturas.js)

```javascript
fetch(`/facturas/detalle/${facturaId}`)
    .then(response => response.json())
    .then(data => {
        // ... código existente ...
        
        // ✅ NUEVO: Mostrar número de factura y serie
        document.getElementById("modal-numeroFactura").innerText = 
            data.numeroFactura || 'N/A';
        document.getElementById("modal-serie").innerText = 
            data.serie || 'N/A';
        
        // ✅ NUEVO: Mostrar fecha de pago
        document.getElementById("modal-fechaPago").innerText = 
            data.fechaPago || 'No especificada';
        
        // ... resto del código ...
    });
```

---

