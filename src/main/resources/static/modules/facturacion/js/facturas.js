// ========================================
// Variables Globales
// ========================================
let facturaModal;
let facturaModalElement;

// ========================================
// Inicialización
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar modal de Bootstrap
    facturaModalElement = document.getElementById('facturaModal');
    if (facturaModalElement) {
        facturaModal = new bootstrap.Modal(facturaModalElement);
    }
    
    // Event listeners para filtros
    const filterBtn = document.getElementById('filterBtn');
    if (filterBtn) {
        filterBtn.addEventListener('click', aplicarFiltros);
    }
    
    const clearFilterBtn = document.getElementById('clearFilterBtn');
    if (clearFilterBtn) {
        clearFilterBtn.addEventListener('click', limpiarFiltros);
    }
});

// ========================================
// Funciones de Modal
// ========================================
function openModal(button) {
    const facturaId = button.getAttribute("data-id");

    httpGet(`/facturas/detalle/${facturaId}`, {
        showLoading: false,
        onSuccess: (data) => {
            console.log('Datos de factura recibidos:', data);
            console.log('Líneas de factura:', data.lineas);
            
            // Datos de la Factura
            document.getElementById("modal-idFactura").innerText = data.idFactura;
            
            // ✅ NUEVO: Mostrar número de factura y serie
            document.getElementById("modal-numeroFactura").innerText = data.numeroFactura || 'N/A';
            document.getElementById("modal-serie").innerText = data.serie || 'N/A';

            // Moneda y tipo de cambio
            const monedaSimbolos = { CRC: '₡', USD: '$', EUR: '€' };
            const monedaCodigo = data.monedaFE || 'CRC';
            const monedaSimbolo = monedaSimbolos[monedaCodigo] || '₡';
            document.getElementById("modal-monedaFE").innerText = monedaCodigo;
            const tcRow = document.getElementById("modal-tipo-cambio-row");
            if (monedaCodigo !== 'CRC' && data.tipoCambio) {
                document.getElementById("modal-tipoCambio").innerText = `1 ${monedaCodigo} = ₡${parseFloat(data.tipoCambio).toFixed(2)}`;
                tcRow.style.display = '';
            } else {
                tcRow.style.display = 'none';
            }
            
            // Fechas con validación
            const createDate = data.createDate || data.fechaEmision || data.updateDate;
            document.getElementById("modal-createDate").innerText = createDate ? new Date(createDate).toLocaleString('es-MX') : 'N/A';
            document.getElementById("modal-updateDate").innerText = data.updateDate ? new Date(data.updateDate).toLocaleString('es-MX') : 'N/A';
            document.getElementById("modal-fechaEntrega").innerText = data.fechaEntrega || 'N/A';
            
            // ✅ NUEVO: Mostrar fecha de pago
            document.getElementById("modal-fechaPago").innerText = data.fechaPago || 'No especificada';
            
            // Estado con badge de Bootstrap
            const estadoBadge = document.getElementById("modal-entregado");
            if (data.entregado) {
                estadoBadge.innerHTML = '<i class="fas fa-check me-1"></i>Entregado';
                estadoBadge.className = 'badge bg-secondary';
            } else {
                estadoBadge.innerHTML = '<i class="fas fa-clock me-1"></i>Pendiente';
                estadoBadge.className = 'badge badge-outline-secondary';
            }

            // Datos del Cliente
            document.getElementById("modal-idCliente").innerText = data.cliente.idCliente;
            document.getElementById("modal-cliente").innerText = data.cliente.nombre;
            document.getElementById("modal-usuario").innerText = data.createBy || 'N/A';
            
            const fechaRegistro = data.cliente.fechaRegistro;
            document.getElementById("modal-fechaRegistro").innerText = fechaRegistro ? new Date(fechaRegistro).toLocaleString('es-MX') : 'N/A';

            // ✅ NUEVO: Mostrar información de Facturación Electrónica
            const feContainer = document.getElementById("modal-fe-container");
            const feBotonEnviar = document.getElementById("modal-fe-boton-enviar");
            const feEstado = document.getElementById("modal-fe-estado");
            
            // Estado FE del comprobante vigente (ADR-031): badge con el color e ícono de EstadoComprobante
            feContainer.style.display = 'block';
            // T-040: la acción del botón depende del comprobante vigente (emitir / reenviar / ninguno)
            const accionEnvio = window.AccionEnvioHacienda.accionEnvioHacienda(data.estadoComprobante);
            if (accionEnvio === 'reenviar' && !data.idComprobante) {
                console.warn('Comprobante vigente sin id: no se puede reenviar desde el modal');
            }
            // T-044 (P-099): «Reenviar» de un comprobante de otro ambiente que el activo se muestra deshabilitado con el
            // motivo fijo del servidor; «Enviar» (sin comprobante o RECHAZADO) emite uno nuevo en el ambiente activo
            const ambienteFe = window.AmbienteComprobante;
            const bloqueoAmbiente = accionEnvio === 'reenviar' && ambienteFe
                ? ambienteFe.bloqueo({ ambienteCoincide: data.ambienteComprobanteCoincide, motivoAmbiente: data.motivoAmbienteComprobante })
                : { bloqueado: false, motivo: null };
            const botonEnvio = document.getElementById('btn-enviar-hacienda');
            if (botonEnvio) {
                botonEnvio.disabled = bloqueoAmbiente.bloqueado;
                botonEnvio.title = bloqueoAmbiente.motivo || '';
                const texto = window.AccionEnvioHacienda.textoBotonEnvio(accionEnvio);
                if (texto) {
                    botonEnvio.innerHTML = `<i class="fas fa-cloud-upload me-1"></i>${texto}`;
                }
                botonEnvio.dataset.accion = accionEnvio || '';
                botonEnvio.dataset.idComprobante = data.idComprobante || '';
            }
            // El aviso «aún no ha sido enviada» solo aplica sin comprobante
            const avisoSinEnvio = feBotonEnviar.querySelector('p.text-muted');
            if (avisoSinEnvio) avisoSinEnvio.style.display = data.estadoComprobante ? 'none' : '';
            if (data.estadoComprobante) {
                const puedeBoton = accionEnvio === 'emitir' || (accionEnvio === 'reenviar' && data.idComprobante);
                feBotonEnviar.style.display = puedeBoton ? 'block' : 'none';
                feBotonEnviar.setAttribute('data-id', data.idFactura);
                feEstado.style.display = 'block';
                const color = escapeHtml(data.estadoComprobanteColor || 'secondary');
                feEstado.innerHTML = `
                    <p class="mb-1"><strong>Estado:</strong>
                        <span class="badge bg-${color}${color === 'warning' ? ' text-dark' : ''}" id="modal-fe-badge">
                            <i class="bi ${escapeHtml(data.estadoComprobanteIcono || '')} me-1"></i>${escapeHtml(data.estadoComprobanteDescripcion || data.estadoComprobante)}
                        </span>
                    </p>
                    ${data.ambienteComprobante && ambienteFe ? `<p class="mb-1"><strong>Ambiente:</strong>
                        <span class="badge ${escapeHtml(ambienteFe.etiqueta(data.ambienteComprobante).clase)}" id="modal-fe-ambiente">${escapeHtml(ambienteFe.etiqueta(data.ambienteComprobante).texto)}</span></p>` : ''}
                    ${data.ambienteComprobanteCoincide === false && data.motivoAmbienteComprobante ? `<div class="alert alert-danger py-2 mb-2 motivo-ambiente"><i class="fas fa-ban me-1"></i>${escapeHtml(data.motivoAmbienteComprobante)}</div>` : ''}
                    ${data.claveComprobante ? `<p class="mb-1"><strong>Clave:</strong> <code>${escapeHtml(data.claveComprobante)}</code></p>` : ''}
                    ${data.revisarEnAtv ? `<div class="alert alert-danger py-2 mb-2"><i class="fas fa-exclamation-circle me-1"></i>Hacienda informó "error" en varias consultas seguidas: revise el comprobante en ATV.</div>` : ''}
                    ${data.idComprobante ? `<button class="btn btn-sm btn-primary mt-1" onclick="verComprobante(${Number(data.idComprobante)})">
                        <i class="bi bi-box-arrow-up-right me-1"></i>Ver Comprobante
                    </button>` : ''}
                `;
            } else {
                // Sin comprobante electrónico: badge «Sin comprobante» y botón para enviarla
                feEstado.style.display = 'block';
                feEstado.innerHTML = `
                    <p class="mb-2"><strong>Estado:</strong>
                        <span class="badge bg-light text-dark border" id="modal-fe-badge">
                            <i class="bi bi-file-earmark me-1"></i>Sin comprobante
                        </span>
                    </p>`;
                feBotonEnviar.style.display = 'block';
                feBotonEnviar.setAttribute('data-id', data.idFactura);
            }

            // Cargar Productos desde el DTO (ya vienen incluidas las líneas)
            const productosTable = document.getElementById("modal-productos");
            productosTable.innerHTML = "";

            if (data.lineas && data.lineas.length > 0) {
                let total = 0;
                data.lineas.forEach(linea => {
                    const subtotal = parseFloat(linea.subtotal || 0);
                    total += subtotal;
                    
                    const row = `
                        <tr>
                            <td>${linea.descripcionProducto || linea.codigoProducto || 'N/A'}</td>
                            <td class="text-center">${linea.cantidad || 0}</td>
                            <td class="text-end">${monedaSimbolo}${parseFloat(linea.precioUnitario || 0).toFixed(2)}</td>
                            <td class="text-end">${monedaSimbolo}${subtotal.toFixed(2)}</td>
                        </tr>
                    `;
                    productosTable.innerHTML += row;
                });

                // Actualizar total
                document.getElementById("modal-total").innerText = `${monedaSimbolo}${total.toFixed(2)}`;
            } else {
                // Si no hay líneas, mostrar mensaje
                productosTable.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-muted">
                            <i class="fas fa-inbox me-2"></i>No hay productos en esta factura
                        </td>
                    </tr>
                `;
                document.getElementById("modal-total").innerText = `${monedaSimbolo}${parseFloat(data.total || 0).toFixed(2)}`;
            }
            
            // Mostrar información de pagos si existe
            const pagosSection = document.getElementById('pagos-section');
            const modalPagosInfo = document.getElementById('modal-pagos-info');
            
            if (data.pagos && data.pagos.length > 0) {
                if (pagosSection) pagosSection.style.display = 'block';
                if (modalPagosInfo) {
                    let pagosHTML = `
                        <div class="mb-2">
                            <strong>Total Pagado:</strong> <span>${monedaSimbolo}${parseFloat(data.totalPagado || 0).toFixed(2)}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Saldo Pendiente:</strong> <span class="${data.saldoPendiente > 0 ? 'text-danger' : 'text-success'}">${monedaSimbolo}${parseFloat(data.saldoPendiente || 0).toFixed(2)}</span>
                        </div>
                        <div class="table-responsive mt-3">
                            <table class="table table-sm table-bordered">
                                <thead class="table-light">
                                    <tr>
                                        <th>Número Pago</th>
                                        <th>Fecha</th>
                                        <th>Método</th>
                                        <th class="text-end">Monto</th>
                                        <th>Estado</th>
                                    </tr>
                                </thead>
                                <tbody>
                    `;
                    
                    data.pagos.forEach(pago => {
                        const estadoClass = pago.estado === 'CONCILIADO' ? 'bg-primary' : 
                                          pago.estado === 'CONFIRMADO' ? 'bg-success' : 
                                          pago.estado === 'PENDIENTE' ? 'bg-warning' : 'bg-secondary';
                        
                        pagosHTML += `
                            <tr>
                                <td>${pago.numeroPago}</td>
                                <td>${new Date(pago.fechaPago).toLocaleDateString('es-MX')}</td>
                                <td>${pago.metodoPagoDescripcion}</td>
                                <td class="text-end">₡${parseFloat(pago.monto).toFixed(2)}</td>
                                <td><span class="badge ${estadoClass}">${pago.estadoDescripcion}</span></td>
                            </tr>
                        `;
                    });
                    
                    pagosHTML += `
                                </tbody>
                            </table>
                        </div>
                    `;
                    
                    modalPagosInfo.innerHTML = pagosHTML;
                }
            } else {
                if (pagosSection) pagosSection.style.display = 'none';
            }

            // Botones de acción según rol del usuario
            const buttonsContainer = document.getElementById("modal-buttons");
            let buttonsHTML = '';
            
            // Botón Eliminar - Solo ADMIN y USER
            if (USER_ROLE === 'ADMIN' || USER_ROLE === 'USER') {
                buttonsHTML += `
                    <button class="btn btn-danger me-2" onclick="deleteFactura(${data.idFactura})">
                        <i class="fas fa-trash me-1"></i>Eliminar
                    </button>
                `;
            }
            
            // Botón Editar - ADMIN, USER y VENDEDOR
            if (USER_ROLE === 'ADMIN' || USER_ROLE === 'USER' || USER_ROLE === 'VENDEDOR') {
                buttonsHTML += `
                    <button class="btn btn-primary" onclick="editFactura(${data.idFactura})">
                        <i class="fas fa-edit me-1"></i>Editar
                    </button>
                `;
            }
            
            // Si es VISUALIZADOR, mostrar mensaje
            if (USER_ROLE === 'VISUALIZADOR') {
                buttonsHTML += `
                    <div class="alert alert-info mb-0">
                        <i class="bi bi-info-circle me-2"></i>
                        Solo tienes permisos de lectura
                    </div>
                `;
            }
            
            buttonsContainer.innerHTML = buttonsHTML;

            // T-039 (ADR-045): vínculos de sustitución (textContent, sin HTML con datos) y botón solo cuando procede
            mostrarVinculoSustitucion('modal-sustituye-row', 'modal-sustituye-link',
                typeof SustitucionFactura !== 'undefined' ? SustitucionFactura.textoSustituye(data.sustituye) : '',
                data.sustituye && data.sustituye.idFactura);
            const avisoSustituta = document.getElementById('modal-sustituta-inventario');
            if (avisoSustituta) avisoSustituta.style.display = data.sustituye && data.sustituye.idFactura ? '' : 'none';
            // T-046 (ADR-050): «Fecha contable» y «Período fiscal (IVA)» de la sustituta
            const atribucion = document.getElementById('modal-sustituta-atribucion');
            if (atribucion) {
                const esSustituta = !!(data.sustituye && data.sustituye.idFactura);
                atribucion.style.display = esSustituta ? '' : 'none';
                const fc = typeof SustitucionFactura !== 'undefined'
                    ? SustitucionFactura.textoFechaContable(data.fechaContable) : '-';
                document.getElementById('modal-fecha-contable').textContent = fc;
                document.getElementById('modal-periodo-fiscal').textContent = data.periodoFiscalAtribucion || '-';
            }
            mostrarVinculoSustitucion('modal-sustituida-por-row', 'modal-sustituida-por-link',
                typeof SustitucionFactura !== 'undefined' ? SustitucionFactura.textoSustituidaPor(data.sustituidaPor) : '',
                data.sustituidaPor && data.sustituidaPor.idFactura);
            if (typeof SustitucionFactura !== 'undefined' && SustitucionFactura.puedeEmitir(data)
                && (USER_ROLE === 'ADMIN' || USER_ROLE === 'USER' || USER_ROLE === 'VENDEDOR')) {
                const boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'btn btn-success me-2';
                boton.id = 'modal-boton-sustituta';
                const icono = document.createElement('i');
                icono.className = 'bi bi-files me-1';
                boton.appendChild(icono);
                boton.appendChild(document.createTextNode('Emitir factura sustituta'));
                boton.addEventListener('click', () => emitirFacturaSustituta(data.idFactura, boton));
                buttonsContainer.insertBefore(boton, buttonsContainer.firstChild);
            } else if (data.motivoSustitutaNoDisponible && (USER_ROLE === 'ADMIN' || USER_ROLE === 'USER' || USER_ROLE === 'VENDEDOR')) {
                // T-044 (P-099): la original es de otro ambiente que el activo; deshabilitado y con el motivo fijo
                const bloque = document.createElement('div');
                bloque.id = 'modal-sustituta-ambiente';
                bloque.className = 'd-inline-block me-2 mb-2 align-top';
                const botonDes = document.createElement('button');
                botonDes.type = 'button';
                botonDes.className = 'btn btn-success';
                botonDes.disabled = true;
                botonDes.title = data.motivoSustitutaNoDisponible;
                botonDes.textContent = 'Emitir factura sustituta';
                const motivo = document.createElement('div');
                motivo.className = 'small text-danger mt-1 motivo-ambiente';
                motivo.textContent = data.motivoSustitutaNoDisponible;
                bloque.appendChild(botonDes);
                bloque.appendChild(motivo);
                buttonsContainer.insertBefore(bloque, buttonsContainer.firstChild);
            }

            // T-042 (ADR-047 §2): «Emitir nota de crédito» según las banderas del servidor (sin roles en JS); con pagos
            // aplicados aparece deshabilitado y con el motivo visible
            if (typeof NotaCredito !== 'undefined' && typeof NotaCreditoModal !== 'undefined') {
                const estadoNc = NotaCredito.estadoBoton(data);
                if (estadoNc.visible) {
                    const bloque = document.createElement('div');
                    bloque.id = 'modal-nota-credito';
                    bloque.className = 'd-inline-block me-2 mb-2 align-top';
                    const botonNc = document.createElement('button');
                    botonNc.type = 'button';
                    botonNc.className = 'btn btn-outline-danger';
                    botonNc.id = 'modal-boton-nota-credito';
                    const iconoNc = document.createElement('i');
                    iconoNc.className = 'fas fa-file-circle-minus me-1';
                    botonNc.appendChild(iconoNc);
                    botonNc.appendChild(document.createTextNode('Emitir nota de crédito'));
                    botonNc.disabled = !estadoNc.habilitado;
                    bloque.appendChild(botonNc);
                    if (estadoNc.habilitado) {
                        botonNc.addEventListener('click', () => NotaCreditoModal.abrir(data));
                    } else {
                        const motivoNc = document.createElement('div');
                        motivoNc.id = 'modal-nota-credito-motivo';
                        motivoNc.className = 'form-text text-muted';
                        motivoNc.textContent = estadoNc.motivo;
                        bloque.appendChild(motivoNc);
                    }
                    buttonsContainer.insertBefore(bloque, buttonsContainer.firstChild);
                }
            }

            // Mostrar el modal con Bootstrap
            facturaModal.show();
        },
        onError: () => {
            showToast('error', 'No se pudo cargar el detalle de la factura.');
        }
    });
}

// ========================================
// Funciones de Facturación Electrónica
// ========================================

/**
 * Envía una factura a Hacienda de Costa Rica.
 * @param {number} facturaId - ID de la factura a enviar
 */
async function enviarAHacienda(button) {
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    // Mostrar modal de confirmación
    const confirmed = await Swal.fire({
        title: '¿Enviar a Hacienda?',
        html: `
            <div class="text-start">
                <p>Se generará y enviará el comprobante electrónico a Hacienda de Costa Rica.</p>
                <div class="alert alert-info mt-3">
                    <strong><i class="bi bi-info-circle me-2"></i>Información:</strong>
                    <ul class="mb-0 mt-2">
                        <li>Se generará el XML según especificación v4.4</li>
                        <li>Se firmará digitalmente con certificado</li>
                        <li>Se enviará a la API de Hacienda</li>
                        <li>Recibirá notificación del resultado</li>
                    </ul>
                </div>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-cloud-upload me-1"></i> Enviar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        showLoaderOnConfirm: true,
        preConfirm: async () => {
            try {
                const headers = {
                    'Content-Type': 'application/json'
                };
                if (csrfHeader && csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }
                const facturaId = button.getAttribute('data-id');
                console.log(facturaId);
                const response = await fetch(`/api/facturas/electronica/comprobantes/procesar/${facturaId}`, {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify({
                        facturaId: facturaId
                    })
                });
                
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || 'Error al enviar comprobante');
                }
                
                return await response.json();
                
            } catch (error) {
                Swal.showValidationMessage(`Error: ${error.message}`);
            }
        },
        allowOutsideClick: () => !Swal.isLoading()
    });
    
    if (confirmed.isConfirmed && confirmed.value) {
        const comprobante = confirmed.value.comprobante;

        // Mostrar resultado: mensaje fijo del servidor según cómo quedó el envío (T-037)
        showToast(confirmed.value.success ? 'success' : 'warning',
            confirmed.value.message || 'Comprobante procesado');

        // Redirigir a comprobantes
        setTimeout(() => {
            window.location.href = `/facturas/comprobantes?id=${comprobante.id}`;
        }, 3000);
    }
}

/**
 * Obtiene la clase de badge según el estado.
 * @param {string} estado - Estado del comprobante
 * @returns {string} Clase de Bootstrap
 */
function getEstadoBadgeClass(estado) {
    const classes = {
        'GENERADO': 'warning',
        'FIRMADO': 'info',
        'ENVIADO': 'primary',
        'PROCESANDO': 'primary',
        'ACEPTADO': 'success',
        'RECHAZADO': 'danger',
        'ERROR': 'danger',
        'ANULADO': 'secondary'
    };
    return classes[estado] || 'secondary';
}

/**
 * Ver detalle de comprobante electrónico.
 * @param {number} comprobanteId - ID del comprobante
 */
function verComprobante(comprobanteId) {
    window.location.href = `/facturas/comprobantes?id=${comprobanteId}`;
}
// ========================================
async function deleteFactura(idFactura) {
    const confirmed = await showConfirmDialog(
        '¿Eliminar factura?',
        'Esta acción no se puede deshacer. ¿Está seguro de eliminar esta factura?',
        'Sí, eliminar'
    );
    if (confirmed) {
        httpDelete(`/facturas/${idFactura}`, {
            successMessage: 'La factura ha sido eliminada correctamente.',
            reloadOnSuccess: true
        });
    }
}

/** Muestra u oculta una fila de vínculo de sustitución; el enlace abre el detalle de la otra factura. */
function mostrarVinculoSustitucion(filaId, enlaceId, texto, idOtra) {
    const fila = document.getElementById(filaId);
    const enlace = document.getElementById(enlaceId);
    if (!fila || !enlace) return;
    if (!texto || !idOtra) {
        fila.style.display = 'none';
        enlace.textContent = '';
        return;
    }
    enlace.textContent = texto;
    enlace.onclick = (ev) => {
        ev.preventDefault();
        openModal({ getAttribute: () => String(idOtra) });
    };
    fila.style.display = '';
}

/** POST /api/facturas/{id}/sustituta y abre la sustituta para editarla (T-039, ADR-045). */
async function emitirFacturaSustituta(idFactura, boton) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    if (boton) boton.disabled = true;
    try {
        const response = await fetch(`/api/facturas/${idFactura}/sustituta`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(SustitucionFactura.cuerpoPedido())
        });
        const cuerpo = await response.json().catch(() => null);
        if (!response.ok || !cuerpo || !cuerpo.data || !cuerpo.data.idFactura) {
            showToast('error', SustitucionFactura.mensajeDeError(cuerpo));
            if (boton) boton.disabled = false;
            return;
        }
        editFactura(cuerpo.data.idFactura);
    } catch (e) {
        showToast('error', SustitucionFactura.mensajeDeError(null));
        if (boton) boton.disabled = false;
    }
}

function editFactura(idFactura) {
    window.location.href = `/facturas/editar/${idFactura}`;
}

async function confirmarEliminacion(event) {
    event.preventDefault();
    const url = event.currentTarget.href;

    const confirmed = await showConfirmDialog(
        '¿Eliminar factura?',
        'Esta acción no se puede deshacer.',
        'Sí, eliminar'
    );
    if (confirmed) {
        window.location.href = url;
    }

    return false;
}

// Anula una factura (ADR-008): solicita el motivo y envía el formulario POST
function confirmarAnulacion(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const motivo = window.prompt('Motivo de la anulación (se revertirá el asiento contable de la venta):');
    if (motivo === null) {
        return false;
    }
    if (motivo.trim() === '') {
        window.alert('El motivo de anulación es obligatorio');
        return false;
    }
    form.querySelector('input[name="motivo"]').value = motivo.trim();
    form.submit();
    return false;
}

// ========================================
// Funciones de Filtros
// ========================================
function aplicarFiltros() {
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const statusFilter = document.getElementById("statusFilter").value;
    const estadoFEFilter = document.getElementById("estadoFEFilter").value;
    const isPaidFilter = document.getElementById("isPaidFilter").value;

    let url = "/facturas?";
    const params = [];

    if (startDate) {
        params.push(`startDate=${startDate}`);
    }
    if (endDate) {
        params.push(`endDate=${endDate}`);
    }
    if (statusFilter) {
        params.push(`status=${statusFilter}`);
    }
    if (estadoFEFilter) {
        params.push(`estadoFE=${estadoFEFilter}`);
    }
    if (isPaidFilter) {
        params.push(`isPaid=${isPaidFilter}`);
    }

    if (params.length > 0) {
        url += params.join('&');
    } else {
        url = "/facturas";
    }

    window.location.href = url;
}

function limpiarFiltros() {
    document.getElementById("startDate").value = '';
    document.getElementById("endDate").value = '';
    document.getElementById("statusFilter").value = '';
    document.getElementById("estadoFEFilter").value = '';
    document.getElementById("isPaidFilter").value = '';
    window.location.href = "/facturas";
}



// ========================================
// Función para Enviar Factura por Email
// ========================================
function enviarFacturaPorEmail(button) {
    const facturaId = button.getAttribute("data-id");
    const email = button.getAttribute("data-email");
    
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    Swal.fire({
        title: '📧 Enviar Factura',
        html: `¿Desea enviar la factura por email a:<br><strong>${email}</strong>?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#0dcaf0',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-paper-plane me-1"></i>Enviar',
        cancelButtonText: 'Cancelar',
        showLoaderOnConfirm: true,
        preConfirm: () => {
            // Crear headers con token CSRF
            const headers = {
                'Content-Type': 'application/json'
            };
            headers[csrfHeader] = csrfToken;
            
            return fetch(`/facturas/${facturaId}/enviar-email`, {
                method: 'POST',
                headers: headers
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.error || 'Error al enviar el email');
                    }).catch(() => {
                        throw new Error('Error al enviar el email');
                    });
                }
                return response.json();
            })
            .catch(error => {
                Swal.showValidationMessage(`Error: ${error.message}`);
            });
        },
        allowOutsideClick: () => !Swal.isLoading()
    }).then((result) => {
        if (result.isConfirmed) {
            showToast('success', `Email enviado exitosamente a: ${email}`);
        }
    });
}

// ========================================
// Enviar por WhatsApp
// ========================================
function enviarFacturaPorWhatsApp(button) {
    const facturaId = button.getAttribute("data-id");
    const telefono = button.getAttribute("data-telefono");
    const cliente = button.getAttribute("data-cliente");
    
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    Swal.fire({
        title: '📱 Enviar por WhatsApp',
        html: `¿Desea enviar la factura por WhatsApp a:<br><strong>${cliente}</strong><br><small class="text-muted">${telefono}</small>?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#25D366',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fab fa-whatsapp me-1"></i>Enviar',
        cancelButtonText: 'Cancelar',
        showLoaderOnConfirm: true,
        preConfirm: () => {
            // Crear headers con token CSRF
            const headers = {
                'Content-Type': 'application/json'
            };
            headers[csrfHeader] = csrfToken;
            
            return fetch(`/api/whatsapp/facturas/enviar`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    idFactura: parseInt(facturaId)
                })
            })
            .then(response => response.json())
            .then(data => {
                if (!data.success) {
                    throw new Error(data.message || 'Error al enviar por WhatsApp');
                }
                return data;
            })
            .catch(error => {
                Swal.showValidationMessage(`Error: ${error.message}`);
            });
        },
        allowOutsideClick: () => !Swal.isLoading()
    }).then((result) => {
        if (result.isConfirmed && result.value) {
            showToast('success', `WhatsApp enviado exitosamente a: ${cliente} (${telefono})`);
        }
    });
}

/**
 * Enviar factura a Hacienda desde el modal de detalle
 */
function enviarFacturaAHacienda() {
    const btnEnviar = document.getElementById('btn-enviar-hacienda');
    const facturaId = document.getElementById('modal-idFactura').innerText;
    // T-040: «reenviar» usa el comprobante vigente (NO_TRANSMITIDO / ERROR); «emitir» sigue por /procesar/{facturaId}
    const reenviar = btnEnviar.dataset.accion === 'reenviar';
    const idComprobante = btnEnviar.dataset.idComprobante;
    const textoBoton = reenviar ? 'Reenviar a Hacienda' : 'Enviar a Hacienda';
    
    if (!facturaId || (reenviar && !idComprobante)) {
        showToast('error', 'No se pudo obtener el ID de la factura');
        return;
    }
    
    // Confirmación
    Swal.fire({
        title: reenviar ? '¿Reenviar a Hacienda?' : '¿Enviar a Hacienda?',
        html: reenviar ? `
            <div class="text-start">
                <p>Se reenviará a Hacienda de Costa Rica el comprobante que no pudo transmitirse.</p>
            </div>
        ` : `
            <div class="text-start">
                <p>Se generará y enviará el comprobante electrónico a Hacienda de Costa Rica.</p>
                <div class="alert alert-info mt-3">
                    <strong><i class="bi bi-info-circle me-2"></i>Información:</strong>
                    <ul class="mb-0 mt-2">
                        <li>Se generará el XML según especificación v4.4</li>
                        <li>Se firmará digitalmente</li>
                        <li>Se enviará a la API de Hacienda</li>
                        <li>El proceso puede tardar unos segundos</li>
                    </ul>
                </div>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: reenviar ? '<i class="bi bi-cloud-upload me-1"></i> Sí, reenviar' : '<i class="bi bi-cloud-upload me-1"></i> Sí, enviar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d'
    }).then(async (result) => {
        if (result.isConfirmed) {
            // Mostrar loading
            btnEnviar.disabled = true;
            btnEnviar.innerHTML = '<i class="spinner-border spinner-border-sm me-1"></i>Enviando...';
            
            // Obtener CSRF token
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            const headers = {
                'Content-Type': 'application/json'
            };
            
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            
            try {
                const url = reenviar
                    ? `/api/facturas/electronica/comprobantes/${idComprobante}/reenviar`
                    : `/api/facturas/electronica/comprobantes/procesar/${facturaId}`;
                const response = await fetch(url, {
                    method: 'POST',
                    headers: headers
                });
                
                const data = await response.json();
                
                if (data.success || response.ok) {
                    // Recibido (success) o no transmitido / incierto (aviso), con el mensaje fijo del servidor (T-037)
                    showToast(data.success ? 'success' : 'warning',
                        data.message || 'El comprobante ha sido enviado exitosamente a Hacienda');

                    // Cerrar modal y recargar
                    const modal = bootstrap.Modal.getInstance(document.getElementById('facturaModal'));
                    modal.hide();
                    window.location.reload();
                } else {
                    // Error
                    showToast('error', data.message || 'No se pudo enviar el comprobante a Hacienda');

                    btnEnviar.disabled = false;
                    btnEnviar.innerHTML = `<i class="fas fa-cloud-upload me-1"></i>${textoBoton}`;
                }
            } catch (error) {
                console.error('Error:', error);

                showToast('error', 'No se pudo conectar con el servidor. Por favor, intente nuevamente.');

                btnEnviar.disabled = false;
                btnEnviar.innerHTML = `<i class="fas fa-cloud-upload me-1"></i>${textoBoton}`;
            }
        }
    });
}

/**
 * Obtener clase de badge según estado del comprobante
 */
function getEstadoBadgeClass(estado) {
    const clases = {
        'ACEPTADO': 'success',
        'RECHAZADO': 'danger',
        'PROCESANDO': 'warning',
        'ERROR': 'danger',
        'PENDIENTE': 'secondary'
    };
    return clases[estado] || 'secondary';
}
