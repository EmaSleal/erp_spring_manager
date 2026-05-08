## Flujo de Operaciones

### 1. AGREGAR Línea (`addLinea()`)

```javascript
function addLinea() {
    const tableBody = document.getElementById("lineas-body");
    const randomId = temporalLineaId--;  // -1, -2, -3...
    
    // Crear objeto de datos para la fila
    const linea = {
        id_linea_factura: randomId,
        numero_linea: tableBody.querySelectorAll('tr').length + 1,
        cantidad: 1,
        precioUnitario: 0,
        subtotal: 0,
        id_producto: randomId,  // Temporal hasta que el usuario seleccione
        // ... más propiedades
    };
    
    // PASO 1: Insertar fila en la tabla (fuente de verdad)
    tableBody.insertAdjacentHTML("beforeend", createLineaRow(linea));
    
    // PASO 2: Inyectar card en el contenedor móvil
    const cardsContainer = document.getElementById("lineas-cards-container");
    if (cardsContainer) {
        cardsContainer.insertAdjacentHTML("beforeend", createLineaCard(linea));
    }
}
```

**Resultado**: Fila + Card con el mismo `id_linea_factura` vinculan ambas vistas.

---

### 2. EDITAR Campo de Línea

Cuando el usuario cambia cantidad, selecciona producto, etc., el flujo es:

```
┌────────────────────────────────────────────────────────────┐
│ Usuario cambia valor en Table/Card                          │
│ (onchange, oninput)                                          │
└─────────────────┬──────────────────────────────────────────┘
                  │
                  ▼
    ┌─────────────────────────────┐
    │ actualizarProductoSeleccionado()  │  (manejador evento)
    └─────────────────────────────┘
             │
             ├─→ 1. Obtener fila fuente (tabla o card)
             │
             ├─→ 2. Actualizar TABLA con nuevo valor
             │      (fuente de verdad)
             │
             ├─→ 3. Recalcular precio/subtotal
             │
             ├─→ 4. Llamar sincronizarCardLinea()
             │      ↓ Actualiza SOLO el card afectado
             │      ↓ sin reconstruir todos
             │
             └─→ 5. Actualizar resumen de totales
```

**Código de actualización incremental (clave)**:

```javascript
function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);  // Encuentra la fila desde cualquier vista
    if (!row) return;
    
    // Obtener valores del usuario
    const select = element.matches('select[name="producto"]') ? element : row.querySelector('select');
    const producto = allProductos.find(p => p.id_producto === parseInt(select.value));
    
    // IMPORTANTE: Actualizar LA TABLA (fuente de verdad)
    row.querySelector('select[name="producto"]').value = select.value;
    row.querySelector('input[name="precio"]').value = producto.precio_institucional;
    row.querySelector('input[name="subtotal"]').value = (producto.precio_institucional * cantidad).toFixed(2);
    
    // Sincronizar SOLO el card afectado (no reconstruir todos)
    sincronizarCardLinea(row);
    
    // Actualizar totales
    actualizarResumenTotales();
}
```

---

### 3. ELIMINAR Línea (`removeLinea()`)

```javascript
function removeLinea(button) {
    const row = obtenerFilaLinea(button);  // Encuentra fila desde tabla o card

    if (row) {
        // PASO 1: Obtener el ID ANTES de eliminar
        const idLinea = row.querySelector('input[name="idLinea"]')?.value;
        
        // PASO 2: Eliminar fila de la tabla
        row.remove();
        
        // PASO 3: Eliminar SOLO el card específico (no reconstruir todos)
        if (idLinea) {
            const card = obtenerCardLineaPorId(idLinea);  // Busca card por ID
            if (card) {
                card.remove();  // Elimina ese card solamente
            }
        }
    }
    
    // PASO 4: Recalcular totales
    actualizarResumenTotales();
    
    // IMPORTANTE: NO llamar actualizarVistaLineas()
    //    (eso reconstruiría TODOS los cards)
}
```

**Por qué esto es importante**:
-  `actualizarVistaLineas()` reconstruye toda la lista → pérdida de foco, estado, etc.
-  `obtenerCardLineaPorId()` + `.remove()` elimina solo lo necesario

---

### 4. GUARDAR Factura y Líneas

```
┌──────────────────────────────────────────┐
│ mostrarPaso2()                            │ (usuario presiona "Siguiente")
└──────────┬───────────────────────────────┘
           │
           ▼
    ┌──────────────────────────────┐
    │ Validar cliente y fecha      │
    └──────────┬───────────────────┘
               │
               ▼
    ┌──────────────────────────────┐
    │ POST /facturas/guardar       │
    │ → Crear factura en BD        │
    │ → Retorna: { idFactura: 5 }  │
    └──────────┬───────────────────┘
               │
               ▼
    ┌──────────────────────────────┐
    │ Guardar Promise en variable  │
    │ facturaCreadaPromise         │
    │ Enable botón "Guardar Líneas"│
    └──────────────────────────────┘
               │
               ▼ (Usuario presiona "Guardar")
    ┌──────────────────────────────┐
    │ guardarLineas()              │
    │ Await facturaCreadaPromise   │ ← Bloquea hasta que factura esté lista
    │ Recolectar datos de tabla    │
    │ Reemplazar -1, -2 con facturaId
    │ PUT /lineas-factura/actualizar
    │ → Inserta/actualiza líneas   │
    └──────────────────────────────┘
```

**Código**:

```javascript
let facturaCreadaPromise = Promise.resolve(null);  // Global

function mostrarPaso2() {
    // ... validaciones ...
    
    // Crear Promise que se resuelve cuando factura está lista
    facturaCreadaPromise = fetch('/facturas/guardar', {
        method: 'POST',
        body: JSON.stringify(factura)
    }).then(res => res.json().then(data => {
        facturaId = data.idFactura;  // Ahora tenemos el ID real
        document.getElementById("btnGuardar").disabled = false;
        return data;
    }));
}

async function guardarLineas() {
    // CLAVE: Esperar a que la factura se haya creado
    await facturaCreadaPromise;  // ← Bloquea aquí hasta que facturaId > 0
    
    if (!facturaId) {
        Swal.fire({ icon: 'warning', text: 'Factura aún no se creó' });
        return;
    }
    
    // Ahora es SEGURO usar facturaId
    const lineas = [...];  // Recolectar datos de tabla
    lineas.forEach(l => l.id_factura = facturaId);  // Usar ID real
    
    fetch('/lineas-factura/actualizar', { ... });
}
```

---

