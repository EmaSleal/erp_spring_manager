## Flujo de Operación en Edición

### 1. Carga Inicial

Cuando el usuario accede a `/facturas/editar/{id}`:

```javascript
// DOMContentLoaded
const path = window.location.pathname;  // "/facturas/editar/5"
if (path.includes('/editar/')) {
    facturaId = path.split("/").pop();  // facturaId = "5"
}

// Cargar productos
fetch(`/productos/records`).then(data => {
    allProductos = data;
    if (facturaId) {
        cargarLineas();  // ← Carga líneas existentes
    }
});

function cargarLineas() {
    fetch(`/lineas-factura/detalle/${facturaId}`)
        .then(data => {
            // Inyectar filas en tabla
            data.forEach(linea => {
                tableBody.innerHTML += createLineaRow(linea);
            });
            // ✅ Generar cards para móvil
            actualizarVistaLineas();
        });
}
```

**Resultado**: 
- Tabla y cards muestran las líneas existentes
- Usuario puede editar, agregar o eliminar líneas
- En móvil, ve cards; en desktop, ve tabla

---

### 2. Editar Línea

Usuario cambia cantidad o producto (igual en ambas vistas):

```javascript
// En desktop (tabla)
<input name="cantidad" oninput="actualizarProductoSeleccionado(this)">

// En móvil (card)
<input name="cantidad" oninput="actualizarProductoSeleccionado(this)">

// La función maneja ambos casos
function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);  // Encuentra fila desde cualquier vista
    // ... actualiza tabla (fuente de verdad) ...
    sincronizarCardLinea(row);  // ← Actualiza SOLO ese card
    actualizarResumenTotales();
}
```

---

### 3. Agregar Línea

Usuario presiona "Agregar Producto":

```javascript
function addLinea() {
    const linea = { ... };  // Objeto temporal
    
    // Insertar en tabla
    tableBody.insertAdjacentHTML("beforeend", createLineaRow(linea));
    
    // Insertar card en móvil
    const cardsContainer = document.getElementById("lineas-cards-container");
    if (cardsContainer) {
        cardsContainer.insertAdjacentHTML("beforeend", createLineaCard(linea));
    }
}
```

---

### 4. Eliminar Línea

Usuario presiona botón de eliminar (en cualquier vista):

```javascript
function removeLinea(button) {
    const row = obtenerFilaLinea(button);  // Funciona desde tabla o card
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    
    // Eliminar de tabla
    row.remove();
    
    // Eliminar SOLO ese card (no reconstruir todos)
    if (idLinea) {
        const card = obtenerCardLineaPorId(idLinea);
        if (card) card.remove();
    }
    
    actualizarResumenTotales();
}
```

---

### 5. Guardar Cambios

Usuario presiona "Guardar Cambios":

```javascript
function guardarLineas() {
    // Recolectar datos DESDE LA TABLA (fuente de verdad)
    const rows = document.querySelectorAll("#lineas-body tr");
    const lineas = [];
    
    rows.forEach(row => {
        const idLinea = row.querySelector('input[name="idLinea"]').value;
        const idProducto = row.querySelector('input[name="idProducto"]').value;
        const cantidad = row.querySelector('input[name="cantidad"]').value;
        // ... más datos ...
        
        lineas.push({
            id_factura: parseInt(facturaId),
            id_linea_factura: parseInt(idLinea),  // ← ID real (> 0), no temporal
            id_producto: parseInt(idProducto),
            cantidad: parseInt(cantidad),
            // ...
        });
    });
    
    // PUT para actualizar líneas
    fetch('/lineas-factura/actualizar', {
        method: 'PUT',
        body: JSON.stringify(lineas)
    }).then(res => {
        if (res.ok) {
            // ✅ Guardar estado de entrega
            fetch(`/facturas/actualizar-estado/${facturaId}?entregado=${estadoEntregado}`, {
                method: 'PUT'
            }).then(() => {
                Swal.fire({ icon: 'success', text: 'Factura actualizada' });
                location.reload();
            });
        }
    });
}
```

---

