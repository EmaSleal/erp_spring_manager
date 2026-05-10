// ============================================================================
// PAGOS.JS - Gestión del módulo de pagos
// ERP Orders Manager
// ============================================================================

/**
 * Toggle del campo referencia según método de pago
 */
function toggleReferencia() {
    const metodoPago = document.getElementById('metodoPago');
    const referenciaInput = document.getElementById('referenciaBancaria');
    const referenciaRequired = document.getElementById('referenciaRequired');
    
    if (metodoPago && metodoPago.selectedIndex > 0) {
        const selectedOption = metodoPago.options[metodoPago.selectedIndex];
        const requiereRef = selectedOption.getAttribute('data-requiere-ref') === 'true';
        
        if (requiereRef) {
            referenciaInput.required = true;
            referenciaRequired.classList.remove('d-none');
            referenciaInput.parentElement.parentElement.classList.add('border-warning', 'border', 'p-2', 'rounded');
        } else {
            referenciaInput.required = false;
            referenciaRequired.classList.add('d-none');
            referenciaInput.parentElement.parentElement.classList.remove('border-warning', 'border', 'p-2', 'rounded');
        }
    } else {
        if (referenciaInput) {
            referenciaInput.required = false;
        }
        if (referenciaRequired) {
            referenciaRequired.classList.add('d-none');
        }
    }
}

/**
 * Toggle del campo factura según tipo de pago
 */
function toggleFacturaField() {
    const tipoPago = document.getElementById('tipoPago');
    const facturaField = document.getElementById('facturaField');
    const idFacturaSelect = document.getElementById('idFactura');
    const facturaRequired = document.getElementById('facturaRequired');
    
    if (tipoPago && tipoPago.selectedIndex > 0) {
        const selectedOption = tipoPago.options[tipoPago.selectedIndex];
        const requiereFactura = selectedOption.getAttribute('data-requiere-factura') === 'true';
        
        if (requiereFactura) {
            idFacturaSelect.required = true;
            if (facturaRequired) {
                facturaRequired.classList.remove('d-none');
            }
            facturaField.classList.remove('d-none');
        } else {
            // Es un adelanto, factura es opcional
            idFacturaSelect.required = false;
            if (facturaRequired) {
                facturaRequired.classList.add('d-none');
            }
            idFacturaSelect.value = ''; // Limpiar selección
        }
    }
}

/**
 * Cargar facturas del cliente seleccionado
 */
async function cargarFacturasCliente() {
    const idCliente = document.getElementById('idCliente').value;
    const facturaSelect = document.getElementById('idFactura');
    const facturaInfo = document.querySelector('.alert-info');
    
    // Limpiar opciones
    facturaSelect.innerHTML = '<option value="">Sin asignar (Adelanto)</option>';
    
    // Ocultar panel de información si existe
    if (facturaInfo) {
        facturaInfo.style.display = 'none';
    }
    
    if (!idCliente) {
        return;
    }
    
    try {
        const response = await fetch(`/facturas/api/cliente/${idCliente}`);
        if (response.ok) {
            const facturas = await response.json();
            
            if (facturas.length === 0) {
                showToast('info', 'El cliente no tiene facturas con saldo pendiente');
            }
            
            facturas.forEach(factura => {
                const option = document.createElement('option');
                option.value = factura.idFactura;
                option.textContent = `${factura.numeroFactura} - Saldo: ₡${formatNumber(factura.saldoPendiente, 2)}`;
                option.dataset.numeroFactura = factura.numeroFactura;
                option.dataset.totalFactura = factura.totalFactura;
                option.dataset.saldoPendiente = factura.saldoPendiente;
                facturaSelect.appendChild(option);
            });
            
            // Event listener para mostrar info de factura seleccionada
            facturaSelect.addEventListener('change', mostrarInfoFactura);
        } else {
            showToast('error', 'Error al cargar facturas del cliente');
        }
    } catch (error) {
        console.error('Error al cargar facturas:', error);
        showToast('error', 'Error de conexión al cargar facturas');
    }
}

/**
 * Mostrar información de la factura seleccionada
 */
function mostrarInfoFactura() {
    const facturaSelect = document.getElementById('idFactura');
    const selectedOption = facturaSelect.options[facturaSelect.selectedIndex];
    const montoInput = document.getElementById('monto');
    
    // Si no hay factura seleccionada, limpiar
    if (!selectedOption.value) {
        if (montoInput) {
            montoInput.removeAttribute('max');
            const formText = montoInput.closest('.mb-3').querySelector('.form-text');
            if (formText) {
                formText.textContent = '';
            }
        }
        ocultarPanelInfoFactura();
        return;
    }
    
    const numeroFactura = selectedOption.dataset.numeroFactura;
    const totalFactura = parseFloat(selectedOption.dataset.totalFactura);
    const saldoPendiente = parseFloat(selectedOption.dataset.saldoPendiente);
    const idCliente = document.getElementById('idCliente');
    const nombreCliente = idCliente.options[idCliente.selectedIndex].text;
    
    // Establecer máximo en monto
    if (montoInput) {
        montoInput.setAttribute('max', saldoPendiente);
        montoInput.value = saldoPendiente; // Auto-llenar con saldo pendiente
        
        const formText = montoInput.closest('.mb-3').querySelector('.form-text');
        if (formText) {
            formText.innerHTML = `Máximo permitido: <span class="fw-bold">₡ ${formatNumber(saldoPendiente, 2)}</span>`;
        }
    }
    
    // Mostrar panel de información
    mostrarPanelInfoFactura(numeroFactura, nombreCliente, totalFactura, saldoPendiente);
}

/**
 * Mostrar panel de información de factura
 */
function mostrarPanelInfoFactura(numeroFactura, nombreCliente, totalFactura, saldoPendiente) {
    let panel = document.querySelector('.alert-info');
    
    if (!panel) {
        // Crear panel si no existe
        panel = document.createElement('div');
        panel.className = 'alert alert-info mb-4';
        panel.innerHTML = `
            <h6 class="alert-heading mb-3"><i class="fas fa-info-circle me-2"></i>Información de la Factura</h6>
            <div class="row g-3">
                <div class="col-md-6">
                    <strong>Número:</strong> <span id="info-numero"></span>
                </div>
                <div class="col-md-6">
                    <strong>Cliente:</strong> <span id="info-cliente"></span>
                </div>
                <div class="col-md-6">
                    <strong>Total Factura:</strong> <span id="info-total"></span>
                </div>
                <div class="col-md-6">
                    <strong>Saldo Pendiente:</strong> <span class="text-danger fw-bold" id="info-saldo"></span>
                </div>
            </div>
        `;
        
        // Insertar antes del campo de factura
        const facturaField = document.getElementById('facturaField');
        facturaField.parentNode.insertBefore(panel, facturaField);
    }
    
    // Actualizar datos (buscar dentro del panel para evitar errores)
    const infoNumero = panel.querySelector('#info-numero') || document.getElementById('info-numero');
    const infoCliente = panel.querySelector('#info-cliente') || document.getElementById('info-cliente');
    const infoTotal = panel.querySelector('#info-total') || document.getElementById('info-total');
    const infoSaldo = panel.querySelector('#info-saldo') || document.getElementById('info-saldo');
    
    if (infoNumero) infoNumero.textContent = numeroFactura;
    if (infoCliente) infoCliente.textContent = nombreCliente;
    if (infoTotal) infoTotal.textContent = `₡ ${formatNumber(totalFactura, 2)}`;
    if (infoSaldo) infoSaldo.textContent = `₡ ${formatNumber(saldoPendiente, 2)}`;
    
    panel.style.display = 'block';
}

/**
 * Ocultar panel de información de factura
 */
function ocultarPanelInfoFactura() {
    const panel = document.querySelector('.alert-info');
    if (panel) {
        panel.style.display = 'none';
    }
}

/**
 * Formatear número con separadores de miles y decimales
 */
function formatNumber(num, decimales = 2) {
    if (num === null || num === undefined || isNaN(num)) {
        return '0.00';
    }
    return parseFloat(num).toLocaleString('es-CR', {
        minimumFractionDigits: decimales,
        maximumFractionDigits: decimales
    });
}

/**
 * Inicializar formulario de pago
 */
function initPagoForm() {
    const pagoForm = document.getElementById('pagoForm');
    
    if (!pagoForm) {
        return;
    }
    
    // Event listeners
    const idClienteSelect = document.getElementById('idCliente');
    if (idClienteSelect) {
        idClienteSelect.addEventListener('change', cargarFacturasCliente);
        
        // Cargar facturas si ya hay un cliente seleccionado (por URL params)
        if (idClienteSelect.value) {
            cargarFacturasCliente().then(() => {
                // Pre-seleccionar factura si viene en URL params
                const urlParams = new URLSearchParams(window.location.search);
                const idFactura = urlParams.get('idFactura');
                if (idFactura) {
                    const facturaSelect = document.getElementById('idFactura');
                    if (facturaSelect) {
                        facturaSelect.value = idFactura;
                        // Disparar evento change para mostrar info
                        mostrarInfoFactura();
                    }
                }
            });
        }
    }
    
    const tipoPagoSelect = document.getElementById('tipoPago');
    if (tipoPagoSelect) {
        tipoPagoSelect.addEventListener('change', toggleFacturaField);
    }
    
    const metodoPagoSelect = document.getElementById('metodoPago');
    if (metodoPagoSelect) {
        metodoPagoSelect.addEventListener('change', toggleReferencia);
    }
    
    // Inicializar toggles
    toggleReferencia();
    toggleFacturaField();
    
    // Manejo del envío del formulario
    pagoForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const formData = new FormData(this);
        const pagoData = {
            idCliente: parseInt(formData.get('idCliente')),
            idFactura: formData.get('idFactura') ? parseInt(formData.get('idFactura')) : null,
            tipoPago: formData.get('tipoPago'),
            monto: parseFloat(formData.get('monto')),
            metodoPago: formData.get('metodoPago'),
            fechaPago: formData.get('fechaPago'),
            referenciaBancaria: formData.get('referenciaBancaria'),
            banco: formData.get('banco'),
            cuentaBancaria: formData.get('cuentaBancaria'),
            comprobanteUrl: formData.get('comprobanteUrl'),
            estado: formData.get('estado'),
            observaciones: formData.get('observaciones'),
            conciliado: formData.get('conciliado') === 'on' || formData.get('conciliado') === 'true'
        };
        
        // Validar datos
        if (!pagoData.idCliente || !pagoData.tipoPago || !pagoData.monto || !pagoData.metodoPago || !pagoData.fechaPago || !pagoData.estado) {
            showToast('error', 'Por favor complete todos los campos obligatorios');
            return;
        }
        
        // Validar factura si el tipo lo requiere
        const tipoPagoSelect = document.getElementById('tipoPago');
        if (tipoPagoSelect && tipoPagoSelect.selectedIndex > 0) {
            const selectedOption = tipoPagoSelect.options[tipoPagoSelect.selectedIndex];
            const requiereFactura = selectedOption.getAttribute('data-requiere-factura') === 'true';
            
            if (requiereFactura && !pagoData.idFactura) {
                showToast('error', 'Este tipo de pago requiere una factura asignada');
                return;
            }
        }
        
        // Enviar datos usando httpPost
        await httpPost('/pagos/registrar', pagoData, {
            successMessage: 'Pago registrado correctamente',
            redirectOnSuccess: '/pagos'
        });
    });
}

/**
 * Conciliar un pago
 * @param {number} idPago - ID del pago a conciliar
 */
async function conciliarPago(idPago) {
    const confirmed = await showConfirmDialog(
        '¿Conciliar pago?',
        'Esta acción marcará el pago como conciliado con el banco',
        'Sí, conciliar'
    );
    
    if (!confirmed) {
        return;
    }
    
    await httpPut(`/pagos/${idPago}/conciliar`, null, {
        successMessage: 'Pago conciliado correctamente',
        reloadOnSuccess: true
    });
}

/**
 * Anular un pago
 * @param {number} idPago - ID del pago a anular
 */
async function anularPago(idPago) {
    const motivo = prompt('¿Por qué desea anular este pago?\n\nMotivo de anulación:');
    
    if (!motivo || motivo.trim() === '') {
        showToast('warning', 'Debe ingresar un motivo de anulación');
        return;
    }
    
    const confirmed = await showConfirmDialog(
        '¿Anular pago?',
        'Esta acción no se puede revertir. El pago quedará marcado como anulado.',
        'Sí, anular'
    );
    
    if (!confirmed) {
        return;
    }
    
    await httpPut(`/pagos/${idPago}/anular?motivo=${encodeURIComponent(motivo)}`, null, {
        successMessage: 'Pago anulado correctamente',
        reloadOnSuccess: true
    });
}

/**
 * Abre el modal de detalle de factura desde un botón
 * Wrapper para la función openModal de facturas.js
 * @param {HTMLElement} button - Botón que contiene data-factura-id
 */
function verDetalleFactura(button) {
    const facturaId = button.getAttribute('data-factura-id');
    if (facturaId && facturaId !== 'null' && facturaId !== '') {
        // Crear un objeto temporal con data-id para pasarlo a openModal
        const tempButton = { getAttribute: () => facturaId };
        if (typeof openModal === 'function') {
            openModal(tempButton);
        } else {
            console.error('La función openModal no está disponible. Asegúrate de incluir facturas.js');
        }
    } else {
        Swal.fire({
            icon: 'info',
            title: 'Sin factura',
            text: 'Este pago no tiene una factura asociada (es un adelanto)',
            confirmButtonText: 'Aceptar'
        });
    }
}

/**
 * Eliminar un pago
 * @param {number} idPago - ID del pago a eliminar
 */
async function eliminarPago(idPago) {
    await httpDeleteWithConfirm(
        `/pagos/${idPago}`,
        {
            title: '¿Eliminar pago?',
            text: 'Esta acción no se puede deshacer. ¿Está seguro de eliminar este pago?',
            confirmButtonText: 'Sí, eliminar'
        },
        {
            successMessage: 'Pago eliminado correctamente',
            reloadOnSuccess: true
        }
    );
}

// ============================================================================
// INICIALIZACIÓN
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    initPagoForm();
});
