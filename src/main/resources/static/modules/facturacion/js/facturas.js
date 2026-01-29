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

    fetch(`/facturas/detalle/${facturaId}`)
        .then(response => response.json())
        .then(data => {
            console.log('Datos de factura recibidos:', data);
            console.log('Líneas de factura:', data.lineas);
            
            // Datos de la Factura
            document.getElementById("modal-idFactura").innerText = data.idFactura;
            
            // ✅ NUEVO: Mostrar número de factura y serie
            document.getElementById("modal-numeroFactura").innerText = data.numeroFactura || 'N/A';
            document.getElementById("modal-serie").innerText = data.serie || 'N/A';
            
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
                estadoBadge.className = 'badge bg-success';
            } else {
                estadoBadge.innerHTML = '<i class="fas fa-clock me-1"></i>Pendiente';
                estadoBadge.className = 'badge bg-warning text-dark';
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
            
            if (data.comprobanteElectronico) {
                // Tiene comprobante electrónico
                feContainer.style.display = 'block';
                feBotonEnviar.style.display = 'none';
                feEstado.style.display = 'block';
                
                const comprobante = data.comprobanteElectronico;
                feEstado.innerHTML = `
                    <div class="alert alert-info">
                        <h6><i class="bi bi-file-earmark-check me-2"></i>Comprobante Electrónico</h6>
                        <p class="mb-1"><strong>Clave:</strong> <code>${comprobante.claveNumerica}</code></p>
                        <p class="mb-1"><strong>Estado:</strong> <span class="badge bg-${getEstadoBadgeClass(comprobante.estado)}">${comprobante.estado}</span></p>
                        ${comprobante.mensajeRespuesta ? `<p class="mb-0"><strong>Mensaje:</strong> ${comprobante.mensajeRespuesta}</p>` : ''}
                        <button class="btn btn-sm btn-primary mt-2" onclick="verComprobante(${comprobante.id})">
                            <i class="bi bi-box-arrow-up-right me-1"></i>Ver Comprobante
                        </button>
                    </div>
                `;
            } else {
                // NO tiene comprobante electrónico
                feContainer.style.display = 'block';
                feBotonEnviar.style.display = 'block';
                feEstado.style.display = 'none';
                feBotonEnviar.setAttribute('data-factura-id', data.idFactura);
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
                            <td class="text-end">$${parseFloat(linea.precioUnitario || 0).toFixed(2)}</td>
                            <td class="text-end">$${subtotal.toFixed(2)}</td>
                        </tr>
                    `;
                    productosTable.innerHTML += row;
                });
                
                // Actualizar total
                document.getElementById("modal-total").innerText = `$${total.toFixed(2)}`;
            } else {
                // Si no hay líneas, mostrar mensaje
                productosTable.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-muted">
                            <i class="fas fa-inbox me-2"></i>No hay productos en esta factura
                        </td>
                    </tr>
                `;
                document.getElementById("modal-total").innerText = `$${parseFloat(data.total || 0).toFixed(2)}`;
            }
            
            // Mostrar información de pagos si existe
            const pagosSection = document.getElementById('pagos-section');
            const modalPagosInfo = document.getElementById('modal-pagos-info');
            
            if (data.pagos && data.pagos.length > 0) {
                if (pagosSection) pagosSection.style.display = 'block';
                if (modalPagosInfo) {
                    let pagosHTML = `
                        <div class="mb-2">
                            <strong>Total Pagado:</strong> <span class="text-success">₡${parseFloat(data.totalPagado || 0).toFixed(2)}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Saldo Pendiente:</strong> <span class="${data.saldoPendiente > 0 ? 'text-danger' : 'text-success'}">₡${parseFloat(data.saldoPendiente || 0).toFixed(2)}</span>
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

            // Mostrar el modal con Bootstrap
            facturaModal.show();
        })
        .catch(error => {
            console.error('Error al cargar la factura:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo cargar el detalle de la factura.',
                confirmButtonText: 'Aceptar'
            });
        });
}

// ========================================
// Funciones de Facturación Electrónica
// ========================================

/**
 * Envía una factura a Hacienda de Costa Rica.
 * @param {number} facturaId - ID de la factura a enviar
 */
async function enviarAHacienda(facturaId) {
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
                
                const response = await fetch(`/api/facturas/electronica/comprobantes`, {
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
        const comprobante = confirmed.value;
        
        // Mostrar resultado
        await Swal.fire({
            title: '¡Enviado!',
            html: `
                <div class="alert alert-success">
                    <h6><i class="bi bi-check-circle me-2"></i>Comprobante enviado exitosamente</h6>
                    <p class="mb-1"><strong>Clave:</strong> <code>${comprobante.claveNumerica}</code></p>
                    <p class="mb-1"><strong>Estado:</strong> <span class="badge bg-${getEstadoBadgeClass(comprobante.estado)}">${comprobante.estado}</span></p>
                </div>
                <p class="mt-3">Será redirigido a la vista de comprobantes electrónicos...</p>
            `,
            icon: 'success',
            timer: 3000,
            timerProgressBar: true,
            showConfirmButton: false
        });
        
        // Redirigir a comprobantes
        window.location.href = `/facturas/comprobantes?id=${comprobante.id}`;
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
function deleteFactura(idFactura) {
    Swal.fire({
        title: '¿Eliminar factura?',
        text: "Esta acción no se puede deshacer. ¿Está seguro de eliminar esta factura?",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/facturas/${idFactura}`, {
                method: "DELETE"
            })
            .then(response => {
                if (response.ok) {
                    Swal.fire({
                        icon: 'success',
                        title: '¡Eliminada!',
                        text: 'La factura ha sido eliminada correctamente.',
                        timer: 2000,
                        showConfirmButton: false
                    }).then(() => {
                        location.reload();
                    });
                } else {
                    throw new Error('Error al eliminar');
                }
            })
            .catch(error => {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'No se pudo eliminar la factura.',
                    confirmButtonText: 'Aceptar'
                });
            });
        }
    });
}

function editFactura(idFactura) {
    window.location.href = `/facturas/editar/${idFactura}`;
}

function confirmarEliminacion(event) {
    event.preventDefault();
    const url = event.target.href;
    
    Swal.fire({
        title: '¿Eliminar factura?',
        text: "Esta acción no se puede deshacer.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = url;
        }
    });
    
    return false;
}

// ========================================
// Funciones de Filtros
// ========================================
function aplicarFiltros() {
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const statusFilter = document.getElementById("statusFilter").value;

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
            Swal.fire({
                icon: 'success',
                title: '✅ Email Enviado',
                html: `La factura ha sido enviada exitosamente a:<br><strong>${email}</strong>`,
                timer: 3000,
                showConfirmButton: false
            });
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
            Swal.fire({
                icon: 'success',
                title: '✅ WhatsApp Enviado',
                html: `La factura ha sido enviada exitosamente a:<br><strong>${cliente}</strong><br><small class="text-muted">${telefono}</small><br><br><span class="badge bg-${result.value.estado === 'PENDIENTE' ? 'warning' : 'success'}">${result.value.estado}</span>`,
                timer: 4000,
                showConfirmButton: false
            });
        }
    });
}
