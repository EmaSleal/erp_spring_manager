## 📝 Archivos Modificados

### **1. reportes/clientes.html**
```html
<!-- ANTES -->
<a th:href="@{/clientes/editar/{id}(id=${cliente.idCliente})}" 
   class="btn btn-sm btn-outline-success" 
   title="Ver detalles">

<!-- DESPUÉS -->
<a th:href="@{/clientes(edit=${cliente.idCliente})}" 
   class="btn btn-sm btn-outline-success" 
   title="Ver detalles">
```

### **2. reportes/productos.html**
```html
<!-- ANTES -->
<a th:href="@{/productos/editar/{id}(id=${producto.idProducto})}" 
   class="btn btn-sm btn-outline-warning" 
   title="Ver detalles">

<!-- DESPUÉS -->
<a th:href="@{/productos(edit=${producto.idProducto})}" 
   class="btn btn-sm btn-outline-warning" 
   title="Ver detalles">
```

### **3. static/js/clientes.js**

**Lógica Añadida en DOMContentLoaded:**
```javascript
// Detectar parámetro 'edit' en la URL y abrir modal automáticamente
const urlParams = new URLSearchParams(window.location.search);
const editId = urlParams.get('edit');
if (editId) {
    // Cargar el cliente desde el servidor y abrir el modal
    fetch(`/clientes/detalle/${editId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Cliente no encontrado');
            }
            return response.json();
        })
        .then(cliente => {
            // Abrir modal de edición con los datos del cliente
            openEditModal(
                cliente.idCliente,
                cliente.nombre,
                cliente.usuario?.telefono || '',
                cliente.tipoCliente
            );
            
            // Limpiar el parámetro de la URL sin recargar la página
            window.history.replaceState({}, '', window.location.pathname);
        })
        .catch(error => {
            console.error('Error al cargar el cliente:', error);
            // Limpiar el parámetro de la URL en caso de error
            window.history.replaceState({}, '', window.location.pathname);
        });
}
```

**Características Técnicas:**
- Utiliza **URLSearchParams API** para parsear query parameters
- Hace **fetch()** al endpoint existente `/clientes/detalle/{id}`
- Manejo de errores con try-catch
- Usa **history.replaceState()** para limpiar URL sin reload

### **4. static/js/productos.js**

**Lógica Añadida en DOMContentLoaded:**
```javascript
// Detectar parámetro 'edit' en la URL y abrir modal automáticamente
const urlParams = new URLSearchParams(window.location.search);
const editId = urlParams.get('edit');
if (editId) {
    // Buscar el producto por ID
    const producto = productos.find(p => p.idProducto == editId);
    if (producto) {
        // Abrir modal de edición con los datos del producto
        openEditModal(
            producto.idProducto,
            producto.codigo,
            producto.descripcion,
            producto?.presentacion?.idPresentacion,
            producto.precioInstitucional,
            producto.precioMayorista,
            producto.active
        );
        
        // Limpiar el parámetro de la URL sin recargar la página
        window.history.replaceState({}, '', window.location.pathname);
    }
}
```

**Diferencias con Clientes:**
- No requiere llamada AJAX (usa array global `productos` ya cargado)
- Búsqueda local con `.find()`
- Más eficiente en memoria

---

