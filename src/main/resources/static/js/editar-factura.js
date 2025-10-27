let allProductos = [];
let newFactura = {}
let facturaId = 0;
let nuevaFacturaModal;
let nuevaFacturaModalElement;

document.addEventListener("DOMContentLoaded", function () {
    // Inicializar el modal de Bootstrap 5
    nuevaFacturaModalElement = document.getElementById('nuevaFacturaModal');
    if (nuevaFacturaModalElement) {
        nuevaFacturaModal = new bootstrap.Modal(nuevaFacturaModalElement);
        
        // Limpiar el formulario cuando se cierra el modal
        nuevaFacturaModalElement.addEventListener('hidden.bs.modal', function () {
            resetForm();
        });
    }

    // Si estamos en la página de edición, extraer el ID de la URL
    const path = window.location.pathname;
    if (path.includes('/editar/')) {
        facturaId = path.split("/").pop();
    }

    // Cargar todos los productos
    fetch(`/productos/records`)
        .then(response => response.json())
        .then(data => {
            allProductos = data;
            if (facturaId) {
                cargarLineas();
            }
        });

    function cargarLineas() {
        if (facturaId) {
            fetch(`/lineas-factura/detalle/${facturaId}`)
                .then(response => response.json())
                .then(data => {
                    const tableBody = document.getElementById("lineas-body");
                    tableBody.innerHTML = "";
                    data.forEach(linea => {
                        tableBody.innerHTML += createLineaRow(linea);
                    });
                });
        }
    }
});

// Función para abrir el modal de nueva factura
function openNuevaFacturaModal() {
    resetForm();
    if (nuevaFacturaModal) {
        nuevaFacturaModal.show();
    }
}

// Función para resetear el formulario
function resetForm() {
    document.getElementById('facturaForm').reset();
    document.getElementById('paso').value = '1';
    document.getElementById('paso-1').style.display = 'block';
    document.getElementById('paso-2').style.display = 'none';
    document.getElementById('btnSiguiente').style.display = 'inline-block';
    document.getElementById('btnGuardar').style.display = 'none';
    document.getElementById('lineas-body').innerHTML = '';
    newFactura = {};
    facturaId = 0;
    actualizarResumenTotales();
}

// ========================================
// NUEVA FUNCIÓN: Calcular Fecha de Pago
// ========================================
function calcularFechaPago() {
    const fechaEntrega = document.getElementById('fechaEntrega');
    const fechaPago = document.getElementById('fechaPago');
    
    if (fechaEntrega && fechaEntrega.value && fechaPago) {
        // Convertir fecha de entrega a objeto Date
        const entrega = new Date(fechaEntrega.value + 'T00:00:00');
        
        // Agregar 7 días
        entrega.setDate(entrega.getDate() + 7);
        
        // Formatear a YYYY-MM-DD para el input
        const year = entrega.getFullYear();
        const month = String(entrega.getMonth() + 1).padStart(2, '0');
        const day = String(entrega.getDate()).padStart(2, '0');
        
        fechaPago.value = `${year}-${month}-${day}`;
    }
}

// ========================================
// NUEVA FUNCIÓN: Actualizar Resumen de Totales
// ========================================
function actualizarResumenTotales() {
    const rows = document.querySelectorAll("#lineas-body tr");
    let subtotal = 0;
    
    rows.forEach(row => {
        const subtotalInput = row.querySelector('input[name="subtotal"]');
        if (subtotalInput && subtotalInput.value) {
            subtotal += parseFloat(subtotalInput.value) || 0;
        }
    });
    
    // IGV es 0% por ahora (puedes cambiarlo a 18% si es necesario)
    const igv = 0; // subtotal * 0.18 para 18%
    const total = subtotal + igv;
    
    // Actualizar elementos en el DOM
    const elementoSubtotal = document.getElementById('resumen-subtotal');
    const elementoIgv = document.getElementById('resumen-igv');
    const elementoTotal = document.getElementById('resumen-total');
    
    if (elementoSubtotal) elementoSubtotal.textContent = `$${subtotal.toFixed(2)}`;
    if (elementoIgv) elementoIgv.textContent = `$${igv.toFixed(2)}`;
    if (elementoTotal) elementoTotal.textContent = `$${total.toFixed(2)}`;
}


function actualizarProductoSeleccionado(element) {
    const row = element.closest("tr");
    const select = row.querySelector('select[name="producto"]');
    const selectedId = parseInt(select.value);
    const producto = allProductos.find(p => p.id_producto === selectedId);
    if (!producto) return;

    // Actualizar el precio
    const inputPrecio = row.querySelector('input[name="precio"]');
    inputPrecio.value = producto.precio_institucional;

    // Actualizar el hidden de idProducto
    const inputIdProducto = row.querySelector('input[name="idProducto"]');
    inputIdProducto.value = producto.id_producto;

    // Recalcular subtotal
    const inputCantidad = row.querySelector('input[name="cantidad"]');
    const subtotalInput = row.querySelector('input[name="subtotal"]');
    const subtotal = parseFloat(producto.precio_institucional) * parseFloat(inputCantidad.value || 1);
    subtotalInput.value = subtotal.toFixed(2);
    
    // ✅ NUEVO: Actualizar resumen de totales
    actualizarResumenTotales();
}




function removeLinea(button) {
    button.closest("tr").remove();
    // ✅ NUEVO: Actualizar resumen después de eliminar línea
    actualizarResumenTotales();
}

function addLinea() {
    const tableBody = document.getElementById("lineas-body");
    const randomId = Date.now();
    const html = createLineaRow({
        producto: {descripcion: ""},
        cantidad: 1,
        precioUnitario: 0,
        idLineaFactura: randomId,
        subtotal: 0,
        idProducto: randomId,
        numero_linea: tableBody.children.length + 1
    });
    tableBody.insertAdjacentHTML("beforeend", html);
}

function createLineaRow(linea) {
    const selectId = `select-producto-${linea.id_linea_factura}`;

    // Opción por defecto para líneas nuevas
    const opcionDefault = linea.id_producto > 1000000000000 
        ? `<option value="" selected>-- Seleccione un producto --</option>` 
        : `<option value="">-- Seleccione un producto --</option>`;

    const opciones = allProductos.map(p => {
        const selected = p.id_producto === linea.id_producto ? "selected" : "";
        return `<option value="${p.id_producto}" ${selected}>${p.nombre} - $${p.precio_institucional}</option>`;
    }).join("");

    return `
    <tr>
        <td class="text-center">
            <input type="hidden" name="numero_linea" value="${linea.numero_linea}">
            ${linea.numero_linea}
        </td>
      <td>
        <input type="hidden" name="idLinea" value="${linea.id_linea_factura}">
        <input type="hidden" name="idProducto" value="${linea.id_producto}">
        <select name="producto" id="${selectId}" class="form-select" onchange="actualizarProductoSeleccionado(this)">
          ${opcionDefault}
          ${opciones}
        </select>
      </td>
      
      <td class="text-center">
        <input type="number" name="cantidad" value="${linea.cantidad}" class="form-control text-center" onchange="actualizarProductoSeleccionado(this)" min="1">
      </td>
      <td class="text-center">
        <input type="number" name="precio" value="${linea.precioUnitario}" class="form-control text-center" disabled>
      </td>
      <td class="text-center">
          <input type="number" name="subtotal" value="${linea.subtotal}" class="form-control text-center" disabled>
      </td>
      <td class="text-center">
        <button type="button" onclick="removeLinea(this)" class="btn btn-sm btn-danger">
          <i class="fas fa-trash"></i>
        </button>
      </td>
    </tr>
  `;
}

function mostrarPaso2() {
    // Validar que se haya seleccionado un cliente y fecha de entrega
    const selectCliente = document.getElementById("cliente");
    const fechaEntrega = document.getElementById("fechaEntrega");
    const tipoFactura = document.getElementById("tipoFactura");

    if (!selectCliente.value) {
        Swal.fire({
            icon: 'warning',
            title: 'Cliente requerido',
            text: 'Por favor seleccione un cliente',
            confirmButtonColor: '#3085d6'
        });
        return;
    }

    if (!fechaEntrega.value) {
        Swal.fire({
            icon: 'warning',
            title: 'Fecha requerida',
            text: 'Por favor seleccione una fecha de entrega',
            confirmButtonColor: '#3085d6'
        });
        return;
    }

    // Ocultar paso 1 y mostrar paso 2
    document.getElementById("paso-1").style.display = "none";
    document.getElementById("paso-2").style.display = "block";
    document.getElementById("paso").value = "2";
    
    // Cambiar botones del footer
    document.getElementById("btnSiguiente").style.display = "none";
    document.getElementById("btnGuardar").style.display = "inline-block";

    addLinea(); // Agrega al menos una línea por defecto

    // ✅ NUEVO: Obtener campos adicionales
    const descripcion = document.getElementById("descripcion");
    const entregado = document.getElementById("entregado");
    const serie = document.getElementById("serie");
    const numeroFactura = document.getElementById("numeroFactura");
    const fechaPago = document.getElementById("fechaPago");

    // Construir el objeto Factura con nuevos campos
    const factura = {
        cliente: {
            idCliente: parseInt(selectCliente.value)
        },
        fechaEntrega: fechaEntrega.value,
        fechaPago: fechaPago.value || null, // ✅ NUEVO
        serie: serie.value || null, // ✅ NUEVO
        numeroFactura: numeroFactura.value || null, // ✅ NUEVO
        descripcion: descripcion.value,
        tipoFactura: tipoFactura.value,
        entregado: entregado.checked
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch('/facturas/guardar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify(factura)
    }).then(res => {
        if (res.ok) {
            res.json().then(data => {
                newFactura = data;
                facturaId = newFactura.idFactura;
                console.log('Factura creada:', newFactura);
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Error al crear la factura',
                confirmButtonColor: '#d33'
            });
        }
    });
}

function guardarLineas() {
    const rows = document.querySelectorAll("#lineas-body tr");
    
    if (rows.length === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin líneas',
            text: 'Debe agregar al menos una línea de producto',
            confirmButtonColor: '#3085d6'
        });
        return;
    }

    const lineas = [];
    let lineasVacias = 0;

    rows.forEach((row, index) => {
        const idLinea = row.querySelector('input[name="idLinea"]').value;
        const idProducto = row.querySelector('input[name="idProducto"]').value;
        const selectProducto = row.querySelector('select[name="producto"]');
        const cantidad = row.querySelector('input[name="cantidad"]').value;
        const precio = row.querySelector('input[name="precio"]').value;
        const numeroLinea = row.querySelector('input[name="numero_linea"]').value;
        const subtotal = row.querySelector('input[name="subtotal"]').value;

        // Validar que se haya seleccionado un producto válido
        // Si el select no tiene un valor válido, o el idProducto es un timestamp (> 1000000000000), saltamos esta línea
        const productoSeleccionado = selectProducto && selectProducto.value;
        const idProductoValido = parseInt(idProducto);
        
        // Un timestamp de Date.now() es mayor a 1000000000000 (13 dígitos)
        // Los IDs de productos normales son mucho menores
        if (!productoSeleccionado || !idProductoValido || idProductoValido > 1000000000000) {
            lineasVacias++;
            console.log(`Línea ${index + 1} omitida: sin producto seleccionado`);
            return; // Saltar esta línea
        }

        lineas.push({
            id_factura: parseInt(facturaId),
            id_linea_factura: parseInt(idLinea),
            numero_linea: lineas.length + 1, // Renumerar basado en líneas válidas
            id_producto: idProductoValido,
            descripcion: null,
            cantidad: parseInt(cantidad),
            precioUnitario: parseFloat(precio),
            subtotal: parseFloat(subtotal),
            create_by: null,
            update_by: null,
            create_date: null,
            update_date: null
        });
    });
    
    // Validar que haya al menos una línea válida
    if (lineas.length === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin productos',
            text: 'Debe seleccionar al menos un producto válido',
            confirmButtonColor: '#3085d6'
        });
        return;
    }
    
    // Informar al usuario si se omitieron líneas vacías
    if (lineasVacias > 0) {
        console.log(`Se omitieron ${lineasVacias} línea(s) vacía(s)`);
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Primero guardar las líneas
    fetch('/lineas-factura/actualizar', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify(lineas)
    }).then(res => {
        if (res.ok) {
            // Luego actualizar el estado de la factura
            const entregadoSelect = document.getElementById("entregado");
            const estadoEntregado = entregadoSelect ? (entregadoSelect.value === 'true') : false;
            
            console.log('Actualizando estado a:', estadoEntregado);
            
            return fetch(`/facturas/actualizar-estado/${facturaId}?entregado=${estadoEntregado}`, {
                method: 'PUT',
                headers: { [csrfHeader]: csrfToken }
            });
        } else {
            throw new Error('Error al guardar las líneas');
        }
    }).then(res => {
        if (res && res.ok) {
            Swal.fire({
                icon: 'success',
                title: '¡Éxito!',
                text: 'Factura guardada correctamente',
                confirmButtonColor: '#28a745',
                timer: 2000
            }).then(() => {
                nuevaFacturaModal.hide();
                location.reload();
            });
        }
    }).catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al guardar la factura: ' + error.message,
            confirmButtonColor: '#d33'
        });
    });
}

