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

let cuentasBancariasFetch = null;

/**
 * ADR-018: la cuenta bancaria de destino solo aplica a los métodos que la requieren
 * (data-requiere-cuenta-bancaria, hoy TRANSFERENCIA). Se muestra, se vuelve obligatoria y se
 * llena con las cuentas activas de la empresa en la moneda del pago.
 */
function actualizarCuentaBancariaDestino() {
    const metodoPago = document.getElementById('metodoPago');
    const grupo = document.getElementById('cuentaBancariaDestinoGroup');
    const select = document.getElementById('idCuentaBancariaDestino');
    const ayuda = document.getElementById('cuentaBancariaDestinoAyuda');
    if (!metodoPago || !grupo || !select) return;

    const opcion = metodoPago.selectedIndex > 0 ? metodoPago.options[metodoPago.selectedIndex] : null;
    const requiere = opcion && opcion.getAttribute('data-requiere-cuenta-bancaria') === 'true';
    if (!requiere) {
        grupo.style.display = 'none';
        select.required = false;
        select.value = '';
        return;
    }

    grupo.style.display = 'block';
    select.required = true;
    const moneda = document.getElementById('monedaPago')?.value || 'CRC';
    const anterior = select.value;
    if (cuentasBancariasFetch) cuentasBancariasFetch.abort();
    cuentasBancariasFetch = new AbortController();
    fetch(`/api/cuentas-bancarias/activas?moneda=${encodeURIComponent(moneda)}`, { signal: cuentasBancariasFetch.signal })
        .then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) }))
        .then(({ ok, data }) => {
            select.innerHTML = '<option value="">Seleccione la cuenta bancaria...</option>';
            const cuentas = ok && data && Array.isArray(data.data) ? data.data : [];
            cuentas.forEach(c => {
                const option = document.createElement('option');
                option.value = c.idCuentaBancaria;
                option.textContent = `${c.entidad} (${c.moneda})${c.cuentaBanco ? ' - ' + c.cuentaBanco : ''}`;
                select.appendChild(option);
            });
            if (cuentas.some(c => String(c.idCuentaBancaria) === anterior)) {
                select.value = anterior;
            }
            if (ayuda) {
                ayuda.textContent = !ok
                    ? ((data && data.message) || 'No se pudieron cargar las cuentas bancarias')
                    : (cuentas.length
                        ? 'Cuenta de la empresa en la que entra la transferencia. Solo cuentas activas en ' + moneda + '.'
                        : 'No hay cuentas bancarias activas en ' + moneda + '. Configúrelas en Configuración → Empresa.');
            }
        })
        .catch(err => { if (err.name !== 'AbortError') console.error('Error al cargar cuentas bancarias:', err); });
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
        const facturas = await httpGet(`/facturas/api/cliente/${idCliente}`, { showLoading: false });

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
            option.dataset.monedaFe = factura.monedaFE || 'CRC';
            option.textContent = `${factura.numeroFactura} - Saldo: ${factura.monedaFE || 'CRC'} ${formatNumber(factura.saldoPendiente, 2)}`;
            facturaSelect.appendChild(option);
        });

        // Event listener para mostrar info de factura seleccionada
        facturaSelect.addEventListener('change', mostrarInfoFactura);
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
    
    // ADR-011: la moneda del pago por defecto es la de la factura (CRC para adelantos)
    const monedaPago = document.getElementById('monedaPago');
    if (monedaPago) {
        monedaPago.value = selectedOption.value ? (selectedOption.dataset.monedaFe || 'CRC') : 'CRC';
    }
    actualizarTipoCambioPago();
    actualizarCuentaBancariaDestino();

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
        actualizarMontoAplicado();
        return;
    }
    
    const numeroFactura = selectedOption.dataset.numeroFactura;
    const totalFactura = parseFloat(selectedOption.dataset.totalFactura);
    const saldoPendiente = parseFloat(selectedOption.dataset.saldoPendiente);
    const idCliente = document.getElementById('idCliente');
    const nombreCliente = idCliente.options[idCliente.selectedIndex].text;
    
    // Auto-llenar con el saldo pendiente (en la moneda de la factura). El máximo se valida
    // sobre el monto aplicado, en actualizarMontoAplicado (ADR-011).
    if (montoInput) {
        montoInput.value = saldoPendiente;
        
        const formText = montoInput.closest('.mb-3').querySelector('.form-text');
        if (formText) {
            formText.innerHTML = `Máximo permitido: <span class="fw-bold">${selectedOption.dataset.monedaFe || 'CRC'} ${formatNumber(saldoPendiente, 2)}</span> (en la moneda de la factura)`;
        }
    }
    actualizarMontoAplicado();
    
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
    const monedaFactura = monedaFacturaSeleccionada();
    if (infoTotal) infoTotal.textContent = `${monedaFactura} ${formatNumber(totalFactura, 2)}`;
    if (infoSaldo) infoSaldo.textContent = `${monedaFactura} ${formatNumber(saldoPendiente, 2)}`;
    
    panel.style.display = 'block';
}

/**
 * Moneda de la factura seleccionada (CRC si no hay factura: adelanto)
 */
function monedaFacturaSeleccionada() {
    const facturaSelect = document.getElementById('idFactura');
    const opcion = facturaSelect ? facturaSelect.options[facturaSelect.selectedIndex] : null;
    return opcion && opcion.value ? (opcion.dataset.monedaFe || 'CRC') : 'CRC';
}

/**
 * Saldo pendiente de la factura seleccionada, en su moneda (null si no hay factura).
 */
function saldoFacturaSeleccionada() {
    const facturaSelect = document.getElementById('idFactura');
    const opcion = facturaSelect ? facturaSelect.options[facturaSelect.selectedIndex] : null;
    if (!opcion || !opcion.value || opcion.dataset.saldoPendiente === undefined) return null;
    const saldo = parseFloat(opcion.dataset.saldoPendiente);
    return isNaN(saldo) ? null : saldo;
}

/**
 * ADR-011: muestra el monto que se aplicará al saldo, en la moneda de la factura, y valida
 * que no exceda el saldo pendiente. Misma regla que ConversionMoneda.montoAplicado:
 *   P = D → monto; P = CRC y D extranjera → monto / tc; P extranjera y D = CRC → monto × tc.
 */
function actualizarMontoAplicado() {
    const preview = document.getElementById('montoAplicadoPreview');
    const montoInput = document.getElementById('monto');
    if (!montoInput) return;
    montoInput.removeAttribute('max');
    montoInput.setCustomValidity('');

    const facturaSelect = document.getElementById('idFactura');
    const hayFactura = facturaSelect && facturaSelect.value;
    const pago = document.getElementById('monedaPago')?.value || 'CRC';
    const documento = monedaFacturaSeleccionada();
    const monto = parseFloat(montoInput.value);
    const tc = parseFloat(document.getElementById('tipoCambioPago')?.value);

    if (!hayFactura || isNaN(monto) || monto <= 0) {
        if (preview) preview.style.display = 'none';
        return;
    }

    let aplicado = null;
    if (pago === documento) {
        aplicado = monto;
    } else if (!isNaN(tc) && tc > 0 && (pago === 'CRC' || documento === 'CRC')) {
        aplicado = pago === 'CRC' ? monto / tc : monto * tc;
    }
    aplicado = aplicado === null ? null : Math.round(aplicado * 100) / 100;

    if (preview) {
        preview.innerHTML = aplicado === null
            ? `<i class="fas fa-calculator me-1"></i>Monto aplicado a la factura: ingrese el tipo de cambio`
            : `<i class="fas fa-calculator me-1"></i>Monto aplicado a la factura: <strong>${documento} ${formatNumber(aplicado, 2)}</strong>`;
        preview.style.display = 'block';
    }

    const saldo = saldoFacturaSeleccionada();
    if (aplicado !== null && saldo !== null && aplicado > saldo + 0.005) {
        montoInput.setCustomValidity(`El monto aplicado (${documento} ${formatNumber(aplicado, 2)}) excede el saldo pendiente (${documento} ${formatNumber(saldo, 2)})`);
    }
}

let tipoCambioPagoFetch = null;

/**
 * ADR-011: muestra el tipo de cambio solo cuando hay moneda extranjera involucrada
 * y lo precarga con la tasa de venta registrada para la fecha del pago (editable).
 */
function actualizarTipoCambioPago() {
    const monedaPago = document.getElementById('monedaPago');
    const grupo = document.getElementById('tipoCambioPagoGroup');
    const input = document.getElementById('tipoCambioPago');
    const ayuda = document.getElementById('tipoCambioPagoAyuda');
    const simbolo = document.getElementById('montoSimbolo');
    if (!monedaPago || !grupo || !input) return;

    const pago = monedaPago.value || 'CRC';
    const documento = monedaFacturaSeleccionada();
    if (simbolo) simbolo.textContent = pago === 'CRC' ? '₡' : pago;

    if (pago !== 'CRC' && documento !== 'CRC' && pago !== documento) {
        grupo.style.display = 'none';
        input.value = '';
        showToast('warning', `No se admite un pago en ${pago} sobre una factura en ${documento}: pague en ${documento} o en CRC`);
        actualizarMontoAplicado();
        return;
    }
    const extranjera = pago !== 'CRC' ? pago : documento;
    if (extranjera === 'CRC') {
        grupo.style.display = 'none';
        input.value = '';
        actualizarMontoAplicado();
        return;
    }

    grupo.style.display = 'block';
    const fechaPago = (document.getElementById('fechaPago')?.value || '').substring(0, 10);
    const fecha = fechaPago || new Date().toISOString().substring(0, 10);
    if (tipoCambioPagoFetch) tipoCambioPagoFetch.abort();
    tipoCambioPagoFetch = new AbortController();
    fetch(`/api/tipos-cambio/venta?moneda=${extranjera}&fecha=${fecha}`, { signal: tipoCambioPagoFetch.signal })
        .then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) }))
        .then(({ ok, data }) => {
            if (ok && data && data.tasaVenta) {
                input.value = data.tasaVenta;
                actualizarMontoAplicado();
                if (ayuda) ayuda.textContent = `Tasa de venta de ${extranjera} al ${fecha}; puede editarla`;
            } else {
                input.value = '';
                if (ayuda) ayuda.textContent = (data && data.message) || `No hay tipo de cambio de ${extranjera} para ${fecha}; ingréselo`;
            }
        })
        .catch(err => { if (err.name !== 'AbortError') { /* el usuario puede digitarlo */ } });
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
        metodoPagoSelect.addEventListener('change', actualizarCuentaBancariaDestino);
    }

    // ADR-011: moneda y tipo de cambio del pago
    const monedaPagoSelect = document.getElementById('monedaPago');
    if (monedaPagoSelect) {
        monedaPagoSelect.addEventListener('change', actualizarTipoCambioPago);
        // ADR-018: la cuenta bancaria de destino debe estar en la moneda del pago
        monedaPagoSelect.addEventListener('change', actualizarCuentaBancariaDestino);
    }
    const fechaPagoInput = document.getElementById('fechaPago');
    if (fechaPagoInput) {
        fechaPagoInput.addEventListener('change', actualizarTipoCambioPago);
    }
    document.getElementById('monto')?.addEventListener('input', actualizarMontoAplicado);
    document.getElementById('tipoCambioPago')?.addEventListener('input', actualizarMontoAplicado);
    
    // Inicializar toggles
    toggleReferencia();
    toggleFacturaField();
    actualizarCuentaBancariaDestino();
    
    // Manejo del envío del formulario
    pagoForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const formData = new FormData(this);
        const pagoData = {
            idCliente: parseInt(formData.get('idCliente')),
            idFactura: formData.get('idFactura') ? parseInt(formData.get('idFactura')) : null,
            tipoPago: formData.get('tipoPago'),
            monto: parseFloat(formData.get('monto')),
            moneda: formData.get('moneda') || null,
            tipoCambio: formData.get('tipoCambio') ? parseFloat(formData.get('tipoCambio')) : null,
            metodoPago: formData.get('metodoPago'),
            fechaPago: formData.get('fechaPago'),
            referenciaBancaria: formData.get('referenciaBancaria'),
            banco: formData.get('banco'),
            cuentaBancaria: formData.get('cuentaBancaria'),
            // ADR-018: cuenta bancaria propia de destino (solo se envía si el método la requiere)
            idCuentaBancariaDestino: (document.getElementById('idCuentaBancariaDestino')?.required
                && formData.get('idCuentaBancariaDestino'))
                ? parseInt(formData.get('idCuentaBancariaDestino')) : null,
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
        
        // ADR-018: con TRANSFERENCIA la cuenta bancaria de destino es obligatoria
        const cuentaDestino = document.getElementById('idCuentaBancariaDestino');
        if (cuentaDestino && cuentaDestino.required && !pagoData.idCuentaBancariaDestino) {
            showToast('error', 'Seleccione la cuenta bancaria de destino');
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
