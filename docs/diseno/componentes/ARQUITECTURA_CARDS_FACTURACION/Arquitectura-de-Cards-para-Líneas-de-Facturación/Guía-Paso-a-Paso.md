## Guía Paso a Paso

### Cómo crear esta arquitectura desde cero

#### Paso 1: Definir estructura HTML

```html
<div class="table-responsive d-none d-md-block">
    <table class="table">
        <tbody id="lineas-body"></tbody>
    </table>
</div>

<div class="d-md-none" id="lineas-cards-container"></div>

<button onclick="addLinea()">Agregar</button>
```

#### Paso 2: Crear funciones de construcción

```javascript
// Retorna HTML de fila
function createLineaRow(linea) {
    return `
    <tr>
        <input type="hidden" name="idLinea" value="${linea.id_linea_factura}">
        <select onchange="actualizarProductoSeleccionado(this)">...</select>
        <input oninput="actualizarProductoSeleccionado(this)">
        <button onclick="removeLinea(this)">Eliminar</button>
    </tr>
    `;
}

// Retorna HTML de card
function createLineaCard(linea) {
    return `
    <div class="card mb-3">
        <input type="hidden" name="idLinea" value="${linea.id_linea_factura}">
        <select onchange="actualizarProductoSeleccionado(this)">...</select>
        <input oninput="actualizarProductoSeleccionado(this)">
        <button onclick="removeLinea(this)">Eliminar</button>
    </div>
    `;
}
```

#### Paso 3: Crear funciones de localización

```javascript
function obtenerFilaLinea(element) {
    const row = element.closest("tr");
    if (row) return row;
    
    const card = element.closest(".card");
    if (!card) return null;
    
    const idLinea = card.querySelector('input[name="idLinea"]')?.value;
    return Array.from(document.querySelectorAll("#lineas-body tr"))
        .find(r => r.querySelector('input[name="idLinea"]')?.value == idLinea);
}

function obtenerCardLineaPorId(idLinea) {
    return Array.from(document.querySelectorAll(".card"))
        .find(c => c.querySelector('input[name="idLinea"]')?.value == idLinea);
}
```

#### Paso 4: Implementar operaciones

```javascript
// AGREGAR
function addLinea() {
    const fila = createLineaRow({...});
    document.getElementById("lineas-body").insertAdjacentHTML("beforeend", fila);
    
    const card = createLineaCard({...});
    document.getElementById("lineas-cards-container").insertAdjacentHTML("beforeend", card);
}

// EDITAR (actualización incremental)
function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);
    // ... actualiza tabla ...
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    const card = obtenerCardLineaPorId(idLinea);
    if (card) {
        card.querySelector('input[name="precio"]').value = nuevoValor;  // Solo este card
    }
}

// ELIMINAR (sin reconstrucción completa)
function removeLinea(button) {
    const row = obtenerFilaLinea(button);
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    
    row.remove();
    
    const card = obtenerCardLineaPorId(idLinea);
    if (card) card.remove();  // Solo este card
}
```

---

