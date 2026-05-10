## ✅ Solución Implementada

### Estrategia

**Filtrar líneas vacías antes de enviar al backend:**
- Validar que cada línea tenga un producto válido seleccionado
- Detectar IDs temporales (timestamps) y omitir esas líneas
- Renumerar las líneas válidas secuencialmente
- Informar al usuario si hay líneas sin producto

### Cambios Realizados

#### 1. Validación en `guardarLineas()`

**Archivo:** `static/js/editar-factura.js`

**Antes:**
```javascript
function guardarLineas() {
    const rows = document.querySelectorAll("#lineas-body tr");
    const lineas = [];

    rows.forEach((row, index) => {
        const idProducto = row.querySelector('input[name="idProducto"]').value;
        // ❌ No validaba si el producto era válido
        
        lineas.push({
            id_producto: parseInt(idProducto), // Envía cualquier valor
            ...
        });
    });
}
```

**Después:**
```javascript
function guardarLineas() {
    const rows = document.querySelectorAll("#lineas-body tr");
    const lineas = [];
    let lineasVacias = 0;

    rows.forEach((row, index) => {
        const idProducto = row.querySelector('input[name="idProducto"]').value;
        const selectProducto = row.querySelector('select[name="producto"]');
        
        // ✅ Validar que se haya seleccionado un producto válido
        const productoSeleccionado = selectProducto && selectProducto.value;
        const idProductoValido = parseInt(idProducto);
        
        // Un timestamp de Date.now() es mayor a 1000000000000 (13 dígitos)
        // Los IDs de productos normales son mucho menores
        if (!productoSeleccionado || !idProductoValido || idProductoValido > 1000000000000) {
            lineasVacias++;
            console.log(`Línea ${index + 1} omitida: sin producto seleccionado`);
            return; // ✅ Saltar esta línea
        }

        lineas.push({
            id_producto: idProductoValido,
            numero_linea: lineas.length + 1, // ✅ Renumerar basado en líneas válidas
            ...
        });
    });
    
    // ✅ Validar que haya al menos una línea válida
    if (lineas.length === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin productos',
            text: 'Debe seleccionar al menos un producto válido',
            confirmButtonColor: '#3085d6'
        });
        return;
    }
    
    // ✅ Informar al usuario si se omitieron líneas vacías
    if (lineasVacias > 0) {
        console.log(`Se omitieron ${lineasVacias} línea(s) vacía(s)`);
    }
    
    // Continúa con el guardado...
}
```

#### 2. Opción por Defecto en Select

**Archivo:** `static/js/editar-factura.js`

**Antes:**
```javascript
function createLineaRow(linea) {
    const opciones = allProductos.map(p => {
        const selected = p.id_producto === linea.id_producto ? "selected" : "";
        return `<option value="${p.id_producto}" ${selected}>${p.nombre}</option>`;
    }).join("");
    
    return `
        <select name="producto" onchange="actualizarProductoSeleccionado(this)">
          ${opciones}  <!-- ❌ Sin opción por defecto -->
        </select>
    `;
}
```

**Después:**
```javascript
function createLineaRow(linea) {
    // ✅ Opción por defecto para líneas nuevas
    const opcionDefault = linea.id_producto > 1000000000000 
        ? `<option value="" selected>-- Seleccione un producto --</option>` 
        : `<option value="">-- Seleccione un producto --</option>`;

    const opciones = allProductos.map(p => {
        const selected = p.id_producto === linea.id_producto ? "selected" : "";
        return `<option value="${p.id_producto}" ${selected}>${p.nombre}</option>`;
    }).join("");

    return `
        <select name="producto" onchange="actualizarProductoSeleccionado(this)">
          ${opcionDefault}  <!-- ✅ Opción placeholder -->
          ${opciones}
        </select>
    `;
}
```

---

