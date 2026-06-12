let allProductos = [];
let newFactura = {}
let facturaId = 0;
let nuevaFacturaModal;
let nuevaFacturaModalElement;
let temporalLineaId = -1;
let facturaCreadaPromise = Promise.resolve(null);

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

    // Si la URL contiene clienteId (ej. /facturas?clienteId=8), preseleccionar cliente y abrir modal
    const urlParams = new URLSearchParams(window.location.search);
    const clienteIdParam = urlParams.get('clienteId');
    if (clienteIdParam) {
        // esperar un tick para asegurar que el DOM esté listo, abrir modal y luego fijar el select
        setTimeout(() => {
            openNuevaFacturaModal(); // esto llama a resetForm(), por eso fijamos el valor después
            const clienteSelect = document.getElementById('cliente');
            if (clienteSelect) {
                clienteSelect.value = clienteIdParam;
                // Disparar change por si hay listeners que reaccionen al cambio
                clienteSelect.dispatchEvent(new Event('change', { bubbles: true }));
            }
            // Limpiar parámetro de URL
            window.history.replaceState({}, '', window.location.pathname);
        }, 50);
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
                    
                    // DESPUÉS de inyectar el HTML, sincronizar precios
                    data.forEach(linea => {
                        const idProducto = linea.id_producto || linea.idProducto || linea.productoId;
                        const producto = allProductos.find(p => p.id_producto == idProducto);
                        
                        if (producto) {
                            // Buscar la fila y actualizar precio y subtotal
                            const row = Array.from(tableBody.querySelectorAll('tr')).find(r => {
                                const rowIdLinea = r.querySelector('input[name="idLinea"]')?.value;
                                return rowIdLinea == linea.id_linea_factura;
                            });
                            
                            if (row) {
                                const precioInput = row.querySelector('input[name="precio"]');
                                const cantidadInput = row.querySelector('input[name="cantidad"]');
                                const subtotalInput = row.querySelector('input[name="subtotal"]');
                                
                                if (precioInput) precioInput.value = producto.precio_institucional;
                                if (cantidadInput && subtotalInput) {
                                    const cantidad = parseFloat(cantidadInput.value) || 1;
                                    const precio = parseFloat(producto.precio_institucional) || 0;
                                    subtotalInput.value = (cantidad * precio).toFixed(2);
                                }
                            }
                        }
                    });
                    
                    // Actualizar vista de cards en móvil después de cargar líneas
                    actualizarVistaLineas();
                    
                    // Actualizar resumen de totales
                    actualizarResumenTotales();
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
    const cardsContainer = document.getElementById('lineas-cards-container');
    if (cardsContainer) {
        cardsContainer.innerHTML = '';
    }
    newFactura = {};
    facturaId = 0;
    temporalLineaId = -1;
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

function obtenerFilaLinea(element) {
    const row = element.closest("tr");
    if (row) {
        return row;
    }

    const card = element.closest(".card");
    if (!card) {
        return null;
    }

    const idLinea = card.querySelector('input[name="idLinea"]')?.value;
    if (!idLinea) {
        return null;
    }

    const rows = document.querySelectorAll("#lineas-body tr");
    for (const currentRow of rows) {
        const rowIdLinea = currentRow.querySelector('input[name="idLinea"]')?.value;
        if (rowIdLinea == idLinea) {
            return currentRow;
        }
    }

    return null;
}

function obtenerCardLineaPorId(idLinea) {
    const cardsContainer = document.getElementById("lineas-cards-container");
    if (!cardsContainer) {
        return null;
    }

    return Array.from(cardsContainer.querySelectorAll(".card")).find(card => {
        const cardIdLinea = card.querySelector('input[name="idLinea"]')?.value;
        return cardIdLinea == idLinea;
    }) || null;
}

function sincronizarCardLinea(row) {
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    const card = idLinea ? obtenerCardLineaPorId(idLinea) : null;
    if (!card) {
        return;
    }

    const selectProducto = row.querySelector('select[name="producto"]');
    const cantidad = row.querySelector('input[name="cantidad"]')?.value ?? '';
    const precio = row.querySelector('input[name="precio"]')?.value ?? '';
    const subtotal = row.querySelector('input[name="subtotal"]')?.value ?? '';

    const cardSelect = card.querySelector('select[name="producto"]');
    if (cardSelect && selectProducto) {
        cardSelect.value = selectProducto.value;
    }

    const cardCantidad = card.querySelector('input[name="cantidad"]');
    if (cardCantidad) {
        cardCantidad.value = cantidad;
    }

    const cardPrecio = card.querySelector('input[name="precio"]');
    if (cardPrecio) {
        cardPrecio.value = precio;
    }

    const cardSubtotal = card.querySelector('input[name="subtotal"]');
    if (cardSubtotal) {
        cardSubtotal.value = subtotal;
    }
}


function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);
    if (!row) return;

    const select = element.matches('select[name="producto"]')
        ? element
        : row.querySelector('select[name="producto"]');

    if (!select) return;

    const cantidadFuente = element.matches('input[name="cantidad"]')
        ? element
        : row.querySelector('input[name="cantidad"]');

    const cantidadValue = cantidadFuente ? cantidadFuente.value : '1';
    const selectedId = parseInt(select.value);
    const producto = allProductos.find(p => p.id_producto === selectedId);
    if (!producto) return;

    // Sincronizar la fila fuente con lo que el usuario cambió
    row.querySelector('select[name="producto"]').value = select.value;
    const inputCantidad = row.querySelector('input[name="cantidad"]');
    if (inputCantidad) {
        inputCantidad.value = cantidadValue;
    }

    // Actualizar el precio en la tabla (fuente de verdad)
    const inputPrecio = row.querySelector('input[name="precio"]');
    inputPrecio.value = producto.precio_institucional;

    // Actualizar el hidden de idProducto
    const inputIdProducto = row.querySelector('input[name="idProducto"]');
    inputIdProducto.value = producto.id_producto;

    // Recalcular subtotal
    const subtotalInput = row.querySelector('input[name="subtotal"]');
    const subtotal = parseFloat(producto.precio_institucional) * parseFloat(cantidadValue || 1);
    subtotalInput.value = subtotal.toFixed(2);
    
    // Sincronizar solo la card correspondiente, sin reconstruir toda la vista
    sincronizarCardLinea(row);

    // ✅ Actualizar resumen de totales
    actualizarResumenTotales();
}




function removeLinea(button) {
    const row = obtenerFilaLinea(button);

    if (row) {
        // Obtener el ID de la línea ANTES de eliminarla
        const idLinea = row.querySelector('input[name="idLinea"]')?.value;
        row.remove();
        
        // Eliminar SOLO el card específico sin reconstruir todos
        if (idLinea) {
            const card = obtenerCardLineaPorId(idLinea);
            if (card) {
                card.remove();
            }
        }
    }
    
    // ✅ Actualizar resumen después de eliminar línea
    actualizarResumenTotales();
}

function addLinea() {
    const tableBody = document.getElementById("lineas-body");
    const randomId = temporalLineaId--;
    // Calcular número de línea basado en filas existentes
    const numeroLinea = tableBody.querySelectorAll('tr').length + 1;
    const linea = {
        producto: {descripcion: ""},
        cantidad: 1,
        precioUnitario: 0,
        idLineaFactura: randomId,
        subtotal: 0,
        idProducto: randomId,
        numero_linea: numeroLinea,
        id_linea_factura: randomId,
        id_producto: randomId
    };
    tableBody.insertAdjacentHTML("beforeend", createLineaRow(linea));

    const cardsContainer = document.getElementById("lineas-cards-container");
    if (cardsContainer) {
        cardsContainer.insertAdjacentHTML("beforeend", createLineaCard(linea));
    }
}

function createLineaRow(linea) {
    const selectId = `select-producto-${linea.id_linea_factura}`;

    // Obtener el ID del producto (intenta múltiples nombres de propiedad)
    const idProducto = linea.id_producto || linea.idProducto || linea.productoId;
    const es_linea_nueva = !idProducto || idProducto < 0;

    // Opción por defecto para líneas nuevas
    const opcionDefault = es_linea_nueva
        ? `<option value="" selected>-- Seleccione un producto --</option>` 
        : `<option value="">-- Seleccione un producto --</option>`;

    const opciones = allProductos.map(p => {
        // Comparar como string para evitar problemas de tipo de dato
        const idProductoStr = String(idProducto || '');
        const pIdStr = String(p.id_producto);
        const selected = pIdStr === idProductoStr && !es_linea_nueva ? "selected" : "";
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
        <input type="hidden" name="idProducto" value="${idProducto}">
        <select name="producto" id="${selectId}" class="form-select" onchange="actualizarProductoSeleccionado(this)">
          ${opcionDefault}
          ${opciones}
        </select>
      </td>
      
      <td class="text-center">
                <input type="number" name="cantidad" value="${linea.cantidad}" class="form-control text-center" oninput="actualizarProductoSeleccionado(this)" min="1">
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

// Nueva función: Crear card de línea para versión móvil
function createLineaCard(linea) {
    const selectId = `select-producto-${linea.id_linea_factura}`;

    // Obtener el ID del producto (intenta múltiples nombres de propiedad)
    const idProducto = linea.id_producto || linea.idProducto || linea.productoId;
    const es_linea_nueva = !idProducto || idProducto < 0;

    // Opción por defecto para líneas nuevas
    const opcionDefault = es_linea_nueva
        ? `<option value="" selected>-- Seleccione un producto --</option>` 
        : `<option value="">-- Seleccione un producto --</option>`;

    const opciones = allProductos.map(p => {
        // Comparar como string para evitar problemas de tipo de dato
        const idProductoStr = String(idProducto || '');
        const pIdStr = String(p.id_producto);
        const selected = pIdStr === idProductoStr && !es_linea_nueva ? "selected" : "";
        return `<option value="${p.id_producto}" ${selected}>${p.nombre} - $${p.precio_institucional}</option>`;
    }).join("");

    return `
    <div class="card mb-3 border-light shadow-sm">
        <div class="card-body">
            <!-- Número de línea -->
            <div class="mb-2">
                <small class="text-muted">Línea #${linea.numero_linea}</small>
                <input type="hidden" name="numero_linea" value="${linea.numero_linea}">
            </div>

            <!-- Producto -->
            <div class="mb-3">
                <label class="form-label fw-bold">
                    <i class="fas fa-box text-primary me-1"></i>Producto
                </label>
                <input type="hidden" name="idLinea" value="${linea.id_linea_factura}">
                <input type="hidden" name="idProducto" value="${idProducto}">
                <select name="producto" id="${selectId}" class="form-select form-select-sm" onchange="actualizarProductoSeleccionado(this)">
                    ${opcionDefault}
                    ${opciones}
                </select>
            </div>

            <!-- Fila: Cantidad y Precio -->
            <div class="row mb-3">
                <div class="col-6">
                    <label class="form-label fw-bold">
                        <i class="fas fa-hashtag text-info me-1"></i>Cantidad
                    </label>
                    <input type="number" name="cantidad" value="${linea.cantidad}" class="form-control form-control-sm" oninput="actualizarProductoSeleccionado(this)" min="1">
                </div>
                <div class="col-6">
                    <label class="form-label fw-bold">
                        <i class="fas fa-dollar-sign text-success me-1"></i>Precio Unitario
                    </label>
                    <input type="number" name="precio" value="${linea.precioUnitario}" class="form-control form-control-sm" disabled>
                </div>
            </div>

            <!-- Subtotal -->
            <div class="alert alert-info mb-2 py-2">
                <div class="d-flex justify-content-between">
                    <span class="fw-bold">Subtotal:</span>
                    <span class="fw-bold text-success">
                        <input type="number" name="subtotal" value="${linea.subtotal}" style="width: 100px;" class="form-control form-control-sm text-end" disabled>
                    </span>
                </div>
            </div>

            <!-- Botón Eliminar -->
            <div class="d-grid">
                <button type="button" onclick="removeLinea(this)" class="btn btn-sm btn-danger">
                    <i class="fas fa-trash me-2"></i>Eliminar línea
                </button>
            </div>
        </div>
    </div>
    `;
}

// Función para actualizar ambos contenedores (tabla y cards)
function actualizarVistaLineas() {
    const rows = document.querySelectorAll("#lineas-body tr");
    const cardsContainer = document.getElementById("lineas-cards-container");
    
    if (cardsContainer) {
        cardsContainer.innerHTML = '';
        rows.forEach(row => {
            const numeroLinea = row.querySelector('input[name="numero_linea"]')?.value;
            const idLinea = row.querySelector('input[name="idLinea"]')?.value;
            const idProducto = row.querySelector('input[name="idProducto"]')?.value;
            const cantidad = row.querySelector('input[name="cantidad"]')?.value;
            const precio = row.querySelector('input[name="precio"]')?.value;
            const subtotal = row.querySelector('input[name="subtotal"]')?.value;

            const linea = {
                numero_linea: numeroLinea,
                id_linea_factura: idLinea,
                id_producto: idProducto,
                cantidad: cantidad,
                precioUnitario: precio,
                subtotal: subtotal
            };

            cardsContainer.innerHTML += createLineaCard(linea);
        });
    }
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
    document.getElementById("btnGuardar").disabled = true;

    addLinea(); // Agrega al menos una línea por defecto

    const descripcion = document.getElementById("descripcion");
    const entregado = document.getElementById("entregado");
    const serie = document.getElementById("serie");
    const numeroFactura = document.getElementById("numeroFactura");
    const fechaPago = document.getElementById("fechaPago");
    const condicionVentaFE = document.getElementById("condicionVentaFE");
    const medioPagoFE = document.getElementById("medioPagoFE");
    const monedaFE = document.getElementById("monedaFE");
    const tipoCambio = document.getElementById("tipoCambio");
    const plazoCredito = document.getElementById("plazoCredito");

    const factura = {
        cliente: {
            idCliente: parseInt(selectCliente.value)
        },
        fechaEntrega: fechaEntrega.value,
        fechaPago: fechaPago.value || null,
        serie: serie.value || null,
        numeroFactura: numeroFactura.value || null,
        descripcion: descripcion.value,
        tipoFactura: tipoFactura.value,
        entregado: entregado.checked,
        condicionVentaFE: condicionVentaFE?.value || null,
        medioPagoFE: medioPagoFE?.value || null,
        monedaFE: monedaFE?.value || null,
        tipoCambio: tipoCambio?.value ? parseFloat(tipoCambio.value) : null,
        plazoCredito: plazoCredito?.value ? parseInt(plazoCredito.value) : null
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    facturaCreadaPromise = fetch('/facturas/guardar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify(factura)
    }).then(res => {
        if (res.ok) {
            return res.json().then(data => {
                newFactura = data;
                facturaId = newFactura.idFactura;
                console.log('Factura creada:', newFactura);
                document.getElementById("btnGuardar").disabled = false;
                return data;
            });
        } else {
            document.getElementById("btnGuardar").disabled = true;
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Error al crear la factura',
                confirmButtonColor: '#d33'
            });
            return null;
        }
    }).catch(error => {
        document.getElementById("btnGuardar").disabled = true;
        console.error('Error al crear factura:', error);
        return null;
    });
}

async function guardarLineas() {
    if (!facturaId) {
        try {
            await facturaCreadaPromise;
        } catch (error) {
            console.error('No se pudo crear la factura antes de guardar líneas:', error);
            return;
        }
    }

    if (!facturaId) {
        Swal.fire({
            icon: 'warning',
            title: 'Factura pendiente',
            text: 'Espere a que se cree la factura antes de guardar las líneas',
            confirmButtonColor: '#3085d6'
        });
        return;
    }

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
            
            const descInput = document.getElementById('descripcion');
            const fechaInput = document.getElementById('fechaEntrega');
            const params = new URLSearchParams({ entregado: estadoEntregado });
            params.append('descripcion', descInput ? descInput.value : '');
            params.append('fechaEntrega', fechaInput ? fechaInput.value : '');

            return fetch(`/facturas/actualizar-estado/${facturaId}?${params.toString()}`, {
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
                if (nuevaFacturaModal) {
                    nuevaFacturaModal.hide();
                    location.reload();
                } else {
                    window.location.href = '/facturas';
                }
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

