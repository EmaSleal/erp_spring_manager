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
            // Datos de la Factura
            document.getElementById("modal-idFactura").innerText = data.idFactura;
            document.getElementById("modal-createDate").innerText = new Date(data.createDate).toLocaleString('es-MX');
            document.getElementById("modal-updateDate").innerText = new Date(data.updateDate).toLocaleString('es-MX');
            document.getElementById("modal-fechaEntrega").innerText = data.fechaEntrega;
            
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
            document.getElementById("modal-usuario").innerText = data.cliente.usuario.nombre;
            document.getElementById("modal-fechaRegistro").innerText = new Date(data.cliente.fechaRegistro).toLocaleString('es-MX');

            // Cargar Productos
            const productosTable = document.getElementById("modal-productos");
            productosTable.innerHTML = "";

            fetch(`/lineas-factura/detalle/${facturaId}`)
                .then(response => response.json())
                .then(lineas => {
                    let total = 0;
                    lineas.forEach(linea => {
                        const subtotal = parseFloat(linea.subtotal);
                        total += subtotal;
                        
                        const row = `
                            <tr>
                                <td>${linea.descripcion}</td>
                                <td class="text-center">${linea.cantidad}</td>
                                <td class="text-end">$${parseFloat(linea.precioUnitario).toFixed(2)}</td>
                                <td class="text-end">$${subtotal.toFixed(2)}</td>
                            </tr>
                        `;
                        productosTable.innerHTML += row;
                    });
                    
                    // Actualizar total
                    document.getElementById("modal-total").innerText = `$${total.toFixed(2)}`;
                });

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
// Funciones de Acciones
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
